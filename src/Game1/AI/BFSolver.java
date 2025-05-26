package Game1.AI;

import Game1.models.Board;
import Game1.models.Block;
import java.awt.Point;
import java.util.*;

/**
 * 优化后的 Beam Search Solver:
 * - 保存状态节点和邻接路径，反向 BFS 标注目标距离 distToDestination。
 * - Beam 选择时结合 g + distToDestination + 启发式 h，提高搜索方向精准度。
 * - 支持路径回溯和终局检测。
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

        // 存储所有访问过的节点，key->state映射，便于后续反向标注距离和路径回溯
        Map<String, State> allStates = new HashMap<>();
        allStates.put(start.key(), start);

        List<State> beam = new ArrayList<>();
        beam.add(start);

        for (int depth = 0; depth <= MAX_DEPTH; depth++) {
            List<State> nextLayer = new ArrayList<>();
            for (State s : beam) {
                if (s.h == 0) {
                    // 找到目标，先做反向 BFS 标注距离，后续查询更快
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
                            // 建立双向路径
                            ns.parentStates.add(s);
                            s.childStates.add(ns);

                            nextLayer.add(ns);
                        } else {
                            // 状态已存在，建立邻接关系，方便反向搜索和距离更新
                            exist.parentStates.add(s);
                            s.childStates.add(exist);
                        }
                    }
                }
            }
            if (nextLayer.isEmpty()) break;

            // 如果反向距离尚未标注，优先用启发值h排序，否则用 g + distToDestination + h 排序
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
     * 反向 BFS 标注所有节点到目标的最短距离 distToDestination。
     * 使用双向路径关系 parentStates 和 childStates。
     */
    private static void backwardDistanceLabel(State goalState, Map<String, State> allStates) {
        Queue<State> queue = new LinkedList<>();
        goalState.distToDestination = 0;
        queue.offer(goalState);

        while (!queue.isEmpty()) {
            State cur = queue.poll();
            int dist = cur.distToDestination;
            // 访问所有前驱和后继节点（双向），保证距离全覆盖
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
        final int g;                   // 已走步数
        final int h;                   // 启发值
        final int[][] grid;
        final List<List<Point>> cells;
        final MoveInfo move;           // 到达该状态的动作
        final List<State> parentStates; // 双向路径，多个父状态（多个路径可能到同状态）
        final List<State> childStates;  // 双向路径，多个子状态
        int distToDestination = -1;   // 反向距离，初始-1表示未标注

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
            // 清除旧位置
            for (Point p : nc.get(idx)) ng[p.y][p.x] = -1;
            // 标记新位置
            for (Point p : nc.get(idx)) {
                int nx = p.x + dx, ny = p.y + dy;
                ng[ny][nx] = idx;
            }
            // 更新块坐标
            for (Point p : nc.get(idx)) p.translate(dx, dy);
            return new State(gNew, ng, nc, new MoveInfo(idx, dir));
        }

        /**
         * 状态唯一标识，基于网格内容生成字符串
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
         * 逆序构建从起点到本状态的移动路径
         */
        List<MoveInfo> buildPath() {
            LinkedList<MoveInfo> path = new LinkedList<>();
            State cur = this;
            while (cur.move != null) {
                path.addFirst(cur.move);
                if (cur.parentStates.isEmpty()) break; // 无父节点时结束
                // 为路径重建，选择一个父状态继续回溯（简单起见取第一个）
                cur = cur.parentStates.get(0);
            }
            return path;
        }
    }
}

