package com.softwaremill.macmemo

/**
 * Taken from MacWire https://github.com/adamw/macwire
 */
private[macmemo] class Debug {
  var ident = 0

  def apply(msg: => String) {
    if (enabled) {
      val prefix = "   " * ident
      println(s"$prefix[debug] $msg")
    }
  }

  def withBlock[T](msg: => String)(block: => T): T = {
    apply(msg)
    beginBlock()
    try {
      block
    } finally {
      endBlock()
    }
  }

  def beginBlock() {
    ident += 1
  }

  def endBlock() {
    ident -= 1
  }

  private val enabled = System.getProperty("macmemo.debug") != null
}