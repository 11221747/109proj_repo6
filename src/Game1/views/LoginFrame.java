package Game1.views;


import Game1.Controllers.GameController;
import Game1.Controllers.MusicPlayer;
import Game1.Controllers.UserController;
import Game1.Controllers.User;
import Game1.models.UserStorage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class LoginFrame extends JFrame {
    private UserController userController;
    private GameController gameController;
    private JTextField usernameField;
    private JTextField passwordField;
    private JButton SubmitBtn;
    private JButton GuestBtn;
    private JButton RegisterBtn;
    private MusicPlayer musicPlayer;


    public LoginFrame(UserController userController, GameController gameController, MusicPlayer musicPlayer) {
        this.userController = userController;
        this.gameController = gameController;
        this.musicPlayer=  musicPlayer;
        initUI();
        musicPlayer.play("data/groove.wav",true); // 循环播放
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                musicPlayer.stop(); // 窗口关闭时关闭背景音乐
            }
        });
    }

    private void initUI() {
        setTitle("Klotski Puzzle - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setLayout(null);//取消容器的默认居中方法，按照坐标xy添加组件
        int windowWidth = 800;
        int windowHeight = 600;
        setSize(windowWidth, windowHeight);
        setLocationRelativeTo(null); // 窗口居中
        this.setDefaultCloseOperation(3);//


//        JLabel userLabel = FrameUtil.createJLabel(this, new Point(50, 20), 120, 60, "username");
//        JLabel passLabel = FrameUtil.createJLabel(this, new Point(50, 80), 120, 60, "password");



        usernameField = FrameUtil.createJTextField(this, new Point(410, 103), 250, 60);
        passwordField = FrameUtil.createJTextField(this, new Point(410, 200), 250, 60);
        Font f = new Font("大字体", Font.PLAIN, 20);
        usernameField.setFont(f);
        passwordField.setFont(f);

//        this.getContentPane().add(userLabel);
        this.getContentPane().add(usernameField);
//        this.getContentPane().add(passLabel);
        this.getContentPane().add(passwordField);



        SubmitBtn = FrameUtil.createButton(this, "Submit", new Point(80, 420), 160, 60);
        SubmitBtn.setFont(new Font("大字体", Font.PLAIN, 10));
        SubmitBtn.setOpaque(false);          // 不绘制背景
        SubmitBtn.setContentAreaFilled(false); // 不填充内容区域
        SubmitBtn.setBorderPainted(false);    // 不绘制边框
        SubmitBtn.setForeground(new Color(0, 0, 0, 0));//透明字体
        SubmitBtn.setFocusPainted(false);

        GuestBtn = FrameUtil.createButton(this, "Guest", new Point(310, 420), 160, 60);
        GuestBtn.setFont(new Font("大字体", Font.PLAIN, 10));
        GuestBtn.setOpaque(false);          // 不绘制背景
        GuestBtn.setContentAreaFilled(false); // 不填充内容区域
        GuestBtn.setBorderPainted(false);    // 不绘制边框
        GuestBtn.setForeground(new Color(0, 0, 0, 0));//透明字体
        GuestBtn.setFocusPainted(false);

        RegisterBtn = FrameUtil.createButton(this, "Register", new Point(595, 420), 160, 60);
        RegisterBtn.setFont(new Font("大字体", Font.PLAIN, 10));
        RegisterBtn.setOpaque(false);          // 不绘制背景
        RegisterBtn.setContentAreaFilled(false); // 不填充内容区域
        RegisterBtn.setBorderPainted(false);    // 不绘制边框
        RegisterBtn.setForeground(new Color(0, 0, 0, 0));//透明字体
        RegisterBtn.setFocusPainted(false);     // 不绘制焦点状态



       ImageIcon originalIcon = new ImageIcon("saves/loginB.jpg");
        Image originalImage = originalIcon.getImage();

        // 保持原始比
        int imageWidth = originalIcon.getIconWidth();
        int imageHeight = originalIcon.getIconHeight();
        // 计算适合窗口缩放
        double widthRatio = (double)windowWidth / imageWidth;
        double heightRatio = (double)windowHeight / imageHeight;
        double scaleRatio = Math.min(widthRatio, heightRatio); // 完整显示，图片
        int scaledWidth = (int)(imageWidth * scaleRatio);
        int scaledHeight = (int)(imageHeight * scaleRatio);
        // 缩放图片
        Image scaledImage = originalImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        JLabel background = new JLabel(new ImageIcon(scaledImage));
        // 计算居中位置
        int x = (windowWidth - scaledWidth) / 2;
        int y = (windowHeight - scaledHeight) / 2;

        background.setBounds(x, y, scaledWidth, scaledHeight);
        this.getContentPane().add(background);


        RegisterBtn.addActionListener(e -> {
            System.out.println("Username = " + usernameField.getText());
            System.out.println("Password = " + passwordField.getText());
            /// ///////获取注册用户密码，储存文件

            try {
                UserStorage.registerUser(usernameField.getText(), passwordField.getText());//保存注册用户信息
                JOptionPane.showMessageDialog(null, "注册成功，请重新登录！", "成功登录窗口", JOptionPane.INFORMATION_MESSAGE
                );

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }


        });


        //////////////////老用户，读文件
        SubmitBtn.addActionListener(e -> {
            System.out.println("Username = " + usernameField.getText());
            System.out.println("Password = " + passwordField.getText());
            /// ////////todo: check login information
            try {
                if (UserStorage.login(usernameField.getText(), passwordField.getText())) {
                    User u = new User(usernameField.getText(), passwordField.getText());
                    gameController.setCurrentUser(u);

                    setTimerMethod();
                    openGameFrame();            //开始游戏
                }else{
                    JOptionPane.showMessageDialog(null, "登录失败，账户或密码错误！", "账户或密码错误窗口", JOptionPane.INFORMATION_MESSAGE
                    );
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            this.setLocationRelativeTo(null);
            this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        });


/// //////////访客按钮直接触发事件，开始游戏
        GuestBtn.addActionListener(e -> {
            usernameField.setText("");
            passwordField.setText("");

            setTimerMethod();
            openGameFrame();//开始游戏
            this.setLocationRelativeTo(null);
            this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        });
    }


//        //GUI加上按钮
//        panel.add(loginPanel);
//        panel.add(loginButton);
//        panel.add(registerButton);
//        panel.add(guestButton);
//        add(panel);

    private void openGameFrame() {
        GameFrame gameFrame = new GameFrame(gameController);
        gameController.setGameframe(gameFrame);
        gameFrame.setVisible(true);
        dispose();
    }

    //设置选择时间模式
    private void setTimerMethod(){
        Object[] options = {"classic mode", "time attack mode"};
        int choice = JOptionPane.showOptionDialog(this,
                "please choose time mode",
                "mode choosing",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (choice == 1) { // 选择限时模式
            String time = (String)JOptionPane.showInputDialog(this,
                    "set time limit: (seconds)  :",
                    "time attack mode set",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    new String[]{"5","180", "240", "300"},
                    "300");

            gameController.setTimeLimit(Integer.parseInt(time));        //todo 这里数据没输入进去
            gameController.setTimerEnabled(true);
        }
    }
}
