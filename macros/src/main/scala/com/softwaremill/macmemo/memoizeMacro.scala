package com.softwaremill.macmemo

import scala.reflect.macros._

object memoizeMacro {
  private val debug = new Debug()

  var uniqueNameCounter: Int = 0

  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    def reportInvalidAnnotationTarget() {
      c.error(c.enclosingPosition, "This annotation can only be used on methods")
    }

    def prepareInjectedBody(name: TermName, valDefs: List[List[ValDef]], bodyTree: Tree, returnTypeTree: Tree) = {

      val names = valDefs.flatten.map(_.name)
      val objName = TermName(s"Memo_${name}_$uniqueNameCounter")
      q"""
        import java.util.concurrent.Callable

          $objName.memo.cache.get($names,
      new Callable[List[$returnTypeTree]] {

        override def call(): List[$returnTypeTree] = {
        List(
            $bodyTree
        )
        }
      }).head"""
    }

    def createNewObj(name: TermName, returnTypeTree: Tree) = {
      val objName = TermName(s"Memo_${name}_$uniqueNameCounter")
      // TODO parameterize stuff provided a 4, 1000 and 5s :)
      q"""
           object $objName {
              import com.softwaremill.macmemo.DefMemo
              import scala.concurrent.duration._

                lazy val memo = {
                  new DefMemo[List[Any], List[$returnTypeTree]](4, 1000, 5 seconds)
                }
           }
      """
    }

    def injectCacheUsage(function: DefDef) = {
      val DefDef(mods, name, tparams, valDefs, returnTypeTree, bodyTree) = function
      val injectedBody = prepareInjectedBody(name, valDefs, bodyTree, returnTypeTree)
      DefDef(mods, name, tparams, valDefs, returnTypeTree, injectedBody)
    }

    val inputs = annottees.map(_.tree).toList
    val (_, expandees) = inputs match {
      case (functionDefinition: DefDef) :: rest =>
        debug(s"Found annotated function [${functionDefinition.name}]")
        val DefDef(mods, name, tparams, valDefs, returnTypeTree, bodyTree) = functionDefinition
        val newObj = createNewObj(name, returnTypeTree)
        val newFunctionDef = injectCacheUsage(functionDefinition)
        (functionDefinition, (newFunctionDef :: rest) :+ newObj)
      case _ => reportInvalidAnnotationTarget(); (EmptyTree, inputs)
    }
    // TODO Check: Is this thread safe? Or maybe the compiler can run macros in parallel?
    uniqueNameCounter = uniqueNameCounter + 1
    c.Expr[Any](Block(expandees, Literal(Constant(()))))

  }

 }
