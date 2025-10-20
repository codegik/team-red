package com.poc.memcached

import com.poc.BitonicService
import net.rubyeye.xmemcached.utils.AddrUtil
import net.rubyeye.xmemcached.{MemcachedClient, XMemcachedClientBuilder}
import zio.{UIO, ZIO, ZLayer}

case class MemcachedConfig(host: String, port: Int)

class BitonicMemcachedService(bitonic: BitonicService, client: MemcachedClient) {
  private val defaultTtlInSeconds = 300 // 5 minutes

  def generateSequence(n: Int, l: Int, r: Int): UIO[Array[Int]] = {
    val key = s"bitonic:n=$n-l=$l-r=$r"

    (for {
      cached <- ZIO.attempt(Option(client.get[String](key)))
      result <- cached match {
        case Some(cachedValue) =>
          val parsedArray = if (cachedValue.isEmpty) Array.empty[Int]
          else cachedValue.split(",").map(_.toInt)
          ZIO.succeed(parsedArray)

        case None =>
          bitonic.generateSequence(n, l, r).flatMap { bitonicArray =>
            val arrayString = bitonicArray.mkString(",")
            ZIO.attempt(client.set(key, defaultTtlInSeconds, arrayString)).as(bitonicArray)
          }
      }
    } yield result).orDie
  }
}

object BitonicMemcachedService {
  val layer: ZLayer[BitonicService & MemcachedConfig, Nothing, BitonicMemcachedService] = ZLayer {
    for {
      bitonic <- ZIO.service[BitonicService]
      config <- ZIO.service[MemcachedConfig]
      builder <- ZIO.succeed(new XMemcachedClientBuilder(AddrUtil.getAddresses(s"${config.host}:${config.port}")))
      client <- ZIO.attempt(builder.build()).orDie
    } yield BitonicMemcachedService(bitonic, client)
  }
}
