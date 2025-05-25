package Game1.Controllers;

import Game1.models.Block;

import javax.swing.*;

public class BlockAnimator {
    private static final int ANIMATION_DURATION = 200; // 动画持续时间（毫秒）
    private static final int FRAME_INTERVAL = 20;      // 帧间隔（毫秒）

    public static void animateMove(Block block, int targetX, int targetY, Runnable onComplete) {
        final int startX = block.getX();
        final int startY = block.getY();
        final long startTime = System.currentTimeMillis();

        Timer timer = new Timer(FRAME_INTERVAL, null);
        timer.addActionListener(e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= ANIMATION_DURATION) {
                block.setPosition(targetX, targetY);
                timer.stop();
                onComplete.run();
                return;
            }

            float progress = (float) elapsed / ANIMATION_DURATION;
            int currentX = (int) (startX + (targetX - startX) * progress);
            int currentY = (int) (startY + (targetY - startY) * progress);

            block.setPosition(currentX, currentY);
//            block.getParent().repaint();
        });
        timer.start();
    }
}
