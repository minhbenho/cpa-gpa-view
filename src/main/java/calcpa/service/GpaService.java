package calcpa.service;

import calcpa.model.Course;

import java.util.*;

public class GpaService {
    private static boolean isIgnoredGrade(String grade) {
        if (grade == null || grade.isBlank()) return true;
        grade = grade.trim().toUpperCase();
        List<String> needToCalGrade= Arrays.asList("A+", "A", "B+", "B", "C+", "C", "D+", "D", "F");
        return !needToCalGrade.contains(grade);
    }

    private static boolean isPassedGrade(String grade) {
        if (isIgnoredGrade(grade)) return false;
        return toGrade4(grade) > 0.0;
    }

    private static double toGrade4(String grade) {
        switch (grade) {
            case "A+","A":  return 4.0;
            case "B+": return 3.5;
            case "B":  return 3.0;
            case "C+": return 2.5;
            case "C":  return 2.0;
            case "D+": return 1.5;
            case "D":  return 1.0;
            case "F": return 0.0;
            default: throw new IllegalArgumentException("Điểm không hợp lệ: " + grade);
        }
    }

    public static double calcGpa(List<Course> courses) {
        if (courses == null || courses.isEmpty()) return 0.0;
        double totalPoints = 0.0;
        int totalCredits = 0;
        for (Course c : courses) {
            String grade = c.getGrade();
            if (isIgnoredGrade(grade)) continue;
            double grade4 = toGrade4(grade);
            totalPoints += grade4 * c.getCredits();
            totalCredits += c.getCredits();
        }
        return totalCredits == 0 ? 0.0 : totalPoints / totalCredits;
    }

    public static Map<String, List<Course>> groupBySemester(List<Course> courses) {
        Map<String, List<Course>> map = new TreeMap<>();
        for (Course c : courses) map.computeIfAbsent(c.getSemester(), k -> new ArrayList<>()).add(c);
        return map;
    }

    private static List<Course> normalizeByBestGrade(List<Course> courses) {
        Map<String, Course> bestByCode = new HashMap<>();
        for (Course c : courses) {
            if (isIgnoredGrade(c.getGrade())) continue;
            String code = c.getCode();
            double grade4 = toGrade4(c.getGrade());
            if (!bestByCode.containsKey(code)) bestByCode.put(code, c);
            else {
                Course old = bestByCode.get(code);
                double oldGrade4 = toGrade4(old.getGrade());
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

    // =========================================================
    // Tín chỉ tích lũy theo kỳ (tính đến kỳ được chọn)
    // Logic: mỗi môn chỉ tính 1 lần; nếu học cải thiện thì lấy điểm cao nhất.
    // Chỉ cộng tín chỉ nếu điểm cao nhất > F.
    // =========================================================
    public static Map<String, Integer> calcCumulativeCreditsBySemester(
            Map<String, List<Course>> grouped,
            Set<String> selectedSemesters
    ) {
        Map<String, Integer> result = new LinkedHashMap<>();
        Map<String, Course> bestByCode = new HashMap<>();

        for (String semester : grouped.keySet()) {
            List<Course> list = grouped.getOrDefault(semester, List.of());
            for (Course c : list) {
                if (isIgnoredGrade(c.getGrade())) continue;
                String code = c.getCode();
                if (code == null || code.isBlank()) continue;
                if (!bestByCode.containsKey(code)) {
                    bestByCode.put(code, c);
                } else {
                    Course old = bestByCode.get(code);
                    double oldGrade4 = toGrade4(old.getGrade());
                    double newGrade4 = toGrade4(c.getGrade());
                    if (newGrade4 > oldGrade4) bestByCode.put(code, c);
                }
            }

            if (selectedSemesters.contains(semester)) {
                int cumulativeCredits = 0;
                for (Course best : bestByCode.values()) {
                    if (isPassedGrade(best.getGrade())) {
                        cumulativeCredits += best.getCredits();
                    }
                }
                result.put(semester, cumulativeCredits);
            }
        }

        return result;
    }

}
