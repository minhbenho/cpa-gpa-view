package calcpa.service;

import calcpa.ExcelReader;
import calcpa.model.Course;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CourseService {

    // =========================================================
    // DATA
    // =========================================================
    private static final List<Course> courses = new ArrayList<>();

    // =========================================================
    // LOAD DATA (CSV → Course)
    // =========================================================
    public static void loadData() {
        courses.clear();
        courses.addAll(ExcelReader.readCourses(null));
    }

    // =========================================================
    // GET ALL
    // =========================================================
    public static List<Course> getAllCourses() {
        return courses;
    }

    // =========================================================
    // REMOVE COURSE
    // =========================================================
    public static void removeCourse(Course c) {
        courses.remove(c);
    }

    // =========================================================
    // SAVE DATA (Course → CSV)
    // =========================================================
    public static void saveData() {
        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(
                        new FileOutputStream("src/main/resources/data.csv"),
                        StandardCharsets.UTF_8
                )
        )) {
            // header
            pw.println("semester,code,name,credits,grade");

            for (Course c : courses) {
                pw.println(c.toCsvLine());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
