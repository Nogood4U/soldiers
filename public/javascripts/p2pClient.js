/**
 * Created by Sergio on 2/1/2017.
 */
function makeCall(ws, obs, playerId, target) {
    // var {ws, obs}= createWsObserver(playerId);
    var peerConn = createPeerConnection();// my connection3
    var remoteConn = createPeerConnection();// my connection
    obs.subscribe((message) => {
        switch (data.messageType) {
            case "offer":
                handleOffer(remoteConn, data, ws);
            case "ice":
                handleIce(peerConn, data);//can be both*/

                break;
        }
    });
    peerConn.onnegotiationneeded = () => {
        peerConn.createOffer().then(function (offer) {
            console.log("---> Creating new description object to send to remote peer", target);
            return peerConn.setLocalDescription(offer);
        }).then(function () {
            console.log("---> Sending offer to remote peer", target);
            ws.send(JSON.stringify({
                messageType: "offer",
                enemyId: target,
                sdp: peerConn.localDescription,
                candidate: ""
            }));
        }).catch((w) => console.log("error onnegotiaiton", w));
    };
    peerConn.onicecandidate = (event) => {
        if (event.candidate) {
            ws.send(JSON.stringify({
                messageType: "ice",
                enemyId: target,
                sdp: "",
                candidate: event.candidate
            }));
        }
    };

    return {};
}

function receiveCall(ws, obs, playerId, target) {
    // var {ws, obs}= createWsObserver(playerId);
    var peerConn = createPeerConnection();// my connection3
    var remoteConn = createPeerConnection();// my connection
    obs.subscribe((message) => {
        switch (data.messageType) {
            case "answer":
                handleAnswer(remoteConn, data);
                break;
            case "ice":
                handleIce(peerConn, data);//can be both*/

                break;
        }
    });
    peerConn.onnegotiationneeded = () => {
        peerConn.createOffer().then(function (offer) {
            console.log("---> Creating new description object to send to remote peer", target);
            return peerConn.setLocalDescription(offer);
        }).then(function () {
            console.log("---> Sending offer to remote peer", target);
            ws.send(JSON.stringify({
                messageType: "offer",
                enemyId: target,
                sdp: peerConn.localDescription,
                candidate: ""
            }));
        }).catch((w) => console.log("error onnegotiaiton", w));
    };
    peerConn.onicecandidate = (event) => {
        if (event.candidate) {
            ws.send(JSON.stringify({
                messageType: "ice",
                enemyId: target,
                sdp: "",
                candidate: event.candidate
            }));
        }
    };

    return {};
}

function handleOffer(peerConn, msg, wsConn) {
    //var localStream = null;
    let targetUsername = msg.enemyId;
    //createPeerConnection();
    let desc = new RTCSessionDescription(msg.sdp);
    peerConn.setRemoteDescription(desc).then(function () {
        peerConn.remoteSet = true;
        return navigator.mediaDevices.getUserMedia(mediaConstraints);//connection established , getting the channels
    }).then(function (stream) {
        // localStream = stream;
        document.getElementById("local_video").srcObject = stream;
        return peerConn.addStream(stream);
    }).then(function () {
        return peerConn.createAnswer();
    }).then(function (answer) {
        return peerConn.setLocalDescription(answer);
    }).then(function () {
        let msg = {
            messageType: "answer",
            enemyId: targetUsername,
            sdp: peerConn.localDescription,
            candidate: ""
        };
        wsConn.send(JSON.stringify(msg));

    }).catch((w) => console.log("error handleOffer", w, msg.enemyId));
};


function createWsObserver(playerId) {
    let new_uri;
    if (location.protocol === "https:") {
        new_uri = "wss:";
    } else {
        new_uri = "ws:";
    }
/////////////////////////////////////////
    let connections = {};
    let connectionsIn = {};
    //let MyPeerCon = createPeerConnection(); //active client peerConnection if i ask them..
    new_uri += "//" + location.host;
    let wsUrl = new_uri + "/p2p/ws/" + playerId;
    let ws = new WebSocket(wsUrl);
    var obs = Rx.Observable.create(function (observer) {
        ws.onopen = function () {
            // Web Socket is connected, send data using send()
            //display message call being made
        };
        ws.onmessage = function (evt) {
            observer.next(evt.data);
        };
        ws.onclose = function () {
            // websocket is closed.
            alert("Connection lost with the Server , page will reload...");
            location.reload();
        };
    });

    return {
        ws: ws,
        obs: obs
    };
}


function createPeerConnection() {
    let mediaConstraints = {
        audio: true, // We want an audio track
        /* video: true // ...and we want a video track*/
    };
    return new RTCPeerConnection({
        iceServers: [     // Information about ICE servers - Use your own!
            {
                urls: "stun:stun.l.google.com:19302" // A TURN server
            }
        ]
    });
};


function handleAnswer(peerConn, msg) {
    console.log("Call recipient has accepted our call", msg.enemyId);
    // Configure the remote description, which is the SDP payload
    // in our "video-answer" message.
    let desc = new RTCSessionDescription(msg.sdp);
    peerConn.setRemoteDescription(desc).then(() => {
        peerConn.remoteSet = true;
    }).catch((w) => console.log("error Answer", w, msg.enemyId));
};


function handleIce(peerConn, msg) {
    //   if (peerConn.remoteSet) {
    let candidate = new RTCIceCandidate(msg.candidate);
    console.log("Remote Set", peerConn.remoteSet);
    console.log("Adding received ICE candidate: " + JSON.stringify(candidate), msg.enemyId);
    peerConn.addIceCandidate(candidate)
        .catch((w) => console.log(w, "error handleIce", msg.enemyId));
    //  }
};