// PixelCanvas.java - Main library class
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;


// the following class was made with help from claude
public class PixelCanvas extends JPanel {
    private BufferedImage canvas;
    private Graphics2D graphics;
    private int width, height;
    private int FinalX, FinalY;

    public PixelCanvas(int width, int height) {
        this.width = width;
        this.height = height;
        this.canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        this.graphics = canvas.createGraphics();

        setPreferredSize(new Dimension(width, height));
        setBackground(Color.BLACK);

        // Initialize with black background
        clear(0, 0, 0);
    }

    // Set a single pixel
    public void setPixel(int x, int y, int red, int green, int blue) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            int rgb = (red << 16) | (green << 8) | blue;
            canvas.setRGB(x, y, rgb);
        }
    }

    // Set pixel with Color object
    public void setPixel(int x, int y, Color color) {
        setPixel(x, y, color.getRed(), color.getGreen(), color.getBlue());
    }

    // Set pixel with hex color
    public void setPixel(int x, int y, int hexColor) {
        int red = (hexColor >> 16) & 0xFF;
        int green = (hexColor >> 8) & 0xFF;
        int blue = hexColor & 0xFF;
        setPixel(x, y, red, green, blue);
    }

    // Get pixel color
    public int getPixel(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return canvas.getRGB(x, y);
        }
        return 0;
    }

    // Fill entire canvas with color
    public void clear(int red, int green, int blue) {
        graphics.setColor(new Color(red, green, blue));
        graphics.fillRect(0, 0, width, height);
    }

    // Draw from 2D array
    public void drawPixelArray(int[][] pixelData) {
        int arrayWidth = Math.min(pixelData.length, width);
        int arrayHeight = Math.min(pixelData[0].length, height);

        for (int x = 0; x < arrayWidth; x++) {
            for (int y = 0; y < arrayHeight; y++) {
                setPixel(x, y, pixelData[x][y]);
            }
        }
    }

    // Draw line between two points
    public void drawLine(int x1, int y1, int x2, int y2, int red, int green, int blue) {
        // Bresenham's line algorithm
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        int x = x1, y = y1;

        while (true) {
            setPixel(x, y, red, green, blue);

            if (x == x2 && y == y2) break;

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    // Draw rectangle
    public void drawRect(int x, int y, int w, int h, int red, int green, int blue) {
        for (int i = x; i < x + w && i < width; i++) {
            for (int j = y; j < y + h && j < height; j++) {
                setPixel(i, j, red, green, blue);
            }
        }
    }

    // Draw circle
    public void drawCircle(int centerX, int centerY, int radius, int red, int green, int blue) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                int dx = x - centerX;
                int dy = y - centerY;
                if (dx * dx + dy * dy <= radius * radius) {
                    setPixel(x, y, red, green, blue);
                }
            }
        }
    }

    // Apply a function to all pixels
    public void applyFunction(PixelFunction function) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int currentColor = getPixel(x, y);
                int newColor = function.apply(x, y, currentColor);
                setPixel(x, y, newColor);
            }
        }
    }

    // Update display (call this after making changes)
    public void update() {
        repaint();
    }

    // Get canvas dimensions
    public int getCanvasWidth() { return width; }
    public int getCanvasHeight() { return height; }

    // Save as PNG file with optional upscaling
    public boolean saveToPNG(String filename) {
        return saveToPNG(filename, 1);
    }

    public boolean saveToPNG(String filename, int scale) {
        try {
            // Add .png extension if not present
            if (!filename.toLowerCase().endsWith(".png")) {
                filename += ".png";
            }

            BufferedImage imageToSave = (scale == 1) ? canvas : upscale(canvas, FinalX, FinalY);
            javax.imageio.ImageIO.write(imageToSave, "PNG", new java.io.File(filename));
            System.out.println("Image saved as: " + filename + (scale > 1 ? " (upscaled " + scale + "x)" : ""));
            return true;
        } catch (java.io.IOException e) {
            System.err.println("Error saving image: " + e.getMessage());
            return false;
        }
    }

    // Save as other formats with optional upscaling
    public boolean saveToFile(String filename, String format) {
        return saveToFile(filename, format, 1);
    }

    public boolean saveToFile(String filename, String format, int scale) {
        try {
            BufferedImage imageToSave = (scale == 1) ? canvas : upscale(canvas, FinalX, FinalY);
            javax.imageio.ImageIO.write(imageToSave, format.toUpperCase(), new java.io.File(filename));
            System.out.println("Image saved as: " + filename + (scale > 1 ? " (upscaled " + scale + "x)" : ""));
            return true;
        } catch (java.io.IOException e) {
            System.err.println("Error saving image: " + e.getMessage());
            return false;
        }
    }

    // Upscale the canvas for display (creates new PixelCanvas)
    public PixelCanvas createUpscaled(int scale) {
        if (scale <= 1) return this;

        PixelCanvas upscaledCanvas = new PixelCanvas(width * scale, height * scale);
        BufferedImage upscaledImage = upscale(canvas, FinalX, FinalY);
        upscaledCanvas.canvas = upscaledImage;
        upscaledCanvas.graphics = upscaledImage.createGraphics();
        upscaledCanvas.width = width * scale;
        upscaledCanvas.height = height * scale;
        upscaledCanvas.setPreferredSize(new Dimension(width * scale, height * scale));
        return upscaledCanvas;
    }

    // Set pixel upscaling mode for display

    public void setDisplayScale(int NewX, int NewY) {
        FinalX = NewX;
        FinalY = NewY;
        setPreferredSize(new Dimension(NewX, NewY));
        repaint();
    }

    // Internal upscaling method
    private BufferedImage upscale(BufferedImage original, int newWidth, int newHeight) {

        double Xscale = (double) newHeight / original.getHeight();
        double Yscale = (double) newWidth / original.getWidth();

        BufferedImage upscaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

        // Nearest neighbor upscaling (preserves pixel art look)
        for (int x = 0; x < original.getWidth(); x++) {
            for (int y = 0; y < original.getHeight(); y++) {
                int pixel = original.getRGB(x, y);

                // Fill the scaled block
                for (int dx = 0; dx < Xscale; dx++) {
                    for (int dy = 0; dy < Yscale; dy++) {
                        upscaled.setRGB((int) (x * Xscale + dx), (int) (y * Yscale + dy), pixel);
                    }
                }
            }
        }

        return upscaled;
    }

    // Advanced upscaling with different algorithms
    public BufferedImage upscaleSmooth(int scale) {
        if (scale <= 1) return canvas;

        int newWidth = width * scale;
        int newHeight = height * scale;
        BufferedImage upscaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = upscaled.createGraphics();

        // Use bilinear interpolation for smooth upscaling
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g2d.drawImage(canvas, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return upscaled;
    }

    // Get the raw BufferedImage (for advanced usage)
    public BufferedImage getImage() {
        return canvas;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (FinalY == width && FinalX == height) {
            g.drawImage(canvas, 0, 0, null);
        } else {
            // Upscale for display using nearest neighbor to preserve pixel art look
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2d.drawImage(canvas, 0, 0, FinalX, FinalY, null);
        }
    }
}

