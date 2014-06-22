package com.softwaremill.macmemo

import java.util.concurrent.TimeUnit

import com.google.common.cache.CacheBuilder

class DefMemo[A <: List[_], R <: Object](maxSize: Long, expiresAfterMillis: Long, concurrencyLevel: Option[Int]) {

  lazy val builder = CacheBuilder.newBuilder()
    .maximumSize(maxSize)
    .expireAfterWrite(expiresAfterMillis, TimeUnit.MILLISECONDS)

  lazy val cache = concurrencyLevel.map(level => builder.concurrencyLevel(level)).getOrElse(builder).build[A, R]()

}
