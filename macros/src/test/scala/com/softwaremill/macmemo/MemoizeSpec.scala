package com.softwaremill.macmemo

import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.util.Random

class ClassWithMemos {

  @memoize(2, 5 days)
  def methodShortBufferLongTime(param: Int) = param * Random.nextInt(100000)

  @memoize(maxSize = 2 + 1, expiresAfter = 5 days)
  def methodShortBufferLongTimeNamed(param: Int) = param * Random.nextInt(10000)

  @memoize(20000000000l, FiniteDuration(2, "seconds"))
  def methodLongBufferShortTime(param: Int) = param * Random.nextInt(10000)

  @memoize(maxSize = 2000000000l * 10, expiresAfter = FiniteDuration(2, "seconds"), concurrencyLevel = Some(5))
  def methodLongBufferShortTimeNamed(param: Int) = param * Random.nextInt(10000)

}

trait TraitWithMemo {

  @memoize(2, 5 days)
  def methodShortBufferLongTime(param: Int) = param * Random.nextInt(100000)

}

object ObjectWithMemos {

  @memoize(2, 4 days, Some(5))
  def methodShortBufferLongTime(param: Int) = param * Random.nextInt(100000)

  @memoize(2, expiresAfter = 52 days)
  def parametrized[T <: List[String]](arg: T) = Random.nextInt(32428)

  def publicDef(param: Int, param2: Int) = privateDef(param, param2)

  @memoize(maxSize = 2, 15 days)
  private def privateDef(param: Int, param2: Int) = { param * param2 * Random.nextInt(5438987)}

  @memoize(maxSize = 3, expiresAfter = 1 day)
  def recursive(param: Int): Int = {
    if (param == 0)
      Random.nextInt(20000)
    else param + recursive(param - 1)
  }
}



class ClassWithTraitWithMemo extends TraitWithMemo

class MemoizeSpec extends FlatSpec with Matchers {

  behavior of "@memoize macro"

  it should "memoize for defined number of calls (unnamed args)" in {
    // given
    val obj = new ClassWithMemos()

    // when
    val firstResult = obj.methodShortBufferLongTime(15)
    val secondResult = obj.methodShortBufferLongTime(15)
    obj.methodShortBufferLongTime(20)
    obj.methodShortBufferLongTime(21)
    val resultAfterExceedingCapacity = obj.methodShortBufferLongTime(15)

    // then
    firstResult should equal(secondResult)
    secondResult should not equal resultAfterExceedingCapacity
  }

  it should "memoize for defined number of calls (named args)" in {
    // given
    val obj = new ClassWithMemos()

    // when
    val firstResult = obj.methodShortBufferLongTimeNamed(34)
    val secondResult = obj.methodShortBufferLongTimeNamed(34)
    obj.methodShortBufferLongTimeNamed(20)
    obj.methodShortBufferLongTimeNamed(21)
    obj.methodShortBufferLongTimeNamed(23)
    val resultAfterExceedingCapacity = obj.methodShortBufferLongTimeNamed(34)

    // then
    firstResult should equal(secondResult)
    secondResult should not equal resultAfterExceedingCapacity
  }

  it should "memoize for given time (unnamed args)" in {
    // given
    val obj = new ClassWithMemos()

    // when
    val firstResult = obj.methodLongBufferShortTime(166)
    for (i <- 1 to 1000) obj.methodLongBufferShortTime(i)
    val resultAfterManyOtherCalls = obj.methodLongBufferShortTime(166)
    Thread.sleep(2000)
    val resultAfterExpiration = obj.methodLongBufferShortTime(166)

    // then
    firstResult should equal(resultAfterManyOtherCalls)
    resultAfterManyOtherCalls should not equal resultAfterExpiration
  }

  it should "memoize for given time (named args)" in {
    // given
    val obj = new ClassWithMemos()

    // when
    val firstResult = obj.methodLongBufferShortTimeNamed(675)
    for (i <- 1 to 1000) obj.methodLongBufferShortTimeNamed(i)
    val resultAfterManyOtherCalls = obj.methodLongBufferShortTimeNamed(675)
    Thread.sleep(2000)
    val resultAfterExpiration = obj.methodLongBufferShortTimeNamed(675)

    // then
    firstResult should equal(resultAfterManyOtherCalls)
    resultAfterManyOtherCalls should not equal resultAfterExpiration
  }

  it should "memoize in traits" in {
    // given
    val obj = new ClassWithTraitWithMemo()

    // when
    val firstResult = obj.methodShortBufferLongTime(7321)
    val secondResult = obj.methodShortBufferLongTime(7321)
    obj.methodShortBufferLongTime(667)
    obj.methodShortBufferLongTime(776)
    val resultAfterExceedingCapacity = obj.methodShortBufferLongTime(7321)

    // then
    firstResult should equal(secondResult)
    secondResult should not equal resultAfterExceedingCapacity
  }

  it should "memoize in objects" in {
    // given
    val obj = ObjectWithMemos

    // when
    val firstResult = obj.methodShortBufferLongTime(16235)
    val secondResult = obj.methodShortBufferLongTime(16235)
    obj.methodShortBufferLongTime(445)
    obj.methodShortBufferLongTime(81)
    val resultAfterExceedingCapacity = obj.methodShortBufferLongTime(16235)

    // then
    firstResult should equal(secondResult)
    secondResult should not equal resultAfterExceedingCapacity
  }

  it should "memoize functions with parametrized types" in {
    // given
    val obj = ObjectWithMemos

    // when
    val firstResult = obj.parametrized(List("a"))
    val secondResult = obj.parametrized(List("a"))
    obj.parametrized(List("a", "c"))
    obj.parametrized(List("b", "c"))
    val resultAfterExceedingCapacity = obj.parametrized(List("a"))

    // then
    firstResult should equal(secondResult)
    secondResult should not equal resultAfterExceedingCapacity
  }

  it should "memoize recursive functions" in {
    // given
    val obj = ObjectWithMemos

    // when
    val firstResult = obj.recursive(14)
    val secondResult = obj.recursive(14)
    obj.recursive(8)
    val resultAfterExceedingCapacity = obj.recursive(14)

    // then
    firstResult should equal(secondResult)
    secondResult should not equal resultAfterExceedingCapacity
  }

}
