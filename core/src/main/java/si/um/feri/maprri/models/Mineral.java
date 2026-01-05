package si.um.feri.maprri.models;

import si.um.feri.maprri.models.enums.MineralGrade;
import si.um.feri.maprri.models.enums.MineralName;

public class Mineral {
    MineralName name;
    private Float min;
    private Float max;
    MineralGrade grade;

    // funkcija preveri če sta grade in ime iz svojih enum datotek
    public boolean validateMineral(){
        return name != null && grade != null;
    }
}
