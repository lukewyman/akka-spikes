package akkahttpclient.client

import akka.actor.{ActorLogging, ActorSystem, Props, Actor}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Created by lukewyman on 1/14/17.
  */
object AskPattern extends App {

  val system = ActorSystem()

  val actor1 = system.actorOf(Actor1.props, "actor1")
  actor1 ! Message("Reverse this!")

}

case class Message(msg: String)

object Actor1{
  def props = Props(new Actor1)
}

class Actor1 extends Actor with ActorLogging {
  import akka.pattern.ask
  import context.dispatcher
  implicit val timeout = Timeout(5 seconds)

  val actor2 = context.actorOf(Props[Actor2], "actor2")

  def receive = {
    case m: Message =>
      log.info(s"Actor1 got message: ${m}. Asking Actor2...")
      val reply = actor2.ask(m)
      log.info("Actor1 got the reply from Actor2. Let's unpack it...")
      reply.onComplete {
        case Success(m)  => log.info(s"Got the message back: ${m}")
        case Failure(e) => log.info(s"Total bomb: ${e}")
      }
  }
}

class Actor2 extends Actor with ActorLogging {

  def receive = {
    case m @ Message(msg) =>
      log.info(s"Actor2 got message: ${m}")
      sender() ! msg.reverse
  }
}


