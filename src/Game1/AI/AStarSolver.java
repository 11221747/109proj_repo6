package Game1.AI;

import Game1.models.Board;
import Game1.models.Block;

import java.awt.Point;
import java.util.*;

/**
 * A*搜索求解器
 * 保障找到最短路径解
 */
public class AStarSolver {
    private static final int EXIT_R = 3, EXIT_C = 1;
    private static int R, C, caoIdx;
    private static List<Block> blocks;

    public static List<MoveInfo> solve(Board board) {
        R = Board.ROWS;
        C = Board.COLS;
        blocks = board.getBlocks();

        // 找曹操索引
        caoIdx = -1;
        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).getType() == Block.BlockType.CAO_CAO) {
                caoIdx = i;
                break;
            }
        }
        if (caoIdx == -1) {
            System.out.println("未找到曹操方块！");
            return Collections.emptyList();
        }

        State start = State.fromBoard();

        PriorityQueue<State> open = new PriorityQueue<>();
        Map<StateKey, Integer> visited = new HashMap<>();

        open.offer(start);
        visited.put(start.key, start.g);

        int maxDepth = 0;

        while (!open.isEmpty()) {
            State cur = open.poll();

            if (cur.g > maxDepth) {
                maxDepth = cur.g;
                System.out.println("当前搜索深度（步数）: " + maxDepth);
            }

            if (cur.h == 0) {
                System.out.println("找到最短路径，步数：" + cur.g);
                return reconstructPath(cur);
            }

            for (int idx = 0; idx < blocks.size(); idx++) {
                for (Board.Direction dir : Board.Direction.values()) {
                    if (!cur.canMove(idx, dir)) continue;
                    if (cur.isReverseMove(idx, dir)) continue; // 优化避免立即反向移动

                    State ns = cur.move(idx, dir);
                    Integer visitedG = visited.get(ns.key);
                    if (visitedG == null || ns.g < visitedG) {
                        visited.put(ns.key, ns.g);
                        open.offer(ns);
                    }
                }
            }
        }
        System.out.println("无解");
        return Collections.emptyList();
    }

    private static List<MoveInfo> reconstructPath(State state) {
        LinkedList<MoveInfo> path = new LinkedList<>();
        while (state.move != null) {
            path.addFirst(state.move);
            state = state.parent;
        }
        return path;
    }

    private static class State implements Comparable<State> {
        final int g; // 实际步数
        final int h; // 启发式估价
        final int f; // f = g + h
        final int[][] grid; // [R][C] 每格存块索引，-1表示空
        final List<List<Point>> cells; // 每块占据的格子坐标
        final MoveInfo move; // 生成本状态的移动
        final State parent;
        final StateKey key; // 紧凑编码用于hash和比较

        private State(int g, int[][] grid, List<List<Point>> cells, MoveInfo move, State parent) {
            this.g = g;
            this.grid = grid;
            this.cells = cells;
            this.move = move;
            this.parent = parent;
            this.h = calcHeuristic();
            this.f = g + h;
            this.key = new StateKey(grid);
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
            return Math.abs(minY - EXIT_R) + Math.abs(minX - EXIT_C);
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

        // 判断是否是上一步的反向移动，避免立即回头
        boolean isReverseMove(int idx, Board.Direction dir) {
            if (parent == null || move == null) return false;
            if (move.blockIndex == idx) {
                Board.Direction lastDir = move.direction;
                return switch (lastDir) {
                    case UP -> dir == Board.Direction.DOWN;
                    case DOWN -> dir == Board.Direction.UP;
                    case LEFT -> dir == Board.Direction.RIGHT;
                    case RIGHT -> dir == Board.Direction.LEFT;
                };
            }
            return false;
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
            // 清空原位置
            for (Point p : nc.get(idx)) ng[p.y][p.x] = -1;
            // 更新新位置
            for (Point p : nc.get(idx)) {
                int nx = p.x + dx, ny = p.y + dy;
                ng[ny][nx] = idx;
            }
            // 更新块坐标
            for (Point p : nc.get(idx)) p.translate(dx, dy);
            return new State(gNew, ng, nc, new MoveInfo(idx, dir), this);
        }

        @Override
        public int compareTo(State o) {
            return Integer.compare(this.f, o.f);
        }
    }

    /**
     * 状态编码类，使用紧凑的 byte[] 存储 grid 信息，
     * 重写 equals 和 hashCode，用于visited判断和HashMap键
     */
    private static class StateKey {
        private final byte[] data;

        StateKey(int[][] grid) {
            data = new byte[R * C];
            for (int y = 0; y < R; y++) {
                for (int x = 0; x < C; x++) {
                    // -1 表示空，转换成0，块索引+1，防止负数
                    data[y * C + x] = (byte) (grid[y][x] + 1);
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StateKey)) return false;
            StateKey other = (StateKey) o;
            return Arrays.equals(data, other.data);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(data);
        }
    }
}
