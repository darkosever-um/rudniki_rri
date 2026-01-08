package si.um.feri.maprri.models;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import si.um.feri.maprri.models.enums.InfrastructureStatus;

import java.net.IDN;

public class Infrastructure implements Json.Serializable {
    private String brand;
    private String model;
    private Integer IDNumber;
    InfrastructureStatus status;
    private long lastMaintenance;
    private float operatingHours;
    private Integer kilometer;

    public Infrastructure(){}

    public Infrastructure(String brand,
                          String model,
                          Integer IDNumber,
                          InfrastructureStatus status,
                          long lastMaintenance,
                          float operatingHours,
                          Integer kilometer){
        this.brand = brand;
        this.model = model;
        this.IDNumber = IDNumber;
        this.status = status;
        this.lastMaintenance = lastMaintenance;
        this.operatingHours = operatingHours;
        this.kilometer = kilometer;
    }

    public Infrastructure(Infrastructure infrastructure){
        this.brand = infrastructure.brand;
        this.model = infrastructure.model;
        this.IDNumber = infrastructure.IDNumber;
        this.status = infrastructure.status;
        this.lastMaintenance = infrastructure.lastMaintenance;
        this.operatingHours = infrastructure.operatingHours;
        this.kilometer = infrastructure.kilometer;
    }


    public boolean validateInfrastructure(){
        if(status == null){
            return false;
        }

        return true;
    }

    public Infrastructure getInfraStructureFromJson(String jsonStr){
        try{
            Json json = new Json();
            return json.fromJson(Infrastructure.class, jsonStr);
        } catch (Exception e) {
            Gdx.app.error("INFRASTRUCTURE", "error loading scores:", e);
        }
        return null;
    }

    @Override
    public void write(Json json) {
        json.writeValue("brand", brand);
        json.writeValue("model", model);
        json.writeValue("IDNumber", IDNumber);
        json.writeValue("status", status.ordinal());

        json.writeObjectStart("lastMaintenance");
        json.writeValue("$date", lastMaintenance);
        json.writeObjectEnd();

        json.writeValue("operatingHours", operatingHours);
        json.writeValue("kilometer", kilometer);
    }

    @Override
    public void read(Json json, JsonValue jsonValue) {
        brand = jsonValue.getString("brand", null);
        model = jsonValue.getString("model", null);
        IDNumber = jsonValue.getInt("IDNumber", 0);
        int statusInt = jsonValue.getInt("status", 7);
        status = InfrastructureStatus.values()[statusInt];

        JsonValue dateObj = jsonValue.get("lastMaintenance");
        if (dateObj != null && dateObj.has("$date")) {
            lastMaintenance = dateObj.getLong("$date", 0);
        }

        operatingHours = jsonValue.getFloat("operatingHours", 0.00F);
        kilometer = jsonValue.getInt("kilometer", 0);
    }

    public String toString(){
        return "Brand: " + brand + "\n"
            + "Model: " + model + "\n"
            + "ID: " + IDNumber + "\n"
            + "Status: " + status + "\n"
        + "Last maintenance: " + lastMaintenance + "\n"
        + "Operation hours: " + operatingHours + "\n"
        + "Kilometers: " + kilometer + "\n";
    }
}
