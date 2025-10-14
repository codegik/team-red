import Bitonic.live
import zio.Scope
import zio.test.junit.JUnitRunnableSpec
import zio.test.{Spec, assertCompletes}

object BitonicTest extends JUnitRunnableSpec {
  def spec: Spec[Scope, Nothing] =
    suite("Bitonic test spec")(
      test("Test example case") {
        val bitonic = live.build.map(_.get)
        for {
          service <- bitonic
          result <- service.bitonicArray(5, 1, 10)
          _ = assert(result.sameElements(Array(9, 10, 9, 8, 7)))
        } yield assertCompletes
      },
      test("Test impossible case") {
        val bitonic = live.build.map(_.get)
        for {
          service <- bitonic
          result <- service.bitonicArray(100, 1, 10)
          _ = assert(result.sameElements(Array(-1)))
        } yield assertCompletes
      }
    )
}