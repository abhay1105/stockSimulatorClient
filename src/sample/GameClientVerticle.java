package sample;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebsocketVersion;
import io.vertx.core.json.JsonObject;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GameClientVerticle extends AbstractVerticle {


    public static String CLIENT_ADDRESS;
    public static Integer CLIENT_PORT;

    private final Logger logger;

    private final String serverAddress;
    private final Integer serverPort;
    private final String nickname;

    private EventBus vertxEventBus = null;
    private MessageConsumer<String> busConsumer = null;
    private MessageProducer<String> busProducer = null;

    private HttpClient  httpClient = null;
    private WebSocket webSocket = null;

    public GameClientVerticle(String serverAddress, Integer serverPort, String nickname) {
        logger = LoggerFactory.getLogger(GameClientVerticle.class);
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.nickname = nickname;
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        logger.info("Deployed");

        this.vertxEventBus = vertx.eventBus();

        // the strings act as addresses
        // essentially any busConsumer with socket-inbound will receive this message
        // likewise any message sent by the busProducer will be sent to this busConsumer as long as the address is the same
        busProducer = vertxEventBus.publisher("socket-inbound");
        busConsumer = vertxEventBus.consumer("socket-outbound");

        // anything that is sent to the GameClientVerticle from our busProducer in the fxController will automatically
        // be directed straight to our server since our WebSocket is bridged to our EventBus
        busConsumer.handler(msg -> {
            // Send message to server
            String message = msg.body();
            logger.info("Sending {}", message);
            sendMessageToServer(message);
        });

        connect();
        startFuture.complete();
    }

    public void connect() throws Exception {

        HttpClientOptions httpOptions = new HttpClientOptions()
                .setConnectTimeout(1000)
                .setIdleTimeout(5000)
                .setDefaultHost("localhost")
                .setDefaultPort(8080)
                .setSsl(false)
                .setLogActivity(true);
        httpClient = vertx.createHttpClient(httpOptions);
        httpClient.websocket(8080, "localhost", "/", socket -> {
            logger.info("Connection ID=" + socket.textHandlerID());
            webSocket = socket;

            // this message below is being sent to our fxController since the event bus is between the fxController
            // and the GameClientVerticle, so when the busConsumer receives this said message, we can then begin
            // sending our own messages since that is officially when the socket is up and running
            JsonObject json = new JsonObject();
            json.put("type", "socket_connected");
            busProducer.write(json.encode());

            webSocket.endHandler((Void) -> {
                logger.info("Socket ended");
                webSocket = null;
            });

            webSocket.closeHandler((Void) -> {
                logger.info("Socket closed");
                webSocket = null;
            });

            webSocket.handler(buffer -> {
                logger.info("Received {}", buffer.toString());
                // automatically sends to all busConsumers because they all have the same address
                busProducer.write(buffer.toString());
            });
//
//            // Register to receive messages
//            vertxEventBus.consumer("socket-inbound", message -> {
//                String messageStr = message.body().toString();
//                logger.info("Received {}", messageStr);
//            });

        });
    }

    private void sendMessageToServer(String message) {
        if (webSocket != null) {
            webSocket.writeTextMessage(message);
        }
    }



}
