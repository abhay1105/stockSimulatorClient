package sample;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.eventbus.MessageProducer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class StartingPage extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        // loads the first page we want to load in our application
        Parent root = FXMLLoader.load(getClass().getResource("StartingPage.fxml"));
        primaryStage.setTitle("Stock Simulator 2.0");
        primaryStage.setScene(new Scene(root, 1500, 750));
        primaryStage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }

}
