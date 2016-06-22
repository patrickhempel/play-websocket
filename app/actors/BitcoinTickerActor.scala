package actors


import akka.actor.{Actor, ActorRef, Cancellable, Props}
import play.api.libs.ws._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.libs.json._


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
    case "history" => {
      val url = "https://api.bitcoinaverage.com/history/EUR/per_minute_24h_sliding_window.csv"
      ws.url( url)
        .get()
        .map {
          response => response.status match {
            case 200 => {
              val json = Json.obj(
                "request" -> "history",
                "data" -> response.body
              )
              out ! ( json.toString)
            }
          }
        }
    }
    case "tick" => {
      val url = "https://api.bitcoinaverage.com/exchanges/EUR"
      ws.url( url)
        .get()
        .map {
            response => response.status match {
              case 200 => {
                val json = Json.obj(
                  "request" -> "tick",
                  "data" -> response.json
                )
                out ! ( json.toString())
              }
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
