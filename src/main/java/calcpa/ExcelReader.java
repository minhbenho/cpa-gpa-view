package calcpa;

import calcpa.model.Course;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ExcelReader {
    public static List<Course> readCourses() {
        List<Course> courses = new ArrayList<>();
        try {
            InputStream is;
            Path devPath = Paths.get("src/main/resources/data.csv");
            if (Files.exists(devPath)) is = Files.newInputStream(devPath);
            else throw new RuntimeException("Không tìm thấy src/main/resources/data.csv");
            BufferedReader br = new BufferedReader( new InputStreamReader(is, StandardCharsets.UTF_8) );
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length < 5) continue;
                String semester = parts[0].trim();
                String code     = parts[1].trim();
                String name     = parts[2].trim();
                int credits     = Integer.parseInt(parts[3].trim());
                String grade    = parts[4].trim();
                courses.add(new Course( code, name, credits, grade, semester ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return courses;
    }
}
