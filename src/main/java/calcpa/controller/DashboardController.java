package calcpa.controller;


import calcpa.model.Course;
import calcpa.service.CourseService;
import calcpa.service.GpaService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import java.util.prefs.Preferences;
import java.util.*;

public class DashboardController {

    @FXML private MenuButton semesterMenuButton;
    @FXML private LineChart<String, Number> gpaChart;
    @FXML private LineChart<String, Number> cpaChart;
    @FXML private CategoryAxis gpaXAxis;
    @FXML private CategoryAxis cpaXAxis;
    @FXML private NumberAxis gpaYAxis;
    @FXML private NumberAxis cpaYAxis;
    // New charts for credits
    @FXML private StackedBarChart<String, Number> creditsBarChart;
    @FXML private CategoryAxis creditsBarXAxis;
    @FXML private NumberAxis creditsBarYAxis;
    @FXML private LineChart<String, Number> creditsCumChart;
    @FXML private CategoryAxis creditsCumXAxis;
    @FXML private NumberAxis creditsCumYAxis;
    private final Set<String> selectedSemesters = new LinkedHashSet<>();

    private void saveSelectedSemesters() {
        Preferences prefs = Preferences.userNodeForPackage(DashboardController.class);
        prefs.put("selectedSemesters", String.join(",", selectedSemesters));
    }

    private void loadSelectedSemesters() {
        Preferences prefs = Preferences.userNodeForPackage(DashboardController.class);
        String saved = prefs.get("selectedSemesters", "");
        if (!saved.isEmpty()) {
            selectedSemesters.clear();
            selectedSemesters.addAll(List.of(saved.split(",")));
        }
    }

    // Course đã được group theo kỳ
    private Map<String, List<Course>> groupedCourses;
    private void initData() {
        List<Course> courses = CourseService.getAllCourses();
        groupedCourses = GpaService.groupBySemester(courses);
    }

