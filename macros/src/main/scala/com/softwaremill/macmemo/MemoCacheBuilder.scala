package com.softwaremill.macmemo

import java.util.concurrent.{Callable, TimeUnit}

import com.google.common.cache.CacheBuilder

/**
 * Memoization parameters container.
 *
 * @param maxSize maximum cache capacity
 * @param expiresAfterMillis expiration time
 * @param concurrencyLevel allowed concurrency among update operations.
 */
case class MemoizeParams(maxSize: Long, expiresAfterMillis: Long, concurrencyLevel: Option[Int])

trait Cache[V] {

  /**
   * Return cached value for given key (method's parameters).
   * If needed, computeValue will be called to obtain it.
   *
   * @param key method argument values
   * @param computeValue a non-strict loader for a value.
   * @return cached value
   */
  def get(key: List[Any], computeValue: => V): V

}

/**
 * Cache instance builder, used to customize underlying storage mechanism.
 */
trait MemoCacheBuilder {

  /**
   * This method builds new instance of a Cache implementation when annotated method is executed for the first time.
   * Returned Cache instance will be scoped for particular enclosure instance and method.
   *
   * @param bucketId cache bucked identifier - currently enclosing type's path and simple name + method name.
   * @param params @memoize annotation's parameters: maximum cache size, expiration time and so on.
   * @tparam V type of a cached value - an annotated method's return type.
   * @return new Cache instance, scoped for particular enclosure instance method.
   */
  def build[V <: Object](bucketId: String, params: MemoizeParams): Cache[V]

}

object MemoCacheBuilder {

  /**
   * Default cache provider
   */
  val guavaMemoCacheBuilder: MemoCacheBuilder = new MemoCacheBuilder {

    override def build[V <: Object](bucketId: String, params: MemoizeParams): Cache[V] = {
      lazy val builder = CacheBuilder.newBuilder()
        .maximumSize(params.maxSize)
        .expireAfterWrite(params.expiresAfterMillis, TimeUnit.MILLISECONDS)

      lazy val cache = params.concurrencyLevel.map(builder.concurrencyLevel(_)).getOrElse(builder).build[List[Any], V]()

      new Cache[V] {
        override def get(key: List[Any], computeValue: => V): V = cache.get(key, new Callable[V] {
          override def call(): V = computeValue
        })
      }

    }

  }

}
