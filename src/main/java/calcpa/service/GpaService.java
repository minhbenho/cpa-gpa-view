package calcpa.service;

import calcpa.model.Course;
import calcpa.util.GradeMapper;

import java.util.*;

public class GpaService {
    private static boolean isIgnoredGrade(String grade) {
        if (grade == null || grade.isBlank()) return true;
        grade = grade.trim().toUpperCase();
        List<String> needToCalGrade= Arrays.asList("A+", "A", "B+", "B", "C+", "C", "D+", "D", "F");
        return needToCalGrade.contains(grade)!=false;
    }

    public static double calcGpa(List<Course> courses) {
        if (courses == null || courses.isEmpty()) return 0.0;

        double totalPoints = 0.0;
        int totalCredits = 0;

        for (Course c : courses) {
            String grade = c.getGrade();

            if (isIgnoredGrade(grade)) continue;

            double grade4 = GradeMapper.toGrade4(grade);

            totalPoints += grade4 * c.getCredits();
            totalCredits += c.getCredits();
        }

        return totalCredits == 0 ? 0.0 : totalPoints / totalCredits;
    }

    // =========================================================
    // Gom môn học theo kỳ
    // =========================================================
    public static Map<String, List<Course>> groupBySemester(List<Course> courses) {
        Map<String, List<Course>> map = new TreeMap<>();

        for (Course c : courses) {
            map.computeIfAbsent(c.getSemester(), k -> new ArrayList<>())
                    .add(c);
        }

        return map;
    }

    // =========================================================
    // CPA tính dồn từ kỳ đầu → kỳ được chọn
    // =========================================================
    private static List<Course> normalizeByBestGrade(List<Course> courses) {
        Map<String, Course> bestByCode = new HashMap<>();
        for (Course c : courses) {
            if (isIgnoredGrade(c.getGrade())) continue;
            String code = c.getCode();
            double grade4 = GradeMapper.toGrade4(c.getGrade());
            if (!bestByCode.containsKey(code)) bestByCode.put(code, c);
            else {
                Course old = bestByCode.get(code);
                double oldGrade4 = GradeMapper.toGrade4(old.getGrade());
                if (grade4 > oldGrade4) bestByCode.put(code, c);
            }
        }
        return new ArrayList<>(bestByCode.values());
    }

    public static Map<String, Double> calcCpaBySemester( Map<String, List<Course>> grouped, Set<String> selectedSemesters ){
        Map<String, Double> result = new LinkedHashMap<>();
        List<Course> accumulated = new ArrayList<>();
        for (String semester : grouped.keySet()) {
            accumulated.addAll(grouped.get(semester));
            if (selectedSemesters.contains(semester)) {
                List<Course> normalized = normalizeByBestGrade(accumulated);
                result.put(semester, calcGpa(normalized));
            }
        }
        return result;
    }
    // =========================================================
// GPA theo từng kỳ (dùng cho Dashboard)
// =========================================================
    public static Map<String, Double> calcGpaBySemester(
            Map<String, List<Course>> grouped,
            Set<String> selectedSemesters
    ) {
        Map<String, Double> result = new LinkedHashMap<>();

        for (String semester : grouped.keySet()) {
            if (selectedSemesters.contains(semester)) {
                result.put(semester, calcGpa(grouped.get(semester)));
            }
        }

        return result;
    }

}
