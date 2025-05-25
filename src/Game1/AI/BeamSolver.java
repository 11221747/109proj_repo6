package Game1.AI;

import Game1.models.Board;
import Game1.models.Block;
import java.awt.Point;
import java.util.*;

/**
 * Enhanced Beam Search Solver with g-based pruning:
 * - 每层保留前 W 个状态，按 f = g + h 排序
 * - 使用 bestG Map 跟踪每个状态最优 g，允许重访更优 g
 * - 扩大深度到 MAX_DEPTH
 */
public class BeamSolver {
    private static final int EXIT_R = 3, EXIT_C = 1;
    private static final int BLOCKER_PENALTY = 2;
    private static final int BEAM_WIDTH = 10000;
    private static final int MAX_DEPTH = 300;

    private static int R, C, caoIdx;
    private static List<Block> blocks;

    public static List<MoveInfo> solve(Board board) {
        R = Board.ROWS;
        C = Board.COLS;
        blocks = board.getBlocks();
        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).getType() == Block.BlockType.CAO_CAO) { caoIdx = i; break; }
        }

        State start = State.fromBoard();
        List<State> beam = new ArrayList<>();
        Map<String,Integer> bestG = new HashMap<>();
        beam.add(start);
        bestG.put(start.key(), 0);

        for (int depth = 0; depth <= MAX_DEPTH; depth++) {
            List<State> next = new ArrayList<>();
            for (State s : beam) {
                if (s.h == 0) {
                    System.out.println("Solution at depth=" + depth + ", length=" + s.g);
                    return s.buildPath();
                }
                for (int idx = 0; idx < blocks.size(); idx++) {
                    for (Board.Direction d : Board.Direction.values()) {
                        if (!s.canMove(idx,d)) continue;
                        State ns = s.move(idx,d);
                        String k = ns.key();
                        Integer prevG = bestG.get(k);
                        if (prevG == null || ns.g < prevG) {
                            bestG.put(k, ns.g);
                            next.add(ns);
                        }
                    }
                }
            }
            if (next.isEmpty()) break;
            next.sort(Comparator.comparingInt(a->a.g + a.h));
            beam = next.subList(0, Math.min(BEAM_WIDTH, next.size()));
            System.out.println("Beam depth=" + depth + ", beam size=" + beam.size());
        }
        System.out.println("Beam search failed after max depth.");
        return Collections.emptyList();
    }

    private static class State {
        final int g, h;
        final int[][] grid;
        final List<List<Point>> cells;
        final State parent;
        final MoveInfo move;

        private State(int g, int[][] grid, List<List<Point>> cells, State parent, MoveInfo move) {
            this.g = g; this.grid = grid; this.cells = cells;
            this.parent = parent; this.move = move;
            this.h = calcHeuristic();
        }

        static State fromBoard() {
            int[][] g0 = new int[R][C];
            for (int[] row:g0) Arrays.fill(row,-1);
            List<List<Point>> c0 = new ArrayList<>();
            for (int i=0;i<blocks.size();i++) c0.add(new ArrayList<>());
            for (int i=0;i<blocks.size();i++){
                Block b=blocks.get(i);
                for(int dy=0;dy<b.getHeight();dy++) for(int dx=0;dx<b.getWidth();dx++){
                    int x=b.getX()+dx, y=b.getY()+dy;
                    g0[y][x]=i; c0.get(i).add(new Point(x,y));
                }
            }
            return new State(0,g0,c0,null,null);
        }

        private int calcHeuristic(){
            List<Point> cao = cells.get(caoIdx);
            int minX=Integer.MAX_VALUE,minY=Integer.MAX_VALUE;
            for(Point p:cao){minX=Math.min(minX,p.x);minY=Math.min(minY,p.y);}
            int d=Math.abs(minY-EXIT_R)+Math.abs(minX-EXIT_C);
            int block=0;
            for(int y=minY;y<EXIT_R;y++){int o=grid[y][EXIT_C];if(o!=-1&&o!=caoIdx) block++;}
            return d+block*BLOCKER_PENALTY;
        }

        boolean canMove(int idx, Board.Direction d){
            int dx=0,dy=0; switch(d){case UP->dy=-1;case DOWN->dy=1;case LEFT->dx=-1;case RIGHT->dx=1;}
            for(Point p:cells.get(idx)){
                int nx=p.x+dx, ny=p.y+dy;
                if(nx<0||nx>=C||ny<0||ny>=R) return false;
                int o=grid[ny][nx]; if(o!=-1&&o!=idx) return false;
            }
            return true;
        }

        State move(int idx, Board.Direction d){
            int dx=0,dy=0; switch(d){case UP->dy=-1;case DOWN->dy=1;case LEFT->dx=-1;case RIGHT->dx=1;}
            int newG=g+1;
            int[][] ng=new int[R][C]; for(int y=0;y<R;y++) System.arraycopy(grid[y],0,ng[y],0,C);
            List<List<Point>> nc=new ArrayList<>();
            for(List<Point> lst:cells){List<Point> cp=new ArrayList<>();for(Point p:lst)cp.add(new Point(p));nc.add(cp);}
            for(Point p:nc.get(idx)) ng[p.y][p.x]=-1;
            for(Point p:nc.get(idx)){int nx=p.x+dx,ny=p.y+dy;ng[ny][nx]=idx;}
            for(Point p:nc.get(idx)) p.translate(dx,dy);
            return new State(newG,ng,nc,this,new MoveInfo(idx,d));
        }

        String key(){
            StringBuilder sb=new StringBuilder(R*C);
            for(int[] row:grid)for(int v:row)sb.append((char)(v+1));
            return sb.toString();
        }

        List<MoveInfo> buildPath(){
            LinkedList<MoveInfo> path=new LinkedList<>();
            for(State s=this;s.parent!=null;s=s.parent) path.addFirst(s.move);
            return path;
        }
    }
}
