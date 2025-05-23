package Game1;

import javax.swing.*;

import Game1.Controllers.GameController;
import Game1.Controllers.MusicPlayer;
import Game1.Controllers.UserController;
import Game1.views.GameFrame;
import Game1.views.LoginFrame;

import java.io.IOException;


public class KlotskiApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            UserController userController = new UserController();
            GameController gameController = null;
            MusicPlayer musicPlayer= new MusicPlayer();
            gameController = new GameController();

            LoginFrame loginFrame = new LoginFrame(userController, gameController ,musicPlayer);
            loginFrame.setVisible(true);
        });
    }
}

