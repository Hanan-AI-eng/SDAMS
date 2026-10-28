package service;

import model.TrainingClass;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ClassService {

    private static final String FILE_PATH = "data/classes.txt";

    public ClassService() {
        File folder = new File("data");
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public boolean addClass(TrainingClass trainingClass)
            throws IOException {

        if (findClass(trainingClass.getClassId()) != null) {
            return false; // Duplicate ID
        }

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(FILE_PATH, true))) {

            writer.write(toLine(trainingClass));
            writer.newLine();
        }

        return true;
    }

    public List<TrainingClass> getAllClasses()
            throws IOException {

        List<TrainingClass> classes = new ArrayList<>();
        File file = new File(FILE_PATH);

        if (!file.exists()) {
            return classes;
        }

        try (BufferedReader reader =
                new BufferedReader(new FileReader(file))) {

            String line;

            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    TrainingClass trainingClass = fromLine(line);

                    if (trainingClass != null) {
                        classes.add(trainingClass);
                    }
                }
            }
        }

        return classes;
    }

    public TrainingClass findClass(String classId)
            throws IOException {

        for (TrainingClass trainingClass : getAllClasses()) {
            if (trainingClass.getClassId().equalsIgnoreCase(classId)) {
                return trainingClass;
            }
        }

        return null;
    }

    public boolean updateClass(TrainingClass updatedClass)
            throws IOException {

        List<TrainingClass> classes = getAllClasses();
        boolean found = false;

        for (int i = 0; i < classes.size(); i++) {
            if (classes.get(i).getClassId()
                    .equalsIgnoreCase(updatedClass.getClassId())) {

                classes.set(i, updatedClass);
                found = true;
                break;
            }
        }

        if (!found) {
            return false;
        }

        saveAll(classes);
        return true;
    }

    public boolean cancelClass(String classId)
            throws IOException {

        TrainingClass trainingClass = findClass(classId);

        if (trainingClass == null) {
            return false;
        }

        trainingClass.setStatus("CANCELLED");
        return updateClass(trainingClass);
    }

    private void saveAll(List<TrainingClass> classes)
            throws IOException {

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(FILE_PATH))) {

            for (TrainingClass trainingClass : classes) {
                writer.write(toLine(trainingClass));
                writer.newLine();
            }
        }
    }

    private String toLine(TrainingClass c) {
        return String.join("|",
                c.getClassId(),
                c.getClassName(),
                c.getDiscipline(),
                c.getInstructorId(),
                c.getDate(),
                c.getStartTime(),
                c.getEndTime(),
                c.getRoom(),
                String.valueOf(c.getCapacity()),
                String.valueOf(c.getFee()),
                c.getStatus());
    }

    private TrainingClass fromLine(String line) {
        try {
            String[] p = line.split("\\|", -1);

            if (p.length != 11) {
                return null;
            }

            TrainingClass c = new TrainingClass(
                    p[0], p[1], p[2], p[3], p[4],
                    p[5], p[6], p[7],
                    Integer.parseInt(p[8]),
                    Double.parseDouble(p[9]));

            c.setStatus(p[10]);
            return c;

        } catch (RuntimeException e) {
            return null;
        }
    }
}