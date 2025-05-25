package Game1.AI;

import Game1.models.Board;
import Game1.models.Block;

import java.util.*;


public class AStarSolver {
    private static class State {
        final int[][] grid;
        final int g;
        final int h;
        final int f;
        final State parent;
        final MoveInfo move;

        State(int[][] grid, int g, State parent, MoveInfo move) {
            this.grid = grid;
            this.g = g;
            this.h = calcHeuristic(grid);
            this.f = this.g + this.h;
            this.parent = parent;
            this.move = move;
        }

        private int calcHeuristic(int[][] grid) {
            int targetR = 3, targetC = 1;
            int minR = Integer.MAX_VALUE, minC = Integer.MAX_VALUE;
            for (int r = 0; r < grid.length; r++) {
                for (int c = 0; c < grid[0].length; c++) {
                    if (grid[r][c] == caoIndex) {
                        minR = Math.min(minR, r);
                        minC = Math.min(minC, c);
                    }
                }
            }
            if (minR==Integer.MAX_VALUE) return Integer.MAX_VALUE;
            return Math.abs(minR - targetR) + Math.abs(minC - targetC);
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

    private static int caoIndex;

    public static List<MoveInfo> solve(Board board) {
        int R = Board.ROWS, C = Board.COLS;
        List<Block> blocks = new ArrayList<>(board.getBlocks());
        // 定位曹操块索引
        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).getType() == Block.BlockType.CAO_CAO) {
                caoIndex = i;
                break;
            }
        }
        int[][] grid0 = new int[R][C];
        for (int[] row : grid0) Arrays.fill(row, -1);
        for (int i = 0; i < blocks.size(); i++) {
            Block b = blocks.get(i);
            for (int dr = 0; dr < b.getHeight(); dr++) {
                for (int dc = 0; dc < b.getWidth(); dc++) {
                    grid0[b.getY() + dr][b.getX() + dc] = i;
                }
            }
        }

        PriorityQueue<State> open = new PriorityQueue<>(Comparator.comparingInt(s -> s.f));
        Map<String, Integer> bestG = new HashMap<>();

        State start = new State(grid0, 0, null, null);
        open.add(start);
        bestG.put(start.key(), 0);

        while (!open.isEmpty()) {
            State cur = open.poll();
            if (cur.h == 0) return buildPath(cur);

            for (int i = 0; i < blocks.size(); i++) {
                for (Board.Direction dir : Board.Direction.values()) {
                    if (!canMove(cur.grid, blocks.get(i), i, dir)) continue;
                    int[][] ng = moveGrid(cur.grid, blocks.get(i), i, dir);
                    State nxt = new State(ng, cur.g + 1, cur, new MoveInfo(i, dir));
                    String key = nxt.key();
                    if (!bestG.containsKey(key) || nxt.g < bestG.get(key)) {
                        bestG.put(key, nxt.g);
                        open.add(nxt);
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    private static boolean canMove(int[][] grid, Block b, int idx, Board.Direction dir) {
        int dx=0, dy=0;
        switch (dir) {
            case UP -> dy=-1;
            case DOWN -> dy=1;
            case LEFT -> dx=-1;
            case RIGHT -> dx=1;
        }
        int R=grid.length, C=grid[0].length;
        List<int[]> cells = new ArrayList<>();
        for (int r=0;r<R;r++) for (int c=0;c<C;c++) if (grid[r][c]==idx) cells.add(new int[]{r,c});
        for (int[] cell:cells) {
            int nr=cell[0]+dy, nc=cell[1]+dx;
            if (nr<0||nr>=R||nc<0||nc>=C) return false;
            if (grid[nr][nc]!=-1 && grid[nr][nc]!=idx) return false;
        }
        return true;
    }

    private static int[][] moveGrid(int[][] grid, Block b, int idx, Board.Direction dir) {
        int R=grid.length, C=grid[0].length;
        int[][] ng = new int[R][C];
        for (int r=0;r<R;r++) System.arraycopy(grid[r],0,ng[r],0,C);
        int dx=0, dy=0;
        switch (dir) {
            case UP -> dy=-1;
            case DOWN -> dy=1;
            case LEFT -> dx=-1;
            case RIGHT -> dx=1;
        }
        List<int[]> cells = new ArrayList<>();
        for (int r=0;r<R;r++) for (int c=0;c<C;c++) if (ng[r][c]==idx) cells.add(new int[]{r,c});
        int minR= Integer.MAX_VALUE, minC=Integer.MAX_VALUE;
        for (int[] cell:cells) {
            minR=Math.min(minR,cell[0]);
            minC=Math.min(minC,cell[1]);
        }
        // 清除原位置
        for (int[] cell:cells) ng[cell[0]][cell[1]]=-1;
        // 填充新位置
        for (int dr=0;dr<b.getHeight();dr++) {
            for (int dc=0;dc<b.getWidth();dc++) {
                ng[minR+dy+dr][minC+dx+dc] = idx;
            }
        }
        return ng;
    }

    private static List<MoveInfo> buildPath(State node) {
        LinkedList<MoveInfo> path = new LinkedList<>();
        while (node.parent != null) {
            path.addFirst(node.move);
            node = node.parent;
        }
        return path;
    }
}
