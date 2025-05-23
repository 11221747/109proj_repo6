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
        final Board.Direction move;
        final StateNode parent;

        StateNode(Board board, Board.Direction move, StateNode parent) {
            this.board = deepCopy(board);
            this.move = move;
            this.parent = parent;
        }

        // 生成唯一状态标识
        String getStateKey() {
            StringBuilder sb = new StringBuilder();
            for (Block block : board.getBlocks()) {
                sb.append(block.getType()).append(":")
                        .append(block.getX()).append(",")
                        .append(block.getY()).append("|");
            }
            return sb.toString();
        }
    }

    public static List<Board.Direction> findSolution(Board initialBoard) {
        Queue<StateNode> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        // 初始状态
        queue.add(new StateNode(initialBoard, null, null));
        visited.add(queue.peek().getStateKey());

        while (!queue.isEmpty()) {
            StateNode current = queue.poll();

            // 胜利条件检查
            if (current.board.isWin()) {
                return buildSolution(current);
            }

            // 生成所有可能的移动
            for (Block block : current.board.getBlocks()) {
                for (Board.Direction dir : Board.Direction.values()) {
                    Board newBoard = deepCopy(current.board);
                    Block newBlock = findCorrespondingBlock(newBoard, block);

                    if (newBoard.canMove(newBlock, dir)) {
                        newBoard.moveBlock(newBlock, dir);

                        StateNode newNode = new StateNode(newBoard, dir, current);
                        if (visited.add(newNode.getStateKey())) {
                            queue.add(newNode);
                        }
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    private static List<Board.Direction> buildSolution(StateNode node) {
        LinkedList<Board.Direction> solution = new LinkedList<>();
        while (node.parent != null) {
            solution.addFirst(node.move);
            node = node.parent;
        }
        return solution;
    }

    // 深度拷贝Board（基于您的序列化方案）
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

    // 在复制后的Board中找到对应的Block
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