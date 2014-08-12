package com.softwaremill.macmemo

import scala.concurrent.duration.FiniteDuration
import scala.reflect.macros._

object memoizeMacro {
  private val debug = new Debug()

  def impl(c: blackbox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    case class MacroArgs(maxSize: Long, expireAfter: FiniteDuration, concurrencyLevel: Option[Int] = None)

    def reportInvalidAnnotationTarget() {
      c.error(c.enclosingPosition, "This annotation can only be used on methods")
    }

    def prepareInjectedBody(newlyGeneratedObjName: TermName, valDefs: List[List[ValDef]], bodyTree: Tree, returnTypeTree: Tree) = {

      val names = valDefs.flatten.map(_.name)
      q"""
        import java.util.concurrent.Callable

          $newlyGeneratedObjName.memo.cache.get($names,
      new Callable[List[$returnTypeTree]] {

        override def call(): List[$returnTypeTree] = {
        List(
            $bodyTree
        )
        }
      }).head"""
    }

    def createNewObj(newlyGeneratedObjName: TermName, returnTypeTree: Tree, macroArgs: MacroArgs) = {
      val maxSize = macroArgs.maxSize
      val ttl = macroArgs.expireAfter
      val concurrencyLevelOpt = macroArgs.concurrencyLevel
      q"""
           object $newlyGeneratedObjName {
              import com.softwaremill.macmemo.DefMemo

                lazy val memo = {
                  new DefMemo[List[Any], List[$returnTypeTree]]($maxSize, ${ttl.toMillis}, $concurrencyLevelOpt)
                }
           }
      """
    }

    def injectCacheUsage(newlyGeneratedObjName: TermName, function: DefDef) = {
      val DefDef(mods, name, tparams, valDefs, returnTypeTree, bodyTree) = function
      val injectedBody = prepareInjectedBody(newlyGeneratedObjName, valDefs, bodyTree, returnTypeTree)
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
    if (System.getProperty("macmemo.disable") != null) {
      annottees.head
    }
    else {
      val inputs = annottees.map(_.tree).toList
      val (_, expandees) = inputs match {
        case (functionDefinition: DefDef) :: rest =>
          debug(s"Found annotated function [${functionDefinition.name}]")
          val DefDef(mods, name, tparams, valDefs, returnTypeTree, bodyTree) = functionDefinition
          val newlyGeneratedObjName = c.freshName(s"Macro_${name}_")
          val macroArgs = extractMacroArgs(c.macroApplication)
          val newObj = createNewObj(newlyGeneratedObjName, returnTypeTree, macroArgs)
          debug("annotations: " + functionDefinition.symbol.annotations)
          val newFunctionDef = injectCacheUsage(newlyGeneratedObjName, functionDefinition)
          (functionDefinition, (newFunctionDef :: rest) :+ newObj)
        case _ => reportInvalidAnnotationTarget(); (EmptyTree, inputs)
      }

      c.Expr[Any](Block(expandees, Literal(Constant(()))))
    }
  }

}

