## Introduction

This project provides an API for generating bitonic sequences using the Bitonic algorithm. A **bitonic sequence** is a sequence of numbers that first increases and then decreases, or vice versa. Bitonic sequences are commonly used in sorting algorithms, such as the Bitonic Sort, which is efficient for parallel processing and hardware implementations.

The main goal of this project is to demonstrate how to generate bitonic sequences and expose this functionality through a simple HTTP API.

For more information on the sbt-dotty plugin, see the
[scala3-example-project](https://github.com/scala/scala3-example-project/blob/main/README.md).


```
### About ZIO

This project is built using **ZIO**, a powerful library for asynchronous and concurrent programming in Scala. ZIO provides a type-safe, composable way to manage effects such as I/O, concurrency, and resource management. We use ZIO in this project to:

- Structure the application using layers and services
- Handle asynchronous operations safely
- Build a robust and scalable HTTP API

ZIO was chosen for its strong support for functional programming, reliability, and its ecosystem for building modern Scala applications.

## How to Run the Project

1. **Prerequisites**
   - [Podman] (https://podman.io/) or [Docker](https://www.docker.com/) installed.

2. **Start compose**
   ```sh
   podman compose up
   ```
Compose will create and run 3 containers:

* redis
* memcached
* bitonic-scala3-app


## How to Test the API

### Health Check

```sh
curl http://localhost:8080/health-check
```

### Generate Bitonic Sequence

```sh
curl -X POST "http://localhost:8080/bitonic?n=5&l=1&r=10"
```

The result will be a JSON array with the generated sequence.

---

**Note:**  
If you are using a different port, adjust the URLs in the commands above.

## Verify Redis Cache

To verify if the app are saving the requests in the Redis Cache, you need to access the Redis Container

### Access the Redis container

```
podman exec -it redis sh
```

### Check the Redis cache

```
redis-cli --scan --pattern "*"
```



### APIs

* Health Check
```shell
curl --request GET \
  --url http://localhost:8080/health-check
```

* Bitonic API (using redis)
```shell
curl --request POST \
  --url 'http://localhost:8080/bitonic?n=5&l=3&r=10'
  
HTTP Status 200
[ 9, 10, 9, 8, 7]
```

* Bitonic API (using memcached  )
```shell
curl --request POST \
  --url 'http://localhost:8080/bitonic-memcached?n=5&l=3&r=10'
  
HTTP Status 200
[ 9, 10, 9, 8, 7]
