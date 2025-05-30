package Game1.AI;

import Game1.models.Board;
import Game1.models.Block;
import java.awt.Point;
import java.util.*;

/**
 * Improved Beam Search Solver:
 * - 每层保留前 W 个状态，按 f = g + h 排序，兼顾路径长度和启发值。
 * - 增加最大深度以确保覆盖可能的解。
 */
public class BeamSolver {
    private static final int EXIT_R = 3, EXIT_C = 1;
    private static final int BLOCKER_PENALTY = 2;
    private static final int BEAM_WIDTH = 3000;  // 增大 beam 宽度
    private static final int MAX_DEPTH = 300;    // 增加最大探索深度

    private static int R, C, caoIdx;
    private static List<Block> blocks;

    public static List<MoveInfo> solve(Board board) {
        R = Board.ROWS;
        C = Board.COLS;
        blocks = board.getBlocks();
        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).getType() == Block.BlockType.CAO_CAO) {
                caoIdx = i;
                break;
            }
        }

        State start = State.fromBoard();
        List<State> beam = new ArrayList<>();
        beam.add(start);
        Set<String> seen = new HashSet<>();
        seen.add(start.key());

        for (int depth = 0; depth <= MAX_DEPTH; depth++) {
            List<State> nextLayer = new ArrayList<>();
            for (State s : beam) {
                if (s.h == 0) {
                    System.out.println("Found solution at depth=" + depth + ", path length=" + s.g);
                    return s.buildPath();
                }
                for (int idx = 0; idx < blocks.size(); idx++) {
                    for (Board.Direction dir : Board.Direction.values()) {
                        if (!s.canMove(idx, dir)) continue;
                        State ns = s.move(idx, dir);
                        if (seen.add(ns.key())) {
                            nextLayer.add(ns);
                        }
                    }
                }
            }
            if (nextLayer.isEmpty()) break;
            // 按 f=g+h 排序，保留前 W
            nextLayer.sort(Comparator.comparingInt(a -> a.g + a.h));
            beam.clear();
            for (int i = 0; i < Math.min(BEAM_WIDTH, nextLayer.size()); i++) {
                beam.add(nextLayer.get(i));
            }
            System.out.println("Beam depth: " + depth + ", beam size: " + beam.size());
        }
        System.out.println("Beam search failed after max depth.");
        return Collections.emptyList();
    }

    private static class State {
        final int g;                   // 已走步数
        final int h;                   // 启发值
        final int[][] grid;
        final List<List<Point>> cells;
        final State parent;
        final MoveInfo move;

        private State(int g, int[][] grid, List<List<Point>> cells, State parent, MoveInfo move) {
            this.g = g;
            this.grid = grid;
            this.cells = cells;
            this.parent = parent;
            this.move = move;
            this.h = calcHeuristic();
        }

        static State fromBoard() {
            int[][] g0 = new int[R][C];
            for (int[] row : g0) Arrays.fill(row, -1);
            List<List<Point>> c0 = new ArrayList<>();
            for (int i = 0; i < blocks.size(); i++) c0.add(new ArrayList<>());
            for (int i = 0; i < blocks.size(); i++) {
                Block b = blocks.get(i);
                for (int dy = 0; dy < b.getHeight(); dy++) {
                    for (int dx = 0; dx < b.getWidth(); dx++) {
                        int x = b.getX() + dx;
                        int y = b.getY() + dy;
                        g0[y][x] = i;
                        c0.get(i).add(new Point(x, y));
                    }
                }
            }
            return new State(0, g0, c0, null, null);
        }

        private int calcHeuristic() {
            List<Point> caoCells = cells.get(caoIdx);
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
            for (Point p : caoCells) {
                minX = Math.min(minX, p.x);
                minY = Math.min(minY, p.y);
            }
            int dist = Math.abs(minY - EXIT_R) + Math.abs(minX - EXIT_C);
            int blockCnt = 0;
            for (int y = minY; y < EXIT_R; y++) {
                int occ = grid[y][EXIT_C];
                if (occ != -1 && occ != caoIdx) blockCnt++;
            }
            return dist + blockCnt * BLOCKER_PENALTY;
        }

        boolean canMove(int idx, Board.Direction dir) {
            int dx = 0, dy = 0;
            switch (dir) {
                case UP -> dy = -1;
                case DOWN -> dy = 1;
                case LEFT -> dx = -1;
                case RIGHT -> dx = 1;
            }
            for (Point p : cells.get(idx)) {
                int nx = p.x + dx, ny = p.y + dy;
                if (nx < 0 || nx >= C || ny < 0 || ny >= R) return false;
                int occ = grid[ny][nx];
                if (occ != -1 && occ != idx) return false;
            }
            return true;
        }

        State move(int idx, Board.Direction dir) {
            int dx = 0, dy = 0;
            switch (dir) {
                case UP -> dy = -1;
                case DOWN -> dy = 1;
                case LEFT -> dx = -1;
                case RIGHT -> dx = 1;
            }
            int gNew = g + 1;
            int[][] ng = new int[R][C];
            for (int y = 0; y < R; y++) System.arraycopy(grid[y], 0, ng[y], 0, C);
            List<List<Point>> nc = new ArrayList<>();
            for (List<Point> lst : cells) {
                List<Point> cp = new ArrayList<>();
                for (Point p : lst) cp.add(new Point(p));
                nc.add(cp);
            }
            for (Point p : nc.get(idx)) ng[p.y][p.x] = -1;
            for (Point p : nc.get(idx)) {
                int nx = p.x + dx, ny = p.y + dy;
                ng[ny][nx] = idx;
            }
            for (Point p : nc.get(idx)) p.translate(dx, dy);
            return new State(gNew, ng, nc, this, new MoveInfo(idx, dir));
        }

        String key() {
            StringBuilder sb = new StringBuilder(R * C);
            for (int[] row : grid) {
                for (int v : row) sb.append((char) (v + 1));
                sb.append('|');
            }
            return sb.toString();
        }

        List<MoveInfo> buildPath() {
            LinkedList<MoveInfo> path = new LinkedList<>();
            for (State s = this; s.parent != null; s = s.parent) {
                path.addFirst(s.move);
            }
            return path;
        }
    }
}


