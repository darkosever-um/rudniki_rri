package si.um.feri.maprri.util;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.json.Statement;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.GHUtility;
import com.graphhopper.util.PointList;
import si.um.feri.maprri.models.PathInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InfrastructurePath {
    private static GraphHopper hopper;

    public static void init(){
        if(hopper != null){
            return;
        }

        String osmFile = "assets/slovenia.osm.pbf";
        String graphFolder = "routing-graph-cache";

        File osm = new File(osmFile);

        if(!osm.exists()){
            System.out.println("ERROR osm file not found!");
            return;
        }

        hopper = new GraphHopper();
        hopper.setOSMFile(osmFile);
        hopper.setGraphHopperLocation(graphFolder);
        hopper.setEncodedValuesString("car_access, car_average_speed");

        CustomModel standardCarModel = new CustomModel();
        standardCarModel.addToSpeed(Statement.If("true", Statement.Op.LIMIT, "car_average_speed"));

        hopper.setProfiles(
            new Profile("car")
                .setWeighting("custom")
                .setCustomModel(standardCarModel)
        );

        hopper.getCHPreparationHandler().setCHProfiles(new CHProfile("car"));
        System.out.println("Loading GraphHopper");
        hopper.importOrLoad();
        System.out.println("Graph loaded!");
    }

    public static PathInfo findPath(double startLat, double startLon, double endLat, double endLon){
        if (hopper == null) {
            return null;
        }

        System.out.println("Finding path");

        GHRequest req = new GHRequest(startLat, startLon, endLat, endLon)
            .setProfile("car")
            .setLocale(Locale.ENGLISH);

        GHResponse res = hopper.route(req);

        if(res.hasErrors()){
            System.out.println("Error searching path: " + res.getErrors());
            return null;
        }

        ResponsePath path = res.getBest();

        PointList pointList = path.getPoints();
        double distance = path.getDistance();
        long timeInMs = path.getTime();

        return new PathInfo(pointList, distance, timeInMs);
    }
}
