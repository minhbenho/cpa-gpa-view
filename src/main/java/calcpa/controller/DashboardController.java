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
    @FXML private LineChart<String, Number> creditsChart;
    @FXML private CategoryAxis creditsXAxis;
    @FXML private NumberAxis creditsYAxis;
    @FXML private CategoryAxis creditsBarXAxis;
    @FXML private NumberAxis creditsBarYAxis;
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
        setupCreditsYAxis();
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
        creditsChart.getData().clear();
        if (groupedCourses == null || selectedSemesters.isEmpty()) return;

        List<String> semesters = new ArrayList<>(selectedSemesters);
        Collections.sort(semesters);
        creditsXAxis.setCategories(FXCollections.observableArrayList(semesters));

        XYChart.Series<String, Number> seriesTotal = new XYChart.Series<>();
        seriesTotal.setName("Tổng số tín chỉ");

        XYChart.Series<String, Number> seriesNew = new XYChart.Series<>();
        seriesNew.setName("Số tín chỉ học trong kỳ");

        XYChart.Series<String, Number> seriesRepeat = new XYChart.Series<>();
        seriesRepeat.setName("Số tín chỉ học cải thiện");

        Set<String> seenCodes = new HashSet<>();

        for (String sem : semesters) {
            List<Course> list = groupedCourses.getOrDefault(sem, List.of());

            int newCredits = 0;
            int repeatCredits = 0;

            for (Course c : list) {
                if (c.getCode() == null) continue;
                if (seenCodes.contains(c.getCode())) {
                    repeatCredits += c.getCredits();
                } else {
                    newCredits += c.getCredits();
                    seenCodes.add(c.getCode());
                }
            }

            int totalCredits = newCredits + repeatCredits;

            seriesNew.getData().add(createPoint(sem, newCredits));
            seriesRepeat.getData().add(createPoint(sem, repeatCredits));
            seriesTotal.getData().add(createPoint(sem, totalCredits));
        }

        // add theo thứ tự: tổng -> mới -> cải thiện
        creditsChart.getData().addAll(seriesTotal, seriesNew, seriesRepeat);

        Platform.runLater(() -> {
            creditsChart.applyCss();
            creditsChart.layout();

            setSeriesColor(seriesTotal, "#1e88e5");   // xanh dương 1e88e5
            setSeriesColor(seriesNew, "#43a047");     // xanh lá 43a047
            setSeriesColor(seriesRepeat, "#fbc02d");  // vàng fbc02d

            addTooltips(seriesTotal);
            addTooltips(seriesNew);
            addTooltips(seriesRepeat);
        });
    }

    private void setSeriesColor(XYChart.Series<String, Number> series, String color) {
        if (series.getNode() != null) {
            series.getNode().lookup(".chart-series-line")
                    .setStyle("-fx-stroke: " + color + ";");
        }
        for (XYChart.Data<String, Number> d : series.getData()) {
            if (d.getNode() != null) {
                d.getNode().lookup("Circle")
                        .setStyle("-fx-stroke: " + color + "; -fx-fill: white;");
                d.getNode().lookup("Label")
                        .setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
            }
        }
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

    private void setupCreditsYAxis() {
        creditsYAxis.setAutoRanging(false);
        creditsYAxis.setLowerBound(0);
        creditsYAxis.setUpperBound(30);
        creditsYAxis.setTickUnit(5);
        creditsYAxis.setMinorTickCount(0);
    }

}
