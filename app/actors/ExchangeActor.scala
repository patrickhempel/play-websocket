package actors

import java.text.SimpleDateFormat

import akka.actor.{Actor, ActorRef}
import play.api.libs.ws.WSClient

import scala.collection.immutable.HashSet
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import java.util.Date

/**
  * Created by patrickhempel on 24.06.16.
  */
class ExchangeActor(exchange:String)(implicit ws:WSClient) extends Actor {
  protected[this] var watchers: HashSet[ActorRef] = HashSet.empty[ActorRef]

  val simpleDateFormat:SimpleDateFormat = new java.text.SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z")

  val exchangeTick = {
    context.system.scheduler.schedule(Duration.Zero, 15 seconds, self, FetchLatest)(context.system.dispatcher)
  }

 var lastResponse:ExchangeUpdate = null;

 def receive = {
   case FetchLatest =>
     val url = "https://api.bitcoinaverage.com/exchanges/EUR"
     ws.url(url)
        .get()
          .map {
            response => response.status match {
              case 200 => {
                val price = (response.json \ exchange \ "rates" \ "bid").validate[Float]
                val timestamp = (response.json \ "timestamp").validate[String]
                lastResponse = ExchangeUpdate(exchange, price.get, simpleDateFormat.parse( timestamp.get))
                watchers.foreach( _ ! lastResponse)
              }
            }
          }

   case WatchExchange(_) =>
      watchers = watchers + sender
      if( lastResponse != null) sender ! lastResponse
   case UnwatchExchange(_) =>
     watchers = watchers - sender
     if( watchers.isEmpty) {
       exchangeTick.cancel()
       context.stop(self)
     }
 }

}

case object FetchLatest

case class ExchangeUpdate(exchange:String, price:Number, timestamp:Date)

case class WatchExchange(exchange:String)

case class UnwatchExchange(exchange:Option[String])
