package Game1.models;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;


public class Block implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public enum BlockType { CAO_CAO, GUAN_YU, GENERAL, SOLDIER }

    private final BlockType type;
    private int x;
    private int y;
    private final int width;
    private final int height;
    private Color color;

    private transient BufferedImage texture; // 新增纹理属性
    public Block(BlockType type, int x, int y, int width, int height) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.texture = loadTexture(type); // 根据类型加载贴图
    }



    private BufferedImage loadTexture(BlockType type) {
        try {
            String path = getImagePath(type);
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) return createFallbackTexture();

            BufferedImage original = ImageIO.read(is);
            int targetWidth = this.width * 80;  // CELL_SIZE=80
            int targetHeight = this.height * 80;

            return resizeImage(original, targetWidth, targetHeight);
        } catch (IOException ex) {
            return createFallbackTexture();
        }
    }

    // 新增备用纹理生成
    private BufferedImage createFallbackTexture() {
        BufferedImage img = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        // 棋盘格背景
        for (int i = 0; i < 80; i += 10) {
            for (int j = 0; j < 80; j += 10) {
                g2d.setColor(((i + j) / 10 % 2 == 0) ? Color.PINK : Color.WHITE);
                g2d.fillRect(i, j, 10, 10);
            }
        }

        g2d.dispose();
        return img;
    }
    private BufferedImage resizeImage(BufferedImage original, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }


    private String getImagePath(BlockType type) {
        switch (type) {
            case CAO_CAO: return "/Game1/pic/Caocao.png";
            case GUAN_YU: return "/Game1/pic/Guanyu.png";
            case GENERAL: return "/Game1/pic/General.png";
            case SOLDIER: return "/Game1/pic/Soldier.png";
            default: return "";
        }
    }
    public BufferedImage getTexture() {
        return texture;
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // 反序列化后重新加载贴图
        this.texture = loadTexture(this.type);
    }

    public BlockType getType() { return type; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Color getColor() { return color; }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean contains(Point p) {
        return p.x >= x && p.x < x + width && p.y >= y && p.y < y + height;
    }

    @Override
    public String toString() {
        return type + " (" + x + "," + y + ") " + width + "x" + height;
    }
}
