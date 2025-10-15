package com.poc

import zio.redis.Redis
import zio.{Duration, UIO, ZIO, ZLayer}

class BitonicCacheService(bitonic: BitonicService, redis: Redis) {
  private val defaultTtl = Duration.fromSeconds(300) // 5 minutes

  def generateSequence(n: Int, l: Int, r: Int): UIO[Array[Int]] = {
    val key = s"bitonic:n=$n-l=$l-r=$r"

    (for {
      cached <- redis.get(key).returning[String]
      result <- cached match {
        case Some(cachedValue) =>
          val parsedArray = if (cachedValue.isEmpty) Array.empty[Int]
                           else cachedValue.split(",").map(_.toInt)
          ZIO.succeed(parsedArray)

        case None =>
          bitonic.generateSequence(n, l, r).flatMap { bitonicArray =>
            val arrayString = bitonicArray.mkString(",")
            redis.set(key, arrayString, Some(defaultTtl)).as(bitonicArray)
          }
      }
    } yield result).orDie
  }
}

object BitonicCacheService {
  val layer: ZLayer[BitonicService & Redis, Nothing, BitonicCacheService] = ZLayer {
    for {
      bitonic <- ZIO.service[BitonicService]
      redis <- ZIO.service[Redis]
    } yield BitonicCacheService(bitonic, redis)
  }
}
