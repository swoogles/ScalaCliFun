//> using dep com.lihaoyi::os-lib:0.10.1
//> using repositories sonatype:snapshots
//> using dep dev.zio::zio-http:3.0.0-RC9+2-3a91fc06-SNAPSHOT
//> using dep dev.zio::zio:2.1.5
//> using dep dev.zio::zio-direct:1.0.0-RC7

//> using dep dev.zio::zio-schema:1.1.0
//> using dep dev.zio::zio-schema-json:1.1.0



import zio.*
import zio.direct.*
import zio.http.codec.{HttpCodec, PathCodec}
import zio.http.endpoint.openapi.{OpenAPIGen, SwaggerUI}
import zio.http.endpoint.{Endpoint, EndpointExecutor, EndpointLocator, EndpointMiddleware}

import zio.http._
import zio.http.Header.Authorization
import zio.schema.*

object ClientServer extends ZIOAppDefault {
  import HttpCodec.query
  import PathCodec._
  val auth = EndpointMiddleware.auth

  case class ApodResponse(date: String, explanation: String, url: String, media_type: String, title: String) derives Schema


  val getUserPosts =
  // Endpoint(Method.GET / "planetary" / int("userId") / "posts" / int("postId"))
    Endpoint(Method.GET / "planetary" / "apod")
      .query(query("api_key"))
      .out[ApodResponse] @@ auth

  def example(client: Client, nasa_api_key: String) = {
      val locator =
        EndpointLocator.fromURL(URL.decode("https://api.nasa.gov").toOption.get)

      val executor: EndpointExecutor[Authorization] =
        EndpointExecutor(client, locator, ZIO.succeed(Authorization.Basic("user", "pass")))

      val x2 = getUserPosts(nasa_api_key)

      val result2: ZIO[Scope, Nothing, ApodResponse] = executor(x2)

      result2.debug
    }

  val program =
    defer: 
      val client = ZIO.service[Client].run
      val api_key = System.env("nasa_api_key").debug("api_key").run.get
      ClientServer.example(client, api_key)
        .timeout(3.second)
        .run

  def run =
    program
      .provide(Client.default, Scope.default)
}

import zio.{Runtime, Task, Unsafe}
