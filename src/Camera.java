public class Camera {
    // in radians
    public double FOV = 1;

    public double FL = 1;

    public int ResX = 1000;
    public int ResY = 1000;
    public double aspectRatio = (double)ResX/ResY;

    public double PosX = 0;
    public double PosY = 0;
    public double PosZ = 0;

    public double RotX = Math.PI/2;
    public double RotY = 0;
    public double RotZ = 0;

    public double[] Translate2cam;

    public double[][][] PixelVectors = new double[ResX][ResY][3];

    private final Etc etc = new Etc();

    public void init(double fov, double focalLength, int resX, int resY, double posX, double posY, double posZ, double rotX, double rotY, double rotZ){
        ResX = resX;
        ResY = resY;
        // change this around if things get fucky
        aspectRatio = (double) ResX /ResY;

        FOV = fov;
        FL = focalLength;

        PosX = posX;
        PosY = posY;
        PosZ = posZ;

        RotX = rotX;
        RotY = rotY;
        RotZ = rotZ;

        CalcTransform();
    }

    public void CalcTransform(){
        double PitchSin = Math.sin(RotX);
        double YawSin = Math.sin(RotY);
        double RollSin = Math.sin(RotZ);

        double PitchCos = Math.cos(RotX);
        double YawCos = Math.cos(RotZ);
        double RollCos = Math.cos(RotY);

        double r00 = YawCos*PitchCos;
        double r01 = YawCos*PitchSin*RollSin - YawSin*RollCos;
        double r02 = YawCos*PitchSin*RollCos + YawSin*RollSin;

        double r10 = YawSin*PitchCos;
        double r11 = YawSin*PitchSin*RollSin + YawCos*RollCos;
        double r12 = YawSin*PitchSin*RollCos - YawCos*RollSin;

        double r20 = -PitchSin;
        double r21 = PitchCos*RollSin;
        double r22 = PitchCos*RollCos;

        double tx = -(r00*PosX + r01*PosY + r02+PosZ);
        double ty = -(r10*PosX + r11*PosY + r12+PosZ);
        double tz = -(r20*PosX + r21*PosY + r22*PosZ);

        Translate2cam = new double[] {
                r00,r10,r20, 0,
                r01,r11,r21, 0,
                r02,r12,r22, 0,
                tx, ty, tz,  1
        };
    }

    public double[] applyTransform(double[] vertexIn){
        return new double[] {
                Translate2cam[0]*vertexIn[0] + Translate2cam[4]*vertexIn[1] + Translate2cam[8]*vertexIn[2] + Translate2cam[12],
                Translate2cam[1]*vertexIn[0] + Translate2cam[5]*vertexIn[1] + Translate2cam[9]*vertexIn[2] + Translate2cam[13],
                Translate2cam[2]*vertexIn[0] + Translate2cam[6]*vertexIn[1] + Translate2cam[10]*vertexIn[2] + Translate2cam[14],
        };
    }

    public void calculatePixelVectors(){
        //calculate width and height of the projection plane
        double planeHeight = FL*Math.tan(FOV*0.5)*2;
        double planeWidth = planeHeight*aspectRatio;
        // define bottom left of camera view space
        double[] bottomLeft = {-planeWidth/2, -planeHeight/2, FL};
        // make the vectors
        for (int x = 0; x < ResX; x++){
            for (int y = 0; y < ResY; y++){
                // calculate the offset
                double tx = (double)x/(ResX-1);
                double ty = (double)y/(ResY-1);
                // calculate point in global
                double[] pointLocal = {bottomLeft[0]*tx, bottomLeft[1]*ty, bottomLeft[2]};
                double[] point = applyTransform(pointLocal);
                double[] preVector = {
                        point[0]-Translate2cam[12],
                        point[1]-Translate2cam[13],
                        point[2]-Translate2cam[14]};
                double[] vector = etc.normaliseVector(preVector);

                PixelVectors[x][y][0] = vector[0];
                PixelVectors[x][y][1] = vector[1];
                PixelVectors[x][y][2] = vector[2];
            }
        }
    }
    public int[] WPP(double[] VertexIn){
        // Xproj = FL*X/FL+Z
        // Yproj = FL*Y/FL+Z
        int Xproj = (int)Math.round((FL*VertexIn[0])/(FL+VertexIn[2]));
        int Yproj = (int)Math.round((FL*VertexIn[1])/(FL+VertexIn[2]));
        return new int[] {Xproj,Yproj};
    }
}
