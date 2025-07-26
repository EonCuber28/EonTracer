import javax.swing.*;

public class Main {
    private static final int ResX = 1000;
    private static final int ResY = 1000;
    private static final int FinalX = 1000;
    private static final int FinalY = 1000;
    private static final String title = "EonTracer";
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
            Mtl mtl = new Mtl();
            mtl.color = new int[]{100,200,50};
            mtl.emissionColor = new int[]{255,255,255};
            mtl.emissionStrength = 1;
            ball.init(0,0,0,1, mtl);
            Camera cam = new Camera();
            cam.CalcTransform();
            cam.calculatePixelVectors();
            double[] origin = {cam.PosX,cam.PosY,cam.PosZ};
            for (int x = 0; x < cam.PixelVectors.length; x++){
                for (int y = 0; y < cam.PixelVectors[0].length; y++){
                    double[] vector = cam.PixelVectors[x][y];
                    HitInfo hitinfo = ball.doesRayHit(vector,origin);
                    if (hitinfo.hits){
                        int red = ball.mtl.color[0];
                        int green = ball.mtl.color[1];
                        int blue = ball.mtl.color[2];
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
