if (window.console) {
  console.log("Welcome to your Play application's JavaScript!");

  var websocket;
  var labels = [], rows = [], chartData = [], graph, chart;



  function init() {
    websocket = new WebSocket( "ws://" + window.location.host + "/ticker");
    websocket.onopen = function(e) { onOpen(e);};
    websocket.onclose = function(e) {onClose(e);};
    websocket.onmessage = function(e) { onMessage(e);};
    websocket.onerror = function(e) { onError(e);};

    chart = new Highcharts.Chart({
        chart: {
            zoomType: 'x',
            renderTo: 'graph',
            type: 'line'
        },
        title: {
            text: 'BTC to EUR'
        },
        xAxis: {
            type: 'datetime'
        },
        yAxis: {
            title: {
                text: '1 BTC to Euro'
            }
        },
        series: [{
            type: 'area',
            name: 'BTC to EUR',
            data: []
        }]
    });
  }

  function onOpen(event) {
    console.log( "connected to websocket");
//    websocket.send("history");
  }

  function onClose(event) {
    console.log( "websocket closed");
  }

  function onError(event) {
    console.log( "error: " + event.data);
  }

  function onMessage(event) {
    var response = JSON.parse( event.data);

    document.write( JSON.stringify(response) + "</br>");

    switch(response.request) {
        case "tick":
            document.getElementById("bid").innerHTML = response.data.timestamp + " " + response.data.kraken.rates.bid;
//            document.write(response.data.kraken.rates.bid + "</br>");
            var x = moment(response.data.timestamp).valueOf();
            var y = parseFloat(response.data.kraken.rates.bid);
            chart.series[0].addPoint([x, y], true, false, true);
            break;
        case "history":
//            var data = response.data;
//
//            rows = response.data.split('\n');
//            rows.shift();
//            for( var i = 0; i < rows.length; i++) {
//                var cols = rows[i].split(',');
//                var x = moment(cols[0]).valueOf();
//                var y = parseFloat(cols[1]);
//                rows[i] = [x,y];
////                chart.series[0].addPoint([x,y], false);
//            }
//            chart.series[0].setData( rows);
//
//            chart.redraw()
            break;
    }

  }

  window.addEventListener("load", init, false);
}
