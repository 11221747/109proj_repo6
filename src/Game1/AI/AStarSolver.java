package Game1.AI;

import Game1.models.Board;
import Game1.models.Block;

import java.awt.Point;
import java.util.*;

public class AStarSolver {
    private static final int EXIT_R = 3, EXIT_C = 1;
    private static final int BLOCKER_PENALTY = 2;

    private static class State {
        int[][] grid;
        List<List<Point>> cells;
        int g, h, f;
        State parent;
        MoveInfo move;

        State(int[][] grid, List<List<Point>> cells, int g, State parent, MoveInfo move) {
            this.grid = grid;
            this.cells = cells;
            this.g = g;
            this.h = calcHeuristic(cells);
            this.f = this.g + this.h;
            this.parent = parent;
            this.move = move;
        }

        private int calcHeuristic(List<List<Point>> cells) {
            List<Point> cao = cells.get(caoIdx);
            int minR = Integer.MAX_VALUE, minC = Integer.MAX_VALUE;
            for (Point p : cao) {
                minR = Math.min(minR, p.y);
                minC = Math.min(minC, p.x);
            }
            int d = Math.abs(minR - EXIT_R) + Math.abs(minC - EXIT_C);
            int block = 0;
            for (int r = minR; r < EXIT_R; r++) {
                int o = grid[r][EXIT_C];
                if (o != -1 && o != caoIdx) block++;
            }
            return d + block * BLOCKER_PENALTY;
        }

        String key() {
            StringBuilder sb = new StringBuilder();
            for (int[] row : grid) {
                for (int v : row) sb.append(v).append(',');
                sb.append(';');
            }
            return sb.toString();
        }
    }

    private static int caoIdx;

    public static List<MoveInfo> solve(Board board) {
        int R = Board.ROWS, C = Board.COLS;
        List<Block> blocks = board.getBlocks();

        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).getType() == Block.BlockType.CAO_CAO) {
                caoIdx = i;
                break;
            }
        }

        int[][] initGrid = new int[R][C];
        for (int[] row : initGrid) Arrays.fill(row, -1);
        List<List<Point>> initCells = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) initCells.add(new ArrayList<>());
        for (int i = 0; i < blocks.size(); i++) {
            Block b = blocks.get(i);
            for (int dr = 0; dr < b.getHeight(); dr++) {
                for (int dc = 0; dc < b.getWidth(); dc++) {
                    int r = b.getY() + dr, c = b.getX() + dc;
                    initGrid[r][c] = i;
                    initCells.get(i).add(new Point(c, r));
                }
            }
        }

        PriorityQueue<State> open = new PriorityQueue<>(Comparator.comparingInt(s -> s.f));
        Map<String, Integer> bestG = new HashMap<>();
        long expanded = 0;

        State start = new State(initGrid, initCells, 0, null, null);
        open.add(start);
        bestG.put(start.key(), 0);

        while (!open.isEmpty()) {
            State cur = open.poll();
            expanded++;
            if (expanded % 1000 == 0) {
                System.out.println("Expanded nodes: " + expanded + ", current g: " + cur.g + ", queue size: " + open.size());
            }

            if (cur.h == 0) {
                System.out.println("Solution found after expanding " + expanded + " nodes, depth " + cur.g);
                return buildPath(cur);
            }

            for (int idx = 0; idx < blocks.size(); idx++) {
                for (Board.Direction dir : Board.Direction.values()) {
                    if (!canMove(cur, idx, dir)) continue;
                    State nxt = makeMove(cur, idx, dir);
                    String k = nxt.key();
                    if (!bestG.containsKey(k) || nxt.g < bestG.get(k)) {
                        bestG.put(k, nxt.g);
                        open.add(nxt);
                    }
                }
            }
        }
        System.out.println("No solution found after expanding " + expanded + " nodes.");
        return Collections.emptyList();
    }

    private static boolean canMove(State s, int idx, Board.Direction d) {
        int dx = 0, dy = 0;
        switch (d) {
            case UP -> dy = -1;
            case DOWN -> dy = 1;
            case LEFT -> dx = -1;
            case RIGHT -> dx = 1;
        }
        int R = s.grid.length, C = s.grid[0].length;
        for (Point p : s.cells.get(idx)) {
            int nr = p.y + dy, nc = p.x + dx;
            if (nr < 0 || nr >= R || nc < 0 || nc >= C) return false;
            int occ = s.grid[nr][nc];
            if (occ != -1 && occ != idx) return false;
        }
        return true;
    }

    private static State makeMove(State cur, int idx, Board.Direction d) {
        int dx = 0, dy = 0;
        switch (d) {
            case UP -> dy = -1;
            case DOWN -> dy = 1;
            case LEFT -> dx = -1;
            case RIGHT -> dx = 1;
        }
        int R = cur.grid.length, C = cur.grid[0].length;
        int[][] ng = new int[R][C];
        for (int r = 0; r < R; r++) System.arraycopy(cur.grid[r], 0, ng[r], 0, C);
        List<List<Point>> ncells = new ArrayList<>();
        for (List<Point> lst : cur.cells) {
            List<Point> copy = new ArrayList<>();
            for (Point p : lst) copy.add(new Point(p));
            ncells.add(copy);
        }
        for (Point p : cur.cells.get(idx)) ng[p.y][p.x] = -1;
        for (Point p : cur.cells.get(idx)) {
            ng[p.y + dy][p.x + dx] = idx;
        }
        List<Point> moved = ncells.get(idx);
        int finalDy = dy;
        int finalDx = dx;
        moved.replaceAll(p -> new Point(p.x + finalDx, p.y + finalDy));

        return new State(ng, ncells, cur.g + 1, cur, new MoveInfo(idx, d));
    }

    private static List<MoveInfo> buildPath(State end) {
        LinkedList<MoveInfo> path = new LinkedList<>();
        while (end.parent != null) {
            path.addFirst(end.move);
            end = end.parent;
        }
        return path;
    }
}