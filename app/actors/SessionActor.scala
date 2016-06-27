package actors

import akka.actor.{Actor, ActorRef}
import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import com.google.inject.name.Named
import play.api.libs.concurrent.InjectedActorSupport
import play.api.libs.json.{JsValue, Json}

/**
  * Created by patrickhempel on 24.06.16.
  */
class SessionActor @Inject()(@Assisted out:ActorRef,
                             @Named("exchangesActor") exchangesActor: ActorRef) extends Actor {

  override def preStart(): Unit = {
    super.preStart()

    configureDefaultExchanges()
  }

  def configureDefaultExchanges(): Unit = {
      val defaultExchanges = List("kraken", "bitstamp", "bitcoin_de")

    for( exchange <- defaultExchanges) {
      exchangesActor ! WatchExchange( exchange)
    }
  }


  override def receive: Receive = {
    case ExchangeUpdate( exchange, price, date) =>
      val message = Json.obj(
        "type" -> "ExchangeUpdate",
        "exchange" -> exchange,
        "bid" -> price.doubleValue(),
        "timestamp" -> date)
      out ! message
    case json: JsValue =>
      val method = (json \ "type").as[String]
      method match {
        case "WatchExchange" =>
          val exchange = (json \ "exchange").as[String]
          exchangesActor ! WatchExchange( exchange)
        case "UnwatchExchange" =>
          val exchange = (json \ "exchange").as[String]
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