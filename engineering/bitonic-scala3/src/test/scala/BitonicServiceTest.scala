import com.poc.BitonicService
import zio.test.junit.JUnitRunnableSpec
import zio.test.{Spec, assertTrue}
import zio.{Scope, ZIO}

object BitonicServiceTest extends JUnitRunnableSpec {
  def spec: Spec[Scope, Nothing] =
    suite("Bitonic test spec")(

      test("Test success case") {
        for {
          bitonic <- ZIO.service[BitonicService]
          result <- bitonic.generateSequence(5, 3, 10)
        } yield assertTrue(result.sameElements(Array(9, 10, 9, 8, 7)))
      },

      test("Test impossible case") {
        for {
          bitonic <- ZIO.service[BitonicService]
          result <- bitonic.generateSequence(100, 1, 10)
        } yield assertTrue(result.sameElements(Array(-1)))
      },

      test("Test edge case - exact boundary") {
        for {
          bitonic <- ZIO.service[BitonicService]
          result <- bitonic.generateSequence(3, 5, 7)
        } yield assertTrue(
          result.length == 3,
          result.sameElements(Array(6, 7, 6))
        )
      }
    ).provide(BitonicService.layer)
}