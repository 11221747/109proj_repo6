package Game1.AI;

import Game1.models.Board;
import Game1.models.Block;

import java.util.*;

public class BFSolver {
    private static class StateNode {
        final List<int[]> positions; // ÿ����� {x,y}
        final MoveInfo move;
        final StateNode parent;

        StateNode(List<int[]> positions, MoveInfo move, StateNode parent) {
            this.positions = positions;
            this.move = move;
            this.parent = parent;
        }

        String key() {
            StringBuilder sb = new StringBuilder();
            for (int[] p : positions) {
                sb.append(p[0]).append(',').append(p[1]).append(';');
            }
            return sb.toString();
        }
    }

    public static List<MoveInfo> solve(Board initialBoard) {
        List<int[]> startPos = new ArrayList<>();
        for (Block b : initialBoard.getBlocks()) {
            startPos.add(new int[]{b.getX(), b.getY()});
        }

        Queue<StateNode> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        StateNode start = new StateNode(startPos, null, null);
        queue.add(start);
        visited.add(start.key());

        while (!queue.isEmpty()) {
            StateNode cur = queue.poll();
            // ���� Board ʵ�������жϺ�������һ��
            Board board = new Board();
            // ���� board �� blocks λ��
            List<Block> bs = board.getBlocks();
            for (int i = 0; i < bs.size(); i++) {
                bs.get(i).setPosition(cur.positions.get(i)[0], cur.positions.get(i)[1]);
            }
            if (board.isWin()) return buildPath(cur);

            // ���������ƶ�
            for (int i = 0; i < bs.size(); i++) {
                Block block = bs.get(i);
                for (Board.Direction dir : Board.Direction.values()) {
                    if (!board.canMove(block, dir)) continue;
                    // ������ positions
                    List<int[]> newPos = new ArrayList<>();
                    for (int[] p : cur.positions) newPos.add(new int[]{p[0], p[1]});
                    newPos.get(i)[0] += dir.dx();
                    newPos.get(i)[1] += dir.dy();

                    StateNode nxt = new StateNode(newPos, new MoveInfo(i, dir), cur);
                    String key = nxt.key();
                    if (visited.add(key)) {
                        queue.add(nxt);
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    private static List<MoveInfo> buildPath(StateNode node) {
        LinkedList<MoveInfo> path = new LinkedList<>();
        while (node.parent != null) {
            path.addFirst(node.move);
            node = node.parent;
        }
        return path;
    }
}

