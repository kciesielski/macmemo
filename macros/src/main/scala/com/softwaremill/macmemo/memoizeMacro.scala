package com.softwaremill.macmemo

import scala.concurrent.duration.FiniteDuration
import scala.reflect.macros._

object memoizeMacro {
  private val debug = new Debug()

  def impl(c: blackbox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    case class MacroArgs(maxSize: Long, expireAfter: FiniteDuration, concurrencyLevel: Option[Int] = None)

    case class MemoIdentifier(methodName: TermName, generatedMemoValName: TermName)

    def reportInvalidAnnotationTarget() {
      c.error(c.enclosingPosition, "This annotation can only be used on methods")
    }

    def prepareInjectedBody(cachedMethodId: MemoIdentifier, valDefs: List[List[ValDef]], bodyTree: Tree, returnTypeTree: Tree): c.type#Tree = {
      val names = valDefs.flatten.map(_.name)
      q"""
      def callRealBody() = { $bodyTree }
      if (System.getProperty("macmemo.disable") != null) {
        callRealBody()
      }
      else {
        ${cachedMethodId.generatedMemoValName}.get($names, {
          List(
            callRealBody()
          )
        }).head.asInstanceOf[$returnTypeTree]
      }"""
    }

    def createMemoVal(cachedMethodId: MemoIdentifier, returnTypeTree: Tree, macroArgs: MacroArgs): c.type#Tree = {

      val enclosure = c.enclosingClass

      def buildCacheBucketId: Tree = {
        val enclosingClassSymbol = enclosure.symbol
        val enclosureFullName = enclosingClassSymbol.fullName + (if (enclosingClassSymbol.isModule) "$." else ".")
        Literal(Constant(
           enclosureFullName + cachedMethodId.methodName.toString))
      }

      def buildParams: Tree = {
        val maxSize = macroArgs.maxSize
        val ttl = macroArgs.expireAfter
        val concurrencyLevelOpt = macroArgs.concurrencyLevel
        q"""com.softwaremill.macmemo.MemoizeParams($maxSize, ${ttl.toMillis}, $concurrencyLevelOpt)"""
      }

      q"""lazy val ${cachedMethodId.generatedMemoValName}: com.softwaremill.macmemo.Cache[List[Any]] =
         com.softwaremill.macmemo.BuilderResolver.resolve($buildCacheBucketId).build($buildCacheBucketId, $buildParams)"""

    }

    def injectCacheUsage(cachedMethodId: MemoIdentifier, function: DefDef) = {
      val DefDef(mods, name, tparams, valDefs, returnTypeTree, bodyTree) = function
      val injectedBody = prepareInjectedBody(cachedMethodId, valDefs, bodyTree, returnTypeTree)
      DefDef(mods, name, tparams, valDefs, returnTypeTree, injectedBody)
    }

    def extractMacroArgs(application: Tree) = {
      debug(s"RAW application = ${reflect.runtime.universe.showRaw(application)}")
      val argsTree = application.children.head.children.head.children
      val maxSize = extractMaxSize(argsTree(1))
      val ttl = extractTtl(argsTree(2))
      val concurrencyLevelOpt = argsTree match {
        case List(_, _, _, concurrencyLevelTree) => extractConcurrencyLevel(concurrencyLevelTree)
        case _ => None
      }
      val args = MacroArgs(maxSize, ttl, concurrencyLevelOpt)
      debug(s"Macro args: $args")
      args
    }

    def extractMaxSize(tree: Tree) = {
      tree match {
        case q"maxSize=$x" => evalLongExpr(x)
        case _ => evalLongExpr(tree)
      }
    }

    def evalLongExpr(tree: Tree) = {
      val length: Any = c.eval(c.Expr(tree))
      length match {
        case intLength: Int => intLength.toLong
        case longLength: Long => longLength
      }
    }

    def extractTtl(tree: Tree) = {
      tree match {
        case q"expiresAfter=$x" => evalFiniteDurationExpr(x)
        case _ => evalFiniteDurationExpr(tree)
      }
    }

    def evalFiniteDurationExpr(tree: Tree) = {
      val newTree = q"import scala.concurrent.duration._; $tree"
      val dur: FiniteDuration = c.eval(c.Expr(newTree))
      dur

    }

    def extractConcurrencyLevel(tree: Tree) = {
      tree match {
        case q"concurrencyLevel=$x" => evalOptionInt(x)
        case _ => evalOptionInt(tree)
      }
    }

    def evalOptionInt(tree: Tree) = {
      val value: Option[Int] = c.eval(c.Expr(tree))
      value
    }
    val inputs = annottees.map(_.tree).toList
    val (_, expandees) = inputs match {
      case (functionDefinition: DefDef) :: rest =>
        debug(s"Found annotated function [${functionDefinition.name}]")
        val DefDef(_, name: TermName, _, _, returnTypeTree, _) = functionDefinition
        val cachedMethodIdentifier = MemoIdentifier(name, TermName(c.freshName(s"memo_${name}_")))
        val macroArgs = extractMacroArgs(c.macroApplication)
        val memoVal = createMemoVal(cachedMethodIdentifier, returnTypeTree, macroArgs)
        val newFunctionDef = injectCacheUsage(cachedMethodIdentifier, functionDefinition)
        (functionDefinition, (newFunctionDef :: rest) :+ memoVal)
      case _ => reportInvalidAnnotationTarget(); (EmptyTree, inputs)
    }

    debug(s"final method = ${show(expandees)}")

    c.Expr[Any](Block(expandees, Literal(Constant(()))))
  }

}

