package Game1.AI;

import Game1.models.Board;
import Game1.models.Block;

import java.awt.Point;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多线程 A* 搜索求解器，带搜索进度输出
 */
public class AStarSolver {
    private static final int EXIT_R = 3, EXIT_C = 1;
    private static int R, C, caoIdx;
    private static List<Block> blocks;
    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    private static final AtomicInteger processed = new AtomicInteger(0);
    private static final AtomicInteger maxDepth = new AtomicInteger(0);

    public static List<MoveInfo> solve(Board board) {
        R = Board.ROWS;
        C = Board.COLS;
        blocks = board.getBlocks();

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
        PriorityBlockingQueue<State> open = new PriorityBlockingQueue<>();
        ConcurrentHashMap<StateKey, Integer> visited = new ConcurrentHashMap<>();

        open.offer(start);
        visited.put(start.key, start.g);

        AtomicBoolean solved = new AtomicBoolean(false);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CompletionService<List<MoveInfo>> completionService = new ExecutorCompletionService<>(executor);

        Runnable worker = () -> {
            while (!solved.get()) {
                State cur;
                try {
                    cur = open.poll(50, TimeUnit.MILLISECONDS);
                    if (cur == null) continue;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                int count = processed.incrementAndGet();
                if (cur.g > maxDepth.get()) maxDepth.set(cur.g);
                if (count % 100000 == 0) {
                    System.out.printf("线程[%s] 已处理状态数: %d, 当前最大深度: %d, open队列大小: %d, visited状态数: %d%n",
                            Thread.currentThread().getName(), count, maxDepth.get(), open.size(), visited.size());
                }

                if (cur.h == 0) {
                    if (solved.compareAndSet(false, true)) {
                        completionService.submit(() -> reconstructPath(cur));
                    }
                    return;
                }

                for (int idx = 0; idx < blocks.size(); idx++) {
                    for (Board.Direction dir : Board.Direction.values()) {
                        if (!cur.canMove(idx, dir)) continue;
                        if (cur.isReverseMove(idx, dir)) continue;

                        State ns = cur.move(idx, dir);
                        Integer prevG = visited.get(ns.key);
                        if (prevG == null || ns.g < prevG) {
                            visited.put(ns.key, ns.g);
                            open.offer(ns);
                        }
                    }
                }
            }
        };

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.execute(worker);
        }

        try {
            Future<List<MoveInfo>> result = completionService.take();
            executor.shutdownNow();
            return result.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
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
        final int g, h, f;
        final int[][] grid;
        final List<List<Point>> cells;
        final MoveInfo move;
        final State parent;
        final StateKey key;

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

        boolean isReverseMove(int idx, Board.Direction dir) {
            if (parent == null || move == null) return false;
            if (move.blockIndex == idx) {
                return switch (move.direction) {
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
            for (Point p : nc.get(idx)) ng[p.y][p.x] = -1;
            for (Point p : nc.get(idx)) {
                int nx = p.x + dx, ny = p.y + dy;
                ng[ny][nx] = idx;
            }
            for (Point p : nc.get(idx)) p.translate(dx, dy);
            return new State(gNew, ng, nc, new MoveInfo(idx, dir), this);
        }

        @Override
        public int compareTo(State o) {
            return Integer.compare(this.f, o.f);
        }
    }

    private static class StateKey {
        private final byte[] data;

        StateKey(int[][] grid) {
            data = new byte[R * C];
            for (int y = 0; y < R; y++) {
                for (int x = 0; x < C; x++) {
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