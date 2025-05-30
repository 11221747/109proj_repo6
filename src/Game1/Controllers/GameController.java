package Game1.Controllers;



import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import Game1.AI.*;
import Game1.models.Block;
import Game1.models.Board;
import Game1.models.GameState;
import Game1.views.GameFrame;

import javax.swing.*;


public class GameController  {
    private static final String SAVE_DIR = "saves/";
    private static final String SAVE_EXT = ".klotski";

    private Board board;
    private User currentUser;

    //计时相关：
    private javax.swing.Timer countdownTimer;
    private int timeLimit;
    private int remainingSeconds ;
    private boolean isTimerEnabled = false; // 是否启用倒计时
    private boolean firstMove_done = false;

    private MusicPlayer musicPlayer;

    private GameFrame gameframe;


    public void setGameframe(GameFrame gameframe) {
        this.gameframe = gameframe;
        initTimer(timeLimit);
    }


    public void moveBlock(Board.Direction direction) {
        if (gameframe.getSelectedBlock() == null) return;

        if (!firstMove_done) {
            countdown_Start();
            setFirstMove_done(true);
        }

        Block block = gameframe.getSelectedBlock();
        if (!getBoard().canMove(block, direction)) return;

        // 计算目标位置
        Point target = new Point(block.getX(), block.getY());
        switch (direction) {
            case UP: target.y--; break;
            case DOWN: target.y++; break;
            case LEFT: target.x--; break;
            case RIGHT: target.x++; break;
        }

        // 播放音效
        musicPlayer.play("src/Game1/data/bubble.wav", false);

        // 启动动画
        gameframe.startAnimation(block, target);

        // 实际移动方块（动画完成后会调用）
        getBoard().moveBlock(block, direction);

        if (isWin()) {
            gameframe.sendMessage_Win();
        }
    }


    //构造方法和设置用户
    public GameController() {
        this.board = new Board();
        this.musicPlayer = new MusicPlayer();
    }



//    public void moveBlock(Board.Direction direction) {
//        if (gameframe.getSelectedBlock() == null) return;
//        //不是空的再接着
//        if (!firstMove_done) {
//            countdown_Start();
//            setFirstMove_done(true);
//        }
//
//        //可以move了之后
//        musicPlayer.play("src/Game1/data/bubble.wav",false);
//        getBoard().moveBlock(gameframe.getSelectedBlock(), direction);
//        //判断胜利直接终止
//        if (isWin()) {
//            gameframe.sendMessage_Win();
//        }
//
//    }




    //方法区
    //写入游戏
    public boolean saveGame() {
        if (currentUser == null) return false;

        //路径
        File dir = new File(SAVE_DIR);

        if (!dir.exists()) {
            dir.mkdir();         //先写上
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(
                //路径--一个用户一个文件--后缀
                new FileOutputStream(SAVE_DIR + currentUser.getUsername() + SAVE_EXT))) {
            //保存棋盘，用户名
            oos.writeObject(   new GameState(board, currentUser.getUsername(),  getRemainingSeconds()  )   );
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }



    //读取存档
    public boolean loadGame() {
        if (currentUser == null) return false;
        //getBoard().clearHistory();     //清除历史

        //载入文件
        File file = new File(SAVE_DIR + currentUser.getUsername() + SAVE_EXT);
        if (!file.exists()) return false;

        //input
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(file))) {
            GameState state = (GameState) ois.readObject();     //state包括username和board

            //读取后设置的参数：
            getCountdownTimer().stop();
            this.board = state.getBoard();

            setFirstMove_done(false);
            setRemainingSeconds(    state.getRemainingSeconds()  );
            initTimer(remainingSeconds);




            return true;

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    //撤回
    public boolean undo() {
        boolean success = board.undo();

        if (success) {
            gameframe.repaint();
        }

        return success;
    }


    //这里交给board去判断
    public boolean isWin() {
        return board.isWin();
    }

    //有关时间：
    private void initTimer(int initialTime ) {
        setRemainingSeconds(initialTime);        //todo     这个要设置为识别的剩余时间

        countdownTimer = new javax.swing.Timer(1000, e -> {
            remainingSeconds--;
            gameframe.updateTimerDisplay(remainingSeconds);     //更新时间条

            if (remainingSeconds <= 0) {
                countdownTimer.stop();
                gameframe.handleTimeOut(); // 显示超时提示
                resetGame();
            }

        });
    }

    // 启动/停止倒计时
    //第一步移动了再启动
    public void countdown_Start() {
        if (isTimerEnabled && !countdownTimer.isRunning()) {

            countdownTimer.start();

        }
    }

    public void countdown_GoOn(){

    }

    public void countdown_Stop() {
        countdownTimer.stop();
    }


    //AI相关
    // AI 自动求解
    public void autoSolve(String algorithm) {
        new SwingWorker<List<MoveInfo>, Void>() {
            @Override
            protected List<MoveInfo> doInBackground() {
                List<MoveInfo> solution = Collections.emptyList();

                // 根据选择的算法调用对应的求解器
                switch (algorithm) {
                    case "AStar":
                        solution = AStarSolver.solve(board);
                        break;
                    case "Beam":
                        solution = BeamSolver.solve(board);
                        break;
                    case "BiDirectional":
                        solution = BiDirectionalSolver.solve(board);
                        break;
                    default:
                        solution = AStarSolver.solve(board); // 默认使用A*
                }

                System.out.println(algorithm + " solution length: " + solution.size());
                return solution;
            }

            @Override
            protected void done() {
                try {
                    List<MoveInfo> solution = get();
                    if (solution == null || solution.isEmpty()) {
                        System.out.println("No solution found or empty list.");
                        JOptionPane.showMessageDialog(gameframe,
                                "AI求解失败，请尝试其他算法！",
                                "提示",
                                JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    // 执行动画
                    new Thread(() -> {
                        for (MoveInfo move : solution) {
                            SwingUtilities.invokeLater(() -> {
                                Block target = board.getBlocks().get(move.blockIndex);
                                gameframe.setSelectedBlock(target);
                                moveBlock(move.direction);
                            });
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }).start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }


    //一些工具javabean
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public Board getBoard() {
        return board;
    }

    public void resetGame() {
        board.reset();
        getBoard().clearHistory();

    }


    public Timer getCountdownTimer() {
        return countdownTimer;
    }

    public void setCountdownTimer(Timer countdownTimer) {
        this.countdownTimer = countdownTimer;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(int remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public boolean isTimerEnabled() {
        return isTimerEnabled;
    }

    public void setTimerEnabled(boolean timerEnabled) {
        isTimerEnabled = timerEnabled;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public boolean isFirstMove_done() {
        return firstMove_done;
    }

    public void setFirstMove_done(boolean firstMove_done) {
        this.firstMove_done = firstMove_done;
    }
}


