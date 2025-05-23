package Game1.models;
import javax.swing.*;
import java.io.*;

public class UserStorage {
    // 注册用户（用户名作为文件名）
    public static void registerUser(String username, String password) throws Exception {
        if (username == null || password==null) {
            JOptionPane.showMessageDialog(
                    null, "用户名或密码不能为空！", "用户名或密码不能为空提示", JOptionPane.WARNING_MESSAGE
            );
        } else {
            File dir = new File("userData/");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File userFile = new File("userData/" + username + ".txt");
            if (userFile.exists()) {
                JOptionPane.showMessageDialog(null, "用户已注册！", "用户已注册窗口", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            try (FileWriter writer = new FileWriter(userFile)) {
                writer.write(password); // 只存密码
            }
        }
    }
    // 验证登录（检查用户名和密码是否匹配）
    public static boolean login(String username, String password) throws IOException {
        File userFile = new File("userData/" + username + ".txt");
        if (!userFile.exists()) {
            JOptionPane.showMessageDialog(null,"用户名不存在！请先注册！","用户名不存在窗口",JOptionPane.INFORMATION_MESSAGE );
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(userFile))) {
            String storedPassword = reader.readLine();
            return storedPassword.equals(password);
        }
    }




//    // 测试
//    public static void main(String[] args) {
//        try {
//            // 注册用户
//            registerUser("alice", "123");
//            registerUser("bob", "456");
//
//            // 测试登录
//            login("alice", "123");  // 正常登录（无异常）
//            login("alice", "wrong"); // 抛出异常：密码错误！
//            login("charlie", "123"); // 抛出异常：用户名不存在！
//        } catch (Exception e) {
//            System.out.println("操作失败: " + e.getMessage()); // 捕获并打印异常
//        }
//    }
}