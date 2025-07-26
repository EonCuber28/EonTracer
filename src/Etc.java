public class Etc{
    public double[] normaliseVector(double[] Vector){
        double a2 = Vector[0]*Vector[0];
        double b2 = Vector[1]*Vector[1];
        double c2 = Vector[2]*Vector[2];
        double length = Math.sqrt(a2+b2+c2);
        return new double[]{
                Vector[0]/length,
                Vector[1]/length,
                Vector[2]/length};
    }
    public double dot(double[] V1, double[] V2){
        double dot = 0;
        int longestLen = Math.max(V1.length, V2.length);
        for (int i = 0; i < longestLen; i++){
            dot += V1[i]*V2[i];
        }
        return dot;
    }
}