package net.valdora.savedata.checkpoints;

public class CheckPoint {
    public String id;
    public String world;
    public double resetPosX;
    public double resetPosY;
    public double resetPosZ;
    public float resetPosYaw;
    public float resetPosPitch;
    public double pos1X;
    public double pos1Y;
    public double pos1Z;
    public double pos2X;
    public double pos2Y;
    public double pos2Z;
    
    public CheckPoint() {
        id = "";
        world = "";
        resetPosX = 0.0;
        resetPosY = 0.0;
        resetPosZ = 0.0;
        resetPosYaw = 0.0f;
        resetPosPitch = 0.0f;
        pos1X = 0.0;
        pos1Y = 0.0;
        pos1Z = 0.0;
        pos2X = 0.0;
        pos2Y = 0.0;
        pos2Z = 0.0;
    }
}
