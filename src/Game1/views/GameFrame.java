package Game1.views;


import javax.swing.*;
import Game1.Controllers.GameController;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

import Game1.Controllers.MusicPlayer;
import Game1.Controllers.UserController;
import Game1.models.Block;
import Game1.models.Board;



public class GameFrame extends JFrame {
    private static final int CELL_SIZE = 80;
    private static final int BOARD_WIDTH = Board.COLS * CELL_SIZE;
    private static final int BOARD_HEIGHT = Board.ROWS * CELL_SIZE;

    private GameController controller;
    private BoardPanel boardPanel;
    private JProgressBar timeBar;
    private UserController userController;
    private GameController gameController;
    private MusicPlayer musicPlayer;


    private Block selectedBlock;

    //构造方法
    public GameFrame(GameController controller) {
        this.controller = controller;
        this.userController= userController;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("Klotski Puzzle");
        setResizable(false);
        setSize(591, 550);////有点矮，如果把按钮移到左边就不矮了
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setLayout(new BorderLayout()); // 添加布局管理器

        layeredPane.setPreferredSize(new Dimension(591, 550));

        setContentPane(layeredPane);
        // 先添加背景
        addBackground(layeredPane);

        // 再初始化UI组件
        initUI(layeredPane);

        setLocationRelativeTo(null);
    }

    protected void addBackground(JLayeredPane layeredPane) {
        try {
            String imagePath = "src/Game1/pic/gameBackground.png";
            ImageIcon originalIcon = new ImageIcon(imagePath);

            int imgWidth = originalIcon.getIconWidth();
            int imgHeight = originalIcon.getIconHeight();

            //填满
            double widthRatio = (double)591 / imgWidth;
            double heightRatio = (double)550 / imgHeight;
            double scaleRatio = Math.max(widthRatio, heightRatio);

            // 计算缩放后尺存
            int scaledWidth = (int)(imgWidth * scaleRatio);
            int scaledHeight = (int)(imgHeight * scaleRatio);

            // 缩放
            Image scaledImage = originalIcon.getImage().getScaledInstance(
                    scaledWidth,
                    scaledHeight,
                    Image.SCALE_SMOOTH
            );

            //左对齐
            JLabel background = new JLabel(new ImageIcon(scaledImage)) {
                @Override
                public void paintComponent(Graphics g) {

                    int y = (getHeight() - scaledHeight) / 2;
                    g.drawImage(scaledImage, 0, y, this);
                }
            };

            background.setBounds(0, 0, 591, 550);
            layeredPane.add(background, JLayeredPane.DEFAULT_LAYER);

        } catch (Exception e) {
//            System.err.println("加载背景图片失败: " + e.getMessage());
            layeredPane.setBackground(Color.CYAN);
        }
    }

