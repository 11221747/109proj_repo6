package Game1.models;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;


public class Block implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;


    public enum BlockType { CAO_CAO, GUAN_YU, GENERAL, SOLDIER, OBSTACLE }

    private final BlockType type;
    private int x;
    private int y;
    private final int width;
    private final int height;
    private Color color;
    private boolean moveable;

    private transient BufferedImage texture;
    public Block(BlockType type, int x, int y, int width, int height) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.texture = loadTexture(type); // 根据类型加载贴图

        set_BlockMoveable();
    }

    public void set_BlockMoveable() {
        if (type == BlockType.OBSTACLE) {
            moveable = false;
        }else{
            moveable = true;
        }
    }


    private BufferedImage loadTexture(BlockType type) {

        String path = getImagePath(type);
        InputStream istr = getClass().getResourceAsStream(path);
        BufferedImage original = null;
        try {
            original = ImageIO.read(istr);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int targetWidth = this.width * 80;  // CELL_SIZE=80，像素大小是80
        int targetHeight = this.height * 80;

        return resizeImage(original, targetWidth, targetHeight);

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
            case OBSTACLE: return "/Game1/pic/stoneObstacle.png";
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


    public boolean isMoveable() {
        return moveable;
    }

    public void setMoveable(boolean moveable) {
        this.moveable = moveable;
    }

}
