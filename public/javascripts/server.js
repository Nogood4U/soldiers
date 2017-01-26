/**
 * Created by Sergio on 1/23/2017.
 */

function server(server, playerId) {
    let loc = window.location, new_uri;
    if (loc.protocol === "https:") {
        new_uri = "wss:";
    } else {
        new_uri = "ws:";
    }
    new_uri += "//" + loc.host;
    var ws = new WebSocket(new_uri +"/ws/" + server + "/" + playerId);


    return {
        stream: Rx.Observable.create(function (observer) {
            ws.onopen = function () {
                // Web Socket is connected, send data using send()
            };

            ws.onmessage = function (evt) {
                observer.next(JSON.parse(evt.data));
            };

            ws.onclose = function () {
                // websocket is closed.
                alert("Connection is closed...");
            };
        }),
        ws: ws
    }
}
