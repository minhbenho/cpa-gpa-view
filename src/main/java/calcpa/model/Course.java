package calcpa.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Course {
    private final StringProperty code = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final IntegerProperty credits = new SimpleIntegerProperty();
    private final StringProperty grade = new SimpleStringProperty();
    private final StringProperty semester = new SimpleStringProperty();

    public Course(String code, String name, int credits, String grade, String semester) {
        this.code.set(code);
        this.name.set(name);
        this.credits.set(credits);
        this.grade.set(grade);
        this.semester.set(semester);
    }

    public String getCode() {
        return code.get();
    }

    public void setCode(String code) {
        this.code.set(code);
    }

    public StringProperty codeProperty() {
        return code;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public int getCredits() {
        return credits.get();
    }

    public void setCredits(int credits) {
        this.credits.set(credits);
    }

    public IntegerProperty creditsProperty() {
        return credits;
    }

    public String getGrade() {
        return grade.get();
    }

    public void setGrade(String grade) {
        this.grade.set(grade);
    }

    public String getSemester() {
        return semester.get();
    }

    public void setSemester(String semester) {
        this.semester.set(semester);
    }

    public String toCsvLine() {
        return String.join(",", getSemester(), getCode(), getName(),
                String.valueOf(getCredits()), getGrade() == null ? "" : getGrade() );
    }

    @Override
    public String toString() {
        return "Course{" + "semester='" + getSemester() + '\'' + ", code='" + getCode() + '\'' + ", name='" +
                getName() + '\'' + ", credits=" + getCredits() + ", grade='" + getGrade() + '\'' + '}';
    }

}
