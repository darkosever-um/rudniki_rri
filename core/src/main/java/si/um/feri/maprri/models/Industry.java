package si.um.feri.maprri.models;

public class Industry {
    public String name;
    public double lat;
    public double lng;

    public Industry(String name, double lat, double lng) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
    }

    public String toString(){
        return "Name: " + name
            + "\nLat,Lng = [" + lat + "," + lng + "]\n";
    }
}
