import javax.swing.*;

public class Main {
    private static final int ResX = 1000;
    private static final int ResY = 1000;
    private static final int FinalX = 1000;
    private static final int FinalY = 1000;
    private static final String title = "EonTracer";
    private static final Etc etc = new Etc();
    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> {
            // normal setup stuff
            JFrame frame = new JFrame(title);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            PixelCanvas canvas = new PixelCanvas(ResX, ResY);
            canvas.setDisplayScale(FinalX,FinalY);

            // referance
            //canvas.setPixel(5,1,255,255,255);
            Balls ball = new Balls();
            ball.init(0,0,0,1);
            ball.color = new int[] {100,255,200};
            Camera cam = new Camera();
            cam.CalcTransform();
            cam.calculatePixelVectors();
            double[] origin = {cam.PosX,cam.PosY,cam.PosZ};
            for (int x = 0; x < cam.PixelVectors.length; x++){
                for (int y = 0; y < cam.PixelVectors[0].length; y++){
                    double[] vector = cam.PixelVectors[x][y];
                    HitInfo hitinfo = ball.doesRayHit(vector,origin);
                    if (hitinfo.hits){
                        int red = ball.color[0];
                        int green = ball.color[1];
                        int blue = ball.color[2];
                        canvas.setPixel(x,y, red,green,blue);
                    }
                }
            }

            canvas.update();

            frame.add(canvas);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
