package actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.event.LoggingReceive
import com.google.inject.Inject
import play.api.libs.ws.WSClient

/**
  * Created by patrickhempel on 24.06.16.
  */
class ExchangesActor @Inject()(implicit ws:WSClient) extends Actor with ActorLogging {

  def receive = LoggingReceive {
    case watchExchange@WatchExchange(exchange) =>
      context.child(exchange).getOrElse {
        context.actorOf(Props( new ExchangeActor( exchange)), exchange)
      } forward watchExchange
    case unwatchExchange@UnwatchExchange(Some(exchange)) =>
      context.child(exchange).foreach(_.forward(unwatchExchange))
    case unwatchExchange@UnwatchExchange(None) =>
      context.children.foreach(_.forward(unwatchExchange))
  }
}
