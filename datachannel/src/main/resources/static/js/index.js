/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

var ws = new WebSocket('wss://' + location.host + '/showdatachannel');
var videoInput;
var videoOutput;
var webRtcPeer;
var state = null;
var channel;

const I_CAN_START = 0;
const I_CAN_STOP = 1;
const I_AM_STARTING = 2;

var chanId = 0;

function getChannelName () {
  return "TestChannel" + chanId++;
}

window.onload = function() {
	console = new Console();
	console.log("Page loaded ...");
	videoInput = document.getElementById('videoInput');
	videoOutput = document.getElementById('videoOutput');
	setState(I_CAN_START);
}

window.onbeforeunload = function() {
	ws.close();
}

ws.onmessage = function(message) {
	var parsedMessage = JSON.parse(message.data);
	console.info('Received message: ' + message.data);

	switch (parsedMessage.id) {
	case 'startResponse':
		startResponse(parsedMessage);
		break;
	case 'error':
		if (state == I_AM_STARTING) {
			setState(I_CAN_START);
		}
		onError("Error message from server: " + parsedMessage.message);
		break;
	case 'iceCandidate':
		webRtcPeer.addIceCandidate(parsedMessage.candidate, function(error) {
			if (error) {
				console.error("Error adding candidate: " + error);
				return;
			}
		});
		break;
	default:
		if (state == I_AM_STARTING) {
			setState(I_CAN_START);
		}
		onError('Unrecognized message', parsedMessage);
	}
}


function getRandomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

var timerId=null;
function sendmsdata()
{
        if(!channel) return;

var d = new Date();
var n = d.getTime();
var n = getRandomInt(-20, 30);
        channel.send(n);
}

function sendmsdatatimer()
{
    if ( document.getElementById('videoE2Elatency').checked) {
	timerId = setInterval(sendmsdata,1000);
    }
    else {
	clearInterval(timerId);
    }

}


function start() {
	console.log("Starting video call ...")
	// Disable start button
	setState(I_AM_STARTING);
	showSpinner(videoInput, videoOutput);

	var servers = null;
    var configuration = null;
    var peerConnection = new RTCPeerConnection(servers, configuration);

    console.log("Creating channel");
    var dataConstraints = null;

    channel = peerConnection.createDataChannel(getChannelName (), dataConstraints);

    channel.onopen = onSendChannelStateChange;
    channel.onclose = onSendChannelStateChange;

    function onSendChannelStateChange(){
        if(!channel) return;
        var readyState = channel.readyState;
        console.log("sencChannel state changed to " + readyState);
	alert("Channel:" + channel);	
        if(readyState == 'open'){
          dataChannelSend.disabled = false;
          dataChannelSend.focus();
          $('#send').attr('disabled', false);
        } else {
          dataChannelSend.disabled = true;
          $('#send').attr('disabled', true);
        }
      }
    
    var sendButton = document.getElementById('send');
    var dataChannelSend = document.getElementById('dataChannelSend');

    sendButton.addEventListener("click", function(){
        var data = dataChannelSend.value;
        console.log("Send button pressed. Sending data " + data);
        channel.send(data);
        dataChannelSend.value = "";
      });
    
	console.log("Creating WebRtcPeer and generating local sdp offer ...");

	var options = {
		peerConnection: peerConnection,
		localVideo : videoInput,
		remoteVideo : videoOutput,
		onicecandidate : onIceCandidate
	}
	webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options,
			function(error) {
				if (error) {
					return console.error(error);
				}
				webRtcPeer.generateOffer(onOffer);
			});
}

function closeChannels(){

	if(channel){
	  channel.close();
	  $('#dataChannelSend').disabled = true;
	  $('#send').attr('disabled', true);
	  channel = null;
	}
}

function onOffer(error, offerSdp) {
	if (error)
		return console.error("Error generating the offer");
	console.info('Invoking SDP offer callback function ' + location.host);
	var message = {
		id : 'start',
		sdpOffer : offerSdp
	}
	sendMessage(message);
}

function onError(error) {
	console.error(error);
}

function onIceCandidate(candidate) {
	console.log("Local candidate" + JSON.stringify(candidate));

	var message = {
		id : 'onIceCandidate',
		candidate : candidate
	};
	sendMessage(message);
}

function startResponse(message) {
	setState(I_CAN_STOP);
	console.log("SDP answer received from server. Processing ...");

	webRtcPeer.processAnswer(message.sdpAnswer, function(error) {
		if (error)
			return console.error(error);
	});
}

function stop() {
	console.log("Stopping video call ...");
	setState(I_CAN_START);
	if (webRtcPeer) {
	    closeChannels();
	    
		webRtcPeer.dispose();
		webRtcPeer = null;

		var message = {
			id : 'stop'
		}
		sendMessage(message);
	}
	hideSpinner(videoInput, videoOutput);
}

function setState(nextState) {
	switch (nextState) {
	case I_CAN_START:
		$('#start').attr('disabled', false);
		$("#start").attr('onclick', 'start()');
		$('#stop').attr('disabled', true);
		$("#stop").removeAttr('onclick');
		break;

	case I_CAN_STOP:
		$('#start').attr('disabled', true);
		$('#stop').attr('disabled', false);
		$("#stop").attr('onclick', 'stop()');
		break;

	case I_AM_STARTING:
		$('#start').attr('disabled', true);
		$("#start").removeAttr('onclick');
		$('#stop').attr('disabled', true);
		$("#stop").removeAttr('onclick');
		break;

	default:
		onError("Unknown state " + nextState);
		return;
	}
	state = nextState;
}

function sendMessage(message) {
	var jsonMessage = JSON.stringify(message);
	console.log('Senging message: ' + jsonMessage);
	ws.send(jsonMessage);
}

function showSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = './img/transparent-1px.png';
		arguments[i].style.background = "center transparent url('./img/spinner.gif') no-repeat";
	}
}

function hideSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].src = '';
		arguments[i].poster = './img/webrtc.png';
		arguments[i].style.background = '';
	}
}
/**
 * Lightbox utility (to display media pipeline image in a modal dialog)
 */
$(document).delegate('*[data-toggle="lightbox"]', 'click', function(event) {
	event.preventDefault();
	$(this).ekkoLightbox();
});
