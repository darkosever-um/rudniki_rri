package si.um.feri.maprri.util;

import si.um.feri.maprri.models.Mine;
import si.um.feri.maprri.models.SimulationHistory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Simulation {
    public Mine mine;
    public LocalDate startDate;
    public LocalDate endDate;
    public double totalExpenses;
    public double dailyExpenses;
    public List<SimulationHistory> simulationHistory;

    public Simulation(){};
    public Simulation(Mine mine, LocalDate startDate, LocalDate endDate, double totalExpenses, double dailyExpenses){
        this.mine = mine;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalExpenses = totalExpenses;
        this.dailyExpenses = dailyExpenses;
        this.simulationHistory = new ArrayList<>();
    }
}
