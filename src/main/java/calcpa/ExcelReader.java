package calcpa;

import calcpa.model.Course;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ExcelReader {

    public static List<Course> readCourses(String ignoredPath) {
        List<Course> courses = new ArrayList<>();

        try {
            InputStream is = ExcelReader.class
                    .getResourceAsStream("/data.csv");

            if (is == null) {
                throw new RuntimeException("Không tìm thấy data.csv trong resources");
            }

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8)
            );

            String line;
            br.readLine(); // bỏ header

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(",");
                if (parts.length < 5) continue;

                String semester = parts[0].trim();
                String code     = parts[1].trim();
                String name     = parts[2].trim();
                int credits     = Integer.parseInt(parts[3].trim());
                String grade    = parts[4].trim(); // GIỮ NGUYÊN STRING

                courses.add(new Course(
                        code,
                        name,
                        credits,
                        grade,
                        semester
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return courses;
    }
}
