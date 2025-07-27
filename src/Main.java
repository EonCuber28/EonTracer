import javax.swing.*;

public class Main {
    private static final int ResX = 1280;
    private static final int ResY = 720;
    private static final int FinalX = 1280;
    private static final int FinalY = 720;
    private static final String title = "EonTracer";
    private static final Rng rng = new Rng();
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
            Balls[] balls;
            if (true){
                // yellow ball (sun)
                Balls sun = new Balls();
                Mtl sunMtl = new Mtl();
                sunMtl.color = new int[]{100,100,100};
                sunMtl.emissionColor = new int[]{0,0,0};
                sunMtl.emissionStrength = 0;
                sun.init(2,0,0,0.5, sunMtl);
                // grey ball (subject)
                Balls subject = new Balls();
                Mtl subjectMtl = new Mtl();
                subjectMtl.color = new int[]{187, 28, 212};
                subjectMtl.emissionColor = new int[]{0,0,0};
                subjectMtl.emissionStrength = 0;
                subject.init(0,2,0,0.5, subjectMtl);
                // purple ball (ground)
                Balls ground = new Balls();
                Mtl groundMtl = new Mtl();
                groundMtl.color = new int[]{133, 75, 17};
                groundMtl.emissionColor = new int[] {0,0,0};
                groundMtl.emissionStrength = 0;
                ground.init(0,0,2,0.5, groundMtl);
                // add to list
                balls = new Balls[]{sun,subject,ground};
            }
            Camera cam = new Camera();
            cam.CalcTransform();
            cam.calculatePixelVectors();
            int maxBounces = 5;
            int RPP = 20; // rays per pixel
            int[] SkyColorHorizon = new int[]{200,200,255};
            int[] SkyColorZenith = new int[]{255,255,255};
            int[] groundColor = new int[]{150,150,150};
            double[] SunlightDirection = new double[]{1,1,0};
            double SunFocus = 1;
            double SunIntensity = 1;
            for (int x = 0; x < cam.PixelVectors.length; x++){
                for (int y = 0; y < cam.PixelVectors[0].length; y++){
                    double[] totalRayColors = new double[]{0,0,0};
                    for (int rayCount = 0; rayCount < RPP; rayCount++) {
                        double[] origin = {cam.PosX, cam.PosY, cam.PosZ};
                        float[] vector = cam.PixelVectors[x][y];
                        double[] rayColor = new double[]{255, 255, 255};
                        double[] incomingLight = new double[]{0, 0, 0};
                        for (int bounces = 0; bounces < maxBounces; bounces++) {
                            HitInfo closestHit = new HitInfo();
                            closestHit.distance = Double.POSITIVE_INFINITY;
                            Mtl closestMtl = null;
                            for (Balls ball : balls) {
                                HitInfo hitinfo = ball.doesRayHit(vector, origin);
                                if (hitinfo.hits && hitinfo.distance <= closestHit.distance) {
                                    closestHit = hitinfo;
                                    closestMtl = ball.mtl;
                                }
                            }
                            if (closestMtl == null) {
                                // calculate environment lighting
                                double skyGradientT = Math.pow(etc.smoothStep(0,0.4, vector[1]), 0.35);
                                double[] skyGradient = new double[]{
                                        etc.lerp(SkyColorHorizon[0],SkyColorZenith[0],skyGradientT),
                                        etc.lerp(SkyColorHorizon[1],SkyColorZenith[1],skyGradientT),
                                        etc.lerp(SkyColorHorizon[2],SkyColorZenith[2],skyGradientT)};
                                double sun = Math.pow(Math.max(0, etc.dot(new double[]{
                                        vector[0],
                                        vector[1],
                                        vector[2]
                                }, new double[]{
                                        -SunlightDirection[0],
                                        -SunlightDirection[1],
                                        -SunlightDirection[2]
                                })), SunFocus)*SunIntensity;
                                // apply
                                double groundToSkyT = etc.smoothStep(-0.01, 0, vector[1]);
                                double sunMask;
                                if (groundToSkyT>=1){sunMask=1;}else{sunMask=0;}
                                incomingLight[0] += etc.lerp(groundColor[0], skyGradient[0], groundToSkyT)+sun*sunMask;
                                incomingLight[1] += etc.lerp(groundColor[1], skyGradient[1], groundToSkyT)+sun*sunMask;
                                incomingLight[2] += etc.lerp(groundColor[2], skyGradient[2], groundToSkyT)+sun*sunMask;
                                break;
                            } else {
                                // redirect the ray
                                vector = rng.RandomVectorOffSurface(closestHit.hitNormal);
                                origin = closestHit.hitPoint;
                                // get emitted light
                                incomingLight[0] += (closestMtl.emissionColor[0] * closestMtl.emissionStrength) * rayColor[0];
                                incomingLight[1] += (closestMtl.emissionColor[1] * closestMtl.emissionStrength) * rayColor[1];
                                incomingLight[2] += (closestMtl.emissionColor[2] * closestMtl.emissionStrength) * rayColor[2];
                                // get base lighting
                                rayColor[0] *= closestMtl.color[0];
                                rayColor[1] *= closestMtl.color[1];
                                rayColor[2] *= closestMtl.color[2];
                            }
                        }
                        totalRayColors[0] += incomingLight[0];
                        totalRayColors[1] += incomingLight[1];
                        totalRayColors[2] += incomingLight[2];
                    }
                    int red = (int)Math.round(totalRayColors[0]/RPP);
                    int green = (int)Math.round(totalRayColors[1]/RPP);
                    int blue = (int)Math.round(totalRayColors[2]/RPP);
                    canvas.setPixel(x,y, red,green,blue);
                    canvas.update();
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
