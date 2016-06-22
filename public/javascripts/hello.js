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
    var response = JSON.parse( event.data);

    switch(response.request) {
        case "tick":
            document.getElementById("bid").innerHTML = response.data.kraken.rates.bid;
            break;
        case "history":
            console.log( response.data);
            break;
    }

  }

  window.addEventListener("load", init, false);
}
