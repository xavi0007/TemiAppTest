package net.majorkernelpanic.streaming.video;

public class Point {
    public int x;
    public int y;

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;

    }

    public double getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public double getAngle() {
        return angle;
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }

    public int angle;
    public Point(int x, int y, int angle) {
        //angle %=360;
        this.x = x;
        this.y = y;
        this.angle = angle;
    }
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
        this.angle=0;
    }


}
