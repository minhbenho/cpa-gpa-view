package calcpa.controller;

import calcpa.model.Course;
import calcpa.service.CourseService;
import calcpa.service.GpaService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

import java.util.*;
import java.util.prefs.Preferences;

public class DashboardCreditController {

    @FXML private MenuButton semesterMenuButton;
    @FXML private LineChart<String, Number> creditsChart;
    @FXML private CategoryAxis creditsXAxis;
    @FXML private NumberAxis creditsYAxis;
    @FXML private LineChart<String, Number> creditsCumChart;
    @FXML private CategoryAxis creditsCumXAxis;
    @FXML private NumberAxis creditsCumYAxis;
    
    private final Set<String> selectedSemesters = new LinkedHashSet<>();
    private Map<String, List<Course>> groupedCourses;

    private void saveSelectedSemesters() {
        Preferences prefs = Preferences.userNodeForPackage(DashboardCreditController.class);
        prefs.put("selectedSemesters", String.join(",", selectedSemesters));
    }

    private void loadSelectedSemesters() {
        Preferences prefs = Preferences.userNodeForPackage(DashboardCreditController.class);
        String saved = prefs.get("selectedSemesters", "");
        if (!saved.isEmpty()) {
            selectedSemesters.clear();
            selectedSemesters.addAll(List.of(saved.split(",")));
        }
    }

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
                saveSelectedSemesters();
                updateCreditsCharts();
                updateCumulativeCreditsChart();
            });
            CustomMenuItem menuItem = new CustomMenuItem(checkBox);
            menuItem.setHideOnClick(false);
            semesterMenuButton.getItems().add(menuItem);
        }
    }

    @FXML
    public void initialize() {
        setupCreditsYAxis();
        setupCumulativeCreditsYAxis();
        creditsChart.setAnimated(false);
        creditsCumChart.setAnimated(false);
        reloadData();
    }

    /**
     * Được gọi khi app cần reload dữ liệu (từ nút Reload).
     */
    public void reloadData() {
        initData();
        loadSelectedSemesters();
        initSemesterMenu();
        updateCreditsCharts();
        updateCumulativeCreditsChart();
    }

    /**
     * Cập nhật biểu đồ số tín chỉ học trong kỳ
     */
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

            setSeriesColor(seriesTotal, "#1e88e5");   // xanh dương
            setSeriesColor(seriesNew, "#43a047");     // xanh lá
            setSeriesColor(seriesRepeat, "#fbc02d");  // vàng

            addTooltips(seriesTotal);
            addTooltips(seriesNew);
            addTooltips(seriesRepeat);
        });
    }

    /**
     * Cập nhật biểu đồ tín chỉ tích lũy đến kỳ
     */
    private void updateCumulativeCreditsChart() {
        creditsCumChart.getData().clear();
        if (groupedCourses == null || selectedSemesters.isEmpty()) return;

        List<String> semesters = new ArrayList<>(selectedSemesters);
        Collections.sort(semesters);
        creditsCumXAxis.setCategories(FXCollections.observableArrayList(semesters));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Tín chỉ tích lũy");

        Set<String> seenCodes = new HashSet<>();
        int cumulativeCredits = 0;

        for (String sem : semesters) {
            List<Course> list = groupedCourses.getOrDefault(sem, List.of());

            for (Course c : list) {
                if (c.getCode() == null) continue;
                if (!seenCodes.contains(c.getCode())) {
                    cumulativeCredits += c.getCredits();
                    seenCodes.add(c.getCode());
                }
            }

            series.getData().add(createPoint(sem, cumulativeCredits));
        }

        creditsCumChart.getData().add(series);

        Platform.runLater(() -> {
            creditsCumChart.applyCss();
            creditsCumChart.layout();
            addTooltips(series);
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

    private void addTooltips(XYChart.Series<String, Number> series) {
        for (XYChart.Data<String, Number> data : series.getData()) {
            if (data.getNode() != null) {
                Tooltip tooltip = new Tooltip(
                        String.format("%.0f", data.getYValue().doubleValue())
                );
                Tooltip.install(data.getNode(), tooltip);
            }
        }
    }

    private XYChart.Data<String, Number> createPoint(String x, double y) {
        // Tạo hình tròn nhỏ cho điểm
        Circle circle = new Circle(5);
        circle.setStyle("""
            -fx-fill: white;
            -fx-stroke: #ff6f00;
            -fx-stroke-width: 2;
        """);
        // Tạo label hiển thị giá trị
        Label label = new Label(String.format("%.0f", y));
        label.setStyle("""
            -fx-font-size: 10px;
            -fx-font-weight: bold;
            -fx-text-fill: #ff6f00;
        """);
        // Đặt label phía trên điểm
        label.setTranslateY(-15);
        // Stack chứa cả circle và label
        StackPane stack = new StackPane(circle, label);
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
        creditsYAxis.setLabel("Số tín chỉ");
    }

    private void setupCumulativeCreditsYAxis() {
        creditsCumYAxis.setAutoRanging(true);
        creditsCumYAxis.setLabel("Tín chỉ tích lũy");
    }
}