// Functional interface for pixel operations
@FunctionalInterface
interface PixelFunction {
    int apply(int x, int y, int currentColor);
}

// Utility class for color operations
class PixelColor {
    public static int rgb(int red, int green, int blue) {
        return (red << 16) | (green << 8) | blue;
    }

    public static int getRed(int rgb) {
        return (rgb >> 16) & 0xFF;
    }

    public static int getGreen(int rgb) {
        return (rgb >> 8) & 0xFF;
    }

    public static int getBlue(int rgb) {
        return rgb & 0xFF;
    }

    public static int blend(int color1, int color2, double ratio) {
        int r1 = getRed(color1), g1 = getGreen(color1), b1 = getBlue(color1);
        int r2 = getRed(color2), g2 = getGreen(color2), b2 = getBlue(color2);

        int r = (int) (r1 * (1 - ratio) + r2 * ratio);
        int g = (int) (g1 * (1 - ratio) + g2 * ratio);
        int b = (int) (b1 * (1 - ratio) + b2 * ratio);

        return rgb(r, g, b);
    }
}

// Clean example usage class
class PixelLibraryExample {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Pixel Library Example");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            PixelCanvas canvas = new PixelCanvas(400, 300);

            // Example 1: Draw smooth gradient
            drawGradient(canvas);

            // Example 2: Draw some clean shapes
            canvas.drawCircle(100, 100, 30, 255, 0, 0);  // Red circle
            canvas.drawRect(200, 50, 50, 100, 0, 255, 0); // Green rectangle

            // No noise or diagonal line - clean output
            canvas.update();

            frame.add(canvas);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static void drawGradient(PixelCanvas canvas) {
        int width = canvas.getCanvasWidth();
        int height = canvas.getCanvasHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int red = (int) (255.0 * x / width);
                int blue = (int) (255.0 * y / height);
                canvas.setPixel(x, y, red, 128, blue);
            }
        }
    }
}