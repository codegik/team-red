## High-Level Overview

- **Goal**: Provide a **Memcached-backed cache** for the same bitonic sequence endpoint that previously relied on **Redis**.
- **Approach**: Define an **abstract interface** (`MemcachedService`) and a **concrete implementation** (`MemcachedServiceImpl`) that uses **XMemcached** Java client. Use **ZIO layers** to wire dependencies and manage resources (client lifecycle).
- **Usage**: `BitonicMemcachedService` composes:
  - A pure **bitonic sequence generator** (`BitonicService`), and
  - The **Memcached service** (`MemcachedService`)
  - to produce cached values with a **TTL of 5 minutes**.
- **Parity with Redis**: Nearly identical behavior to `BitonicCacheService` (Redis), but the storage backend is Memcached.

---

## Files & Responsibilities

### 1) `MemcachedConfig.scala`
```scala
package com.poc.memcached

case class MemcachedConfig(host: String, port: Int)
```
- A **`case class`** to hold configuration (host, port).
- **Why case class?**
  - Provides **concise syntax** and built-in goodies: immutability, `equals/hashCode`, pattern matching support, and easy construction (`copy`, `apply`, etc.).
  - Ideal for **configuration objects** and **simple data carriers**.

---

### 2) `MemcachedService.scala`
```scala
package com.poc.memcached

import zio.Task

trait MemcachedService {
  def get(key: String): Task[Option[String]]
  def set(key: String, value: String, ttlSeconds: Int): Task[Boolean]
}
```
- A **`trait`** declares the **capabilities** of a Memcached client exposed to the rest of the app.
- **Why trait?**
  - It acts like a **typeclass/interface** contract.
  - Encourages **dependency inversion**: other modules depend on the **trait** (abstraction), not the concrete client.
  - Enables **testability**: in tests, you can provide a **fake/in-memory** implementation to the same trait.
- **Return types**:
  - `Task[Option[String]]` for `get`:
    - `Task[A]` is an alias for `ZIO[Any, Throwable, A]` (may fail with `Throwable`).
    - `Option[String]` models **cache hit/miss** (`Some(value)` / `None`), avoiding `null`.
  - `Task[Boolean]` for `set`:
    - Indicates whether the **store operation** succeeded (`true`/`false`).
    - Failures at the effect level are expressed as **ZIO errors** (`Throwable`) rather than `false`.

> Why `Task` instead of `UIO`? Because **network I/O** can **fail**. `Task` makes the failure **typed and explicit** in the effect.  

---

### 3) `MemcachedServiceImpl.scala`
```scala
package com.poc.memcached

import net.rubyeye.xmemcached.utils.AddrUtil
import net.rubyeye.xmemcached.{MemcachedClient, XMemcachedClientBuilder}
import zio.*

class MemcachedServiceImpl(client: MemcachedClient) extends MemcachedService {
  override def get(key: String): Task[Option[String]] =
    ZIO.attempt(Option(client.get[String](key)))

  override def set(key: String, value: String, ttlSeconds: Int): Task[Boolean] =
    ZIO.attempt(client.set(key, ttlSeconds, value))
}

object MemcachedServiceImpl {
  val layer: ZLayer[MemcachedConfig, Throwable, MemcachedService] =
    ZLayer.scoped {
      for {
        config  <- ZIO.service[MemcachedConfig]
        builder <- ZIO.succeed(new XMemcachedClientBuilder(AddrUtil.getAddresses(s"${config.host}:${config.port}")))
        client  <- ZIO.acquireRelease(ZIO.attempt(builder.build()))(c => ZIO.attempt(c.shutdown()).ignore)
      } yield MemcachedServiceImpl(client)
    }
}
```
- **Implementation** using **XMemcached** (Java client).
- `get` & `set` are wrapped in `ZIO.attempt(...)` to **capture exceptions** (I/O errors) into the ZIO error channel.
- **`layer`**:
  - Declares a `ZLayer[MemcachedConfig, Throwable, MemcachedService]`:
    - **Input**: `MemcachedConfig`
    - **Output**: `MemcachedService`
    - **Error**: `Throwable` (e.g., failure to build/shutdown the client)
  - Uses `ZLayer.scoped` + `ZIO.acquireRelease` to **acquire** the `MemcachedClient` and **release** it safely (shutdown) when the layer is finalized (graceful resource management).

