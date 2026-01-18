package si.um.feri.maprri.util;

import com.badlogic.gdx.maps.MapObject;
import jdk.vm.ci.meta.Local;
import si.um.feri.maprri.models.*;
import si.um.feri.maprri.models.enums.InfrastructureStatus;
import si.um.feri.maprri.models.enums.MineralName;

import java.time.LocalDate;
import java.util.*;

public class Simulation {
    public Mine mine;
    public LocalDate startDate;
    public LocalDate endDate;
    public String stepUnit;
    public double totalExpenses;
    public List<SimulationHistory> simulationHistory;

    private Map<MineralName, Double> mineralReserves;

    private LocalDate currentDate;
    private int stepCounter;
    private boolean running;
    private boolean finished;
    private Random random;

    public static double FUEL_PRICE = 1.5f;
    public static double DAILY_KM = 250.0f;
    public static double DAILY_HOURS = 8.0f;

    public Simulation(Mine mine){
        this.mine = mine;
        this.totalExpenses = 0;
        this.simulationHistory = new ArrayList<>();
        this.mineralReserves = new HashMap<>();
        random = new Random();
    }
    public Simulation(){};
    public Simulation(Mine mine, LocalDate startDate, LocalDate endDate, double totalExpenses, String stepUnit){
        this.mine = mine;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalExpenses = totalExpenses;
        this.simulationHistory = new ArrayList<>();
        this.stepUnit = stepUnit;
    }

    public void reset(){
        this.currentDate = startDate;
        this.stepCounter = 1;
        this.totalExpenses = 0;
        this.simulationHistory.clear();
        this.running = false;
        this.finished = false;
        this.mineralReserves.clear();
        if(mine != null && mine.getMinerals() != null) {
            for(Mineral m : mine.getMinerals()) {
                double quantity = m.min + (m.max - m.min) * random.nextDouble();
                mineralReserves.put(m.name, quantity);
            }
        }
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isFinished() {
        return finished;
    }

    public SimulationHistory getLastStep() {
        if (simulationHistory.isEmpty()) return null;
        return simulationHistory.get(simulationHistory.size() - 1);
    }

    public void calculateNextStep(){
        if(mine == null || startDate == null || endDate == null) {
            System.out.println("ERROR IN SIMULATION" + mine==null + ", " + startDate + ", " + endDate);
            return;
        }
        if (finished || currentDate.isAfter(endDate)) {
            finished = true;
            running = false;
            return;
        }

        SimulationHistory step = new SimulationHistory();
        step.step = stepCounter;
        step.date = currentDate;
        step.workerAttendance = new HashMap<>();
        step.infrastructureSimStepList = new ArrayList<>();
        step.minedAmount = new HashMap<>();
        step.expenses = 0.0f;

        int activeWorkersCounter = 0;
        int activeVehicleCounter = 0;
        //Handle workers
        if(mine.getWorkers() != null){
            for(Worker worker : mine.getWorkers()){
                boolean absent = random.nextDouble() < worker.absenceChance;
                step.workerAttendance.put(worker.idNumber, !absent);
                if(!absent){
                    activeWorkersCounter++;
                    step.expenses += worker.salary / 30.0f;
                }
            }
        }
        //Handle infrastructure
        if(mine.getInfrastructures() != null){
            for(Infrastructure temp : mine.getInfrastructures()){
                InfrastructureSimStep infrastructure = new InfrastructureSimStep(temp);
                double fuel = 0.0;
                double hours = 0.0f;
                int km = 0;

                if(infrastructure.status == InfrastructureStatus.BROKEN){
                    // < 60% to go in repair
                    if(random.nextDouble() < 0.60){
                        infrastructure.status = InfrastructureStatus.INREPAIR;
                    }
                }else if(infrastructure.status == InfrastructureStatus.ACTIVE){
                    activeVehicleCounter++;
                    if(random.nextDouble() < infrastructure.breakDownChance){
                        infrastructure.status = InfrastructureStatus.BROKEN;
                        hours = DAILY_HOURS / 2;
                        km = (int)DAILY_KM / 2;
                    } else if (random.nextDouble() < infrastructure.cleaningChance){
                        step.expenses += 20;
                        hours += DAILY_HOURS - 1;
                        km = (int)DAILY_KM - 40;
                    } else {
                        hours += DAILY_HOURS;
                        km = (int)DAILY_KM;
                    }

                    infrastructure.operatingHours += (float)hours;
                    infrastructure.kilometer += km;
                    //km driven * avgFuel(per 100km) / 100;
                    fuel = (km * infrastructure.avgFuelConsumption)/100;
                    step.expenses += fuel * FUEL_PRICE;
                } else if (infrastructure.status == InfrastructureStatus.INREPAIR){
                    // <460% to go in repair
                    if(random.nextDouble() < 0.40){
                        infrastructure.status = InfrastructureStatus.ACTIVE;
                        step.expenses += 1500;
                    }
                }

                step.infrastructureSimStepList.add(infrastructure);
            }
        }

        //Handle minerals
        double workerKoef = activeWorkersCounter * 5.0;
        double infraKoef = activeVehicleCounter * 50.0;

        if(mine.getMinerals() != null){
            for(Mineral mineral : mine.getMinerals()){
                double gradeKoef = mineral.grade.getCoefficient();
                double currentReserves = mineralReserves.getOrDefault(mineral.name, 0.0);
                if(currentReserves <= 0){
                    step.minedAmount.put(mineral.name, 0.0);
                    continue;
                }

                double base = (workerKoef + infraKoef) * gradeKoef;
                double variance = 1.0 + (random.nextGaussian() / 4.0);
                if (variance < 0.1) {
                    variance = 0.1;
                }
                double amount = base * variance;

                if(amount > currentReserves){
                    amount = currentReserves;
                }
                mineralReserves.put(mineral.name, currentReserves - amount);

                step.minedAmount.put(mineral.name, amount);
            }
        }

        this.totalExpenses += step.expenses;
        this.simulationHistory.add(step);
        if(Objects.equals(stepUnit, "Dan")){
            currentDate = currentDate.plusDays(1);
            stepCounter++;
        } else if (Objects.equals(stepUnit, "Ura")){
            if(stepCounter  % 24 == 0){
                currentDate = currentDate.plusDays(1);
            }
            stepCounter++;
        }

        if (currentDate.isAfter(endDate)) {
            finished = true;
            running = false;
        }
    }
}
