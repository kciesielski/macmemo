package com.softwaremill.macmemo.examples

import org.scalatest.{Matchers, FlatSpec}

import scala.concurrent.duration._
import scala.util.Random
import com.softwaremill.macmemo.TestMemoCacheBuilder
import com.softwaremill.macmemo.memoize

import scala.language.reflectiveCalls

package object packageScopeProvider {
  implicit val testCacheProvider = new TestMemoCacheBuilder
}

package packageScopeProvider {

  class ClassWithMemo {

    @memoize(2, 5 days)
    def someMethod(param: Int) = param * Random.nextInt(100000)

  }

}

package classScope {

  class ClassWithMemo {

    implicit val testCacheProvider = new TestMemoCacheBuilder

    @memoize(2, 5 days)
    def someMethod(param: Int) = param * Random.nextInt(100000)

  }

}

package importScopeBuilder {

  package somePackage {
    object MemoCacheBuilders {
      implicit val someArbitraryBuilder = new TestMemoCacheBuilder
    }
  }

  package yetAnotherPackage {

    import com.softwaremill.macmemo.examples.importScopeBuilder.somePackage.MemoCacheBuilders.someArbitraryBuilder

    class ClassWithMemo {

      @memoize(2, 5 days)
      def someMethod(param: Int) = param * Random.nextInt(100000)

    }

  }

}

package objectScope {

  object ObjectWithMemo {

    implicit val testCacheProvider = new TestMemoCacheBuilder

    @memoize(2, 5 days)
    def someMethod(param: Int) = param * Random.nextInt(100000)

  }

}

class MemoCacheBuilderResolutionSpec extends FlatSpec with Matchers {

  behavior of "MemoCacheBuilder resolution"

  it should "bring builder from a package scope" in {
    // given
    val obj = new packageScopeProvider.ClassWithMemo
    val testMemoCache = packageScopeProvider.testCacheProvider

    // when
    obj.someMethod(15)
    obj.someMethod(15)
    obj.someMethod(20)
    obj.someMethod(21)

    // then
    val methodHits = testMemoCache.hitsOn(obj, "someMethod")
    methodHits.total should equal(4)
    methodHits.withArgs(15) should equal(2)
    methodHits.withArgs(20) should equal(1)
    methodHits.withArgs(21) should equal(1)
  }

  it should "bring builder from a class scope" in {
    // given
    val obj = new classScope.ClassWithMemo
    val testMemoCache = obj.testCacheProvider

    // when
    obj.someMethod(15)
    obj.someMethod(15)
    obj.someMethod(20)
    obj.someMethod(21)

    // then
    val methodHits = testMemoCache.hitsOn(obj, "someMethod")
    methodHits.total should equal(4)
    methodHits.withArgs(15) should equal(2)
    methodHits.withArgs(20) should equal(1)
    methodHits.withArgs(21) should equal(1)
  }

  it should "bring builder from an import scope" in {
    // given
    val obj = new importScopeBuilder.yetAnotherPackage.ClassWithMemo
    val testMemoCache = importScopeBuilder.somePackage.MemoCacheBuilders.someArbitraryBuilder

    // when
    obj.someMethod(15)
    obj.someMethod(15)
    obj.someMethod(20)
    obj.someMethod(21)

    // then
    val methodHits = testMemoCache.hitsOn(obj, "someMethod")
    methodHits.total should equal(4)
    methodHits.withArgs(15) should equal(2)
    methodHits.withArgs(20) should equal(1)
    methodHits.withArgs(21) should equal(1)
  }

  it should "bring builder from an object scope" in {
    // given
    val obj = objectScope.ObjectWithMemo
    val testMemoCache = obj.testCacheProvider

    // when
    obj.someMethod(15)
    obj.someMethod(15)
    obj.someMethod(20)
    obj.someMethod(21)

    // then
    val methodHits = testMemoCache.hitsOn(obj, "someMethod")
    methodHits.total should equal(4)
    methodHits.withArgs(15) should equal(2)
    methodHits.withArgs(20) should equal(1)
    methodHits.withArgs(21) should equal(1)
  }


}
