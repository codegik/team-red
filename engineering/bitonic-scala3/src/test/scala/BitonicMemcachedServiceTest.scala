package com.poc.memcached

import com.poc.BitonicService
import net.rubyeye.xmemcached.MemcachedClient
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import zio.test.junit.JUnitRunnableSpec
import zio.test.{Spec, assertTrue}
import zio.{Scope, ULayer, ZIO, ZLayer}

object BitonicMemcachedServiceTest extends JUnitRunnableSpec {

  // Mock BitonicMemcachedService layer with mocked MemcachedClient
  private def mockBitonicMemcachedServiceLayer(
    getCalls: Map[String, Option[String]] = Map.empty,
    simulateSetFailure: Boolean = false
  ): ULayer[BitonicMemcachedService] = ZLayer.fromZIO {
    ZIO.succeed {
      val bitonic = new BitonicService()
      val client = mock(classOf[MemcachedClient])

      // Configure get behavior
      getCalls.foreach { case (key, value) =>
        when(client.get[String](key)).thenReturn(value.orNull)
      }

      // Configure set behavior
      if (simulateSetFailure) {
        when(client.set(anyString(), anyInt(), anyString()))
          .thenThrow(new RuntimeException("Set operation failed"))
      } else {
        when(client.set(anyString(), anyInt(), anyString()))
          .thenReturn(true)
      }

      new BitonicMemcachedService(bitonic, client)
    }
  }

  def spec: Spec[Scope, Nothing] =
    suite("BitonicMemcachedService test spec")(

      test("Cache miss - should compute and cache result") {
        val expectedResult = Array(9, 10, 9, 8, 7)

        for {
          service <- ZIO.service[BitonicMemcachedService]
          result <- service.generateSequence(5, 3, 10)
        } yield assertTrue(
          result.sameElements(expectedResult),
          result.length == 5
        )
      }.provide(
        mockBitonicMemcachedServiceLayer(getCalls = Map("bitonic:n=5-l=3-r=10" -> None))
      ),

      test("Cache hit - should return cached value without recomputing") {
        val expectedResult = Array(9, 10, 9, 8, 7)

        for {
          service <- ZIO.service[BitonicMemcachedService]
          result <- service.generateSequence(5, 3, 10)
        } yield assertTrue(
          result.sameElements(expectedResult),
          result.length == 5
        )
      }.provide(
        mockBitonicMemcachedServiceLayer(getCalls = Map("bitonic:n=5-l=3-r=10" -> Option("9,10,9,8,7")))
      ),

      test("Cache hit with empty array") {
        for {
          service <- ZIO.service[BitonicMemcachedService]
          result <- service.generateSequence(100, 1, 10)
        } yield assertTrue(
          result.sameElements(Array(-1)),
          result.length == 1
        )
      }.provide(
        mockBitonicMemcachedServiceLayer(getCalls = Map("bitonic:n=100-l=1-r=10" -> Option("-1")))
      ),

      test("Cache miss for impossible case") {
        val expectedResult = Array(-1)

        for {
          service <- ZIO.service[BitonicMemcachedService]
          result <- service.generateSequence(100, 1, 10)
        } yield assertTrue(
          result.sameElements(expectedResult),
          result.length == 1
        )
      }.provide(
        mockBitonicMemcachedServiceLayer(getCalls = Map("bitonic:n=100-l=1-r=10" -> None))
      ),

      test("Cache hit with edge case - exact boundary") {
        val expectedResult = Array(6, 7, 6)

        for {
          service <- ZIO.service[BitonicMemcachedService]
          result <- service.generateSequence(3, 5, 7)
        } yield assertTrue(
          result.sameElements(expectedResult),
          result.length == 3
        )
      }.provide(
        mockBitonicMemcachedServiceLayer(getCalls = Map("bitonic:n=3-l=5-r=7" -> Option("6,7,6")))
      ),

      test("Cache hit with empty string should return empty array") {
        for {
          service <- ZIO.service[BitonicMemcachedService]
          result <- service.generateSequence(0, 1, 10)
        } yield assertTrue(
          result.isEmpty,
          result.length == 0
        )
      }.provide(
        mockBitonicMemcachedServiceLayer(getCalls = Map("bitonic:n=0-l=1-r=10" -> Option("")))
      ),

      test("Multiple calls with different parameters") {
        for {
          service <- ZIO.service[BitonicMemcachedService]
          result1 <- service.generateSequence(5, 3, 10)
          result2 <- service.generateSequence(3, 5, 7)
          result3 <- service.generateSequence(100, 1, 10)
        } yield assertTrue(
          result1.sameElements(Array(9, 10, 9, 8, 7)),
          result2.sameElements(Array(6, 7, 6)),
          result3.sameElements(Array(-1))
        )
      }.provide(
        mockBitonicMemcachedServiceLayer(getCalls = Map(
          "bitonic:n=5-l=3-r=10" -> None,
          "bitonic:n=3-l=5-r=7" -> None,
          "bitonic:n=100-l=1-r=10" -> None
        ))
      )
    )
}
