package sample;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.slf4j.Logger;

import javafx.event.ActionEvent;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

public class AccountPageController {

    // all fx elements will be initialized here
    @FXML
    AnchorPane anchAccountPage;
    @FXML
    ListView lstStockSelect, lstGamePlayers;
    @FXML
    Label lblAccountName, lblStockName, lblCurrentSharePrice, lblCurrentSharePriceNumber, lblAccountBalance,
            lblShareNumber, lblAmountInvested, lblProfitOrLoss, lblCurrentValue, lblAmountInvestedStock,
            lblCurrentValueStock, lblGameCode, lblTimeRemaining;
    @FXML
    NumberAxis xaxisStock, yaxisStock;
    @FXML
    TextField txtfiNumOfShares;
    @FXML
    LineChart chrtStock;
    @FXML
    Button btnBuyShares, btnSellShares, btnStart;

    private ClientHandler clientHandler = null;
    private Logger logger = null;

    // to communicate with vertx websocket using verticle
    private EventBus vertxEventBus = null;
    // to receive messages from server
    private MessageConsumer<String> busConsumer = null;
    // to send messages to server
    private MessageProducer<String> busProducer = null;

    // method that will receive all of the objects required in order to communicate with the server (this was all
    // initialized in the StartingPageController, so it needs to be transferred to every other fxController class)
    public void transferVertxObjects(ClientHandler clientHandler) {

        this.clientHandler = clientHandler;
        this.logger = LoggerFactory.getLogger(AccountPageController.class);
        this.vertxEventBus = clientHandler.getVertxEventBus();
        this.busConsumer = vertxEventBus.consumer("socket-inbound");
        this.busProducer = vertxEventBus.publisher("socket-outbound");

        // attach a handler to receive messages
        this.busConsumer.handler(msg -> {

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

    // method takes in every message that is incoming from the busProducer in the GameClientVerticle and processes it
    public void handleMessage(String message) throws IOException {
        JsonObject json = new JsonObject(message);
        String messageType = json.getString("type");
        if (messageType.equals("stock_chart_data_full")) {
            // Platform.runLater()s must consistently be used due to the fact that JavaFX changes and vertx processes
            // cannot occur on the same Thread
            Platform.runLater(() -> {
                updateChartFull(json);
            });
        } else if (messageType.equals("stock_chart_data_interval")) {
            Platform.runLater(() -> {
                updateChartInterval(json);
            });
        } else if (messageType.equals("empty_timer_run")) {
            Platform.runLater(() -> {
                double totalNumOfMillisecondsLeft = json.getDouble("total_num_of_milliseconds_left");
                int minutesRemaining = (int) totalNumOfMillisecondsLeft / 60000;
                int secondsRemaining = ((int) totalNumOfMillisecondsLeft % 60000) / 1000;
                if (secondsRemaining < 10) {
                    lblTimeRemaining.setText(minutesRemaining + ":0" + secondsRemaining);
                } else {
                    lblTimeRemaining.setText(minutesRemaining + ":" + secondsRemaining);
                }
            });
        }
    }

    // methods below will update the graph to whatever stock has been selected at the given time

    // method will send a request to server in order to get the full data for a specific stock
    public void sendSelectedStockFull() {
        String stockSymbol = (String) lstStockSelect.getSelectionModel().getSelectedItem();
        JsonObject json = new JsonObject();
        json.put("stock_symbol", stockSymbol);
        json.put("type", "stock_data_request_full");
        busProducer.write(json.encode());
    }

    // these variables will be decided by the actual player in the game, but we can use test values for now
    private double intervalTime = 3000; // in milliseconds
    private double amountOfDataPerInterval = 10; // in point of data

    // method will send a request to server in order to get data in smaller intervals
    public void sendSelectedStockInterval() {
        btnStart.setDisable(true);

        JsonArray jsonArray = new JsonArray();
        for (Object stockSymbol: lstStockSelect.getItems()) {
            jsonArray.add((String) stockSymbol);
        }

        JsonObject json = new JsonObject();
        json.put("stock_symbols", jsonArray);
        json.put("interval_time", intervalTime);
        json.put("amount_per_interval", amountOfDataPerInterval);
        json.put("type", "stock_data_request_interval");
        busProducer.write(json.encode());
    }

    // method will update the leaderboard accordingly
    public void updateLeaderboard(JsonObject jsonObject) {
        System.out.println("leaderboard update function");
        JsonArray names = jsonObject.getJsonArray("players");
        JsonArray scores = jsonObject.getJsonArray("scores");
        lstGamePlayers.getItems().clear();
        ArrayList<String> namesList = new ArrayList<>();
        ArrayList<Double> scoresList = new ArrayList<>();
        for (int i = 0;i < names.size();i++) {
            namesList.add(names.getString(i));
            scoresList.add(scores.getDouble(i));
        }
        if (namesList.size() == 2) {
            if (scoresList.get(0) > scoresList.get(1)) {
                lstGamePlayers.getItems().add(namesList.get(0) + "               $" + scoresList.get(0));
                lstGamePlayers.getItems().add(namesList.get(1) + "               $" + scoresList.get(1));
            } else {
                lstGamePlayers.getItems().add(namesList.get(1) + "               $" + scoresList.get(1));
                lstGamePlayers.getItems().add(namesList.get(0) + "               $" + scoresList.get(0));
            }
        } else if (namesList.size() > 2) {
            // Insertion Sort to set scores and names in ascending order
            for (int j = 1;j < scoresList.size() - 1;j++) {
                double tempNum = scoresList.get(j);
                String tempName = namesList.get(j);
                int k = j - 1;
                while (k >= 0 && tempNum <= scoresList.get(k)) {
                    scoresList.set(k + 1, scoresList.get(k));
                    namesList.set(k + 1, namesList.get(k));
                    k = k - 1;
                }
                scoresList.set(k + 1, tempNum);
                namesList.set(k + 1, tempName);
            }
            for (int h = namesList.size() - 1;h >= 0;h--) {
                lstGamePlayers.getItems().add(namesList.get(h) + "                    $" + scoresList.get(h));
            }
        }
    }

    // method will actually receive the full amount of data and update the graph accordingly
    public void updateChartFull(JsonObject json) {

        lblStockName.setText(json.getString("stock_name") + " (" + json.getString("stock_symbol") + ")");
        lblCurrentSharePrice.setText("The Current Share Price for " + json.getString("stock_symbol") + ":");
        lblCurrentSharePriceNumber.setText("--- USD");

        chrtStock.getData().clear();

        xaxisStock.setLabel("Daily Time Series");
        yaxisStock.setLabel("Open Price Per Day");

        double minDoubleValue = Double.MAX_VALUE;
        double maxDoubleValue = Double.MIN_VALUE;
        XYChart.Series series = new XYChart.Series();
        for (int i = 0;i < json.getJsonArray("open_prices").size();i += 2) {
            double yValue = json.getJsonArray("open_prices").getDouble(i);
            series.getData().add(new XYChart.Data(i, yValue));
            if (yValue <= minDoubleValue) {
                minDoubleValue = yValue;
            } else if (yValue >= maxDoubleValue) {
                maxDoubleValue = yValue;
            }
        }

        yaxisStock.setAutoRanging(false);
        yaxisStock.setLowerBound(minDoubleValue - 10.0);
        yaxisStock.setUpperBound(maxDoubleValue + 10.0);

        xaxisStock.setAutoRanging(true);

        chrtStock.setCreateSymbols(false);
        chrtStock.setLegendVisible(false);

        chrtStock.getData().add(series);

    }

    // used in our updateChartInterval() method
    private boolean firstTimeRun = true;
    private ArrayList<StockIntervalData> stockIntervalDataList = new ArrayList<>();
    private StockIntervalData currentStock = null;
    // for now the first thing the graph shows will automatically be set to the first stock in the list
    private String previousStockSelected = "--NONE--";
    private String selectedStockSymbol;

    // method will be built in order to update a chart based on intervals of data that come in from the server
    public void updateChartInterval(JsonObject json) {

        selectedStockSymbol = (String) lstStockSelect.getItems().get(0);

        // if-statement so that we only create all of our StockIntervalData objects only once
        if (firstTimeRun) {
            for (int i = 0;i < json.getJsonArray("complete_interval_data").size();i++) {
                JsonObject jsonObject = json.getJsonArray("complete_interval_data").getJsonObject(i);
                stockIntervalDataList.add(new StockIntervalData(jsonObject.getString("stock_name"), jsonObject.getString("stock_symbol"), jsonObject.getDouble("number_of_shares")));
            }
            firstTimeRun = false;
        }

        // code to update the leaderboard located here
        updateLeaderboard(json);

        // clearing graph and adding a different series to graph if user clicks on a different stock
        selectedStockSymbol = (String) lstStockSelect.getSelectionModel().getSelectedItem();
        if (selectedStockSymbol != null) {

            if (!previousStockSelected.equals(selectedStockSymbol)) {
                previousStockSelected = selectedStockSymbol;
                chrtStock.getData().clear();
                for (StockIntervalData stock: stockIntervalDataList) {
                    if (stock.getStockSymbol().equals(selectedStockSymbol)) {
                        stock.addSeriesToGraph(chrtStock);
                        currentStock = stock;
                    }
                }
            }

            // actually retrieving the data from the JsonObjects
            StockIntervalData selectedStockIntervalData = null;
            for (int i = 0;i < json.getJsonArray("complete_interval_data").size();i++) {
                JsonObject jsonObject = json.getJsonArray("complete_interval_data").getJsonObject(i);
                for (StockIntervalData stockIntervalData: stockIntervalDataList) {
                    if (stockIntervalData.getStockSymbol().equals(jsonObject.getString("stock_symbol"))) {
                        selectedStockIntervalData = stockIntervalData;
                        for (int j = 0;j < jsonObject.getJsonArray("open_prices").size();j++) {
                            stockIntervalData.addData(jsonObject.getJsonArray("open_prices").getDouble(j));
                        }
                        stockIntervalData.setNumOfShares(jsonObject.getDouble("number_of_shares"));
                        stockIntervalData.setCurrentValueStock(jsonObject.getDouble("current_value_stock"));
                        stockIntervalData.setAmountInvestedStock(jsonObject.getDouble("amount_invested_stock"));
                    }
                }
            }

            // will update the time remaining in terms of minutes and seconds
            double totalNumOfMillisecondsLeft = json.getDouble("total_num_of_milliseconds_left");
            int minutesRemaining = (int) totalNumOfMillisecondsLeft / 60000;
            int secondsRemaining = ((int) totalNumOfMillisecondsLeft % 60000) / 1000;
            if (secondsRemaining < 10) {
                lblTimeRemaining.setText(minutesRemaining + ":0" + secondsRemaining);
            } else {
                lblTimeRemaining.setText(minutesRemaining + ":" + secondsRemaining);
            }

            lblCurrentValue.setText("$" + json.getDouble("current_value_account"));
            lblAmountInvested.setText("$" + json.getDouble("money_invested_account"));
            lblProfitOrLoss.setText("$" + json.getDouble("profit_or_loss_account"));

            // method call will change all the labels associated with the graph, as well as any axis changes that might be required
            currentStock.graphData(chrtStock, xaxisStock, yaxisStock, lblStockName, lblCurrentSharePrice, lblCurrentSharePriceNumber, lblShareNumber, lblCurrentValueStock, lblAmountInvestedStock);

            // method call will update all of the account statistics that come from the server
            if (selectedStockIntervalData != null) {
                updateAccountStatistics(json.getDouble("account_balance"));
            }

        }

    }

    // method will be used in order to update all of our account statistics
    public void updateAccountStatistics(double accountBalance) {
        lblAccountBalance.setText("$" + accountBalance);
    }

    // method will be used for abstraction purposes in both the buy and sell request functions below
    public void sendShareTransaction(String type) {
        String latestSharePrice = lblCurrentSharePriceNumber.getText().substring(0, lblCurrentSharePriceNumber.getText().indexOf(" "));
        int numOfSharesRequested = Integer.parseInt(txtfiNumOfShares.getText());
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("stock_symbol", selectedStockSymbol);
        jsonObject.put("share_price", latestSharePrice);
        jsonObject.put("number_of_shares", numOfSharesRequested);
        jsonObject.put("type", type);
        System.out.println(jsonObject.encode());
        busProducer.write(jsonObject.encode());
        System.out.println(type + "       " + numOfSharesRequested + "      " + selectedStockSymbol + "     " + latestSharePrice);
    }

    // method will send a request to the server so that the player can buy shares in a stock
    public void sendBuyRequest() {
        sendShareTransaction("buy_request");
    }

    // method will send a request to the server so that the player can sell shares in a stock
    public void sendSellRequest() {
        sendShareTransaction("sell_request");
    }

}
