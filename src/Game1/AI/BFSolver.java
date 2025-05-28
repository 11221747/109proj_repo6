package Game1.AI;


import Game1.models.Board;
import Game1.models.Block;
import java.awt.Point;
import java.util.*;

/**
 * �Ż���� Beam Search Solver:
 * - ����״̬�ڵ���ڽ�·�������� BFS ��עĿ����� distToDestination��
 * - Beam ѡ��ʱ��� g + distToDestination + ����ʽ h�������������׼�ȡ�
 * - ֧��·�����ݺ��վּ�⡣
 */
public class BFSolver {

    private static final int EXIT_R = 3, EXIT_C = 1;
    private static final int BLOCKER_PENALTY = 2;
    private static final int BEAM_WIDTH = 2000;
    private static final int MAX_DEPTH = 200;

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

        // �洢���з��ʹ��Ľڵ㣬key->stateӳ�䣬���ں��������ע�����·������
        Map<String, State> allStates = new HashMap<>();
        allStates.put(start.key(), start);

        List<State> beam = new ArrayList<>();
        beam.add(start);

        for (int depth = 0; depth <= MAX_DEPTH; depth++) {
            List<State> nextLayer = new ArrayList<>();
            for (State s : beam) {
                if (s.h == 0) {
                    // �ҵ�Ŀ�꣬�������� BFS ��ע���룬������ѯ����
                    backwardDistanceLabel(s, allStates);
                    System.out.println("Found solution at depth=" + depth + ", path length=" + s.g);
                    return s.buildPath();
                }

                for (int idx = 0; idx < blocks.size(); idx++) {
                    for (Board.Direction dir : Board.Direction.values()) {
                        if (!s.canMove(idx, dir)) continue;
                        State ns = s.move(idx, dir);
                        String key = ns.key();
                        State exist = allStates.get(key);
                        if (exist == null) {
                            allStates.put(key, ns);
                            // ����˫��·��
                            ns.parentStates.add(s);
                            s.childStates.add(ns);

                            nextLayer.add(ns);
                        } else {
                            // ״̬�Ѵ��ڣ������ڽӹ�ϵ�����㷴�������;������
                            exist.parentStates.add(s);
                            s.childStates.add(exist);
                        }
                    }
                }
            }
            if (nextLayer.isEmpty()) break;

            // ������������δ��ע������������ֵh���򣬷����� g + distToDestination + h ����
            boolean hasDistLabels = allStates.values().stream().anyMatch(st -> st.distToDestination != -1);
            if (hasDistLabels) {
                nextLayer.sort(Comparator.comparingInt(a -> a.g + (a.distToDestination == -1 ? 1000000 : a.distToDestination) + a.h));
            } else {
                nextLayer.sort(Comparator.comparingInt(a -> a.g + a.h));
            }

            beam.clear();
            for (int i = 0; i < Math.min(BEAM_WIDTH, nextLayer.size()); i++) {
                beam.add(nextLayer.get(i));
            }

            System.out.println("Beam depth: " + depth + ", beam size: " + beam.size());
        }

        System.out.println("Beam search failed after max depth.");
        return Collections.emptyList();
    }

    /**
     * ���� BFS ��ע���нڵ㵽Ŀ�����̾��� distToDestination��
     * ʹ��˫��·����ϵ parentStates �� childStates��
     */
    private static void backwardDistanceLabel(State goalState, Map<String, State> allStates) {
        Queue<State> queue = new LinkedList<>();
        goalState.distToDestination = 0;
        queue.offer(goalState);

        while (!queue.isEmpty()) {
            State cur = queue.poll();
            int dist = cur.distToDestination;
            // ��������ǰ���ͺ�̽ڵ㣨˫�򣩣���֤����ȫ����
            List<State> neighbors = new ArrayList<>();
            neighbors.addAll(cur.parentStates);
            neighbors.addAll(cur.childStates);

            for (State neighbor : neighbors) {
                if (neighbor.distToDestination == -1 || neighbor.distToDestination > dist + 1) {
                    neighbor.distToDestination = dist + 1;
                    queue.offer(neighbor);
                }
            }
        }
    }

    private static class State {
        final int g;                   // ���߲���
        final int h;                   // ����ֵ
        final int[][] grid;
        final List<List<Point>> cells;
        final MoveInfo move;           // �����״̬�Ķ���
        final List<State> parentStates; // ˫��·���������״̬�����·�����ܵ�ͬ״̬��
        final List<State> childStates;  // ˫��·���������״̬
        int distToDestination = -1;   // ������룬��ʼ-1��ʾδ��ע

        private State(int g, int[][] grid, List<List<Point>> cells, MoveInfo move) {
            this.g = g;
            this.grid = grid;
            this.cells = cells;
            this.move = move;
            this.parentStates = new ArrayList<>();
            this.childStates = new ArrayList<>();
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
            return new State(0, g0, c0, null);
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
            // �����λ��
            for (Point p : nc.get(idx)) ng[p.y][p.x] = -1;
            // �����λ��
            for (Point p : nc.get(idx)) {
                int nx = p.x + dx, ny = p.y + dy;
                ng[ny][nx] = idx;
            }
            // ���¿�����
            for (Point p : nc.get(idx)) p.translate(dx, dy);
            return new State(gNew, ng, nc, new MoveInfo(idx, dir));
        }

        /**
         * ״̬Ψһ��ʶ�������������������ַ���
         */
        String key() {
            StringBuilder sb = new StringBuilder(R * C);
            for (int[] row : grid) {
                for (int v : row) sb.append((char) (v + 1));
                sb.append('|');
            }
            return sb.toString();
        }

        /**
         * ���򹹽�����㵽��״̬���ƶ�·��
         */
        List<MoveInfo> buildPath() {
            LinkedList<MoveInfo> path = new LinkedList<>();
            State cur = this;
            while (cur.move != null) {
                path.addFirst(cur.move);
                if (cur.parentStates.isEmpty()) break; // �޸��ڵ�ʱ����
                // Ϊ·���ؽ���ѡ��һ����״̬�������ݣ������ȡ��һ����
                cur = cur.parentStates.get(0);
            }
            return path;
        }
    }
}

