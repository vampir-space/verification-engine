package com.example;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class RosbridgeMessage {
  public String op;
  public String id;
  public String service;   // for call_service / service_response
  public String topic;     // for subscribe/publish
  public String type;      // for subscribe
  public Object args;      // for call_service
  public Object msg;       // for publish / service_response.values

  public RosbridgeMessage() {}

  // constructor for call_service
  public RosbridgeMessage(String op, String service, Object args, String id) {
    this.op = op;
    this.service = service;
    this.args = args;
    this.id = id;
  }

  // constructor for subscribe
  public RosbridgeMessage(String op, String topic, String type, String id) {
    this.op = op;
    this.topic = topic;
    this.type = type;
    this.id = id;
  }
}

public class Ros2WebSocketClient {
  public static void main(String[] args) throws Exception {
    String url = "ws://localhost:9090";
    OkHttpClient client = new OkHttpClient();
    ObjectMapper mapper = new ObjectMapper()
      .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    CountDownLatch latch = new CountDownLatch(1);
    AtomicInteger idCounter = new AtomicInteger(1);

    Request request = new Request.Builder().url(url).build();

    WebSocket ws = client.newWebSocket(request, new WebSocketListener() {
    // shared state
    Set<String> seenTopics = ConcurrentHashMap.newKeySet();
    ScheduledExecutorService scheduler =
    Executors.newSingleThreadScheduledExecutor();

    // 1) onOpen: start periodic poll
    @Override
    public void onOpen(WebSocket ws, Response resp) {
      System.out.println("▶ Connected, starting topic poll…");
      scheduler.scheduleAtFixedRate(() -> {
        String reqId = String.valueOf(idCounter.getAndIncrement());
        RosbridgeMessage poll = new RosbridgeMessage(
          "call_service", "/rosapi/topics", Map.of(), reqId
        );
        try {
          ws.send(mapper.writeValueAsString(poll));
        } catch (JsonProcessingException e) {
          System.err.println("Error serializing poll message: " + e.getMessage());
        }
      }, 0, 2, TimeUnit.SECONDS);
    }

    // 2) onMessage: handle either a service_response or publishes
    @Override
    public void onMessage(WebSocket ws, String text) {
      try {
        Map<?,?> m = mapper.readValue(text, Map.class);
        String op = (String)m.get("op");

        if ("service_response".equals(op)
            && "/rosapi/topics".equals(m.get("service"))) {
          Map<?,?> vals = (Map<?,?>)m.get("values");
          List<String> topics = (List<String>)vals.get("topics");
          List<String> types  = (List<String>)vals.get("types");

          for (int i = 0; i < topics.size(); i++) {
            String topic = topics.get(i);
            String type  = types .get(i);
            if (seenTopics.add(topic)) {
              // new topic → subscribe
              String subId = String.valueOf(idCounter.getAndIncrement());
              RosbridgeMessage sub = new RosbridgeMessage(
                "subscribe", topic, type, subId
              );
              try {
                ws.send(mapper.writeValueAsString(sub));
                System.out.println("► Subscribed to new topic: " + topic);
              } catch (JsonProcessingException e) {
                System.err.println("Error serializing subscribe message: " + e.getMessage());
              }
            }
          }
        }
        else if ("publish".equals(op)) {
          // just print
          String topic = (String)m.get("topic");
          Object msg    = m.get("msg");
          try {
            System.out.printf("▶ [%s] %s%n", topic, mapper.writeValueAsString(msg));
          } catch (JsonProcessingException e) {
            System.out.printf("▶ [%s] <error serializing message: %s>%n", topic, e.getMessage());
          }
        }
      } catch (JsonProcessingException e) {
        System.err.println("Error parsing incoming message: " + e.getMessage());
      }
    }

      @Override
      public void onFailure(WebSocket webSocket, Throwable t, Response resp) {
        t.printStackTrace();
        latch.countDown();
      }

      @Override
      public void onClosed(WebSocket webSocket, int code, String reason) {
        System.out.println("◼ Disconnected: " + reason);
        latch.countDown();
      }
    });

    // Block until the socket closes
    latch.await();
    client.dispatcher().executorService().shutdown();
  }
}