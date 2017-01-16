package akkahttpclient.client

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{Unmarshaller, Unmarshal}
import akka.stream.{ActorMaterializerSettings, ActorMaterializer}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import akkahttpclient.domain.Car
import de.heikoseeberger.akkahttpcirce.CirceSupport

import scala.concurrent.{ExecutionContext, Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Created by lukewyman on 1/12/17.
  */
object ActorClient extends App {

  val system = ActorSystem()
  implicit def executionContext = system.dispatcher
  implicit val timeOut = Timeout(3 seconds)
  val intermediate = system.actorOf(Props(new Intermediate), "intermediate")

  intermediate ! GetCar(2) // OK
  Thread.sleep(3000)
  intermediate ! GetCar(7) // NotFound
  Thread.sleep(3000)
  intermediate ! GetCar(0) // BadRequest

  Thread.sleep(5000)
  Await.ready(system.terminate, Duration.Inf)

}

case class GetCar(id: Int)

class Intermediate(implicit val timeout: Timeout) extends Actor with ActorLogging {
  import akka.pattern.ask
  import context.dispatcher

  val carClient = context.actorOf(Props(new CarClient), "car-client")

  def receive = {
    case gc: GetCar =>
      log.info(s"Intermediate got message: ${gc}. Asking CarClient...")
      val result = carClient.ask(gc)
      log.info("Intermediate got the reply from CarClient. Let's unpack it...")
      result.onComplete {
        case Success(hcr) => log.info(s"Intermediate got the Car back: ${hcr}")
        case Failure(e) => log.info(s"Car crashed: ${e}")
      }
  }
}

class CarClient extends Actor with ActorLogging with HttpClient {

  import akka.pattern.pipe
  implicit def executionContext = context.dispatcher
  implicit def system = context.system

  def receive = {
    case GetCar(id) =>
      getCar(id).pipeTo(sender())
  }

}

trait HttpClient {
  import HttpClient._
  import CirceSupport._
  import io.circe.generic.auto._

  implicit def system: ActorSystem
  implicit def executionContext: ExecutionContext
  val http = Http(system)

  implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))

  private def deserialize[T](r: HttpResponse)(implicit um: Unmarshaller[ResponseEntity, T]): Future[HttpClientResult[T]] =
    r.status match {
      case StatusCodes.OK => Unmarshal(r.entity).to[T] map Right.apply
      case StatusCodes.NotFound => Future(Left(NotFound(r.entity.toString)))
      case StatusCodes.BadRequest => Future(Left(BadRequest(r.entity.toString)))
      case StatusCodes.Unauthorized => Future(Left(Unauthorized(r.entity.toString)))
      case _ => Future(Left(UnexpectedStatusCode(r.status)))
    }

  def getCar(id: Int): Future[HttpClientResult[Car]] = {

    val source = Source.single(HttpRequest(uri = Uri(path = Path(s"/cars/${id}"))))
    val flow = http.outgoingConnection(host = "localhost", port = 8000).mapAsync(1) { r =>
      deserialize[Car](r)
    }

    source.via(flow).runWith(Sink.head)
  }
}

object HttpClient {
  type HttpClientResult[T] = Either[HttpClientError, T]

  sealed trait HttpClientError
  case class NotFound(error: String) extends HttpClientError
  case class BadRequest(error: String) extends HttpClientError
  case class Unauthorized(error: String) extends HttpClientError
  case class UnexpectedStatusCode(statusCode: StatusCode) extends HttpClientError
}