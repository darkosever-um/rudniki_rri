package si.um.feri.maprri.models;

import si.um.feri.maprri.models.enums.InfrastructureStatus;

public class InfrastructureSimStep {
    public String brand;
    public String model;
    public Integer IDNumber;
    public InfrastructureStatus status;
    public long lastMaintenance;
    public float operatingHours;
    public Integer kilometer;
    public double avgFuelConsumption;

    public double expenses;

    public double breakDownChance = Math.random() * 0.01;
    public double cleaningChance = Math.random() * 0.10;

    public InfrastructureSimStep(){}
    public InfrastructureSimStep(String brand,
                          String model,
                          Integer IDNumber,
                          InfrastructureStatus status,
                          long lastMaintenance,
                          float operatingHours,
                          Integer kilometer, double avgFuelConsumption, double expenses){
        this.brand = brand;
        this.model = model;
        this.IDNumber = IDNumber;
        this.status = status;
        this.lastMaintenance = lastMaintenance;
        this.operatingHours = operatingHours;
        this.kilometer = kilometer;
        this.avgFuelConsumption = avgFuelConsumption;
        this.expenses = expenses;
    }
    public InfrastructureSimStep(InfrastructureSimStep infrastructure){
        this.brand = infrastructure.brand;
        this.model = infrastructure.model;
        this.IDNumber = infrastructure.IDNumber;
        this.status = infrastructure.status;
        this.lastMaintenance = infrastructure.lastMaintenance;
        this.operatingHours = infrastructure.operatingHours;
        this.kilometer = infrastructure.kilometer;
        this.avgFuelConsumption = infrastructure.avgFuelConsumption;
        this.expenses = infrastructure.expenses;
    }
    public InfrastructureSimStep(Infrastructure infrastructure){
        this.brand = infrastructure.brand;
        this.model = infrastructure.model;
        this.IDNumber = infrastructure.IDNumber;
        this.status = infrastructure.status;
        this.lastMaintenance = infrastructure.lastMaintenance;
        this.operatingHours = infrastructure.operatingHours;
        this.kilometer = infrastructure.kilometer;
        this.avgFuelConsumption = infrastructure.avgFuelConsumption;
        this.breakDownChance = Math.random() * 0.01;
        this.cleaningChance = Math.random() * 0.10;
    }
}
