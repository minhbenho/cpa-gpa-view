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
    private static final List<Course> courses = new ArrayList<>();

    public static void loadData() {
        courses.clear();
        courses.addAll(ExcelReader.readCourses());
    }

    public static List<Course> getAllCourses() {
        return courses;
    }

    public static void removeCourse(Course c) {
        courses.remove(c);
    }

    public static void changeSemester(Course c, String newSemester){
        c.setSemester(newSemester);
    }

    public static void saveData() {
        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter( new FileOutputStream("src/main/resources/data.csv"),
                        StandardCharsets.UTF_8 ))) {
            pw.println("semester,code,name,credits,grade");
            String s=courses.getFirst().getSemester();
            String ss;
            for (Course c : courses){
                ss=c.getSemester();
                if(!ss.equals(s)){
                    pw.println();
                    s=ss;
                }
                pw.println(c.toCsvLine());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}