> This is the ZIO way to do **resource-safe construction** — a major advantage over ad-hoc `try/finally` scattered in code.

---

### 4) `BitonicMemcachedService.scala`
```scala
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
      bitonic         <- ZIO.service[BitonicService]
      memcachedClient <- ZIO.service[MemcachedService]
    } yield BitonicMemcachedService(bitonic, memcachedClient)
  }
}
```
- **Behavior** mirrors the Redis version:
  1. Build cache **key** from `(n, l, r)`.
  2. **Try `get`** from Memcached → `Option[String]`:
     - **Some** → parse CSV → return `Array[Int]`.
     - **None** → compute via `bitonic.generateSequence`, stringify via `mkString(",")`, **set** with TTL, return original array.
  3. `.orDie`: convert any `Task` failure into a **fiber defect** (so final type is `UIO[Array[Int]]`).  
     - This means callers don’t handle cache errors; failures crash the fiber. Consider using a typed error channel if you plan to recover gracefully.

- **Layer** takes `BitonicService` and `MemcachedService` and **produces** `BitonicMemcachedService` (wires dependencies).

---

## Similarities with `BitonicCacheService` (Redis)

| Concern | Redis Version (`BitonicCacheService`) | Memcached Version (`BitonicMemcachedService`) |
|---------|----------------------------------------|-----------------------------------------------|
| Backend | `zio-redis` client | XMemcached Java client via wrapper |
| API shape | `generateSequence(n,l,r): UIO[Array[Int]]` | Same |
| Cache key | `bitonic:n=...-l=...-r=...` | Same |
| Cache read | `redis.get(...).returning[String]` | `memcachedService.get(...)` |
| Cache write | `redis.set(..., Some(ttl)).as(array)` | `memcachedService.set(..., ttl).as(array)` |
| TTL | `Duration.fromSeconds(300)` | `300` seconds |
| Error policy | `.orDie` at the end | `.orDie` at the end |
| DI | `ZLayer` wiring | `ZLayer` wiring |

**Takeaway**: The Memcached path is a **drop-in alternative** to Redis for the same endpoint semantics.

---

## Why These Types? (Design Rationale)

- **`case class MemcachedConfig(host: String, port: Int)`**  
  - Immutable, concise, pattern-match friendly configuration object.

- **`trait MemcachedService`**  
  - Abstraction (like an interface) to decouple usage from any specific client library.  
  - Enables **swapping implementations** (e.g., fake in tests, local stub, remote proxy).

- **`Task[Option[String]]` for `get`**  
  - `Task[_]` acknowledges **I/O can fail** (network, serialization).  
  - `Option[String]` models **hit/miss** without `null`.  
  - Clear semantics: **errors** are exceptional (fail effect), **misses** are normal (`None`).

- **`Task[Boolean]` for `set`**  
  - Indicates success/failure of the store operation as a **boolean result**, while **transport errors** are effect failures.  
  - Keeps a simple signature; if you need richer status, you can return a sealed ADT instead.

- **`ZLayer.scoped` + `ZIO.acquireRelease`**  
  - Correct, **resource-safe** lifecycle of the Memcached client (init + shutdown), even on errors/cancellations.

- **`BitonicMemcachedService` returning `UIO[Array[Int]]`**  
  - Public API is **unfailing** (by turning failures into defects with `.orDie`).  
  - Simple for callers; but if you want recoverable cache errors, keep a typed error (e.g., `IO[CacheError, Array[Int]]`).
