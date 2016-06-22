if (window.console) {
  console.log("Welcome to your Play application's JavaScript!");

  var websocket;

  function init() {
    websocket = new WebSocket( "ws://" + window.location.host + "/ticker");
    websocket.onopen = function(e) { onOpen(e);};
    websocket.onclose = function(e) {onClose(e);};
    websocket.onmessage = function(e) { onMessage(e);};
    websocket.onerror = function(e) { onError(e);};
  }

  function onOpen(event) {
    console.log( "connected to websocket");
  }

  function onClose(event) {
    console.log( "websocket closed");
  }

  function onError(event) {
    console.log( "error: " + event.data);
  }

  function onMessage(event) {
    var data = JSON.parse( event.data);
    console.log("tick");
    document.getElementById("bid").innerHTML = data.kraken.rates.bid;
  }

  window.addEventListener("load", init, false);
}
