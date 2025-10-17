package com.poc.memcached

import zio.Task

trait MemcachedService {
  def get(key: String): Task[Option[String]]
  def set(key: String, value: String, ttlSeconds: Int): Task[Boolean]
}
