package com.nrl;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.WebSocketBase;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQConsumer;
import io.vertx.rabbitmq.RabbitMQOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RabbitListenerVerticle extends AbstractVerticle {

    private final Logger log = LoggerFactory.getLogger(RabbitListenerVerticle.class);

    private HttpServer httpServer;
    private WebSocketBase webSocketBase;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        httpServer = vertx.createHttpServer();

        RabbitMQOptions config = new RabbitMQOptions();
        // Each parameter is optional
        // The default parameter with be used if the parameter is not set
        config.setUser("guest");
        config.setPassword("guest");
        config.setHost("localhost");
        config.setPort(52742);
        config.setVirtualHost("/");
        config.setConnectionTimeout(6000); // in milliseconds
        config.setRequestedHeartbeat(60); // in seconds
        config.setHandshakeTimeout(6000); // in milliseconds
        config.setRequestedChannelMax(5);
        config.setNetworkRecoveryInterval(500); // in milliseconds
        config.setAutomaticRecoveryEnabled(true);
        config.setReconnectAttempts(10);
        config.setReconnectInterval(10000);
        RabbitMQClient client = RabbitMQClient.create(vertx, config);
        httpServer.webSocketHandler(socket -> {
            log.info("WebSocket connection established");
            webSocketBase = socket;
            webSocketBase = socket.textMessageHandler(message -> {
                log.info("Received message from client: " + message);
            });

            socket.closeHandler(close -> {
                log.info("WebSocket connection closed");
            });
        });

        httpServer.listen(3000, result -> {
            if (result.succeeded()) {
                log.info("WebSocket server is now listening on port 3000");
            } else {
                log.info("Failed to start WebSocket server");
            }
        });
        createClientWithManualParams(vertx, client);

    }

    public void createClientWithManualParams(Vertx vertx, RabbitMQClient client) {
        // Connect
        client.start().onComplete(asyncResult -> {
            if (asyncResult.succeeded()) {
                log.info("RabbitMQ successfully connected!");

                basicConsumer(vertx, client);
                deadQueuConsumer(vertx,client);
                basicConsumerFanout2(vertx,client);
                directConsumer(vertx,client);
            } else {
                log.error("Fail to connect to RabbitMQ {}", asyncResult.cause().getMessage());
            }
        });

    }


    public void basicConsumer(Vertx vertx, RabbitMQClient client) {
        client.basicConsumer("fanout.queue").onComplete(rabbitMQConsumerAsyncResult -> {
            if (rabbitMQConsumerAsyncResult.succeeded()) {
                log.info("RabbitMQ consumer created !");
                RabbitMQConsumer mqConsumer = rabbitMQConsumerAsyncResult.result();
                mqConsumer.handler(message -> {
                    log.info("Got message: {}", message.body().toString());
                    sendWs(message.body().toString());
                });
            } else {
                log.error("exc", rabbitMQConsumerAsyncResult.cause());
            }
        });

    }

    public void directConsumer(Vertx vertx, RabbitMQClient client) {
        client.basicConsumer("direct.Queue").onComplete(rabbitMQConsumerAsyncResult -> {
            if (rabbitMQConsumerAsyncResult.succeeded()) {
                log.info("RabbitMQ consumer created !");
                RabbitMQConsumer mqConsumer = rabbitMQConsumerAsyncResult.result();
                mqConsumer.handler(message -> {
                    log.info("Got message: {}", message.body().toString());
                    sendWs(message.body().toString());
                });
            } else {
                log.error("exc", rabbitMQConsumerAsyncResult.cause());
            }
        });

    }

    public void basicConsumerFanout2(Vertx vertx, RabbitMQClient client) {
        client.basicConsumer("fanout.queue2").onComplete(rabbitMQConsumerAsyncResult -> {
            if (rabbitMQConsumerAsyncResult.succeeded()) {
                log.info("RabbitMQ consumer created !");
                RabbitMQConsumer mqConsumer = rabbitMQConsumerAsyncResult.result();
                mqConsumer.handler(message -> {
                    log.info("Fanout2 same message type: {}", message.body().toString());
                    sendWs(message.body().toString()+" Fanout2 same message type");
                });
            } else {
                log.error("exc", rabbitMQConsumerAsyncResult.cause());
            }
        });

    }

    public void deadQueuConsumer(Vertx vertx, RabbitMQClient client) {
        client.basicConsumer("dead.letter.queue").onComplete(rabbitMQConsumerAsyncResult -> {
            if (rabbitMQConsumerAsyncResult.succeeded()) {
                log.info("RabbitMQ consumer created !");
                RabbitMQConsumer mqConsumer = rabbitMQConsumerAsyncResult.result();
                mqConsumer.handler(message -> {
                    log.info("Dead queue(late message): {}", message.body().toString());
                    sendWs(message.body().toString()+" Dead Queue");
                });
            } else {
                log.error("dead exc:", rabbitMQConsumerAsyncResult.cause());
            }
        });
    }

    public void sendWs(String msg) {
        webSocketBase.writeTextMessage(msg);
    }

}