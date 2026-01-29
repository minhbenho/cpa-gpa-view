package calcpa.util;

public class GradeMapper {

    public static double toGrade4(String grade) {

        grade = grade.toUpperCase();

        switch (grade) {
            case "A+":  return 4.0;
            case "A":  return 4.0;
            case "B+": return 3.5;
            case "B":  return 3.0;
            case "C+": return 2.5;
            case "C":  return 2.0;
            case "D+": return 1.5;
            case "D":  return 1.0;
            case "F": return 0.0;
        }

        // nếu là số
        try {
            double score10 = Double.parseDouble(grade);
            if (score10 >= 8.5) return 4.0;
            if (score10 >= 7.0) return 3.0;
            if (score10 >= 5.5) return 2.0;
            if (score10 >= 4.0) return 1.0;
        } catch (NumberFormatException ignored) {}

        // không bao giờ nên tới đây
        throw new IllegalArgumentException("Điểm không hợp lệ: " + grade);
    }
}
