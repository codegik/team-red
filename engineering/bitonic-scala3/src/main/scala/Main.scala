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
    Method.POST / "bitonic" -> handler { (req: Request) =>
      ZIO.logInfo(s"Called /bitonic with queryParams=${req.url.queryParams}") *> {
        val n = req.queryOrElse[Int]("n", 0)
        val l = req.queryOrElse[Int]("l", 0)
        val r = req.queryOrElse[Int]("r", 0)
        val key = s"bitonic:n=$n-l=$l-r=$r"
        (for {
          bitonic <- ZIO.service[Bitonic.Impl]
          bitonicArray <- bitonic.bitonicArray(n, l, r)
          _ <- ZIO.logInfo(s"Bitonic array generated: ${bitonicArray.mkString("Array(", ", ", ")")}")
          redis <- ZIO.service[Redis]
          cached <- redis.get(key).returning[String]
          _ <- ZIO.logInfo(s"Cache lookup for key '$key': ${cached.getOrElse("MISS")}")
          result <- cached match {
            case Some(g) => ZIO.succeed((g, true))
            case None =>
              val g = bitonicArray.mkString("Array(", ", ", ")")
              redis.set(key, g, Some(5.minutes)).as((g, false))
          }
          (bitonic, wasCached) = result
          _ <- ZIO.logInfo(s"Bitonic Array: $bitonic (cached: $wasCached)")
          resp = Response.text(bitonic).addHeader("X-Cache", if (wasCached) "HIT" else "MISS")
        } yield resp)
          .catchAll(e => ZIO.fail(Response.text(s"Unexpected error: ${e.getMessage}").status(Status.InternalServerError)))
      }
    }

  val routes: Routes[Redis & Bitonic.Impl, Nothing] = Routes(
    healthRoute,
    bitonicRoute
  )

  object ProtobufCodecSupplier extends CodecSupplier {
    def get[A: Schema]: BinaryCodec[A] = ProtobufCodec.protobufCodec
  }

  // Environment variables for Redis configuration
  private val redisHost = sys.env.getOrElse("REDIS_HOST", "localhost")
  private val redisPort = sys.env.get("REDIS_PORT").flatMap(_.toIntOption).getOrElse(6379)
  private val appPort = sys.env.get("APP_PORT").flatMap(_.toIntOption).getOrElse(8080)

  def run: ZIO[Any, Throwable, Nothing] =
    Server
      .serve(routes)
      .provide(
        Server.defaultWithPort(appPort),
        Redis.singleNode,
        ZLayer.succeed[RedisConfig](RedisConfig(host = redisHost, port = redisPort)),
        ZLayer.succeed[CodecSupplier](ProtobufCodecSupplier),
        Bitonic.live
      )
}