    private void initSemesterMenu() {
        semesterMenuButton.getItems().clear();
        for (String semester : groupedCourses.keySet()) {
            CheckBox checkBox = new CheckBox(semester);
            checkBox.setSelected(selectedSemesters.contains(semester));
            checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) selectedSemesters.add(semester);
                else selectedSemesters.remove(semester);
                saveSelectedSemesters();   // ⭐ dòng mấu chốt
                updateGpaChart();
                updateCpaChart();
                updateCreditsCharts();
            });
            CustomMenuItem menuItem = new CustomMenuItem(checkBox);
            menuItem.setHideOnClick(false); // ✅ HỢP LỆ 100%
            semesterMenuButton.getItems().add(menuItem);
        }
    }

    private void addTooltips(XYChart.Series<String, Number> series) {
        for (XYChart.Data<String, Number> data : series.getData()) {
            if (data.getNode() != null) {
                Tooltip tooltip = new Tooltip(
                        String.format("%.2f", data.getYValue().doubleValue())
                );
                Tooltip.install(data.getNode(), tooltip);
            }
        }
    }

    private void updateGpaChart() {
        List<String> semesters = new ArrayList<>(selectedSemesters);
        gpaXAxis.setCategories(FXCollections.observableArrayList(semesters));
        cpaXAxis.setCategories(FXCollections.observableArrayList(semesters));
        gpaChart.getData().clear();
        if (selectedSemesters.isEmpty()) return;
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("GPA");
        var gpaMap = GpaService.calcGpaBySemester(groupedCourses, selectedSemesters);
        gpaMap.forEach((k, v) -> series.getData().add(createPoint(k, v)));
        gpaChart.getData().add(series);
        Platform.runLater(() -> { gpaChart.applyCss(); gpaChart.layout(); addTooltips(series); });
    }

    private void updateCpaChart() {
        List<String> semesters = new ArrayList<>(selectedSemesters);
        gpaXAxis.setCategories(FXCollections.observableArrayList(semesters));
        cpaXAxis.setCategories(FXCollections.observableArrayList(semesters));
        cpaChart.getData().clear();
        if (selectedSemesters.isEmpty()) return;
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("CPA");
        var cpaMap = GpaService.calcCpaBySemester(groupedCourses, selectedSemesters);
        cpaMap.forEach((k, v) -> series.getData().add(createPoint(k, v)));
        cpaChart.getData().add(series);
        Platform.runLater(() -> { cpaChart.applyCss(); cpaChart.layout(); addTooltips(series); });
    }

    @FXML public void initialize() {
        setupYAxis(gpaYAxis);
        setupYAxis(cpaYAxis);
        gpaChart.setAnimated(false);
        cpaChart.setAnimated(false);
        reloadData();
    }

    /**
     * Được gọi khi app cần reload dữ liệu (từ nút Reload).
     * - Đọc lại data từ CourseService
     * - Rebuild groupedCourses, menu kỳ
     * - Vẽ lại chart GPA / CPA
     */
    public void reloadData() {
        initData();
        loadSelectedSemesters();
        initSemesterMenu();
        updateGpaChart();
        updateCpaChart();
        updateCreditsCharts();
    }

    private void updateCreditsCharts() {
        if (groupedCourses == null || groupedCourses.isEmpty()) {
            if (creditsBarChart != null) creditsBarChart.getData().clear();
            if (creditsCumChart != null) creditsCumChart.getData().clear();
            return;
        }

        // Ensure chronological order
        List<String> allSemesters = new ArrayList<>(groupedCourses.keySet());
        Collections.sort(allSemesters);

        XYChart.Series<String, Number> seriesNew = new XYChart.Series<>();
        seriesNew.setName("Học lần đầu");
        XYChart.Series<String, Number> seriesRepeat = new XYChart.Series<>();
        seriesRepeat.setName("Học lại");
        XYChart.Series<String, Number> seriesCum = new XYChart.Series<>();
        seriesCum.setName("Tích lũy");

        Set<String> seenCodes = new HashSet<>();
        int runningUniqueCredits = 0;

        for (String sem : allSemesters) {
            List<Course> list = groupedCourses.getOrDefault(sem, List.of());
            int newCredits = 0;
            int repeatCredits = 0;

            for (Course c : list) {
                String code = c.getCode();
                if (code == null) continue;
                if (seenCodes.contains(code)) {
                    repeatCredits += c.getCredits();
                } else {
                    newCredits += c.getCredits();
                    seenCodes.add(code);
                    runningUniqueCredits += c.getCredits();
                }
            }

            if (selectedSemesters.contains(sem)) {
                seriesNew.getData().add(new XYChart.Data<>(sem, newCredits));
                seriesRepeat.getData().add(new XYChart.Data<>(sem, repeatCredits));
                seriesCum.getData().add(new XYChart.Data<>(sem, runningUniqueCredits));
            }
        }

        if (creditsBarChart != null) {
            creditsBarChart.getData().clear();
            creditsBarChart.getData().addAll(seriesNew, seriesRepeat);
        }
        if (creditsCumChart != null) {
            creditsCumChart.getData().clear();
            creditsCumChart.getData().add(seriesCum);
        }

        Platform.runLater(() -> {
            if (creditsBarChart != null) { creditsBarChart.applyCss(); creditsBarChart.layout(); }
            if (creditsCumChart != null) { creditsCumChart.applyCss(); creditsCumChart.layout(); }
            addTooltips(seriesNew);
            addTooltips(seriesRepeat);
            addTooltips(seriesCum);
        });
    }

    private void setupYAxis(NumberAxis axis) {
        axis.setAutoRanging(false);
        axis.setLowerBound(2.8);
        axis.setUpperBound(4.4);
        axis.setTickUnit(0.4);
        axis.setMinorTickCount(0);
    }

    private XYChart.Data<String, Number> createPoint(String x, double y) {
        // Tạo hình tròn nhỏ cho điểm
        Circle circle = new Circle(5);
        circle.setStyle("""
            -fx-fill: white;
            -fx-stroke: #ff6f00;
            -fx-stroke-width: 2;
        """);
        // Tạo label hiển thị giá trị (không có background)
        Label label = new Label(String.format("%.2f", y));
        label.setStyle("""
            -fx-font-size: 10px;
            -fx-font-weight: bold;
            -fx-text-fill: #ff6f00;
        """);
        // Đặt label phía trên điểm
        label.setTranslateY(-15);
        // Stack chứa cả circle và label
        StackPane stack = new StackPane(circle, label);
        // ẨN HÌNH CHỮ NHẬT MẶC ĐỊNH
        stack.setStyle("""
            -fx-background-color: transparent;
            -fx-padding: 0;
        """);
        XYChart.Data<String, Number> data = new XYChart.Data<>(x, y);
        data.setNode(stack);
        return data;
    }
}
