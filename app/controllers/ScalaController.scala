package controllers

import scala.concurrent.Future
import play.api.mvc.Controller
import play.Logger
import play.api.libs.ws.WS
import concurrent.ExecutionContext.Implicits.global
import play.api.mvc.Action
import play.api.Play.current

object ScalaController extends Controller {

  val names: List[String] = List("Jim", "Dean", "Kunal")

  def lower(str: String): String = str.toLowerCase()

  def strlen(str: String): Int = str.length

  def explode(str: String): List[Char] = str.toCharArray().toList

  def index = Action {
    val loweredNames: List[String] = names.map(lower)
    println(s"$names.map(lower) yields $loweredNames")

    val lengthOfNames: List[Int] = names.map(strlen)
    println(s"$names.map(strlen) yields $lengthOfNames")

    val explodedNames: List[List[Char]] = names.map(explode)
    println(s"$names.map(explode) yields $explodedNames")

    val flattenedExplodedNames: List[Char] = names.flatMap(explode)
    println(s"$names.map(explode) yields $flattenedExplodedNames")

    Ok(lower("Hello World"))
  }

  def proxy = Action.async {
    val responseFuture = WS.url("http://example.com").get()

    Logger.info("Before map")
    val resultFuture = responseFuture.map { resp =>
        Logger.info("Within map")
        Status(resp.status)(resp.body).as(resp.header("Content-Type").get)
    }

    Logger.info("After map")
    resultFuture
  }

  def parallel = Action.async {
    val start = System.currentTimeMillis()
    def getLatency(r: Any): Long = System.currentTimeMillis() - start

    val google = WS.url("http://google.com").get().map(getLatency)
    val yahoo = WS.url("http://yahoo.com").get().map(getLatency)

    google.flatMap { googleResponseTime: Long =>
        yahoo.map { yahooResponseTime: Long =>
            Ok(s"Google response time:  $googleResponseTime; " +
               s"Yahoo response time: $yahooResponseTime")
        }
    }
  }

  def paramsFromFoo(x: Any): String = "?bar=baz"

  def sequential = Action.async {
    val foo = WS.url("http://www.foo.com").get()

    foo.flatMap { fooResponse =>
        // Use data in fooResponse to build the second request
        val bar = WS.url("http://www.bar.com/" + paramsFromFoo(fooResponse)).get()

        bar.map { barResponse =>
            // Now you can use barResponse and fooResponse to build a Result
            Ok(s"response from foo.com is ${fooResponse.status} & " +
               s"from bar.com is ${barResponse.status}")
        }
    }
  }

  // Handle Exceptions in Futures by logging them and returning a fallback value
  def withErrorHandling[T](f: Future[T], fallback: T): Future[T] = {
    f.recover {
      case t: Throwable =>
        Logger.error("Something went wrong!", t)
        fallback
    }
  }

  def checkHostName(hostName: String) = Action.async {
    // try using "thisdomaindoesnotexist" 
    val myFuture = WS.url(s"http://www.$hostName.com").get()
      .map { resp => resp.statusText }

    val myFutureWithFallback = withErrorHandling(myFuture, "fallback value")

    // str either contains the result of myFuture's async I/O or
    // "fallback value" if any Exception was thrown
    myFutureWithFallback.map { str => Ok(str) }
  }

}
