package org.teamtators.common.characterization;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class CharacterizationRunner {
    public static void main(String[] args) throws IOException {
        characterizeSmartMotion("datalogs");
    }

    public static void characterizeSmartMotion(String directory) {
        File datalogsDir = new File(directory);
        ArrayList<CharacterizationDataset> dataset = new ArrayList<>();
        for(File f : datalogsDir.listFiles( (f, s) -> s.endsWith(".csv") && !s.equals("out.csv"))) {
            CharacterizationDataset data = null;
            try { data = CharacterizationDataset.fromCSV(f); } catch (Exception e) { e.printStackTrace();}
            if(data != null) {
                dataset.add(data);
            }
        }
        if(dataset.isEmpty()) {
            System.err.println("No datalogs!");
            return;
        }
        SmartMotionCharacterizationRoutine routine = new SmartMotionCharacterizationRoutine();
        routine.characterize(dataset);
    }
}
