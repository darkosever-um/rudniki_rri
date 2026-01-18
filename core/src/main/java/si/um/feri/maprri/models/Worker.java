package si.um.feri.maprri.models;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class Worker implements Json.Serializable {
    public String firstName;
    public String lastName;
    public long birthDate;
    public int idNumber;
    public Integer type;
    public double salary;

    public double absenceChance = Math.random() * 0.10;
    public Worker() {}

    @Override
    public void write(Json json) {
        json.writeValue("firstName", firstName);
        json.writeValue("lastName", lastName);

        json.writeObjectStart("birthDate");
        json.writeValue("$date", birthDate);
        json.writeObjectEnd();

        json.writeValue("IDNumber", idNumber);
        json.writeValue("type", type);
        json.writeValue("salary", salary);
    }

    @Override
    public void read(Json json, JsonValue jsonValue) {
        firstName = jsonValue.getString("firstName");
        lastName = jsonValue.getString("lastName");
        idNumber = jsonValue.getInt("IDNumber");
        type = jsonValue.getInt("type");
        salary = jsonValue.getDouble("salary");

        if (jsonValue.has("birthDate")) {
            JsonValue dateObj = jsonValue.get("birthDate");
            if (dateObj != null && dateObj.has("$date") && !dateObj.get("$date").isNull()) {
                birthDate = dateObj.getLong("$date");
            } else {
                birthDate = 0;
            }
        }
    }
}
