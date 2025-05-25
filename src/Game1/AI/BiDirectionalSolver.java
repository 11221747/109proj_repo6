package Game1.AI;

import Game1.models.Board;
import Game1.models.Block;

import java.awt.Point;
import java.util.*;

/**
 * �Ż���˫�� BFS �������
 * - Ԥ������������ʱ Board ʵ��
 * - �������淽������
 * - ���� blocksSize������ repeated ��������
 * - ʵʱ��ӡ��չ�ڵ��������ǰ��ȡ����д�С
 */
public class BiDirectionalSolver {
    private static final int MAX_DEPTH = 200;

    // �������飬�� Board.Direction ˳��һ��
    private static final Board.Direction[] DIRS = Board.Direction.values();
    private static final int[] DX = { 0, 0, -1, 1 }; // UP, DOWN, LEFT, RIGHT
    private static final int[] DY = { -1, 1, 0, 0 };

    private static class StateNode {
        final List<Point> pos;
        final StateNode parent;
        final MoveInfo move;
        final int depth;
        final String key;  // Ԥ����ã������ظ�����

        StateNode(List<Point> pos, StateNode parent, MoveInfo move, int depth) {
            this.pos = pos;
            this.parent = parent;
            this.move = move;
            this.depth = depth;
            this.key = buildKey(pos);
        }

        private static String buildKey(List<Point> pList) {
            StringBuilder sb = new StringBuilder(pList.size() * 2);
            for (Point p : pList) {
                sb.append((char) ('A' + p.x)).append((char) ('A' + p.y));
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

        // 1. ���� start/goal positions
        List<Point> startPos = new ArrayList<>(n);
        for (Block b : blocks) startPos.add(new Point(b.getX(), b.getY()));
        StateNode start = new StateNode(startPos, null, null, 0);

        List<Point> goalPos = new ArrayList<>(n);
        for (Block b : blocks) goalPos.add(new Point(b.getX(), b.getY()));
        // �Ѳܲٿ�ŵ�����(1,3)
        for (int i = 0; i < n; i++) if (blocks.get(i).getType() == Block.BlockType.CAO_CAO) {
            goalPos.get(i).setLocation(1, 3);
            break;
        }
        StateNode goal = new StateNode(goalPos, null, null, 0);

        // 2. ��������ʼ�¼
        Deque<StateNode> front = new ArrayDeque<>(), back = new ArrayDeque<>();
        Map<String, StateNode> visF = new HashMap<>(), visB = new HashMap<>();
        front.add(start); visF.put(start.key, start);
        back .add(goal ); visB.put( goal.key,  goal);

        // 3. Ԥ����һ����ʱ Board ���� canMove
        Board tempBoard = new Board(board);

        int depth = 0;
        long expandedF = 0, expandedB = 0;

        while (!front.isEmpty() && !back.isEmpty() && depth < MAX_DEPTH) {
            depth++;
            System.out.printf("Depth=%d, frontSize=%d, backSize=%d%n",
                    depth, front.size(), back.size());

            // ����չ��Сһ��
            if (front.size() <= back.size()) {
                if (expand(front, visF, visB, blocks, tempBoard, ++expandedF, "F")) {
                    return merge(visF, visB);
                }
            } else {
                if (expand(back, visB, visF, blocks, tempBoard, ++expandedB, "B")) {
                    return merge(visF, visB);
                }
            }
        }

        System.out.printf("Fail: expandedF=%d, expandedB=%d%n", expandedF, expandedB);
        return Collections.emptyList();
    }

    private static boolean expand(Deque<StateNode> queue,
                                  Map<String, StateNode> selfVis,
                                  Map<String, StateNode> otherVis,
                                  List<Block> blocks,
                                  Board tempBoard,
                                  long expandedCount,
                                  String tag) {
        int layerSize = queue.size();
        for (int k = 0; k < layerSize; k++) {
            StateNode cur = queue.poll();

            // �������
            if (expandedCount <= 10 || expandedCount % 1000 == 0) {
                System.out.printf("[%s] expanded=%d, depth=%d, queue=%d%n",
                        tag, expandedCount, cur.depth, queue.size());
            }

            // �ָ� tempBoard �� cur.pos
            for (int i = 0; i < blocks.size(); i++) {
                Point p = cur.pos.get(i);
                tempBoard.getBlocks().get(i).setPosition(p.x, p.y);
            }

            // �����ƶ�ÿ����
            for (int i = 0; i < blocks.size(); i++) {
                for (int d = 0; d < DIRS.length; d++) {
                    if (!tempBoard.canMove(tempBoard.getBlocks().get(i), DIRS[d])) continue;

                    // ����λ���б�
                    List<Point> np = new ArrayList<>(cur.pos.size());
                    for (Point p : cur.pos) np.add(new Point(p));

                    // �ƶ��� i ��
                    np.get(i).translate(DX[d], DY[d]);

                    StateNode next = new StateNode(np, cur, new MoveInfo(i, DIRS[d]), cur.depth + 1);
                    if (selfVis.containsKey(next.key)) continue;
                    selfVis.put(next.key, next);

                    if (otherVis.containsKey(next.key)) {
                        System.out.println("Meet at depth=" + next.depth);
                        return true;
                    }
                    queue.add(next);
                }
            }
        }
        return false;
    }

    private static List<MoveInfo> merge(Map<String, StateNode> visF,
                                        Map<String, StateNode> visB) {
        for (Map.Entry<String, StateNode> e : visF.entrySet()) {
            String key = e.getKey();
            if (visB.containsKey(key)) {
                StateNode midF = e.getValue(), midB = visB.get(key);

                // ǰ��·��
                List<MoveInfo> p1 = midF.buildPath();
                // ����·�������򡢷�����
                List<MoveInfo> p2 = midB.buildPath();
                Collections.reverse(p2);
                for (int i = 0; i < p2.size(); i++) {
                    MoveInfo mv = p2.get(i);
                    p2.set(i, new MoveInfo(mv.blockIndex, opposite(mv.direction)));
                }
                p1.addAll(p2);
                System.out.println("Total depth=" + p1.size());
                return p1;
            }
        }
        return Collections.emptyList();
    }

    private static Board.Direction opposite(Board.Direction d) {
        switch (d) {
            case UP:    return Board.Direction.DOWN;
            case DOWN:  return Board.Direction.UP;
            case LEFT:  return Board.Direction.RIGHT;
            default:    return Board.Direction.LEFT;
        }
    }
}
