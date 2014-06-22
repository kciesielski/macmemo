package com.softwaremill.macmemo

import scala.annotation.StaticAnnotation
import scala.concurrent.duration.FiniteDuration
import scala.language.experimental.macros

class memoize(val maxSize: Long, expiresAfter: FiniteDuration, val concurrencyLevel: Option[Int] = None)
  extends StaticAnnotation {

  def macroTransform(annottees: Any*): Any = macro memoizeMacro.impl
}
