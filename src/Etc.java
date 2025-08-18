public class Etc{
    public float[] normaliseVector(double[] Vector){
        double a2 = Vector[0]*Vector[0];
        double b2 = Vector[1]*Vector[1];
        double c2 = Vector[2]*Vector[2];
        double length = Math.sqrt(a2+b2+c2);
        return new float[]{
                (float)(Vector[0]/length),
                (float)(Vector[1]/length),
                (float)(Vector[2]/length)};
    }
    public double dot(double[] V1, double[] V2){
        double dot = 0;
        int longestLen = Math.max(V1.length, V2.length);
        for (int i = 0; i < longestLen; i++){
            dot += V1[i]*V2[i];
        }
        return dot;
    }
    public double dot(float[] V1, double[] V2){
        double dot = 0;
        int longestLen = Math.max(V1.length, V2.length);
        for (int i = 0; i < longestLen; i++){
            dot += V1[i]*V2[i];
        }
        return dot;
    }
    public double dot(double[] V1, float[] V2){
        double dot = 0;
        int longestLen = Math.max(V1.length, V2.length);
        for (int i = 0; i < longestLen; i++){
            dot += V1[i]*V2[i];
        }
        return dot;
    }
    public double dot(float[] V1, float[] V2){
        double dot = 0;
        int longestLen = Math.max(V1.length, V2.length);
        for (int i = 0; i < longestLen; i++){
            dot += V1[i]*V2[i];
        }
        return dot;
    }
    public double lerp(double v1, double v2, double t){
        return (1-t)*v1+t*v2;
    }
    public double smoothStep(double edge0, double edge1, double x){
        x = Math.clamp((x-edge0)/(edge1-edge0), 0,1);
        return x*x*(3-2*x);
    }
}