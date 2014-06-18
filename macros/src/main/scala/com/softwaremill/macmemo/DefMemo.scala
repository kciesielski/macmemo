package com.softwaremill.macmemo

import java.util.concurrent.TimeUnit

import com.google.common.cache.CacheBuilder

import scala.concurrent.duration.FiniteDuration

class DefMemo[A <: List[_], R <: Object](concurrencyLevel: Int, maxSize: Long, ttl: FiniteDuration) {

  lazy val cache = CacheBuilder.newBuilder()
    .concurrencyLevel(concurrencyLevel)
    .maximumSize(maxSize)
    .expireAfterWrite(ttl.toMillis, TimeUnit.MILLISECONDS)
    .build[A, R]()
}
