/**
 * Created by Sergio on 1/23/2017.
 */

function server(server, playerId) {
    let loc = window.location, new_uri;
    let gameId = document.getElementById("gameId").value
    if (loc.protocol === "https:") {
        new_uri = "wss:";
    } else {
        new_uri = "ws:";
    }
    new_uri += "//" + loc.host;
    let getCall = $.get(loc.protocol + "//" + loc.host + "/joinGame/" + server + "/" + playerId + "/" + gameId);
    getCall.fail(function (data) {
        alert("Could not Create Player..Try a different Name :)..");
        location.reload();
    });
    return okPromise = getCall.then(function () {
        let wsUrl = new_uri + "/ws/" + server + "/" + playerId + "?gameId=" + gameId;
        let ws = new WebSocket(wsUrl);
        ws.binaryType = 'arraybuffer';
        let obj = {};
        return protobuf.load("/assets/proto/state.proto").then(root => {
            console.log("loadedProto");
            return root.lookup("game.core.State");
        }).then(State => {
            return {
                stream: Rx.Observable.create(function (observer) {
                    ws.onopen = function () {
                        // Web Socket is connected, send data using send()
                    };
                    ws.onmessage = function (evt) {
                        let msg = State.decode(new Uint8Array(evt.data));
                        observer.next(msg);
                    };
                    ws.onclose = function () {
                        // websocket is closed.
                        alert("Connection lost with the Server , page will reload...");
                        location.reload();
                    };
                    ws.sendX = function (msg) {
                        ws.send(str2ab(msg));
                    }
                }),
                ws: ws,
                stateBuilder: State
            };
        });
    });

}
function str2ab(str) {
    let buf = new ArrayBuffer(str.length * 2); // 2 bytes for each char
    let bufView = new Uint16Array(buf);
    for (let i = 0, strLen = str.length; i < strLen; i++) {
        bufView[i] = str.charCodeAt(i);
    }
    return buf;
}