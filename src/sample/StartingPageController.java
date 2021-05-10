package sample;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.slf4j.Logger;

import javafx.event.ActionEvent;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;

public class StartingPageController {

    // all fx elements located here
    @FXML
    TextField txtfiEnterNickname, txtfiGameMode, txtfiStartingBalance;
    @FXML
    Button btnConnect, btnSetStartBalance, btnAddSearchedStock, btnRemoveSelectedStock;
    @FXML
    AnchorPane anchOptionsScreen;
    @FXML
    TextArea txtarStockSearch;
    @FXML
    ListView lstSearchResults, lstSelectedStocks;
    @FXML
    Label lblSearch;

    private final ClientHandler clientHandler;
    private final Logger logger;

    // to communicate with vertx websocket using verticle
    private final EventBus vertxEventBus;
    // to receive messages from server
    private final MessageConsumer<String> busConsumer;
    // to send messages to server
    private final MessageProducer<String> busProducer;

    private AccountPageController accountPageController;
    private ActionEvent actionEvent;
    private Parent root;

    // global variables to help keep track of desired game settings
    private String mode;
    private double startingBalance;
    private double timeInMin;
    private int maxNumStocks;
    private ArrayList<String> stockSymbols = new ArrayList<>();
    private ArrayList<String> stockNames = new ArrayList<>();

