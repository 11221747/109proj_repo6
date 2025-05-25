package Game1.AI;

import Game1.models.Board;
import Game1.models.Block;
import java.util.*;
import java.awt.Point;

/**
 * Bidirectional BFS Solver:
 * - 从初始状态和目标状态同时搜索，中途相遇。
 * - 状态用块位置数组表示，避免深拷贝 Board 对象。
 * - 通常能在最优步数分钟级范围内快速找到解。
 */
public class BiDirectionalSolver {
    // 定义状态节点
    private static class StateNode {
        final List<Point> positions;
        final StateNode parent;
        final MoveInfo move;
        final int depth;

        StateNode(List<Point> positions, StateNode parent, MoveInfo move, int depth) {
            this.positions = positions;
            this.parent = parent;
            this.move = move;
            this.depth = depth;
        }

        String key() {
            StringBuilder sb = new StringBuilder(positions.size() * 8);
            for (Point p : positions) {
                sb.append((char) (p.x + 1)).append((char) (p.y + 1));
            }
            return sb.toString();
        }

        List<MoveInfo> buildPath() {
            LinkedList<MoveInfo> path = new LinkedList<>();
            StateNode cur = this;
            while (cur.parent != null) {
                path.addFirst(cur.move);
                cur = cur.parent;
            }
            return path;
        }
    }

    public static List<MoveInfo> solve(Board board) {
        List<Block> blocks = board.getBlocks();
        int n = blocks.size();

        List<Point> startPos = new ArrayList<>(n);
        for (Block b : blocks) startPos.add(new Point(b.getX(), b.getY()));
        StateNode start = new StateNode(startPos, null, null, 0);

        List<Point> goalPos = new ArrayList<>(n);
        for (Block b : blocks) goalPos.add(new Point(b.getX(), b.getY()));
        for (int i = 0; i < n; i++) if (blocks.get(i).getType() == Block.BlockType.CAO_CAO) {
            goalPos.get(i).setLocation(1, 3);
            break;
        }
        StateNode goal = new StateNode(goalPos, null, null, 0);

        Deque<StateNode> front = new ArrayDeque<>(), back = new ArrayDeque<>();
        Map<String, StateNode> visitedFront = new HashMap<>(), visitedBack = new HashMap<>();

        front.add(start); visitedFront.put(start.key(), start);
        back.add(goal);  visitedBack.put(goal.key(), goal);

        int depth = 0;
        while (!front.isEmpty() && !back.isEmpty()) {
            depth++;
            System.out.println("Bidirectional depth=" + depth + ", front size=" + front.size() + ", back size=" + back.size());
            if (front.size() <= back.size()) {
                if (expandLayer(front, visitedFront, visitedBack, board, blocks))
                    return mergePaths(visitedFront, visitedBack);
            } else {
                if (expandLayer(back, visitedBack, visitedFront, board, blocks))
                    return mergePaths(visitedFront, visitedBack);
            }
            if (depth > 200) {
                System.out.println("Reached max depth limit");
                break;
            }
        }
        return Collections.emptyList();
    }

    private static boolean expandLayer(Deque<StateNode> queue,
                                       Map<String, StateNode> selfVisited,
                                       Map<String, StateNode> otherVisited,
                                       Board boardTemplate,
                                       List<Block> blocks) {
        int layerSize = queue.size();
        for (int k = 0; k < layerSize; k++) {
            StateNode cur = queue.poll();
            Board temp = new Board(boardTemplate);
            List<Point> pos = cur.positions;
            for (int i = 0; i < blocks.size(); i++) temp.getBlocks().get(i).setPosition(pos.get(i).x, pos.get(i).y);

            for (int i = 0; i < blocks.size(); i++) {
                for (Board.Direction d : Board.Direction.values()) {
                    if (!temp.canMove(temp.getBlocks().get(i), d)) continue;
                    List<Point> newPos = new ArrayList<>(pos.size());
                    for (Point p : pos) newPos.add(new Point(p));
                    newPos.get(i).translate(d.dx(), d.dy());

                    StateNode next = new StateNode(newPos, cur, new MoveInfo(i, d), cur.depth + 1);
                    String key = next.key();
                    if (selfVisited.containsKey(key)) continue;
                    selfVisited.put(key, next);
                    if (otherVisited.containsKey(key)) {
                        System.out.println("Meet at depth=" + next.depth);
                        return true;
                    }
                    queue.add(next);
                }
            }
        }
        return false;
    }

    private static List<MoveInfo> mergePaths(Map<String, StateNode> fVis,
                                             Map<String, StateNode> bVis) {
        for (String key : fVis.keySet()) {
            if (bVis.containsKey(key)) {
                StateNode midF = fVis.get(key), midB = bVis.get(key);
                List<MoveInfo> p1 = midF.buildPath();
                List<MoveInfo> p2 = midB.buildPath();
                Collections.reverse(p2);
                for (MoveInfo mv : p2) mv = new MoveInfo(mv.blockIndex, opposite(mv.direction));
                p1.addAll(p2);
                System.out.println("Total depth=" + p1.size());
                return p1;
            }
        }
        return Collections.emptyList();
    }

    private static Board.Direction opposite(Board.Direction d) {
        return switch (d) {
            case UP -> Board.Direction.DOWN;
            case DOWN -> Board.Direction.UP;
            case LEFT -> Board.Direction.RIGHT;
            case RIGHT -> Board.Direction.LEFT;
        };
    }
}
