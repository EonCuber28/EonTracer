import jdk.jshell.execution.JdiDefaultExecutionControl;

public class Balls {
    double X;
    double Y;
    double Z;

    double R;

    public Mtl mtl;

    public final Etc etc = new Etc();
    public void init(double x, double y, double z, double r, Mtl material){
        X=x;
        Y=y;
        Z=z;
        R=r;
        mtl = material;
    }
    public HitInfo doesRayHit(double[] direction, double[] origin){
        double[] offsetOrigen = {
                origin[0]-X,
                origin[1]-Y,
                origin[2]-Z
        };
        double a = etc.dot(direction,direction);
        double b = 2*etc.dot(offsetOrigen, direction);
        double c = etc.dot(offsetOrigen,offsetOrigen) - R*R;

        double discriminant = b*b - 4*a*c;

        HitInfo hitinfo = new HitInfo();
        hitinfo.hits = false;

        if (discriminant >= 0){
            double dist = (-b - Math.sqrt(discriminant))/(2*a);

            if (dist >= 0){
                hitinfo.hits = true;
                hitinfo.distance = dist;
                hitinfo.hitPoint = new double[]{
                        origin[0] + direction[0] * dist,
                        origin[1] + direction[1] * dist,
                        origin[2] + direction[2] * dist};
                hitinfo.hitNormal = etc.normaliseVector(new double[] {
                        hitinfo.hitPoint[0]-X,
                        hitinfo.hitPoint[1]-Y,
                        hitinfo.hitPoint[2]-Z,});
            }
        }
        return hitinfo;
    }
}
class HitInfo{
    boolean hits;
    double distance;
    double[] hitPoint;
    double[] hitNormal;
}