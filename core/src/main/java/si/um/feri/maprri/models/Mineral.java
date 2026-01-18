package si.um.feri.maprri.models;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import si.um.feri.maprri.models.enums.MineralGrade;
import si.um.feri.maprri.models.enums.MineralName;

public class Mineral implements Json.Serializable {
    public MineralName name;
    public Float min;
    public Float max;
    public MineralGrade grade;

    // funkcija preveri če sta grade in ime iz svojih enum datotek
    public boolean validateMineral(){
        return name != null && grade != null;
    }

    @Override
    public void write(Json json) {
        json.writeValue("name", name.ordinal());
        json.writeValue("min", min);
        json.writeValue("max", max);
        json.writeValue("min", min);
    }

    @Override
    public void read(Json json, JsonValue jsonValue) {
        int nameInt = jsonValue.getInt("name");
        name = MineralName.values()[nameInt];

        min = jsonValue.getFloat("min", 0.0f);
        max = jsonValue.getFloat("max", 0.0f);

        int gradeInt = jsonValue.getInt("grade", 3);
        grade = MineralGrade.values()[gradeInt];
    }

    public String toString(){
        return "Name: " + name + "\n"
            + "Min: " + min + "\n"
            + "Max: " + max + "\n"
            + "Grade: " + grade + "\n";
    }
}
