package net.valdora.areanotifications;

public class AreaNotification {
    public String id;
    public String name;
    public double x1;
    public double y1;
    public double z1;
    public double x2;
    public double y2;
    public double z2;

    public AreaNotification(String id, String name, double x1, double y1, double z1, double x2, double y2, double z2) {
        this.id = id;
        this.name = name;
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
    }
}
