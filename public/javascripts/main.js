$(document).ready( function($) {

    var app = new Marionette.Application();

    var vent = new Backbone.Wreqr.EventAggregator();

    //Models
    var ExchangeUpdate = Backbone.Model.extend({});
    var ExchangeHistory = Backbone.Model.extend({
        defaults: {
            type: 'ExchangeHistory',
        },
    });

    var WatchExchange = Backbone.Model.extend({
        defaults: {
            type: 'WatchExchange'
        }
    });
    var UnwatchExchange = Backbone.Model.extend({
        defaults: {
            type: 'UnwatchExchange'
        }
    });

    /** Collections **/
    var ExchangeList = Backbone.Collection.extend({

    });


    /** VIEWS **/
    var RootView = Marionette.LayoutView.extend({
                el: '#root',
                template: '#rootTemplate',
                regions: {
                    chart: '#chart',
                    subscribe: '#subscribe'
                }
            });

    app.rootView = new RootView();


    //ChartView
    var ChartView = Marionette.ItemView.extend({
        initialize: function() {
            //start listening on the vent
            vent.on('ExchangeUpdate', this.onExchangeUpdate, this);
            vent.on('UnwatchExchange', this.onUnwatchExchange, this);
            vent.on('ExchangeHistory', this.onExchangeHistory, this);
        },
        template: "#chart",
        onExchangeUpdate: function( exchangeUpdate) {
            //search for the series of exchange
            var series = this.chart.get( exchangeUpdate.get('exchange'));

            if( series === null) {
                //create new series
                this.chart.addSeries({
                    id: exchangeUpdate.get('exchange'),
                    name: exchangeUpdate.get('displayName'),
                    data: [
                        [exchangeUpdate.get('timestamp'),
                        exchangeUpdate.get('bid')]
                    ]
                });
            } else {
                //adding new point to series
                var point = [
                    exchangeUpdate.get('timestamp'),
                    exchangeUpdate.get('bid')
                ];
                series.addPoint(point, true, false, false);
            }

        },
        onUnwatchExchange: function( unwatchExchange) {
            var series = this.chart.get( unwatchExchange.get('exchange'));

            if( series !== null) {
                series.remove(true);
            }
        },
        onExchangeHistory: function( exchangeHistory) {
            var series = this.chart.get( exchangeHistory.get('exchange'));

            if( series === null) {
                //create new series
                this.chart.addSeries({
                    id: exchangeHistory.get('exchange'),
                    name: exchangeHistory.get('displayName'),
                    data:
                        _.map( exchangeHistory.get('history'), function( item) {
                            return [
                                item['timestamp'],
                                item['bid']
                            ];
                        })

                });
              }
        },
        onRender: function() {
            //setup chart
            this.chart = new Highcharts.Chart({
                chart: {
                    renderTo: 'chart',
                    zoomType: 'x'
                },
                title: {
                    text: 'Bitcoin'
                },
                xAxis: {
                   type: 'datetime'
                },
                yAxis: {
                   title: {
                       text: 'EUR'
                   }
                }
             });
        }
    });

    // Sub/Unsub View
    var SubUnsubView = Backbone.Marionette.CollectionView.extend({
        childView: Marionette.ItemView.extend({
            tagName: 'a',
            attributes: {
                'href': '#'
            },
            className: function() {
                var classes = ['list-group-item']

                if( this.model.get('watched')) {
                    classes.push( 'active');
                }

                return classes.join(' ');
            },
            template: _.template("<%= displayName %>"),
            ui: {
                anchor: 'a'
            },
            events: {
                'click': 'onClick'
            },
            onClick: function() {
                this.trigger("exchange:clicked");
            }
        }),
        collection: new Backbone.Collection(),
        tagName: "div",
        className: "list-group",
        initialize: function() {
            vent.on("ExchangeList", this.onExchangeList, this);
        },
        template: '#subunsubTemplate',
        childEvents: {
            'exchange:clicked': 'onClick'
        },
        onClick: function( childView) {
            var exchange = childView.model.get('name');
            var checked = childView.$el.hasClass('active');

            if( !checked) {
                vent.trigger("WatchExchange", new WatchExchange({ exchange: exchange}));
            } else {
                vent.trigger("UnwatchExchange", new UnwatchExchange({ exchange: exchange}));
            }

            childView.$el.toggleClass('active');

        },
        onExchangeList: function( exchangeList) {
            this.collection.add( exchangeList);
        }
    });

    /*
     Simple wrapper to hold a WebSocket
     */
    var BackendSocket = function() {
        var url = "ws://" + window.location.host + "/ticker";

        this.connect = function() {

            console.log("Trying to connect to Backend via Websocket");

            var socket = new WebSocket( url);

            socket.onopen = function( event) {
                vent.on("WatchExchange UnwatchExchange", function( event) {
                    socket.send( JSON.stringify(event.toJSON()));
                });

                vent.trigger("SocketOpen");
            };

            socket.onclose = function( event) {
            };

            socket.onmessage = function( event) {
                var response = JSON.parse( event.data);

                switch(response.type) {
                    case "ExchangeUpdate":
                        vent.trigger("ExchangeUpdate", new ExchangeUpdate( response));
                    break;
                    case "ExchangeHistory":
                        var exchangeHistory = new ExchangeHistory( response);
                        vent.trigger("ExchangeHistory", exchangeHistory);
                    break;
                    case "ExchangeList":
                        vent.trigger("ExchangeList", response.exchanges);
                    break;
                }
            };

            socket.onerror = function( event) {

            };

            return socket;
        }
    };

    app.on('start', function() {
        app.rootView.render();
        Backbone.history.start();

        app.rootView.getRegion("chart").show( new ChartView());
        app.rootView.getRegion('subscribe').show( new SubUnsubView());

        var backendSocket = new BackendSocket();
        backendSocket.connect();
    });

    vent.on("SocketOpen", function() {
    });

    app.start();
});

