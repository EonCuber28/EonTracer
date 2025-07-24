import javax.swing.*;
import java.util.Vector;

public class Main {
    private static final int ResX = 100;
    private static final int ResY = 100;
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
            Camera cam = new Camera();
            cam.ResX = ResX;
            cam.ResY = ResY;
            cam.CalcTransform();
            cam.calculatePixelVectors();
            for (int x = 0; x < cam.PixelVectors.length; x++){
                for (int y = 0; y < cam.PixelVectors[0].length; y++){
                    double[] vector = cam.PixelVectors[x][y];

                    int red = (int)Math.round(Math.abs((vector[0]*255)));
                    int green = (int)Math.round(Math.abs((vector[1]*255)));
                    int blue = (int)Math.round(Math.abs((vector[2]*255)));

                    canvas.setPixel(x,y,red,green,blue);
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
