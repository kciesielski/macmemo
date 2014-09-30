package com.softwaremill.macmemo

import scala.language.experimental.macros

object BuilderResolver {

  def resolve(methodFullName: String): MemoCacheBuilder = macro builderResolverMacro_impl

  def builderResolverMacro_impl(c: scala.reflect.macros.whitebox.Context)(methodFullName: c.Expr[String]): c.Expr[MemoCacheBuilder] = {
    import c.universe._

    def bringDefaultBuilder: Tree = {
      val Literal(Constant(mfn: String)) = methodFullName.tree
      val msg = s"Cannot find custom memo builder for '$mfn' - default builder will be used"
      c.info(c.enclosingPosition, msg, false)
      reify {
        MemoCacheBuilder.guavaMemoCacheBuilder
      }.tree
    }

    val builderTree = c.inferImplicitValue(typeOf[MemoCacheBuilder]) match {
      case EmptyTree => bringDefaultBuilder
      case foundBuilderTree => foundBuilderTree
    }

    c.Expr[MemoCacheBuilder](Block(List(), builderTree))
  }

}