    private void initUI(JLayeredPane layeredPane) {
        setLayout(null);
        setTitle("Klotski Puzzle");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        // 游戏主面板（需手动设置位置和大小）
        boardPanel = new BoardPanel();
        boardPanel.setOpaque(false);
        boardPanel.setBounds(250, 50, 591, 400);
        layeredPane.add(boardPanel, JLayeredPane.PALETTE_LAYER);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(null);
        controlPanel.setBounds(0, 450, 591, 100); // 控制面板位置

        JButton upButton = new JButton("Up");
        upButton.setFont(new Font("大字体", Font.PLAIN, 10));

        JButton downButton = new JButton("Down");
        downButton.setFont(new Font("大字体", Font.PLAIN, 10));

        JButton leftButton = new JButton("Left");
        leftButton.setFont(new Font("大字体", Font.PLAIN, 10));

        JButton rightButton = new JButton("Right");
        rightButton.setFont(new Font("大字体", Font.PLAIN, 10));

        JButton resetButton = new JButton("Reset");
        resetButton.setFont(new Font("大字体", Font.PLAIN, 10));

        JButton saveButton = new JButton("Save");
       saveButton.setFont(new Font("大字体", Font.PLAIN, 10));

        JButton loadButton = new JButton("Load");
        loadButton.setFont(new Font("大字体", Font.PLAIN, 10));

        JButton undoButton = new JButton("Undo");//撤回键的initial在这
        undoButton.setFont(new Font("大字体", Font.PLAIN, 10));

        int btnWidth = 80;
        int btnHeight = 30;

        upButton.setBounds(65, 100, btnWidth, btnHeight);
        leftButton.setBounds(30, 135, btnWidth, btnHeight);
        rightButton.setBounds(110, 135, btnWidth, btnHeight);
        downButton.setBounds(65, 180, btnWidth, btnHeight);

        resetButton.setBounds(65, 270, btnWidth, btnHeight);
        saveButton.setBounds(65, 305, btnWidth, btnHeight);
        loadButton.setBounds(65, 340, btnWidth, btnHeight);
        undoButton.setBounds(65, 375, btnWidth, btnHeight);


        upButton.addActionListener(e -> controller.moveBlock(Board.Direction.UP));
        downButton.addActionListener(e ->  controller.moveBlock(Board.Direction.DOWN));
        leftButton.addActionListener(e ->  controller.moveBlock(Board.Direction.LEFT));
        rightButton.addActionListener(e ->  controller.moveBlock(Board.Direction.RIGHT));


        saveButton.addActionListener(e -> {
            if (controller.saveGame()) {
                JOptionPane.showMessageDialog(this, "Game saved successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "Failed to save game.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        loadButton.addActionListener(e -> {
            if (controller.loadGame()) {
                selectedBlock = null;
                boardPanel.repaint();
                JOptionPane.showMessageDialog(this, "Game loaded successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "Failed to load game.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        resetButton.addActionListener(e -> {
            controller.resetGame();
            selectedBlock = null;
            boardPanel.repaint();
        });

        //新加的撤回按钮
        undoButton.addActionListener(e -> {
            if (controller.undo()) {
                selectedBlock = null; // 清空选中块
                boardPanel.repaint();
            } else {
                JOptionPane.showMessageDialog(this,
                        "没有更多可撤销的步骤",
                        "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });


        layeredPane.add(upButton, JLayeredPane.PALETTE_LAYER);
        layeredPane.add(downButton, JLayeredPane.PALETTE_LAYER);
        layeredPane.add(rightButton, JLayeredPane.PALETTE_LAYER);
        layeredPane.add(leftButton, JLayeredPane.PALETTE_LAYER);
        layeredPane.add(saveButton, JLayeredPane.PALETTE_LAYER);
        layeredPane.add(loadButton, JLayeredPane.PALETTE_LAYER);
        layeredPane.add(undoButton, JLayeredPane.PALETTE_LAYER);
        layeredPane.add(resetButton, JLayeredPane.PALETTE_LAYER);
        //限时相关的

        timeBar = new JProgressBar(0, controller.getTimeLimit());
        timeBar.setStringPainted(true);
        timeBar.setBounds(0, 0, 591, 20);
        layeredPane.add(timeBar, JLayeredPane.PALETTE_LAYER);


        // 创建AI选择下拉框
        String[] aiAlgorithms = {"A* Search(优先40s)", "Beam Search (2s多步)", "Bidirectional BFS (快，步少，不一定成功)", "BFS (基础)"};
        JComboBox<String> aiComboBox = new JComboBox<>(aiAlgorithms);
        aiComboBox.setBounds(65, 450, 150, 25); // 调整宽度以显示完整名称

        // AI执行按钮
        JButton aiSolveBtn = new JButton("AI求解");
        aiSolveBtn.setBounds(65, 480, 150, 30);

        // 添加监听器
        aiSolveBtn.addActionListener(e -> {
            String selectedAlgorithm = (String) aiComboBox.getSelectedItem();
            String algorithmKey = "";

            // 映射UI显示名称到算法标识
            if (selectedAlgorithm.startsWith("AStar")) {
                algorithmKey = "AStar";
            } else if (selectedAlgorithm.startsWith("Beam")) {
                algorithmKey = "Beam";
            } else if (selectedAlgorithm.startsWith("Bidirectional")) {
                algorithmKey = "BiDirectional";
            } else {
                algorithmKey = "BFS";
            }

            controller.autoSolve(algorithmKey);
        });

         // 添加到界面
        layeredPane.add(aiComboBox, JLayeredPane.PALETTE_LAYER);
        layeredPane.add(aiSolveBtn, JLayeredPane.PALETTE_LAYER);
    }

    public void updateTimerDisplay(int seconds) {
        SwingUtilities.invokeLater(() -> {
            timeBar.setValue(seconds);
            timeBar.setString(String.format("%02d:%02d", seconds/60, seconds%60));
        });
    }

    public void handleTimeOut() {
        JOptionPane.showMessageDialog(this, "time is up! game is over");
        setVisible(false);
        this.dispose();
        System.exit(0);
        // 返回登录界面
        //之后怎么做？？todo
        //超时了之后游戏其实因该继续

    }

    public void setTimeBar(JProgressBar timeBar) {
        this.timeBar = timeBar;
    }


    private class BoardPanel extends JPanel {
        public BoardPanel() {

            setFocusable(true);  // 允许面板获得焦点     //确实聚焦了，虽然也不完全清楚背后原理
            requestFocusInWindow(); // 主动请求焦点           监听是不是太多了点？

            //尝试1绑定键盘
            InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap actionMap = getActionMap();

            // 绑定方向键
            //这个版本必须先用鼠标点一下？这样就focus了才行？
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "moveUp");
            actionMap.put("moveUp", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    controller.moveBlock(Board.Direction.UP);
                }
            });
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "moveDown");
            actionMap.put("moveDown", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    controller.moveBlock(Board.Direction.DOWN);
                }
            });
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "moveLeft");
            actionMap.put("moveLeft", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    controller.moveBlock(Board.Direction.LEFT);
                }
            });
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "moveRight");
            actionMap.put("moveRight", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    controller.moveBlock(Board.Direction.RIGHT);
                }
            });


            //重新获取聚焦    测试了用不到不管了
            /*
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    requestFocusInWindow();
                }
            });
            */

            //set...
            setPreferredSize(new Dimension(BOARD_WIDTH, BOARD_HEIGHT));
            setBackground(Color.LIGHT_GRAY);

            //获取选中的方块
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent evt) {
                    Point p = new Point(evt.getX() / CELL_SIZE, evt.getY() / CELL_SIZE);
                    selectedBlock = controller.getBoard().getBlockAt(p);
                    repaint();
                }
            });
        }


        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // 绘制背景网格
            g.setColor(new Color(200, 200, 200, 150)); // 半透明灰色
            for (int i = 0; i <= Board.ROWS; i++) {
                g.drawLine(0, i * CELL_SIZE, BOARD_WIDTH, i * CELL_SIZE);
            }
            for (int j = 0; j <= Board.COLS; j++) {
                g.drawLine(j * CELL_SIZE, 0, j * CELL_SIZE, BOARD_HEIGHT);
            }


            // Draw blocks
            for (Block block : controller.getBoard().getBlocks()) {
                int x = block.getX() * CELL_SIZE;
                int y = block.getY() * CELL_SIZE;
                int width = block.getWidth() * CELL_SIZE;
                int height = block.getHeight() * CELL_SIZE;
                // 绘制贴图
                BufferedImage texture = block.getTexture();

                    g.drawImage(texture, x, y, width, height, this);

                // 绘制选中框
                if (block == selectedBlock) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setColor(Color.BLUE);
                    g2d.setStroke(new BasicStroke(3));
                    g2d.drawRect(x, y, width, height);
                    g2d.dispose();
                }

                // 绘制出口
                Graphics2D exitG = (Graphics2D) g.create();
                exitG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                exitG.setColor(new Color(253, 229, 45, 87));
                exitG.fillRect(2 * CELL_SIZE, 4 * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                exitG.dispose();

            }

            // Highlight selected block
            if (selectedBlock != null) {
                int x = selectedBlock.getX() * CELL_SIZE;
                int y = selectedBlock.getY() * CELL_SIZE;
                int width = selectedBlock.getWidth() * CELL_SIZE;
                int height = selectedBlock.getHeight() * CELL_SIZE;

                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(Color.orange);
                g2.setStroke(new BasicStroke(3));
                g2.drawRect(x, y, width, height);
            }

            // Draw move count
            g.setColor(Color.BLACK);
            g.drawString("Moves: " + controller.getBoard().getMoves(), 10, 20);
        }
    }

    //1.0版本之后新增的方法：
    public void sendMessage_Win(){
        JOptionPane.showMessageDialog(this,
                "Congratulations! You won in " + this.getController().getBoard().getMoves() + " moves!");

        getController().resetGame();
        setSelectedBlock(null);
        SwingUtilities.invokeLater(this::repaint);
    }



    //javabean

    public Block getSelectedBlock() {
        return selectedBlock;
    }

    public void setSelectedBlock(Block selectedBlock) {
        this.selectedBlock = selectedBlock;
    }

    public BoardPanel getBoardPanel() {
        return boardPanel;
    }

    public void setBoardPanel(BoardPanel boardPanel) {
        this.boardPanel = boardPanel;
    }

    public GameController getController() {
        return controller;
    }

    public void setController(GameController controller) {
        this.controller = controller;
    }


}
