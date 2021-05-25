package sample;

import java.util.Comparator;

// class will be used in order to make the leaderboard easy to put in descending order
public class ComparePlayers implements Comparator<TempPlayer> {

    @Override
    public int compare(TempPlayer o1, TempPlayer o2) {
        return (int) (o2.getScore() - o1.getScore());
    }

}
