package calcpa.controller;

import calcpa.model.Course;
import calcpa.service.CourseService;
import calcpa.service.GpaService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.*;

public class SemesterController {

    @FXML private ComboBox<String> semesterCombo;
    @FXML private Button btnAddCourse;
    @FXML private TableView<Course> courseTable;
    @FXML private TableColumn<Course, String> colCode;
    @FXML private TableColumn<Course, String> colName;
    @FXML private TableColumn<Course, Integer> colCredits;
    @FXML private TableColumn<Course, String> colGrade;
    @FXML private TableColumn<Course, Void> colAction;
    @FXML private Label gpaLabel;
    @FXML private Label cpaLabel;

    private Map<String, List<Course>> groupedCourses = new LinkedHashMap<>();
    private final ObservableList<Course> courseList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableColumns();
        semesterCombo.setOnAction(e -> onSemesterSelected());
        reloadData();
    }

    /**
     * Được gọi khi app cần reload dữ liệu (nút Reload hoặc mở màn Semester).
     */
    public void reloadData() {
        initData();
        initSemesterCombo();

        // Giữ lại kỳ đang chọn nếu còn tồn tại
        String current = semesterCombo.getValue();
        if (current != null && groupedCourses.containsKey(current)) {
            semesterCombo.setValue(current);
            onSemesterSelected();
        } else {
            courseList.clear();
            btnAddCourse.setVisible(false);
            gpaLabel.setText("GPA: ");
            cpaLabel.setText("CPA: ");
        }
    }

    private void initData() {
        List<Course> courses = CourseService.getAllCourses();
        groupedCourses = GpaService.groupBySemester(courses);
    }

    private void initSemesterCombo() {
        // Lấy danh sách các kỳ từ groupedCourses
        List<String> semesters = new ArrayList<>(groupedCourses.keySet());
        Collections.sort(semesters);
        semesterCombo.setItems(FXCollections.observableArrayList(semesters));
    }

    private void setupTableColumns() {
        colCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCredits.setCellValueFactory(new PropertyValueFactory<>("credits"));
        colGrade.setCellValueFactory(new PropertyValueFactory<>("grade"));
        
        // Cột Action với nút Xóa
        colAction.setCellFactory(param -> new TableCell<Course, Void>() {
            private final Button deleteBtn = new Button("Xóa");
            
            {
                deleteBtn.setOnAction(event -> {
                    Course course = getTableView().getItems().get(getIndex());
                    CourseService.removeCourse(course);
                    courseList.remove(course);
                    updateGpaCpa();
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteBtn);
                }
            }
        });
        
        courseTable.setItems(courseList);
    }

    private void onSemesterSelected() {
        String selectedSemester = semesterCombo.getValue();
        if (selectedSemester == null) {
            courseList.clear();
            btnAddCourse.setVisible(false);
            gpaLabel.setText("GPA: ");
            cpaLabel.setText("CPA: ");
            return;
        }

        // Hiển thị danh sách môn học của kỳ được chọn
        List<Course> courses = groupedCourses.get(selectedSemester);
        courseList.setAll(courses);
        
        // Hiển thị nút Thêm môn
        btnAddCourse.setVisible(true);
        
        // Cập nhật GPA và CPA
        updateGpaCpa();
    }

    private void updateGpaCpa() {
        String selectedSemester = semesterCombo.getValue();
        if (selectedSemester == null) {
            gpaLabel.setText("GPA: ");
            cpaLabel.setText("CPA: ");
            return;
        }

        // Tính GPA của kỳ được chọn
        List<Course> semesterCourses = groupedCourses.getOrDefault(selectedSemester, List.of());
        double gpa = GpaService.calcGpa(semesterCourses);
        gpaLabel.setText(String.format("GPA: %.2f", gpa));

        // Tính CPA (tích lũy từ đầu đến kỳ được chọn)
        Set<String> selectedSemesters = new LinkedHashSet<>();
        for (String semester : groupedCourses.keySet()) {
            selectedSemesters.add(semester);
            if (semester.equals(selectedSemester)) {
                break;
            }
        }
        
        Map<String, Double> cpaMap = GpaService.calcCpaBySemester(groupedCourses, selectedSemesters);
        double cpa = cpaMap.getOrDefault(selectedSemester, 0.0);
        cpaLabel.setText(String.format("CPA: %.2f", cpa));
    }

    @FXML
    private void onAddSemester() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText("Chức năng sẽ được update sau");
        alert.showAndWait();
    }
}