    // controller constructor
    public StartingPageController() throws IOException {

        // since this is the first controller our application will go to, we will need to create new objects in order
        // to handle the incoming server messages, as well to send messages to our server
        clientHandler = new ClientHandler("127.0.0.1", 8080, "nickname");
        logger = LoggerFactory.getLogger(StartingPageController.class);
        vertxEventBus = clientHandler.getVertxEventBus();
        // doing .consumer() and .publisher() creates new busProducers and busConsumers with the string addresses
        busConsumer = vertxEventBus.consumer("socket-inbound");
        busProducer = vertxEventBus.publisher("socket-outbound");

        // lines below will allow us to make sure that any other fxControllers have the access to the objects
        // required to communicate with the server
        FXMLLoader loader = new FXMLLoader(getClass().getResource("AccountPage.fxml"));
        root = loader.load();
        accountPageController = loader.getController();
        accountPageController.transferVertxObjects(clientHandler);

        // attach a handler to receive messages
        busConsumer.handler(msg -> {
            String message = msg.body();
            logger.info("Received {}", message);

            // handles all incoming messages from GameClientVerticle
            try {
                handleMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // for now echo it back
            // busProducer.write("ECHO " + message);
        });
    }

    // method to connect the client to the server
    public void connectToServer(ActionEvent event) {

        // connecting the javafx client to our http server
        clientHandler.startClient("127.0.0.1", 8080, "nickname");
        // used to load next scene
        actionEvent = event;

    }

    // method takes in every message that is incoming from the busProducer in the GameClientVerticle and processes it
    public void handleMessage(String message) throws IOException {
        JsonObject json = new JsonObject(message);
        if (json.getString("type").equals("socket_connected")) {
            // the first thing we are doing when the socket is officially connected
            sendNewPlayerName();
            sendCurrentOrNewGame();
        } else if (json.getString("type").equals("updated_account_page_info")) {
            receiveUpdatedAccountPage(json);
        }
    }

    // method to send a message of the new player's name to the server
    public void sendNewPlayerName() {
        String name = txtfiEnterNickname.getText();
        JsonObject json = new JsonObject();
        json.put("player_name", name);
        json.put("type", "new_player");
        busProducer.write(json.encode());
    }

    // method to send a message to the server letting them know whether a current game is being joined
    // or a new game has to be created
    public void sendCurrentOrNewGame() {
        // hardcoded to yes, we want a new game. we can change the input later depending on how we want to ask the
        // user this in our UI
        String choice = "yes";
        JsonObject json = new JsonObject();
        json.put("player_choice", choice);
        json.put("type", "new_or_current_game");
        busProducer.write(json.encode());
    }

    // method to load in the account page of the particular player
    public void receiveUpdatedAccountPage(JsonObject json) throws IOException {

        Platform.runLater(() -> {

            // setting the scene to the next page
            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            stage.setTitle("Stock Simulator 2.0 Account Page");
            stage.setScene(new Scene(root, 1500, 750));
            // actually updating our accountPage with the new player information
            accountPageController.lblAccountName.setText(json.getString("player_name") + "'s Account");
            accountPageController.lstStockSelect.getItems().clear();
            JsonArray jsonArray = json.getJsonArray("stocks");
            for (int index = 0; index < jsonArray.size(); ++index) {
                accountPageController.lstStockSelect.getItems().add(jsonArray.getString(index));
            }
            stage.show();

            System.out.println("everything updated very nicely ###################");

        });

    }

    // method will switch between the options screen for creating a new game and the main screen for entering a name
    // and either joining or actually creating a game
    boolean visibility = false;
    public void switchOptionsScreen() {
        visibility = !visibility;
        anchOptionsScreen.setVisible(visibility);
    }

    // all settings related methods will be found below
    public void singleChoiceMode() { mode = "single-stock"; maxNumStocks = 1; lblSearch.setText("Select Stocks for Play (Max " + maxNumStocks + ".):"); }
    public void multiChoiceMode() { mode = "multiple-stocks"; maxNumStocks = 10; lblSearch.setText("Select Stocks for Play (Max " + maxNumStocks + ".):"); }
    public void freestyleChoiceMode() { mode = "freestyle"; maxNumStocks = 50; lblSearch.setText("Select Stocks for Play (Max " + maxNumStocks + ".):"); }
    public void setStartingBalance() { startingBalance = Double.parseDouble(txtfiStartingBalance.getText()); }
    public void twoMinChoiceTime() { timeInMin = 2; }
    public void fiveMinChoiceTime() { timeInMin = 5; }
    public void tenMinChoiceTime() { timeInMin = 10; }
    public void updateStockSearchResults() throws IOException, InterruptedException {
        String searchEntry = txtarStockSearch.getText();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.alphavantage.co/query?function=SYMBOL_SEARCH&keywords=" + searchEntry + "&apikey=9MFKMGNE0JRT1HWU"))
                .header("x-rapidapi-key", "SIGN-UP-FOR-KEY")
                .header("x-rapidapi-host", "alpha-vantage.p.rapidapi.com")
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
        JsonObject json = new JsonObject(response.body());
        JsonArray jsonArray = json.getJsonArray("bestMatches");
        lstSearchResults.getItems().clear();
        for (Object object: jsonArray) {
            JsonObject jsonObject = (JsonObject) object;
            String name = jsonObject.getString("2. name");
            String symbol = jsonObject.getString("1. symbol");
            lstSearchResults.getItems().add(name + " (" + symbol + ")");
        }
    }
    public void addSearchedStock() {
        if (stockSymbols.size() < maxNumStocks) {
            String selectedString = lstSearchResults.getSelectionModel().getSelectedItem().toString();
            stockNames.add(selectedString.substring(0, selectedString.indexOf("(") - 1));
            stockSymbols.add(selectedString.substring(selectedString.indexOf("(") + 1, selectedString.indexOf(")")));
            lstSelectedStocks.getItems().add(selectedString);
        }
    }
    public void removeSelectedStock() {
        String selectedString = lstSelectedStocks.getSelectionModel().getSelectedItem().toString();
        for (int i = 0;i < stockSymbols.size();i++) {
            if (selectedString.substring(0, selectedString.indexOf("(") - 1).equals(stockNames.get(i))) {
                stockNames.remove(i);
            }
            if (selectedString.substring(selectedString.indexOf("(") + 1, selectedString.indexOf(")")).equals(stockSymbols.get(i))) {
                stockSymbols.remove(i);
                break;
            }
        }
        lstSelectedStocks.getItems().remove(selectedString);
    }


}
