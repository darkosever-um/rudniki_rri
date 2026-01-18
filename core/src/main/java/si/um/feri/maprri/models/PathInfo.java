package si.um.feri.maprri.models;

import com.graphhopper.util.PointList;

public class PathInfo {
    public static PointList points;
    public double distance;
    public long timeInMs;

    public PathInfo(){}
    public PathInfo(PointList points, double distance, long timeInMs){
        PathInfo.points = points;
        this.distance = distance;
        this.timeInMs = timeInMs;
    }
}
