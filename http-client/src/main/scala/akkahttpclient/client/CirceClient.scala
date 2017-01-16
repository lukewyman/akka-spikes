package akkahttpclient.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{Unmarshaller, Unmarshal}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akkahttpclient.domain.Car
import de.heikoseeberger.akkahttpcirce.CirceSupport

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by lukewyman on 1/12/17.
  */
object CirceClient extends App {

  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher

  val cc = new CirceCarClient

  // Get 1 car
  Await.ready(cc.getCar(1), Duration.Inf).onComplete { c =>
    println("Getting car for id 1:")
    println(s"Car: ${c.get}")
  }

  // Get several cars
  val cars = Future.sequence(Seq(1,2,3,4,5).map(id => cc.getCar(id)))

  Await.ready(cars, Duration.Inf).onComplete { r =>
    println("Getting car for ids 1 thru 5:")
    r.map(cs => cs.map(c => println(c)))
  }

  // Get NotFoound
  Await.ready(cc.getCar(7), Duration.Inf).onComplete { bc =>
    println("Getting Car that doesn't exist for id 7:")
    println(s"Expecting bad status: ${bc}")
  }

  // Get BadRequest
  Await.ready(cc.getCar(0), Duration.Inf).onComplete {bc =>
    println("Getting Car that doesn't exist for id 7:")
    println(s"Expecting bad status: ${bc}")
  }

  Await.ready(system.terminate(), Duration.Inf)

}

class CirceCarClient(implicit val system: ActorSystem) {
  import CirceSupport._
  import io.circe.generic.auto._
  import CirceCarClient._

  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  private def deserialize[T](r: HttpResponse)(implicit um: Unmarshaller[ResponseEntity, T]): Future[ClientResult[T]] =
    r.status match {
      case StatusCodes.OK => Unmarshal(r.entity).to[T] map Right.apply
      case StatusCodes.NotFound => Future(Left(NotFound(r.entity.toString)))
      case StatusCodes.BadRequest => Future(Left(BadRequest(r.entity.toString)))
      case StatusCodes.Unauthorized => Future(Left(Unauthorized(r.entity.toString)))
      case _ => Future(Left(UnexpectedStatusCode(r.status)))
    }

  def getCar(id: Int): Future[ClientResult[Car]] = {
    val source = Source.single(HttpRequest(uri = Uri(path = Path(s"/cars/${id}"))))
    val flow = Http().outgoingConnection(host = "localhost", port = 8000).mapAsync(1) { r =>
      deserialize[Car](r)
    }

    source.via(flow).runWith(Sink.head)
  }

}

object CirceCarClient {
  type ClientResult[T] = Either[CirceClientError, T]

  sealed trait CirceClientError
  case class NotFound(error: String) extends CirceClientError
  case class BadRequest(error: String) extends CirceClientError
  case class Unauthorized(error: String) extends CirceClientError
  case class UnexpectedStatusCode(statusCode: StatusCode) extends CirceClientError
}