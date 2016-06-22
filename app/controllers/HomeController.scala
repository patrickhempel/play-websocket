package controllers

import javax.inject._

import actors.BitcoinTickerActor
import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.libs.streams.ActorFlow
import play.api.libs.ws.WSClient
import play.api.mvc._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(implicit system: ActorSystem, materializer: Materializer, ws: WSClient) extends Controller {

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = Action {
    Ok(views.html.index())
  }

  def ticker = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef(out => BitcoinTickerActor.props(out, ws))
  }
}
