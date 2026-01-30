package calcpa.service;

public class GradeMapper {
    static double toGrade4(String grade) {
        grade = grade.toUpperCase();
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
}
