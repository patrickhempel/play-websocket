package actors


import akka.actor.{Actor, ActorRef, Cancellable, Props}
import play.api.libs.ws._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


/**
  * Created by patrickhempel on 22.06.16.
  */
class BitcoinTickerActor(out: ActorRef, ws:WSClient) extends Actor {

  private var scheduler: Cancellable = _

  override def preStart(): Unit = {
      scheduler = context.system.scheduler.schedule(
        initialDelay = 0 seconds,
        interval = 15 seconds,
        receiver = self,
        message = "tick"
      )
  }

  def receive = {
    case "tick" => {
      val url = "https://api.bitcoinaverage.com/exchanges/EUR"
      ws.url( url)
        .get()
        .map {
            response => response.status match {
              case 200 => out ! ( response.body)
            }
        }
    }
  }

  override def postStop() = {
    scheduler.cancel()
  }
}

object BitcoinTickerActor {
  def props(out: ActorRef, wSClient: WSClient) = Props( new BitcoinTickerActor( out, wSClient))
}
