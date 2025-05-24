package Game1.AI;

import Game1.models.Board;

public class MoveInfo {


    public final int blockIndex;
    public final Board.Direction direction;

    public MoveInfo(int blockIndex, Board.Direction direction) {
        this.blockIndex = blockIndex;
        this.direction = direction;
    }


}