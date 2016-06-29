package actors

import akka.actor.{Actor, ActorLogging}
import play.api.libs.json.Writes
import play.api.libs.ws.WSClient
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by patrickhempel on 27.06.16.
  */
class ListExchangesActor()(implicit ws:WSClient) extends Actor with ActorLogging {



  override def receive: Receive = {
    case FetchList =>
      val url = "https://api.bitcoinaverage.com/exchanges/EUR"
      ws.url(url)
        .get()
        .map {
          response => response.status match {
            case 200 => {
              //cast response.json to JsObject, remove the last key, and make it a list
              val jsonObject = response.json.as[JsObject]
              val keys = jsonObject.keys.dropRight(1).toList

              val exchanges = keys.map( k => {
                Exchange(
                  k,
                  (response.json \ k \ "display_name").as[String]
                )
              }).toList

              context.parent ! ExchangeList(exchanges)
              //this actor has successfully done its single purpose. stop it, shall we?
              context.stop(self)
            }
          }
        }
  }
}

case object FetchList
case class ExchangeList( list:List[Exchange])

case object Exchange {
  implicit val ExchangeReads: Reads[Exchange] = (
      (JsPath \ "display_name").read[String] and
      (JsPath \ "display_name").read[String] and
      (JsPath \ "watched").read[Boolean]
    )(Exchange.apply _)

  implicit val ExchangeWrites: Writes[Exchange] = (
      (__ \ "name").write[String] and
      (__ \ "displayName").write[String] and
      (__ \ "watched").write[Boolean]
    )(unlift(Exchange.unapply))
}

case class Exchange( name:String, displayName:String, var watched:Boolean = false)