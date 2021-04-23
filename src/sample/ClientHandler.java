package sample;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientHandler {

    private final Logger logger;
    private final Vertx vertx;

    public Vertx getVertxInstance() {
        return vertx;
    }

    public EventBus getVertxEventBus() {
        return vertx.eventBus();
    }

    public ClientHandler(String serverAddress, Integer serverPort, String nickname) {
        logger = LoggerFactory.getLogger(ClientHandler.class);
        this.vertx = Vertx.vertx();
    }

    public void startClient(String serverAddress, Integer serverPort, String nickname) {

        final GameClientVerticle gameClientVerticle = new GameClientVerticle(serverAddress, serverPort, nickname);
        // deployVerticle is asynchronous (occurs after a period of time)
        vertx.deployVerticle(gameClientVerticle, deployStatus -> {
            if (deployStatus.succeeded()) {
                logger.info("Deployed callback");
            } else {
                logger.info(deployStatus.cause().getLocalizedMessage());
            }
        });

    }

}
