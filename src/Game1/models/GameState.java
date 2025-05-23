package Game1.models;



import java.io.Serial;
import java.io.Serializable;



public class GameState implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Board board;
    private String username;
    private int remainingSeconds;

    public GameState(Board board, String username, int remainingSeconds) {
        this.board = board;
        this.username = username;
        this.remainingSeconds = remainingSeconds;
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
}