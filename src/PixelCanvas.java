import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class PixelCanvas extends JPanel {
    private BufferedImage canvas;
    private int canvasWidth, canvasHeight;    // Drawing resolution
    private int displayWidth, displayHeight; // Display resolution
    private volatile boolean needsRepaint = false;
    private boolean isUpscaling = true; // true = upscale, false = downscale

    // Constructor 1: Original style (canvas = display, upscaling)
    public PixelCanvas(int width, int height) {
        this(width, height, width, height);
    }

    // Constructor 2: Separate canvas and display resolutions
    public PixelCanvas(int canvasW, int canvasH, int displayW, int displayH) {
        this.canvasWidth = canvasW;
        this.canvasHeight = canvasH;
        this.displayWidth = displayW;
        this.displayHeight = displayH;

        // Determine if we're upscaling or downscaling
        this.isUpscaling = (displayW >= canvasW && displayH >= canvasH);

        this.canvas = new BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_RGB);
        setPreferredSize(new Dimension(displayW, displayH));

        clear();
        startRepaintTimer();

        String scaleType = isUpscaling ? "upscaling" : "downscaling";
        System.out.println("Canvas: " + canvasW + "x" + canvasH +
                " -> Display: " + displayW + "x" + displayH + " (" + scaleType + ")");
    }

    // Thread-safe pixel setting
    public synchronized void setPixel(int x, int y, int red, int green, int blue) {
        if (x >= 0 && x < canvasWidth && y >= 0 && y < canvasHeight) {
            int rgb = (red << 16) | (green << 8) | blue;
            canvas.setRGB(x, y, rgb);
            needsRepaint = true;
        }
    }

    public synchronized void clear() {
        Graphics2D g = canvas.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, canvasWidth, canvasHeight);
        g.dispose();
        needsRepaint = true;
    }

    // Backwards compatible method (old name)
    public void setScale(int newWidth, int newHeight) {
        setDisplaySize(newWidth, newHeight);
    }

    // Change display resolution
    public void setDisplaySize(int newWidth, int newHeight) {
        this.displayWidth = newWidth;
        this.displayHeight = newHeight;
        this.isUpscaling = (newWidth >= canvasWidth && newHeight >= canvasHeight);

        setPreferredSize(new Dimension(newWidth, newHeight));
        SwingUtilities.invokeLater(() -> {
            repaint();
            // Notify parent to repack if needed
            Container parent = getParent();
            while (parent != null && !(parent instanceof JFrame)) {
                parent = parent.getParent();
            }
            if (parent instanceof JFrame) {
                ((JFrame) parent).pack();
            }
        });

        String scaleType = isUpscaling ? "upscaling" : "downscaling";
        System.out.println("Display changed to: " + newWidth + "x" + newHeight + " (" + scaleType + ")");
    }

    // Periodic repaint on EDT
    private void startRepaintTimer() {
        Timer repaintTimer = new Timer(16, e -> { // ~60 FPS
            if (needsRepaint) {
                needsRepaint = false;
                repaint();
            }
        });
        repaintTimer.start();
    }

    // Get canvas dimensions (what you draw on)
    public int getCanvasWidth() { return canvasWidth; }
    public int getCanvasHeight() { return canvasHeight; }

    // Get display dimensions (what you see)
    public int getDisplayWidth() { return displayWidth; }
    public int getDisplayHeight() { return displayHeight; }

    // Check scaling direction
    public boolean isUpscaling() { return isUpscaling; }
    public boolean isDownscaling() { return !isUpscaling; }

    // Backwards compatible method (old name)
    public boolean savePNG(String filename) {
        return saveCanvas(filename);
    }

    // Save canvas at original resolution
    public synchronized boolean saveCanvas(String filename) {
        try {
            if (!filename.toLowerCase().endsWith(".png")) {
                filename += ".png";
            }
            javax.imageio.ImageIO.write(canvas, "PNG", new java.io.File(filename));
            System.out.println("Saved canvas (" + canvasWidth + "x" + canvasHeight + "): " + filename);
            return true;
        } catch (java.io.IOException e) {
            System.err.println("Export failed: " + e.getMessage());
            return false;
        }
    }

    // Save display at current display resolution
    public synchronized boolean saveDisplay(String filename) {
        try {
            if (!filename.toLowerCase().endsWith(".png")) {
                filename += ".png";
            }

            // Create image at display resolution
            BufferedImage displayImage = new BufferedImage(displayWidth, displayHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = displayImage.createGraphics();

            // Apply appropriate scaling
            if (isUpscaling) {
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            } else {
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
            }

            g2d.drawImage(canvas, 0, 0, displayWidth, displayHeight, null);
            g2d.dispose();

            javax.imageio.ImageIO.write(displayImage, "PNG", new java.io.File(filename));
            System.out.println("Saved display (" + displayWidth + "x" + displayHeight + "): " + filename);
            return true;
        } catch (java.io.IOException e) {
            System.err.println("Export failed: " + e.getMessage());
            return false;
        }
    }

    // Backwards compatible update method
    public void update() {
        SwingUtilities.invokeLater(() -> repaint());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Choose appropriate scaling algorithm
        if (isUpscaling) {
            // Pixel-perfect upscaling (nearest neighbor)
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        } else {
            // High-quality downscaling (bilinear)
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
        }

        synchronized(this) {
            g2d.drawImage(canvas, 0, 0, displayWidth, displayHeight, null);
        }
    }
}