package controllers

import javax.inject._

import actors.{SessionParentActor, UnwatchExchange}
import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.stream.scaladsl._
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.Timeout
import org.reactivestreams.Publisher
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(@Named("exchangesActor") exchangesActor: ActorRef,
                               @Named("sessionParentActor") sessionParentAcotor:ActorRef)
    (                          implicit actorSystem: ActorSystem,
                               materializer: Materializer,
                               ec: ExecutionContext, ws: WSClient) extends Controller {

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = Action {
    Ok(views.html.index())
  }


  def ticker = WebSocket.acceptOrResult[JsValue,JsValue] {
    case rh =>
      wsFutureFlow(rh).map {
        flow => Right(flow)
      }.recover {
        case e: Exception => Left( InternalServerError)
      }
  }


  def wsFutureFlow( request: RequestHeader): Future[Flow[JsValue, JsValue, NotUsed]] = {
    val (webSocketOut: ActorRef, webSocketIn: Publisher[JsValue]) = createWebSocketConnections()

    val userActorFuture = createSessionActor(request.id.toString, webSocketOut)

    userActorFuture.map { userActor =>
      createWebSocketFlow(webSocketIn, userActor)
    }
  }

  def createWebSocketConnections(): (ActorRef, Publisher[JsValue]) = {
    val source: Source[JsValue, ActorRef] = {
      Source.actorRef[JsValue](10, OverflowStrategy.dropTail).log("actorRefSource")
    }

    val sink: Sink[JsValue, Publisher[JsValue]] = Sink.asPublisher( fanout = false)

    source.toMat(sink)(Keep.both).run()
  }


  def createWebSocketFlow(webSocketIn: Publisher[JsValue], sessionActor: ActorRef): Flow[JsValue, JsValue, NotUsed] = {
    val flow = {
      val sink = Sink.actorRef(sessionActor, akka.actor.Status.Success(()))
      val source = Source.fromPublisher(webSocketIn)
      Flow.fromSinkAndSource(sink, source)
    }

    val flowWatch: Flow[JsValue, JsValue, NotUsed] = flow.watchTermination() { (_, termination) =>
      termination.foreach { done =>
        exchangesActor.tell( UnwatchExchange(None), sessionActor)
        actorSystem.stop( sessionActor)
      }
      NotUsed
    }

    flowWatch
  }


  def createSessionActor(name:String, webSocketOut: ActorRef): Future[ActorRef] = {
    val userActorFuture = {
      implicit val timeout = Timeout(100.millis)
      ( sessionParentAcotor ? SessionParentActor.Create( name, webSocketOut)).mapTo[ActorRef]
    }

    userActorFuture
  }
}
