package com.softwaremill.macmemo

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros

class memoize extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro memoizeMacro.impl
}
