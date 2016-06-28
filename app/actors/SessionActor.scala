package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import com.google.inject.name.Named
import play.api.libs.concurrent.InjectedActorSupport
import play.api.libs.json._
import play.api.libs.ws.WSClient

import scala.collection.mutable.ListBuffer

/**
  * Created by patrickhempel on 24.06.16.
  */
class SessionActor @Inject()(@Assisted out:ActorRef,
                             @Named("exchangesActor") exchangesActor: ActorRef, ws:WSClient) extends Actor with ActorLogging {

  implicit val ExchangeUpdateWrites = new Writes[ExchangeUpdate] {
    def writes(exchangeUpdate: ExchangeUpdate) = Json.obj(
      "type" -> "ExchangeUpdate",
      "exchange" -> exchangeUpdate.exchange,
      "displayName" -> exchangeUpdate.displayName,
      "bid" -> exchangeUpdate.price.doubleValue(),
      "timestamp" -> exchangeUpdate.timestamp
    )
  }

  //special actor fetching the available exchanges
  val fetchListActor:ActorRef = context.actorOf( Props( new ListExchangesActor()(ws)), "fetchlist")

  //holds all exchanges
  var watchedExchanges = new ListBuffer[String]()

  override def preStart(): Unit = {
    super.preStart()

    configureDefaultExchanges()

    fetchListActor ! FetchList
  }

  def configureDefaultExchanges(): Unit = {
    val defaultExchanges = List("kraken", "bitstamp", "bitbay")
    watchedExchanges = watchedExchanges ++ defaultExchanges

    for( exchange <- defaultExchanges) {
      exchangesActor ! WatchExchange( exchange)
    }
  }


  override def receive = LoggingReceive {
    case ExchangeUpdate( exchange, displayName, price, date) =>
      val message = Json.obj(
        "type" -> "ExchangeUpdate",
        "exchange" -> exchange,
        "displayName" -> displayName,
        "bid" -> price.doubleValue(),
        "timestamp" -> date)
      out ! message
    case ExchangeHistory( exchange, displayName, history) =>
      val message = Json.obj(
        "type" -> "ExchangeHistory",
        "exchange" -> exchange,
        "displayName" -> displayName,
        "history" -> history
      )
      out ! message
    case ExchangeList( list) =>
      //mark all watched exchanges
      list.filter( exc => watchedExchanges.contains( exc.name)).foreach( _.watched = true)
      out ! Json.obj( "type" -> "ExchangeList", "exchanges" -> list)
    case json: JsValue =>
      val method = (json \ "type").as[String]
      method match {
        case "WatchExchange" =>
          val exchange = (json \ "exchange").as[String]
          watchedExchanges += exchange
          exchangesActor ! WatchExchange( exchange)
        case "UnwatchExchange" =>
          val exchange = (json \ "exchange").as[String]
          watchedExchanges -= exchange
          exchangesActor ! UnwatchExchange( Some(exchange))
      }
  }
}

class SessionParentActor @Inject()(childFactory: SessionActor.Factory) extends Actor with InjectedActorSupport {
  import SessionParentActor._

  override def receive: Receive = {
    case Create(id, out) =>
      val child: ActorRef = injectedChild( childFactory(out), s"sessionActor-$id")
      sender() ! child
  }
}

object SessionParentActor {
  case class Create(id: String, out: ActorRef)
}


object SessionActor {
  trait Factory {
    def apply(out:ActorRef): Actor
  }
}