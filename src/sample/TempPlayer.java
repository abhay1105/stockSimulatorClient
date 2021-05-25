package sample;

import java.util.Comparator;

// class will make ordering the scores in the leaderboard much easier
public class TempPlayer {

    // all variables located here
    private String name;
    private double score;

    // class constructor here
    public TempPlayer(String name, double score) {
        this.name = name;
        this.score = score;
    }

    // all getter methods located here
    public String getName() { return name; }
    public double getScore() { return score; }


}
