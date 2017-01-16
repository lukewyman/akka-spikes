package akkahttpclient.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives
import akka.stream.ActorMaterializer
import akkahttpclient.domain.Car
import de.heikoseeberger.akkahttpcirce.CirceSupport

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.StdIn

/**
  * Created by lukewyman on 1/12/17.
  */
object Server extends App with ServerSuport {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  Http().bindAndHandle(route, "127.0.0.1", 8000)

  StdIn.readLine("Server up at localhost:8000. Hit <ENTER> to quit.")
  Await.ready(system.terminate(), Duration.Inf)
}

trait ServerSuport {
  import Directives._
  import CirceSupport._
  import io.circe.generic.auto._
  import StatusCodes._

  val cars: Map[Int, Car] = Map(
    (1 -> Car("honda", "civic")),
    (2 -> Car("toyota", "corolla")),
    (3 -> Car("chevrolet", "volt")),
    (4 -> Car("ford", "focus")),
    (5 -> Car("nissan", "sentra"))
  )

  def route: Route = {
    path("cars" / IntNumber) { id =>
      pathEndOrSingleSlash {
        get {
          if (id <= 0) complete(BadRequest) else
          cars.get(id) match {
            case Some(c) => complete(c)
            case None => complete(NotFound)
          }
        }
      }
    }
  }
}

