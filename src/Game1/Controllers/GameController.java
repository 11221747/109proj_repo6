package Game1.Controllers;



import java.io.*;
import java.util.List;


import Game1.AI.AStarSolver;
import Game1.AI.BeamSolver;
import Game1.AI.MoveInfo;
import Game1.models.Block;
import Game1.models.Board;
import Game1.models.GameState;
import Game1.views.GameFrame;
import Game1.views.LoginFrame;

import javax.swing.*;


public class GameController {
    private static final String SAVE_DIR = "saves/";
    private static final String SAVE_EXT = ".klotski";

    private Board board;
    private User currentUser;

    //计时相关：
    private javax.swing.Timer countdownTimer;
    private int timeLimit;
    private int remainingSeconds;
    private boolean isTimerEnabled = false; // 是否启用倒计时
    private boolean firstMove_done = false;

    private MusicPlayer musicPlayer;

    private GameFrame gameframe;
    private int level;
    private LoginFrame loginFrame;

    public void setLoginFrame(LoginFrame loginFrame) {
        this.loginFrame = loginFrame;
    }


    public void setGameframe(GameFrame gameframe) {
        this.gameframe = gameframe;
        initTimer(timeLimit);
    }


    //构造方法和设置用户
    public GameController() {
        this.board = new Board();
        this.musicPlayer = new MusicPlayer();

    }


    public void moveBlock(Board.Direction direction) {
        if (gameframe.getSelectedBlock() == null) return;

        //不是空的再接着
        if (!firstMove_done) {
            countdown_Start();
            setFirstMove_done(true);
        }


        //可以move了之后
        musicPlayer.play("src/Game1/data/bubble.wav", false);
        getBoard().moveBlock(gameframe.getSelectedBlock(), direction);
        gameframe.repaint();

        //判断胜利直接终止
        if (isWin()) {
            gameframe.sendMessage_Win();

        }

    }


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
            oos.writeObject(   new GameState(board, currentUser.getUsername(), getRemainingSeconds(), getLevel() , isTimerEnabled)   );
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    //读取存档
    public boolean loadGame() {
        if (currentUser == null) return false;
        getBoard().clearHistory();     //清除历史

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
            setLevel(state.getLevel());
            setFirstMove_done(false);

            setRemainingSeconds(state.getRemainingSeconds());
            setTimerEnabled(state.isTimer_flag());
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
    private void initTimer(int initialTime) {
        setRemainingSeconds(initialTime);

        countdownTimer = new javax.swing.Timer(1000, e -> {
            remainingSeconds--;
            gameframe.updateTimerDisplay( getRemainingSeconds()  );     //更新时间条

            if (remainingSeconds <= 0) {
                countdownTimer.stop();
                gameframe.handleTimeOut(); // 显示超时提示
                resetGame();
            }

        });

        setCountdownTimer(  countdownTimer  );
    }

    // 启动/停止倒计时
    //第一步移动了再启动
    public void countdown_Start() {
        if (isTimerEnabled && ! getCountdownTimer().isRunning()) {
            getCountdownTimer().start();

        }
    }


    //AI相关
    // AI 自动求解
    public void autoSolve() {
        new SwingWorker<List<MoveInfo>, Void>() {
            @Override
            protected List<MoveInfo> doInBackground() {

                //这里要选算法了
                //beam最快但是绕远路
                //双向BFS比较快，路径很短，但是无法正确走完
                //Astar慢，但是是最快路径
                List<MoveInfo> solution = BeamSolver.solve(board);


                System.out.println("AI solution length: " + solution.size());
                return solution;
            }

            @Override
            protected void done() {
                try {
                    List<MoveInfo> solution = get();
                    if (solution == null || solution.isEmpty()) {
                        System.out.println("No solution found or empty list.");
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
                                Thread.sleep(250);
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


    //选关相关
    public void initialize_Board() {
        getBoard().initializeBoard(level);
    }

    public void startLevel(){
        loginFrame.openGameFrame(level);
    }



    //一些工具javabean



    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public Board getBoard() {
        return board;
    }

    public void resetGame() {
        board.reset(level);
        getBoard().setMoves(0);
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

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

}


