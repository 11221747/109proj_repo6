package Game1.AI;


import Game1.models.Board;
import Game1.models.Block;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;

public class BFSolver {
    private static class StateNode {
        final Board board;
        final MoveInfo move;
        final StateNode parent;
        final int depth;

        StateNode(Board board, MoveInfo move, StateNode parent, int depth) {
            this.board = deepCopy(board);
            this.move = move;
            this.parent = parent;
            this.depth = depth;
        }


        String getStateKey() {
            List<Block> sortedBlocks = new ArrayList<>(board.getBlocks());
            sortedBlocks.sort(Comparator.comparingInt(Block::getY)
                    .thenComparingInt(Block::getX)
                    .thenComparing(Block::getType));
            StringBuilder sb = new StringBuilder();
            for (Block block : sortedBlocks) {
                sb.append(block.getX()).append(",")
                        .append(block.getY()).append(";");
            }
            return sb.toString();
        }
    }

    public static List<MoveInfo> findSolution(Board initialBoard) {
        Queue<StateNode> queue = new LinkedList<>();
        Map<String, Integer> visited = new HashMap<>();

        // 初始状态
        queue.add(new StateNode(initialBoard, null, null, 0));
        visited.put(queue.peek().getStateKey(), 0);

        while (!queue.isEmpty()) {
            StateNode current = queue.poll();

            if (current.board.isWin()) {
                return buildSolution(current);
            }

            // 遍历所有方块
            List<Block> blocks = current.board.getBlocks();
            for (int i = 0; i < blocks.size(); i++) {
                Block block = blocks.get(i);

                // 尝试所有方向
                for (Board.Direction dir : Board.Direction.values()) {
                    if (current.board.canMove(block, dir)) {
                        Board newBoard = deepCopy(current.board);
                        Block newBlock = newBoard.getBlocks().get(i);

                        newBoard.moveBlock(newBlock, dir);
                        StateNode newNode = new StateNode(newBoard,
                                new MoveInfo(i, dir), current, current.depth + 1);

                        String key = newNode.getStateKey();
                        Integer existDepth = visited.get(key);

                        // 仅当新状态深度更低时才加入队列
                        if (existDepth == null || existDepth > newNode.depth) {
                            visited.put(key, newNode.depth);
                            queue.add(newNode);
                        }
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    private static List<MoveInfo> buildSolution(StateNode node) {
        LinkedList<MoveInfo> solution = new LinkedList<>();
        while (node.parent != null) {
            solution.addFirst(node.move);
            node = node.parent;
        }
        return solution;
    }

    // 深度拷贝Board
    private static Board deepCopy(Board original) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(original);

            ByteArrayInputStream bais =
                    new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);

            return (Board) ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException("Deep copy failed", e);
        }
    }








    // 在复制后的Board中找到对应的Block    暂时用不到？
    private static Block findCorrespondingBlock(Board newBoard, Block original) {
        for (Block b : newBoard.getBlocks()) {
            if (b.getType() == original.getType() &&
                    b.getX() == original.getX() &&
                    b.getY() == original.getY()) {
                return b;
            }
        }
        return null;
    }




}

