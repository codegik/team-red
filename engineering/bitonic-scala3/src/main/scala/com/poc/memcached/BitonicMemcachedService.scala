package com.poc.memcached

import com.poc.BitonicService
import zio.{UIO, ZIO, ZLayer}

class BitonicMemcachedService(bitonic: BitonicService, memcachedService: MemcachedService) {
  private val defaultTtlInSeconds = 300 // 5 minutes

  def generateSequence(n: Int, l: Int, r: Int): UIO[Array[Int]] = {
    val key = s"bitonic:n=$n-l=$l-r=$r"

    (for {
      cached <- memcachedService.get(key)
      result <- cached match {
        case Some(cachedValue) =>
          val parsedArray = if (cachedValue.isEmpty) Array.empty[Int]
          else cachedValue.split(",").map(_.toInt)
          ZIO.succeed(parsedArray)

        case None =>
          bitonic.generateSequence(n, l, r).flatMap { bitonicArray =>
            val arrayString = bitonicArray.mkString(",")
            memcachedService.set(key, arrayString, defaultTtlInSeconds).as(bitonicArray)
          }
      }
    } yield result).orDie
  }
}

object BitonicMemcachedService {
  val layer: ZLayer[BitonicService & MemcachedService, Nothing, BitonicMemcachedService] = ZLayer {
    for {
      bitonic <- ZIO.service[BitonicService]
      memcachedService <- ZIO.service[MemcachedService]
    } yield BitonicMemcachedService(bitonic, memcachedService)
  }
}
