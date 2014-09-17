package com.softwaremill.macmemo

class TestMemoCacheBuilder extends MemoCacheBuilder {

  case class MethodHits(totalHits: Int, private val hitsPerArgs: Map[List[Any], Int]) {

    def registerHitFor(key: List[Any]): MethodHits =
      copy(totalHits + 1, hitsPerArgs + (key -> (hitsPerArgs.getOrElse(key, 0) + 1)))

    def withArgs(args: List[Any]): Int = hitsPerArgs.get(args).getOrElse(0)
  }

  private var spy = Map[String, MethodHits]()

  def hitsOn(obj: AnyRef, methodName: String) =
    TestMemoCacheHits(spy.get(s"${obj.getClass.getName}.$methodName"))

  case class TestMemoCacheHits(hits: Option[MethodHits]) {

    def total: Int = hits.map(_.totalHits).getOrElse(0)

    def withArgs(args: Any*): Int = hits.map(_.withArgs(args.toList)).getOrElse(0)
  }

  override def build[V <: Object](bucketId: String, p: MemoizeParams): Cache[V] = {
    new Cache[V] {
      override def get(key: List[Any], compute: => V): V = {
        spy += (bucketId -> spy.get(bucketId).map(_.registerHitFor(key)).getOrElse(MethodHits(1, Map(key -> 1))))
        compute
      }

      override def toString: String = s"""TestMemoCache($spy)"""
    }
  }

  override def toString: String = s"""TestMemoCacheBuilder($spy)"""

}
