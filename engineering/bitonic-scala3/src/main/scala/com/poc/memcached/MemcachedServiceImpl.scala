package com.poc.memcached

import net.rubyeye.xmemcached.utils.AddrUtil
import net.rubyeye.xmemcached.{MemcachedClient, XMemcachedClientBuilder}
import zio.*

class MemcachedServiceImpl(client: MemcachedClient) extends MemcachedService {
  override def get(key: String): Task[Option[String]] =
    ZIO.attempt(Option(client.get[String](key)))

  override def set(key: String, value: String, ttlSeconds: Int): Task[Boolean] =
    ZIO.attempt(client.set(key, ttlSeconds, value))
}

object MemcachedServiceImpl {
  val layer: ZLayer[MemcachedConfig, Throwable, MemcachedService] =
    ZLayer.scoped {
      for {
        config <- ZIO.service[MemcachedConfig]
        builder <- ZIO.succeed(new XMemcachedClientBuilder(AddrUtil.getAddresses(s"${config.host}:${config.port}")))
        client <- ZIO.acquireRelease(ZIO.attempt(builder.build()))(c => ZIO.attempt(c.shutdown()).ignore)
      } yield MemcachedServiceImpl(client)
    }
}
