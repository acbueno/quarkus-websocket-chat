package br.com.acbueno.quarkus.websocket.chat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.jboss.logging.Logger;

@ServerEndpoint("/chat/{username}")
@ApplicationScoped
public class ChatWebSocket {

  @Inject
  Logger logger;

  Map<String, Session> sessions = new ConcurrentHashMap<>();

  @OnOpen
  public void onOpen(Session session, @PathParam("username") String userName) {
    sessions.put(userName, session);
  }

  @OnClose
  public void onClose(Session session, @PathParam("username") String userName) {
    sessions.remove(userName);
    broadcast("User " + userName + " left");
  }

  @OnError
  public void onError(Session session, @PathParam("username") String userName, Throwable throwable) {
    sessions.remove(userName);
    broadcast("User " + userName + " left on error: " + throwable);

  }

  @OnMessage
  public void onMessage(String message, @PathParam("username") String userName) {
    if (message.equalsIgnoreCase("_ready_")) {
      broadcast("User " + userName + " joined");
    } else {
      broadcast(">> " + userName + ": " + message);
    }
  }

  private void broadcast(String message) {
    sessions.values().forEach(session -> {
      session.getAsyncRemote().sendObject(message, result -> {
        if (result.getException() != null) {
          logger.infof("Não foi possivel enviar message: %s", result.getException());
        }
      });
    });
  }

}
