package com.poc

import zio.{UIO, ULayer, ZIO, ZLayer}

import scala.collection.mutable

object BitonicService {
  val layer: ULayer[BitonicService] = ZLayer.succeed(BitonicService())
}

class BitonicService {
  def generateSequence(n: Int, l: Int, r: Int): UIO[Array[Int]] = ZIO.succeed {
    if (n > (r - l) * 2 + 1) {
      Array(-1)
    } else {
      val dq = mutable.ArrayDeque[Int]()
      dq.append(r - 1)
      var i = r
      while (i >= l && dq.size < n) {
        dq.append(i)
        i -= 1
      }
      i = r - 2
      while (i >= l && dq.size < n) {
        dq.prepend(i)
        i -= 1
      }
      dq.toArray
    }
  }
}