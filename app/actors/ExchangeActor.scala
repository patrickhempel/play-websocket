package actors

import java.text.SimpleDateFormat

import akka.actor.{Actor, ActorRef}
import play.api.libs.ws.WSClient

import scala.collection.immutable.{Queue, HashSet}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import java.util.Date

/**
  * Created by patrickhempel on 24.06.16.
  */
class ExchangeActor(exchange:String)(implicit ws:WSClient) extends Actor {
  protected[this] var watchers: HashSet[ActorRef] = HashSet.empty[ActorRef]

  var maxQueueLenght:Int = 1000

  val simpleDateFormat:SimpleDateFormat = new java.text.SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z")

  val exchangeTick = {
    context.system.scheduler.schedule(Duration.Zero, 15 seconds, self, FetchLatest)(context.system.dispatcher)
  }

 var lastResponse:ExchangeUpdate = null;

  var exchangeHistory: Queue[ExchangeUpdate] = Queue[ExchangeUpdate]()

 def receive = {
   case FetchLatest =>
     val url = "https://api.bitcoinaverage.com/exchanges/EUR"
     ws.url(url)
        .get()
          .map {
            response => response.status match {
              case 200 => {
                val displayName = (response.json \ exchange \ "display_name").validate[String]
                val price = (response.json \ exchange \ "rates" \ "bid").validate[Float]
                val timestamp = (response.json \ "timestamp").validate[String]
                lastResponse = ExchangeUpdate(
                  exchange,
                  displayName.getOrElse(""),
                  price.get,
                  simpleDateFormat.parse( timestamp.get)
                )
                //adding lastResponse to history queue
                if( exchangeHistory.size +1 > maxQueueLenght) exchangeHistory.drop(1)
                exchangeHistory = exchangeHistory :+ lastResponse
                watchers.foreach( _ ! lastResponse)
              }
            }
          }

   case WatchExchange(exchange) =>
      watchers = watchers + sender
      if( !exchangeHistory.isEmpty) {
        sender ! ExchangeHistory( exchange, lastResponse.displayName, List(exchangeHistory: _*))
      }
   case UnwatchExchange(_) =>
     watchers = watchers - sender
     if( watchers.isEmpty) {
       exchangeTick.cancel()
       context.stop(self)
     }
 }

}

case object FetchLatest

case class ExchangeUpdate(exchange:String, displayName:String, price:Number, timestamp:Date)

case class WatchExchange(exchange:String)

case class UnwatchExchange(exchange:Option[String])

case class ExchangeHistory(exchange:String, displayName:String, history:List[ExchangeUpdate])
