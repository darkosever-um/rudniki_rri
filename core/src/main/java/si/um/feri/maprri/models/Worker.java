package si.um.feri.maprri.models;

import si.um.feri.maprri.models.enums.WorkerType;

public class Worker {

    private String firstName;
    private String lastName;
    private Long birthDate;
    private Integer IDNumber;
    private WorkerType type;
    private Double salary;

    public Worker() {
    }

    public Worker(String firstName, String lastName, Long birthDate, Integer IDNumber, WorkerType type, Double salary) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.IDNumber = IDNumber;
        this.type = type;
        this.salary = salary;
    }

    // Preveri da ni nič null
    public boolean validateWorkers() {
        return firstName != null && !firstName.isEmpty() &&
            lastName != null && !lastName.isEmpty() &&
            type != null &&
            IDNumber != null;
    }
}
