package si.um.feri.maprri.models;

import si.um.feri.maprri.models.enums.InfrastructureStatus;

public class Infrastructure {
    private String brand;
    private String model;
    private Integer IDNumber;
    InfrastructureStatus status;
    private long lastMaintenance;
    private double operatingHours;
    private Integer kilometer;


    public boolean validateInfrastructure(){
        if(status == null){
            return false;
        }

        return true;
    }
}
