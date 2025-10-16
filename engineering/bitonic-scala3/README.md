## sbt project compiled with Scala 3

### Usage

This is a normal sbt project. You can compile code with `sbt compile`, run it with `sbt run`, and `sbt console` will start a Scala 3 REPL.

For more information on the sbt-dotty plugin, see the
[scala3-example-project](https://github.com/scala/scala3-example-project/blob/main/README.md).

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
```