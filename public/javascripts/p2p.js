/**
 * Created by Sergio on 1/31/2017.
 */

$(() => {
    let mediaConstraints = {
        audio: true, // We want an audio track
        video: true // ...and we want a video track
    };
    let createPeerConnection = () => {
        return new RTCPeerConnection({
            iceServers: [     // Information about ICE servers - Use your own!
                {
                    urls: "stun:stun.l.google.com:19302" // A TURN server
                }
            ]
        });
    };

    let sendOffer = (peerCon) => {
        return navigator.mediaDevices.getUserMedia(mediaConstraints)
            .then(function (localStream) {
                document.getElementById("local_video").srcObject = localStream;
                peerCon.addStream(localStream);
            });
        /*
         .catch((e) => {
         switch (e.name) {
         case "NotFoundError":
         alert("Unable to open your call because no camera and/or microphone" +
         "were found.");
         break;
         case "SecurityError":
         case "PermissionDeniedError":
         // Do nothing; this is the same as the user canceling the call.
         break;
         default:
         alert("Error opening your camera and/or microphone: " + e.message);
         break;
         }
         //close connection here
         });*/
    };

    let handleOffer = (peerConn, msg, wsConn) => {
        //var localStream = null;
        let targetUsername = msg.enemyId;
        //createPeerConnection();
        let desc = new RTCSessionDescription(msg.sdp);
        peerConn.setRemoteDescription(desc).then(function () {
            peerConn.remoteSet = true;
            return navigator.mediaDevices.getUserMedia(mediaConstraints);
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
                target: targetUsername,
                messageType: "answer",
                sdp: peerConn.localDescription,
                candidate: ""
            };
            wsConn.send(JSON.stringify(msg));

        }).catch(console.log);
    };

    let handleAnswer = (peerConn, msg) => {
        console.log("Call recipient has accepted our call", msg.enemyId);
        // Configure the remote description, which is the SDP payload
        // in our "video-answer" message.
        let desc = new RTCSessionDescription(msg.sdp);
        peerConn.setRemoteDescription(desc).then(() => {
            peerConn.remoteSet = true;
        }).catch(console.log);
    };

    let handleIce = (peerConn, msg) => {
        //   if (peerConn.remoteSet) {
        let candidate = new RTCIceCandidate(msg.candidate);
        console.log("Remote Set", peerConn.remoteSet);
        console.log("Adding received ICE candidate: " + JSON.stringify(candidate), msg.enemyId);
        peerConn.addIceCandidate(candidate)
            .catch(console.log);
        //  }
    };

    let new_uri;
    if (location.protocol === "https:") {
        new_uri = "wss:";
    } else {
        new_uri = "ws:";
    }
/////////////////////////////////////////
    let connections = {};
    let offering = false;
    let MyPeerCon = createPeerConnection(); //active client peerConnection if i ask them..
    new_uri += "//" + location.host;
    let wsUrl = new_uri + "/p2p/ws/" + playerId;
    let ws = new WebSocket(wsUrl);
    ws.onmessage = function (evt) {

        let data = JSON.parse(evt.data);
        let peerCon;

        if (!connections[data.enemyId]) {
            peerCon = createPeerConnection(); //passive peerConnection ,if they ask me..
            connections[data.enemyId] = peerCon;
            addConnectionEventHandlers(peerCon, ws, data.enemyId, true);
        } else {
            peerCon = connections[data.enemyId];
        }

        switch (data.messageType) {
            case "answer":
                handleAnswer(MyPeerCon, data);
                break;
            case "offer":
                handleOffer(peerCon, data, ws);
                break;
            case "ice":
                handleIce(peerCon, data);//can be both
                if (offering /*&& MyPeerCon.remoteSet*/)
                    handleIce(MyPeerCon, data);//can be both
                break;
        }
    };
    $("#call").click(() => {
        offering = true;
        sendOffer(MyPeerCon).then(() => {
            addConnectionEventHandlers(MyPeerCon, ws, "", false);// added event handlers
        });
    });

});

function addConnectionEventHandlers(peerConn, wsConn, target, client) {
    if (!client)
        peerConn.onnegotiationneeded = () => {
            peerConn.createOffer().then(function (offer) {
                console.log("---> Creating new description object to send to remote peer", target);
                return peerConn.setLocalDescription(offer);
            }).then(function () {
                console.log("---> Sending offer to remote peer", target);
                wsConn.send(JSON.stringify({
                    messageType: "offer",
                    enemyId: target,
                    sdp: peerConn.localDescription,
                    candidate: ""
                }));
            }).catch(console.log);
        };

    peerConn.onicecandidate = (event) => {
        if (event.candidate) {
            wsConn.send(JSON.stringify({
                messageType: "ice",
                enemyId: target,
                sdp: "",
                candidate: event.candidate
            }));
        }
    };

    peerConn.onaddstream = (event) => {
        console.log("*** Stream added");
        document.getElementById("received_video").srcObject = event.stream;
        // document.getElementById("hangup-button").disabled = false;
    };
}