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

package org.kurento.tutorial.metadata;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.OnIceCandidateEvent;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.client.*;
import org.kurento.client.Stats;
import java.util.Map;
import java.util.Set;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.module.datachannelexample.*;
import org.kurento.module.datachannelexample.KmsDetectFaces;
import org.kurento.module.datachannelexample.KmsShowFaces;
import java.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Magic Mirror handler (application and media logic).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @since 6.0.0
 */
public class MetadataHandler extends TextWebSocketHandler {

  private final Logger log = LoggerFactory.getLogger(MetadataHandler.class);
  private static final Gson gson = new GsonBuilder().create();

  private final ConcurrentHashMap<String, UserSession> users = new ConcurrentHashMap<>();

    private PrintWriter out;
  @Autowired
  private KurentoClient kurento;

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);



    log.debug("Incoming message: {}", jsonMessage);

    switch (jsonMessage.get("id").getAsString()) {
		case "get_stats":			
			getStats(session);
			break;
      case "start":
        start(session, jsonMessage);
        break;
      case "stop": {
        UserSession user = users.remove(session.getId());
        if (user != null) {
          user.release();
        }
        break;
      }
      case "tester": {
	  System.err.println("CHANGE TESTER");
	  changeTester(session, jsonMessage);
      }
      case "onIceCandidate": {
        JsonObject jsonCandidate = jsonMessage.get("candidate").getAsJsonObject();

        UserSession user = users.get(session.getId());
        if (user != null) {
          IceCandidate candidate = new IceCandidate(jsonCandidate.get("candidate").getAsString(),
              jsonCandidate.get("sdpMid").getAsString(),
              jsonCandidate.get("sdpMLineIndex").getAsInt());
          user.addCandidate(candidate);
        }
        break;
      }
      default:
	  System.err.println("INVALID");
        sendError(session, "Invalid message with id " + jsonMessage.get("id").getAsString());
        break;
    }
  }

    private void changeTester(final WebSocketSession session, JsonObject jsonMessage) {
        UserSession user = users.get(session.getId());
        if (user != null) {
	    System.err.println("\n\n\n\n\n\n\nSTOP A");
	    users.remove(session.getId());
	    System.err.println("STOP B");
	    user.release();
	    System.err.println("START");
	    start(session, jsonMessage);	    
	}
    }

  private void start(final WebSocketSession session, JsonObject jsonMessage) {
    try {
      // User session
      UserSession user = new UserSession();
      MediaPipeline pipeline = kurento.createMediaPipeline();
      pipeline.setLatencyStats(true);
      user.setMediaPipeline(pipeline);
      WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
      user.setWebRtcEndpoint(webRtcEndpoint);
      users.put(session.getId(), user);

      // ICE candidates
      webRtcEndpoint.addOnIceCandidateListener(new EventListener<OnIceCandidateEvent>() {
        @Override
        public void onEvent(OnIceCandidateEvent event) {
          JsonObject response = new JsonObject();
          response.addProperty("id", "iceCandidate");
          response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
          try {
            synchronized (session) {
              session.sendMessage(new TextMessage(response.toString()));
            }
          } catch (IOException e) {
            log.debug(e.getMessage());
          }
        }
      });

      //Vol 1
      
      // Media logic
      KmsShowFaces showFaces = new KmsShowFaces.Builder(pipeline).build();

      System.err.println("\n\n\nGOTTA: " + jsonMessage.get("tester").getAsString());
      String tester = jsonMessage.get("tester").getAsString();

      if("true".equals(tester)){
	  System.err.println("\n\n\n\nGO CHARTER");
	  KmsCharter detectFaces = new KmsCharter.Builder(pipeline).build();
      
	  webRtcEndpoint.connect(detectFaces);
	  detectFaces.connect(showFaces);
      }
      else{
	  System.err.println("\n\n\n\nGO FACES");
	  KmsDetectFaces detectFaces = new KmsDetectFaces.Builder(pipeline).build();
      
	  webRtcEndpoint.connect(detectFaces);
	  detectFaces.connect(showFaces);
      }
      

      showFaces.connect(webRtcEndpoint);

      // SDP negotiation (offer and answer)
      String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
      String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

      JsonObject response = new JsonObject();
      response.addProperty("id", "startResponse");
      response.addProperty("sdpAnswer", sdpAnswer);

      synchronized (session) {
        session.sendMessage(new TextMessage(response.toString()));
      }

      webRtcEndpoint.gatherCandidates();
			out = new PrintWriter("smart.txt");
    } catch (Throwable t) {


      sendError(session, t.getMessage());

StringWriter sw = new StringWriter();
PrintWriter pw = new PrintWriter(sw);
t.printStackTrace(pw);
//sw.toString();

System.err.println(sw.toString());

        UserSession user = users.remove(session.getId());
        if (user != null) {

          user.release();
        }
    }
  }

  private void sendError(WebSocketSession session, String message) {
    try {
      JsonObject response = new JsonObject();
      response.addProperty("id", "error");
      response.addProperty("message", message);
      session.sendMessage(new TextMessage(response.toString()));
    } catch (IOException e) {
      log.error("Exception sending message", e);
    }
  }

    private void smart(String msg, double time){
	out.println(msg + (int)time);
	out.flush();
	System.err.println(msg + "#" + time);
    }

    private void getStats(WebSocketSession session){    	
    	try {
	    UserSession user = users.get(session.getId());
	    if (user == null) {
		return;
	    }
	    WebRtcEndpoint webRtcEndpoint  = user.getWebRtcEndpoint();
	    Map<String,Stats> wr_stats= webRtcEndpoint.getStats();
	    //System.err.println("GET STATS..." + wr_stats);
	    for (Stats s :  wr_stats.values()) {
		//System.err.println("STATS:" + s);    		
		switch (s.getType()) {		
		case endpoint:{
		    //System.err.println("STATS endpoint");
		    EndpointStats end_stats= (EndpointStats) s;
		    double  e2eVideLatency= end_stats.getVideoE2ELatency() / 1000000;		    
		    smart("***SMART E2E\t", e2eVideLatency);
		    JsonObject response = new JsonObject();
		    response.addProperty("id", "videoE2Elatency");
		    response.addProperty("message", e2eVideLatency);				
		    
		    synchronized (session) {
			session.sendMessage(new TextMessage(response.toString()));				
		    }
		}
		    break;
		    
		case inboundrtp:{
		    RTCInboundRTPStreamStats stats = (RTCInboundRTPStreamStats)s;
		    //System.err.println(stats.getJitter());
		}
		    break;
		case outboundrtp:{
		    RTCOutboundRTPStreamStats stats = (RTCOutboundRTPStreamStats)s;
		}
		    break;
		    
		default:	
		    //System.err.println("STATS DEFAULTS: " + s.getType() + "#" + s.getClass());
		    break;
		}				
	    }
	} 
	catch (IOException e) {
	    log.error("Exception sending message", e);
	}	
    }
}
