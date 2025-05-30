package Game1.views;


import Game1.Controllers.GameController;

import javax.swing.*;
import java.awt.*;

public class LevelSelectFrame extends JFrame {
    private GameController gameController;

    private int level;


    public LevelSelectFrame(GameController gameController) {
        this.gameController = gameController;

        initUI();
    }

    private void initUI() {
        setTitle("Select Level");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(0, 3, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        for (int i = 1; i <= 4; i++) {
            level = i;
            JButton levelButton = new JButton("Level " + (i ));
            int finalI = i;
            levelButton.addActionListener(e -> {

                gameController.setLevel(finalI);
                gameController.initialize_Board();
                gameController.startLevel();
                dispose();

            });
            panel.add(levelButton);
        }

        add(panel);
    }
}
