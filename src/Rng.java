public class Rng {
    public final Etc etc = new Etc();
    public float RandomValueOnNormalDistro(){
        // use gaussian distroutions instead of normal randomness
        float theta = (float)(2*Math.PI*Math.random());
        float rho = (float)Math.sqrt(-2*Math.log(Math.random()));
        return (float)(rho*Math.cos(theta));
    }
    public float[] RandomVector(){
        return new float[] {
                RandomValueOnNormalDistro(),
                RandomValueOnNormalDistro(),
                RandomValueOnNormalDistro()
        };
    }
    public float[] RandomVectorOffSurface(float[] surfaceNormal){
        float[] Rvector = RandomVector();
        double dot = etc.dot(new double[]{
                        (double)surfaceNormal[0],
                        (double)surfaceNormal[1],
                        (double)surfaceNormal[2]},
                new double[]{
                        (double)Rvector[0],
                        (double)Rvector[1],
                        (double)Rvector[2],
                });
        if (dot < 0){
            Rvector = new float[]{
                    -Rvector[0],
                    -Rvector[1],
                    -Rvector[2]};
        }
        return Rvector;
    }
}
