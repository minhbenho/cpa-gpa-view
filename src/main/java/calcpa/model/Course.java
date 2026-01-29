package calcpa.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class Course {

    // ================= FIELDS =================
    private final StringProperty code = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final IntegerProperty credits = new SimpleIntegerProperty();
    private final StringProperty grade = new SimpleStringProperty();
    private final StringProperty semester = new SimpleStringProperty();

    // ================= CONSTRUCTOR =================
    public Course(String code, String name, int credits, String grade, String semester) {
        this.code.set(code);
        this.name.set(name);
        this.credits.set(credits);
        this.grade.set(grade);
        this.semester.set(semester);
    }

    // ================= CODE =================
    public String getCode() {
        return code.get();
    }

    public void setCode(String code) {
        this.code.set(code);
    }

    public StringProperty codeProperty() {
        return code;
    }

    // ================= NAME =================
    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return name;
    }

    // ================= CREDITS =================
    public int getCredits() {
        return credits.get();
    }

    public void setCredits(int credits) {
        this.credits.set(credits);
    }

    public IntegerProperty creditsProperty() {
        return credits;
    }

    // ================= GRADE =================
    public String getGrade() {
        return grade.get();
    }

    public void setGrade(String grade) {
        this.grade.set(grade);
    }

    public StringProperty gradeProperty() {
        return grade;
    }

    // ================= SEMESTER =================
    public String getSemester() {
        return semester.get();
    }

    public void setSemester(String semester) {
        this.semester.set(semester);
    }

    public StringProperty semesterProperty() {
        return semester;
    }

    // ================= CSV SUPPORT =================
    /**
     * Dùng khi ghi ra file CSV
     */
    public String toCsvLine() {
        return String.join(",",
                getSemester(),
                getCode(),
                getName(),
                String.valueOf(getCredits()),
                getGrade() == null ? "" : getGrade()
        );
    }

    // ================= DEBUG =================
    @Override
    public String toString() {
        return "Course{" +
                "semester='" + getSemester() + '\'' +
                ", code='" + getCode() + '\'' +
                ", name='" + getName() + '\'' +
                ", credits=" + getCredits() +
                ", grade='" + getGrade() + '\'' +
                '}';
    }

}
