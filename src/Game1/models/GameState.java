package Game1.models;



import java.io.Serial;
import java.io.Serializable;



public class GameState implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Board board;
    private String username;
    private int level;
    private int remainingSeconds;
    private boolean timer_flag;

    public GameState(Board board, String username, int remainingSeconds, int level , boolean timer_flag) {
        this.board = board;
        this.username = username;
        this.remainingSeconds = remainingSeconds;
        this.level = level;
        this.timer_flag = timer_flag;
    }


    public int getLevel() {
        return level;
    }

    public Board getBoard() {
        return board;
    }

    public String getUsername() {
        return username;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(int remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public boolean isTimer_flag() {
        return timer_flag;
    }

    public void setTimer_flag(boolean timer_flag) {
        this.timer_flag = timer_flag;
    }
}