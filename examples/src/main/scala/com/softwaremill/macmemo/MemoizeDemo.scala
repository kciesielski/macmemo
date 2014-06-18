package com.softwaremill.macmemo

import scala.util.Random


class Foo2 {

  @memoize
  def someMethod(param: Int, param2: String, crazy: Option[List[_ <: Int]] = None): Int = {
    param * Random.nextInt(10000) + param2.toInt
  }

}


object MemoizeDemo extends App {

  val obj = new Foo2()
  println(obj.someMethod(5, "3"))
  println(obj.someMethod(5, "3"))
  println(obj.someMethod(5, "3"))
  println(obj.someMethod(5, "3"))
  Thread.sleep(6000)
  println(obj.someMethod(5, "3"))


}

