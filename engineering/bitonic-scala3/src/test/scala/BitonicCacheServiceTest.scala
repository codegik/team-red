import zio.test._
import zio.test.Assertion._
import zio.{UIO, ZIO}
import zio.redis.Redis
import zio.mock._

object BitonicCacheServiceSpec extends ZIOSpecDefault {

  def spec = suite("BitonicCacheService")(
    suite("generateSequence")(
      test("returns cached value when present in Redis") {
        val redisMock = MockRedis.get(equalTo("bitonic:n=5-l=1-r=10"), value(Some("1,2,3,4,5")))
        val bitonicMock = MockBitonicService.generateSequence(anything, value(UIO.succeed(Array.empty[Int])))
        val service = BitonicCacheService(bitonicMock, redisMock)

        for {
          result <- service.generateSequence(5, 1, 10)
        } yield assert(result)(equalTo(Array(1, 2, 3, 4, 5)))
      },
      test("generates sequence and caches it when not present in Redis") {
        val redisMock = MockRedis.get(equalTo("bitonic:n=5-l=1-r=10"), value(None)) ++
          MockRedis.set(equalTo(("bitonic:n=5-l=1-r=10", "1,2,3,4,5", Some(Duration.fromSeconds(300)))), value(true))
        val bitonicMock = MockBitonicService.generateSequence(equalTo((5, 1, 10)), value(UIO.succeed(Array(1, 2, 3, 4, 5))))
        val service = BitonicCacheService(bitonicMock, redisMock)

        for {
          result <- service.generateSequence(5, 1, 10)
        } yield assert(result)(equalTo(Array(1, 2, 3, 4, 5)))
      },
      test("returns empty array when cached value is empty") {
        val redisMock = MockRedis.get(equalTo("bitonic:n=5-l=1-r=10"), value(Some("")))
        val bitonicMock = MockBitonicService.generateSequence(anything, value(UIO.succeed(Array.empty[Int])))
        val service = BitonicCacheService(bitonicMock, redisMock)

        for {
          result <- service.generateSequence(5, 1, 10)
        } yield assert(result)(isEmpty)
      }
    )
  )
}
