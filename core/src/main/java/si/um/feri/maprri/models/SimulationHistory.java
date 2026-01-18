package si.um.feri.maprri.models;

import si.um.feri.maprri.models.enums.InfrastructureStatus;
import si.um.feri.maprri.models.enums.MineralName;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class SimulationHistory {
    public int step;
    public LocalDate date;
    public double expenses;
    public Map<Integer, Boolean> workerAttendance;
    public List<InfrastructureSimStep> infrastructureSimStepList;
    public Map<MineralName, Double> minedAmount;
}
