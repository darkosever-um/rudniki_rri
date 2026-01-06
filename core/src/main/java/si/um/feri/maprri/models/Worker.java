package si.um.feri.maprri.models;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import si.um.feri.maprri.models.enums.WorkerType;

import java.net.IDN;

public class Worker implements Json.Serializable{

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

    @Override
    public void write(Json json) {
        json.writeValue("firstName", firstName);
        json.writeValue("lastName", lastName);

        json.writeObjectStart("birthDate");
        json.writeValue("$date", birthDate);
        json.writeObjectEnd();

        json.writeValue("IDNumber", IDNumber);
        json.writeValue("type", type.ordinal());
        json.writeValue("salary", salary);
    }

    @Override
    public void read(Json json, JsonValue jsonValue) {
        int typeInt = jsonValue.getInt("type");
        type = WorkerType.values()[typeInt];
        firstName = jsonValue.getString("firstName");
        lastName = jsonValue.getString("lastName");

        JsonValue dateObj = jsonValue.get("birthDate");
        if (dateObj != null && dateObj.has("$date")) {
            birthDate = dateObj.getLong("$date");
        }

        IDNumber = jsonValue.getInt("IDNumber");
        salary = jsonValue.getDouble("salary");
    }

    public String toString(){
        return "First name: " + firstName + "\n"
            + "Last name: " + lastName + "\n"
            + "Birth date: " + birthDate + "\n"
            + "ID number: " + IDNumber + "\n"
            + "Salary: " + salary + "\n"
            + "Position: " + type + "\n";
    }
}
