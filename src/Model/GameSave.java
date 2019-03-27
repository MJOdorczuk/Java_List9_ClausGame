package Model;

import java.io.Serializable;

public class GameSave implements Serializable {
    private final Torus.Occupation[][] occupations;
    public GameSave(Torus.Occupation[][] occupations)
    {
        this.occupations = occupations;
    }
    public Torus.Occupation[][] getOccupations(){
        return occupations;
    }
}
