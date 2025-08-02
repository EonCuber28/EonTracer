import javax.swing.*;
import java.util.Scanner;

public class Main {
    private static final int ResX = 3840;
    private static final int ResY = 2160;
    private static final int FinalX = 1280;
    private static final int FinalY = 720;

    private static final String title = "EonTracer";

    private static final Rng rng = new Rng();
    private static final Etc etc = new Etc();

    private static final Camera cam = new Camera();
    private static Balls[] balls = new Balls[]{};
    // render stetings
    private static final int maxBounces = 15;
    private static final int RPP = 5000;
    // sky color settings
    private static final double[] SkyColorHorizon = new double[]{20,20,20};
    private static final double[] SkyColorZenith = new double[]{20,20,20};
    private static final double[] groundColor = new  double[]{20,20,20};
    private static final float[] SunlightDirection = etc.normaliseVector(new double[]{1,1,1});
    private static final double SunFocus = 0;
    private static final double SunIntensity = 0;

    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> {
            // initialise camera data
            cam.ResX = ResX;
            cam.ResY = ResY;
            cam.CalcTransform();
            cam.calculatePixelVectors();
            // load ball data
            if (true) {
                // yellow ball (sun)
                Balls sun = new Balls();
                Mtl sunMtl = new Mtl();
                sunMtl.color = new double[]{0, 0, 0};
                sunMtl.emissionColor = new double[]{255, 255, 255};
                sunMtl.emissionStrength = 0.8;
                sun.init(2, 0, 0, 1, sunMtl);
                // grey ball (subject)
                Balls subject = new Balls();
                Mtl subjectMtl = new Mtl();
                subjectMtl.color = new double[]{0, 255, 0};
                subjectMtl.emissionColor = new double[]{0, 0, 0};
                subjectMtl.emissionStrength = 0;
                subject.init(0, 2, 0, 1, subjectMtl);
                // purple ball (ground)
                Balls ground = new Balls();
                Mtl groundMtl = new Mtl();
                groundMtl.color = new double[]{255, 0, 0};
                groundMtl.emissionColor = new double[]{0, 0, 0};
                groundMtl.emissionStrength = 0;
                ground.init(0, 0, 2, 1, groundMtl);
                // add to list
                balls = new Balls[]{sun, subject, ground};
            }
            // normal setup stuff
            JFrame frame = new JFrame(title);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            PixelCanvas canvas = new PixelCanvas(ResX, ResY, FinalX,FinalY);
            frame.add(canvas);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            // sent it to a seperate threads for no fucky wuckys with the main thread handleing the canvas.
            // get the amount of available cores
            int availableCores = Runtime.getRuntime().availableProcessors();
            // divide the screen space into sectors for each of the cores
            int[][] Sectors = generateSectors(ResX,ResY, availableCores);
            // run the cores
            for (int sectorIndex = 0; sectorIndex < Sectors.length; sectorIndex++){
                int[] sector = Sectors[sectorIndex];
                new Thread(() ->{
                    runRaytracerForSector(canvas, sector);
                }).start();
            }
            new Thread(()-> saveImage(canvas)).start();
        });}
    public static void saveImage(PixelCanvas canvas){
        Scanner scanner = new Scanner(System.in);
        System.out.print("save image? (y or n): ");
        String response = scanner.nextLine();
        System.out.println("You entered: '" + response + "'");

        if (response.trim().equalsIgnoreCase("y")) {
            System.out.println("Attempting to save...");
            boolean success = canvas.savePNG("Screenshot");
            System.out.println("Save result: " + success);
        } else {
            System.out.println("Not saving.");
        }
        scanner.close();
    }
    public static int[][] generateSectors(int width, int height, int coreCount){
        // first edge cases check
        if (width <= 0 || height <= 0){
            System.out.println("RESOLUTION MUST NOT BE NEGATIVE");
        } else if (coreCount <= 0){
            System.out.println("yo pc aint built different, it built incorrectly man.");
        }
        int totalPixels = width*height;
        // give a warming that the mount of cores is more than the total amount of pixels
        if (coreCount > totalPixels){
            System.out.println("WARNING: the amount of pixels being processed is less than the amount of cores, limiting to 1 pixel per core");
            coreCount = totalPixels;
        }

        // determine the optimal sector grid (rows X columns)
        int best_rows = 1;
        int best_columns = coreCount;
        int smallestRowColumnDifference = coreCount;
        for (int cantidateRows = 1; cantidateRows < coreCount+1; cantidateRows++){
            if (coreCount%cantidateRows == 0){
                int cantidateColumns = Math.floorDiv(coreCount, cantidateRows);
                int rowColumnDiff = Math.abs(cantidateRows-cantidateColumns);
                if (rowColumnDiff < smallestRowColumnDifference){
                    best_rows = cantidateRows;
                    best_columns = cantidateColumns;
                    smallestRowColumnDifference = rowColumnDiff;
                }
            }
        }
        int[][] sectors = new int[coreCount][4];
        // generate the x and y min/max boundries for each core
        int sectorIndex = 0;
        for (int rowIndex = 0; rowIndex < best_rows; rowIndex++){
            int Ystart = (rowIndex*Math.floorDiv(height,best_rows)+Math.min(rowIndex,(height%best_rows)));
            int extra = 0;
            if (rowIndex < height%best_rows) extra = 1;
            int Yend = (Ystart+Math.floorDiv(height,best_rows)+extra);
            for (int columnIndex = 0; columnIndex < best_columns; columnIndex++){
                int Xstart = (columnIndex*Math.floorDiv(width,best_columns)+Math.min(columnIndex,(width%best_columns)));
                extra = 0; if (columnIndex < width%best_columns) extra = 1;
                int Xend = (Xstart+Math.floorDiv(width,best_columns)+extra);
                sectors[sectorIndex] = new int[]{Xstart,Xend, Ystart,Yend};
                sectorIndex++;
            }
        }
        return sectors;
    }
    public static void runRaytracerForSector(PixelCanvas canvas, int[] sector) {
        // organise the sector data
        int Xstart = sector[0];
        int Xend = sector[1];

        int Ystart = sector[2];
        int Yend = sector[3];
        // start raytracing
        for (int x = Xstart; x < Xend; x++) {
            for (int y = Ystart; y < Yend; y++) {
                double[] totalRayColors = new double[]{0, 0, 0};
                for (int rayCount = 0; rayCount < RPP; rayCount++) {
                    double[] origin = {cam.PosX, cam.PosY, cam.PosZ};
                    float[] vector = cam.PixelVectors[x][y];
                    double[] rayColor = new double[]{1, 1, 1};
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
                            double skyGradientT = Math.pow(etc.smoothStep(0, 0.4, vector[1]), 0.35);
                            double[] skyGradient = new double[]{
                                    etc.lerp(SkyColorHorizon[0]/255, SkyColorZenith[0]/255, skyGradientT),
                                    etc.lerp(SkyColorHorizon[1]/255, SkyColorZenith[1]/255, skyGradientT),
                                    etc.lerp(SkyColorHorizon[2]/255, SkyColorZenith[2]/255, skyGradientT)};
                            double sun = Math.pow(Math.max(0, etc.dot(new double[]{
                                    vector[0],
                                    vector[1],
                                    vector[2]
                            }, new double[]{
                                    -SunlightDirection[0],
                                    -SunlightDirection[1],
                                    -SunlightDirection[2]
                            })), SunFocus) * SunIntensity;
                            // apply
                            double groundToSkyT = etc.smoothStep(-0.01, 0, vector[1]);
                            double sunMask;
                            if (groundToSkyT >= 1) {
                                sunMask = 1;
                            } else {
                                sunMask = 0;
                            }
                            incomingLight[0] += etc.lerp(groundColor[0]/255, skyGradient[0], groundToSkyT) + sun * sunMask;
                            incomingLight[1] += etc.lerp(groundColor[1]/255, skyGradient[1], groundToSkyT) + sun * sunMask;
                            incomingLight[2] += etc.lerp(groundColor[2]/255, skyGradient[2], groundToSkyT) + sun * sunMask;
                            break;
                        } else {
                            // redirect the ray
                            vector = rng.RandomVectorOffSurface(closestHit.hitNormal);
                            origin = closestHit.hitPoint;
                            // get emitted light
                            double emmittedRed = (closestMtl.emissionColor[0]/255) * closestMtl.emissionStrength;
                            double emmittedGreen = (closestMtl.emissionColor[1]/255) * closestMtl.emissionStrength;
                            double emmittedBlue = (closestMtl.emissionColor[2]/255) * closestMtl.emissionStrength;
                            // add emmitted light
                            incomingLight[0] += emmittedRed * rayColor[0];
                            incomingLight[1] += emmittedGreen * rayColor[1];
                            incomingLight[2] += emmittedBlue * rayColor[2];
                            // get base lighting
                            rayColor[0] *= closestMtl.color[0]/255;
                            rayColor[1] *= closestMtl.color[1]/255;
                            rayColor[2] *= closestMtl.color[2]/255;
                        }
                    }
                    totalRayColors[0] += incomingLight[0];
                    totalRayColors[1] += incomingLight[1];
                    totalRayColors[2] += incomingLight[2];
                }
                int red = (int) Math.round((totalRayColors[0]/RPP)*255);
                int green = (int) Math.round((totalRayColors[1]/RPP)*255);
                int blue = (int) Math.round((totalRayColors[2]/RPP)*255);
                canvas.setPixel(x, y, red, green, blue);
            }
        }
    }
    public static void runRaytracerSingleThread(PixelCanvas canvas, int[] sector) {
        Balls[] balls;
        if (true) {
            // yellow ball (sun)
            Balls sun = new Balls();
            Mtl sunMtl = new Mtl();
            sunMtl.color = new double[]{0, 0, 0};
            sunMtl.emissionColor = new double[]{255, 255, 255};
            sunMtl.emissionStrength = 0.8;
            sun.init(2, 0, 0, 1, sunMtl);
            // grey ball (subject)
            Balls subject = new Balls();
            Mtl subjectMtl = new Mtl();
            subjectMtl.color = new double[]{0, 255, 0};
            subjectMtl.emissionColor = new double[]{0, 0, 0};
            subjectMtl.emissionStrength = 0;
            subject.init(0, 2, 0, 1, subjectMtl);
            // purple ball (ground)
            Balls ground = new Balls();
            Mtl groundMtl = new Mtl();
            groundMtl.color = new double[]{255, 0, 0};
            groundMtl.emissionColor = new double[]{0, 0, 0};
            groundMtl.emissionStrength = 0;
            ground.init(0, 0, 2, 1, groundMtl);
            // add to list
            balls = new Balls[]{sun, subject, ground};
        }
        Camera cam = new Camera();
        cam.CalcTransform();
        cam.calculatePixelVectors();
        int maxBounces = 10;
        int RPP = 1000; // rays per pixel
        double[] SkyColorHorizon = new double[]{20, 20, 20};
        double[] SkyColorZenith = new double[]{20, 20, 20};
        double[] groundColor = new double[]{20, 20, 20};
        float[] SunlightDirection = etc.normaliseVector(new double[]{1, 1, 1});
        double SunFocus = 0;
        double SunIntensity = 0;
        for (int x = 0; x < cam.PixelVectors.length; x++) {
            for (int y = 0; y < cam.PixelVectors[0].length; y++) {
                double[] totalRayColors = new double[]{0, 0, 0};
                for (int rayCount = 0; rayCount < RPP; rayCount++) {
                    double[] origin = {cam.PosX, cam.PosY, cam.PosZ};
                    float[] vector = cam.PixelVectors[x][y];
                    double[] rayColor = new double[]{1, 1, 1};
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
                            double skyGradientT = Math.pow(etc.smoothStep(0, 0.4, vector[1]), 0.35);
                            double[] skyGradient = new double[]{
                                    etc.lerp(SkyColorHorizon[0]/255, SkyColorZenith[0]/255, skyGradientT),
                                    etc.lerp(SkyColorHorizon[1]/255, SkyColorZenith[1]/255, skyGradientT),
                                    etc.lerp(SkyColorHorizon[2]/255, SkyColorZenith[2]/255, skyGradientT)};
                            double sun = Math.pow(Math.max(0, etc.dot(new double[]{
                                    vector[0],
                                    vector[1],
                                    vector[2]
                            }, new double[]{
                                    -SunlightDirection[0],
                                    -SunlightDirection[1],
                                    -SunlightDirection[2]
                            })), SunFocus) * SunIntensity;
                            // apply
                            double groundToSkyT = etc.smoothStep(-0.01, 0, vector[1]);
                            double sunMask;
                            if (groundToSkyT >= 1) {
                                sunMask = 1;
                            } else {
                                sunMask = 0;
                            }
                            incomingLight[0] += etc.lerp(groundColor[0]/255, skyGradient[0], groundToSkyT) + sun * sunMask;
                            incomingLight[1] += etc.lerp(groundColor[1]/255, skyGradient[1], groundToSkyT) + sun * sunMask;
                            incomingLight[2] += etc.lerp(groundColor[2]/255, skyGradient[2], groundToSkyT) + sun * sunMask;
                            break;
                        } else {
                            // redirect the ray
                            vector = rng.RandomVectorOffSurface(closestHit.hitNormal);
                            origin = closestHit.hitPoint;
                            // get emitted light
                            double emmittedRed = (closestMtl.emissionColor[0]/255) * closestMtl.emissionStrength;
                            double emmittedGreen = (closestMtl.emissionColor[1]/255) * closestMtl.emissionStrength;
                            double emmittedBlue = (closestMtl.emissionColor[2]/255) * closestMtl.emissionStrength;
                            // add emmitted light
                            incomingLight[0] += emmittedRed * rayColor[0];
                            incomingLight[1] += emmittedGreen * rayColor[1];
                            incomingLight[2] += emmittedBlue * rayColor[2];
                            // get base lighting
                            rayColor[0] *= closestMtl.color[0]/255;
                            rayColor[1] *= closestMtl.color[1]/255;
                            rayColor[2] *= closestMtl.color[2]/255;
                        }
                    }
                    totalRayColors[0] += incomingLight[0];
                    totalRayColors[1] += incomingLight[1];
                    totalRayColors[2] += incomingLight[2];
                }
                int red = (int) Math.round((totalRayColors[0]/RPP)*255);
                int green = (int) Math.round((totalRayColors[1]/RPP)*255);
                int blue = (int) Math.round((totalRayColors[2]/RPP)*255);
                canvas.setPixel(x, y, red, green, blue);
            }
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("save image? (y or n): ");
        String response = scanner.nextLine();
        System.out.println("You entered: '" + response + "'");

        if (response.trim().equalsIgnoreCase("y")) {
            System.out.println("Attempting to save...");
            boolean success = canvas.savePNG("Screenshot");
            System.out.println("Save result: " + success);
        } else {
            System.out.println("Not saving.");
        }
        scanner.close();
    }
}