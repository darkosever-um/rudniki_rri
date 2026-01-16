package si.um.feri.maprri.models;

import com.graphhopper.util.PointList;

public class PathInfo {
    public PointList points;
    public double distance;
    public long timeInMs;

    public PathInfo(){}
    public PathInfo(PointList points, double distance, long timeInMs){
        this.points = points;
        this.distance = distance;
        this.timeInMs = timeInMs;
    }
}
