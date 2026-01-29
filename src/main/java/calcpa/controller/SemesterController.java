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
import java.util.prefs.Preferences;

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
    
    // Preferences để lưu trữ kỳ được chọn cuối cùng
    private static final Preferences preferences = Preferences.userNodeForPackage(SemesterController.class);
    private static final String SELECTED_SEMESTER_KEY = "selectedSemester";

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

        // Lấy kỳ được chọn lần trước từ Preferences
        String savedSemester = preferences.get(SELECTED_SEMESTER_KEY, null);
        String current = semesterCombo.getValue();
        
        // Ưu tiên: kỳ được lưu > kỳ hiện tại > không có
        String toSelect = null;
        if (savedSemester != null && groupedCourses.containsKey(savedSemester)) {
            toSelect = savedSemester;
        } else if (current != null && groupedCourses.containsKey(current)) {
            toSelect = current;
        } else if (!groupedCourses.isEmpty()) {
            // Chọn kỳ đầu tiên nếu có
            toSelect = groupedCourses.keySet().iterator().next();
        }
        
        if (toSelect != null) {
            semesterCombo.setValue(toSelect);
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

        // ====== EDIT CREDITS (SỐ TÍN CHỈ) BẰNG CHUỘT PHẢI ======
        colCredits.setCellFactory(col -> new TableCell<Course, Integer>() {
            {
                // Context menu chuột phải
                ContextMenu menu = new ContextMenu();
                MenuItem editItem = new MenuItem("Sửa số tín chỉ");
                editItem.setOnAction(e -> editCredits());
                menu.getItems().add(editItem);

                setContextMenu(menu);
            }

            private void editCredits() {
                Course course = getCurrentCourse();
                if (course == null) return;

                String current = String.valueOf(course.getCredits());
                TextInputDialog dialog = new TextInputDialog(current);
                dialog.setTitle("Sửa số tín chỉ");
                dialog.setHeaderText(null);
                dialog.setContentText("Số tín chỉ (số tự nhiên > 0):");

                dialog.showAndWait().ifPresent(text -> {
                    String trimmed = text.trim();
                    try {
                        int value = Integer.parseInt(trimmed);
                        if (value <= 0) {
                            showError("Số tín chỉ phải là số tự nhiên > 0.");
                            return;
                        }
                        course.setCredits(value);
                        courseTable.refresh();
                        // Lưu lại ra CSV và reload toàn app (Dashboard, Semester, ...)
                        persistAndReload();
                    } catch (NumberFormatException ex) {
                        showError("Số tín chỉ phải là số nguyên hợp lệ.");
                    }
                });
            }

            private Course getCurrentCourse() {
                if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) return null;
                return getTableView().getItems().get(getIndex());
            }

            private void showError(String message) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Giá trị không hợp lệ");
                alert.setHeaderText(null);
                alert.setContentText(message);
                alert.showAndWait();
            }

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.valueOf(item));
            }
        });

        // ====== EDIT GRADE (ĐIỂM) BẰNG CHUỘT PHẢI ======
        colGrade.setCellFactory(col -> new TableCell<Course, String>() {
            private final Set<String> ALLOWED =
                    new LinkedHashSet<>(Arrays.asList("NA", "X", "R", "A+", "A",
                            "B+", "B", "C+", "C", "D+", "D", "F"));

            {
                ContextMenu menu = new ContextMenu();
                MenuItem editItem = new MenuItem("Sửa điểm");
                editItem.setOnAction(e -> editGrade());
                menu.getItems().add(editItem);
                setContextMenu(menu);
            }

            private void editGrade() {
                Course course = getCurrentCourse();
                if (course == null) return;

                String current = course.getGrade() == null ? "" : course.getGrade();
                TextInputDialog dialog = new TextInputDialog(current);
                dialog.setTitle("Sửa điểm");
                dialog.setHeaderText("Giá trị hợp lệ: " + String.join(", ", ALLOWED));
                dialog.setContentText("Điểm:");

                dialog.showAndWait().ifPresent(text -> {
                    String value = text.trim().toUpperCase();
                    if (!ALLOWED.contains(value)) {
                        showError("Điểm phải thuộc một trong các giá trị: " +
                                String.join(", ", ALLOWED));
                        return;
                    }
                    course.setGrade(value);
                    courseTable.refresh();
                    // Lưu lại ra CSV và reload toàn app
                    persistAndReload();
                });
            }

            private Course getCurrentCourse() {
                if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) return null;
                return getTableView().getItems().get(getIndex());
            }

            private void showError(String message) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Giá trị không hợp lệ");
                alert.setHeaderText(null);
                alert.setContentText(message);
                alert.showAndWait();
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
            }
        });

        // Cột Action với nút "Thao tác khác"
        colAction.setCellFactory(param -> new TableCell<Course, Void>() {
            private final Button actionBtn = new Button("Thao tác khác");
            
            {
                actionBtn.setOnAction(event -> {
                    Course course = getTableView().getItems().get(getIndex());
                    showActionMenu(course);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(actionBtn);
                }
            }
        });
        
        courseTable.setItems(courseList);
    }

    /**
     * Ghi lại dữ liệu ra CSV và yêu cầu MainController reload toàn bộ app
     * (Dashboard, Semester, ...), tránh việc người dùng phải bấm Reload tay.
     */
    private void persistAndReload() {
        // Ghi ra data.csv
        CourseService.saveData();
        // Cập nhật lại GPA/CPA tại màn hiện tại
        updateGpaCpa();
        // Gọi reloadApp() ở MainController nếu có
        MainController main = MainController.getInstance();
        if (main != null) {
            main.reloadApp();
        }
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

        // Lưu kỳ được chọn vào Preferences
        preferences.put(SELECTED_SEMESTER_KEY, selectedSemester);

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

    /**
     * Hiển thị menu thao tác cho môn học
     */
    private void showActionMenu(Course course) {
        showCommingSoon();
//        ContextMenu menu = new ContextMenu();
//
//        // Tùy chọn 1: Xóa môn học
//        MenuItem deleteItem = new MenuItem("Xóa môn học");
//        deleteItem.setOnAction(e -> deleteSubject(course));
//        menu.getItems().add(deleteItem);
//
//        // Tùy chọn 2: Di chuyển sang kỳ khác
//        MenuItem moveItem = new MenuItem("Di chuyển sang kỳ khác");
//        moveItem.setOnAction(e -> moveToOtherSemester(course));
//        menu.getItems().add(moveItem);
//
//        // Hiển thị menu tại vị trí con trỏ chuột
//        menu.show(courseTable, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    /**
     * Xóa môn học
     */
    private void deleteSubject(Course course) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận xóa");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Bạn có chắc chắn muốn xóa môn '" + course.getName() + "' không?");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                CourseService.removeCourse(course);
                courseList.remove(course);
                persistAndReload();
            }
        });
    }

    /**
     * Di chuyển môn học sang kỳ khác
     */
    private void moveToOtherSemester(Course course) {
        // Lấy danh sách các kỳ (trừ kỳ hiện tại)
        String currentSemester = semesterCombo.getValue();
        List<String> otherSemesters = new ArrayList<>(groupedCourses.keySet());
        otherSemesters.remove(currentSemester);
        
        if (otherSemesters.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Thông báo");
            alert.setHeaderText(null);
            alert.setContentText("Không có kỳ khác để di chuyển.");
            alert.showAndWait();
            return;
        }
        
        // Hiển thị dialog chọn kỳ
        ChoiceDialog<String> dialog = new ChoiceDialog<>(otherSemesters.get(0), otherSemesters);
        dialog.setTitle("Chọn kỳ đích");
        dialog.setHeaderText(null);
        dialog.setContentText("Chọn kỳ để di chuyển môn học:");
        
        dialog.showAndWait().ifPresent(selectedSemester -> {
            // Kiểm tra xem kỳ đó đã có môn này chưa
            List<Course> targetSemesterCourses = groupedCourses.get(selectedSemester);
            boolean isDuplicate = targetSemesterCourses.stream()
                    .anyMatch(c -> c.getCode().equals(course.getCode()));
            
            if (isDuplicate) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Cảnh báo");
                alert.setHeaderText(null);
                alert.setContentText("Môn '" + course.getName() + "' đã tồn tại trong kỳ '" + selectedSemester + "'.");
                alert.showAndWait();
                return;
            }
            
            // Xác nhận di chuyển
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Xác nhận di chuyển");
            confirmAlert.setHeaderText(null);
            confirmAlert.setContentText("Bạn có chắc chắn muốn di chuyển môn '" + course.getName() + 
                                       "' sang kỳ '" + selectedSemester + "' không?");
            
            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    // Cập nhật semester cho môn học
                    course.setSemester(selectedSemester);
                    
                    // Cập nhật lại groupedCourses
                    groupedCourses.get(currentSemester).remove(course);
                    groupedCourses.get(selectedSemester).add(course);
                    
                    // Cập nhật danh sách hiển thị trên bảng
                    courseList.remove(course);
                    
                    // Lưu lại ra CSV và reload toàn app
                    persistAndReload();
                }
            });
        });
    }

    @FXML
    private void onAddSemester() {
        showCommingSoon();
    }
    @FXML
    private void onAddCourse() {
        showCommingSoon();
    }
    private void showCommingSoon(){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText("Chức năng sẽ được update sau");
        alert.showAndWait();
    }
}
