package Game1.Controllers;



import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;


import Game1.AI.*;
import Game1.models.Block;
import Game1.models.Board;
import Game1.models.GameState;
import Game1.views.GameFrame;
import Game1.views.LoginFrame;

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
    private int level;
    private LoginFrame loginFrame;

    private Timer replayTimer;
    private Stack<Board> replayHistory;
    private boolean isReplaying = false;


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
        if (isReplaying) return;
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
            getBoard().saveState();
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
            oos.writeObject(   new GameState(board, currentUser.getUsername(), getRemainingSeconds(), getLevel() , isTimerEnabled, getBoard().getHistory()  ));
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
            getBoard().setHistory(   state.getHistory()   );

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
    public void autoSolve(String algorithm) {
        new SwingWorker<List<MoveInfo>, Void>() {
            @Override
            protected List<MoveInfo> doInBackground() {
                List<MoveInfo> solution = switch (algorithm) {
                    case "AStar" -> AStarSolver.solve(board);
                    case "Beam" -> BeamSolver.solve(board);
                    case "BiDirectional" -> BiDirectionalSolver.solve(board);

                    default -> AStarSolver.solve(board); // 默认使用A*
                };

                // 根据选择的算法调用对应的求解器

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
                                Thread.sleep(250);                      //ai解谜的步间间隔
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }).start();
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(gameframe,
                            "AI求解时发生错误: " + e.getMessage(),
                            "错误",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    //精彩回放
    public void playReplay() {
        // 获取当前棋盘的历史状态
        Stack<Board> history = board.getHistory();

        if (history == null || history.isEmpty()) {
            JOptionPane.showMessageDialog(gameframe,
                    "没有可回放的游戏记录",
                    "回放",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // 停止当前计时器
        if (countdownTimer != null && countdownTimer.isRunning()) {
            countdownTimer.stop();
        }

        // 创建回放历史副本（反转顺序：从最早到最新）
        replayHistory = new Stack<>();
        List<Board> historyList = new ArrayList<>(history);
        Collections.reverse(historyList); // 反转顺序
        replayHistory.addAll(historyList);

        // 添加当前状态作为最后一个状态
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(  getBoard()   );
            oos.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            Board currentCopy = (Board) ois.readObject();
            replayHistory.push(currentCopy);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        // 设置回放状态
        isReplaying = true;

        // 重置到初始状态
        if (!replayHistory.isEmpty()) {
            board = replayHistory.firstElement();
            gameframe.repaint();
        }

        // 创建回放计时器
        replayTimer = new Timer(250, e -> replayNextStep());    // 每...毫秒一步
        replayTimer.start();
    }

    // 回放下一步
    private void replayNextStep() {
        if (replayHistory == null || replayHistory.isEmpty()) {
            // 回放结束
            replayTimer.stop();
            isReplaying = false;
            JOptionPane.showMessageDialog(gameframe,
                    "回放结束",
                    "回放",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // 获取下一个状态
        Board nextState = replayHistory.pop();
        this.board = nextState;
        gameframe.repaint();


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
        // 停止回放
        if (replayTimer != null && replayTimer.isRunning()) {
            replayTimer.stop();
        }
        isReplaying = false;

        // 原有逻辑保持不变...
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

    public boolean isReplaying() {
        return isReplaying;
    }

    public void setReplaying(boolean replaying) {
        isReplaying = replaying;
    }
}


