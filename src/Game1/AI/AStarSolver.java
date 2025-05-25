package Game1.AI;

import Game1.models.Board;
import Game1.models.Block;

import java.awt.Point;
import java.util.*;

/**
 * A*���������
 * �����ҵ����·����
 */
public class AStarSolver {
    private static final int EXIT_R = 3, EXIT_C = 1;
    private static int R, C, caoIdx;
    private static List<Block> blocks;

    public static List<MoveInfo> solve(Board board) {
        R = Board.ROWS;
        C = Board.COLS;
        blocks = board.getBlocks();

        // �Ҳܲ�����
        caoIdx = -1;
        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).getType() == Block.BlockType.CAO_CAO) {
                caoIdx = i;
                break;
            }
        }
        if (caoIdx == -1) {
            System.out.println("δ�ҵ��ܲٷ��飡");
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
                System.out.println("��ǰ������ȣ�������: " + maxDepth);
            }

            if (cur.h == 0) {
                System.out.println("�ҵ����·����������" + cur.g);
                return reconstructPath(cur);
            }

            for (int idx = 0; idx < blocks.size(); idx++) {
                for (Board.Direction dir : Board.Direction.values()) {
                    if (!cur.canMove(idx, dir)) continue;
                    if (cur.isReverseMove(idx, dir)) continue; // �Ż��������������ƶ�

                    State ns = cur.move(idx, dir);
                    Integer visitedG = visited.get(ns.key);
                    if (visitedG == null || ns.g < visitedG) {
                        visited.put(ns.key, ns.g);
                        open.offer(ns);
                    }
                }
            }
        }
        System.out.println("�޽�");
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
        final int g; // ʵ�ʲ���
        final int h; // ����ʽ����
        final int f; // f = g + h
        final int[][] grid; // [R][C] ÿ����������-1��ʾ��
        final List<List<Point>> cells; // ÿ��ռ�ݵĸ�������
        final MoveInfo move; // ���ɱ�״̬���ƶ�
        final State parent;
        final StateKey key; // ���ձ�������hash�ͱȽ�

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

        // �ж��Ƿ�����һ���ķ����ƶ�������������ͷ
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
            // ���ԭλ��
            for (Point p : nc.get(idx)) ng[p.y][p.x] = -1;
            // ������λ��
            for (Point p : nc.get(idx)) {
                int nx = p.x + dx, ny = p.y + dy;
                ng[ny][nx] = idx;
            }
            // ���¿�����
            for (Point p : nc.get(idx)) p.translate(dx, dy);
            return new State(gNew, ng, nc, new MoveInfo(idx, dir), this);
        }

        @Override
        public int compareTo(State o) {
            return Integer.compare(this.f, o.f);
        }
    }

    /**
     * ״̬�����࣬ʹ�ý��յ� byte[] �洢 grid ��Ϣ��
     * ��д equals �� hashCode������visited�жϺ�HashMap��
     */
    private static class StateKey {
        private final byte[] data;

        StateKey(int[][] grid) {
            data = new byte[R * C];
            for (int y = 0; y < R; y++) {
                for (int x = 0; x < C; x++) {
                    // -1 ��ʾ�գ�ת����0��������+1����ֹ����
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
