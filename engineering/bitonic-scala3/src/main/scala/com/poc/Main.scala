package com.poc

import com.poc.memcached.{BitonicMemcachedService, MemcachedConfig, MemcachedServiceImpl}
import zio.*
import zio.http.*
import zio.redis.*
import zio.schema.*
import zio.schema.codec.{BinaryCodec, ProtobufCodec}

object Main extends ZIOAppDefault {

  private val healthRoute = Method.GET / "health-check" -> handler {
    ZIO.logInfo("Called /health-check") *> ZIO.succeed(Response.text("Ok"))
  }

  private val bitonicRoute =
    Method.POST / "bitonic-redis" -> handler { (req: Request) =>
      ZIO.logInfo(s"Called /bitonic with queryParams=${req.url.queryParams}") *> {
        val n = req.queryOrElse[Int]("n", 0)
        val l = req.queryOrElse[Int]("l", 0)
        val r = req.queryOrElse[Int]("r", 0)

        for {
          cacheService <- ZIO.service[BitonicCacheService]
          result <- cacheService.generateSequence(n, l, r)
          arrayString = result.mkString("[", ",", "]")
        } yield Response.json(arrayString)
      }
    }

  private val bitonicMemcachedRoute =
      Method.POST / "bitonic-memcached" -> handler { (req: Request) =>
        ZIO.logInfo(s"Called /bitonic memcached with queryParams=${req.url.queryParams}") *> {
          val n = req.queryOrElse[Int]("n", 0)
          val l = req.queryOrElse[Int]("l", 0)
          val r = req.queryOrElse[Int]("r", 0)

          for {
            cacheService <- ZIO.service[BitonicMemcachedService]
            result <- cacheService.generateSequence(n, l, r)
            arrayString = result.mkString("[", ",", "]")
          } yield Response.json(arrayString)
        }
      }

  private val bitonicDirectRoute =
    Method.POST / "bitonic" -> handler { (req: Request) =>
      ZIO.logInfo(s"Called /bitonic (direct calculation) with queryParams=${req.url.queryParams}") *> {
        val n = req.queryOrElse[Int]("n", 0)
        val l = req.queryOrElse[Int]("l", 0)
        val r = req.queryOrElse[Int]("r", 0)

        for {
          service <- ZIO.service[BitonicService]
          result <- service.generateSequence(n, l, r)
          arrayString = result.mkString("[", ",", "]")
        } yield Response.json(arrayString)
      }
    }

  private val routes: Routes[BitonicCacheService & BitonicMemcachedService & BitonicService, Nothing] = Routes(
    healthRoute,
    bitonicRoute,
    bitonicMemcachedRoute,
    bitonicDirectRoute
  )

  private object ProtobufCodecSupplier extends CodecSupplier {
    def get[A: Schema]: BinaryCodec[A] = ProtobufCodec.protobufCodec
  }

  private val redisHost = sys.env.getOrElse("REDIS_HOST", "localhost")
  private val redisPort = sys.env.get("REDIS_PORT").flatMap(_.toIntOption).getOrElse(6379)

  private val memcachedHost = sys.env.getOrElse("MEMCACHED_HOST", "localhost")
  private val memcachedPort = sys.env.get("MEMCACHED_PORT").flatMap(_.toIntOption).getOrElse(11211)

  private val appPort = sys.env.get("APP_PORT").flatMap(_.toIntOption).getOrElse(8080)

  def run: ZIO[Any, Throwable, Nothing] =
    Server
      .serve(routes)
      .provide(
        Server.defaultWithPort(appPort),

        // Redis
        Redis.singleNode,
        ZLayer.succeed[RedisConfig](RedisConfig(host = redisHost, port = redisPort)),

        // Memcached
        MemcachedServiceImpl.layer,
        ZLayer.succeed(MemcachedConfig(memcachedHost, memcachedPort)),

        ZLayer.succeed[CodecSupplier](ProtobufCodecSupplier),
        BitonicCacheService.layer,
        BitonicMemcachedService.layer,
        BitonicService.layer,
      )
}