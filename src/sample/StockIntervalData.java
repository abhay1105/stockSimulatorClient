package sample;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.util.ArrayList;

// class will help us manage data being sent to client by the server
public class StockIntervalData {

    // all variables located here
    private String stockName, stockSymbol;
    private ArrayList<Double> data;
    private XYChart.Series series;
    private int seriesDataCount;
    private double numOfShares;
    private double currentValueStock;
    private double amountInvestedStock;

    // class constructor
    public StockIntervalData(String stockName, String stockSymbol, double numOfShares) {
        this.stockName = stockName;
        this.stockSymbol = stockSymbol;
        data = new ArrayList<>();
        series = new XYChart.Series();
        this.numOfShares = numOfShares;
    }

    // all getter methods located here
    public String getStockName() { return stockName; }
    public String getStockSymbol() { return stockSymbol; }
    public ArrayList<Double> getData() { return data; }
    public XYChart.Series getSeries() { return series; }
    public int getSeriesDataCount() { return seriesDataCount; }
    public double getNumOfShares() { return numOfShares; }
    public double getCurrentValueStock() { return currentValueStock; }
    public double getAmountInvestedStock() { return amountInvestedStock; }

    // all setter methods located here
    public void setNumOfShares(double numOfShares) { this.numOfShares = numOfShares; }
    public void setCurrentValueStock(double value) { currentValueStock = value; }
    public void setAmountInvestedStock(double amount) { amountInvestedStock = amount; }

    // method to add data to both our arrayList as well as our series
    public void addData(double datum) {
        data.add(datum);
        series.getData().add(new XYChart.Data<>(seriesDataCount, datum));
        seriesDataCount++;
    }

    // method to add the respective series to the chart
    public void addSeriesToGraph(LineChart lineChart) {
        lineChart.getData().add(series);
    }

    // method to graph the interval data
    public void graphData(LineChart lineChart, NumberAxis xaxisStock, NumberAxis yaxisStock, Label lblStockName,
                          Label lblCurrentSharePrice, Label lblCurrentSharePriceNumber, Label lblShareNumber, Label lblCurrentValueStock, Label lblAmountInvestedStock) {
        System.out.println("CURRENTLY GRAPHING        " + stockName);
        lblStockName.setText(stockName + " (" + stockSymbol + ")");
        lblCurrentSharePrice.setText("The Current Share Price for " + stockSymbol + ":");
        lblCurrentSharePriceNumber.setText(data.get(data.size() - 1) + " USD");
        lblShareNumber.setText("You currently own " + numOfShares + " shares in " + stockSymbol + ".");
        lblCurrentValueStock.setText("$" + currentValueStock);
        lblAmountInvestedStock.setText("$" + amountInvestedStock);

        xaxisStock.setLabel("Daily Time Series");
        yaxisStock.setLabel("Open Price Per Day");

        double minDoubleValue = Double.MAX_VALUE;
        double maxDoubleValue = Double.MIN_VALUE;
        for (double yValue : data) {
            if (yValue <= minDoubleValue) {
                minDoubleValue = yValue;
            } else if (yValue >= maxDoubleValue) {
                maxDoubleValue = yValue;
            }
        }

        yaxisStock.setAutoRanging(false);
        if (minDoubleValue - 10.0 < 0) {
            yaxisStock.setLowerBound(0);
        } else {
            yaxisStock.setLowerBound(minDoubleValue - 10.0);
        }
        yaxisStock.setUpperBound(maxDoubleValue + 10.0);


        xaxisStock.setAutoRanging(true);

        lineChart.setCreateSymbols(false);
        lineChart.setLegendVisible(false);
    }



}
