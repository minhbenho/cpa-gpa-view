package calcpa.controller;

import calcpa.model.Course;
import calcpa.service.CourseService;
import calcpa.service.GpaService;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.*;
import java.util.prefs.Preferences;

public class DashboardCreditController {

    @FXML private MenuButton semesterMenuButton;
    @FXML private StackedBarChart<String, Number> creditsChart;
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

        XYChart.Series<String, Number> seriesNew = new XYChart.Series<>();
        seriesNew.setName("Tín chỉ tích lũy thêm");

        XYChart.Series<String, Number> seriesRepeat = new XYChart.Series<>();
        seriesRepeat.setName("Tín chỉ học cải thiện");

        Set<String> seenCodes = new HashSet<>();
        Map<String, int[]> creditsData = new LinkedHashMap<>(); // sem -> [newCredits, repeatCredits]

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

            creditsData.put(sem, new int[]{newCredits, repeatCredits});
            seriesNew.getData().add(new XYChart.Data<>(sem, newCredits));
            seriesRepeat.getData().add(new XYChart.Data<>(sem, repeatCredits));
        }

        // add theo thứ tự: tích lũy thêm (dưới) -> cải thiện (trên)
        creditsChart.getData().addAll(seriesNew, seriesRepeat);

        Platform.runLater(() -> {
            creditsChart.applyCss();
            creditsChart.layout();

            setBarSeriesColor(seriesNew, "#43a047");     // xanh lá
            setBarSeriesColor(seriesRepeat, "#fbc02d");  // vàng

            addBarTooltips(seriesNew);
            addBarTooltips(seriesRepeat);
            
            // Delay thêm labels để bar rendering xong
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.millis(100));
            pause.setOnFinished(event -> addLabelsToChart(creditsData));
            pause.play();
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

        Map<String, Integer> cumulativeBySemester =
                GpaService.calcCumulativeCreditsBySemester(groupedCourses, selectedSemesters);
        for (String sem : semesters) {
            int credits = cumulativeBySemester.getOrDefault(sem, 0);
            series.getData().add(createPoint(sem, credits));
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

    private void setBarSeriesColor(XYChart.Series<String, Number> series, String color) {
        for (XYChart.Data<String, Number> d : series.getData()) {
            if (d.getNode() != null) {
                d.getNode().setStyle("-fx-bar-fill: " + color + ";");
            }
        }
    }

    private void addBarTooltips(XYChart.Series<String, Number> series) {
        for (XYChart.Data<String, Number> data : series.getData()) {
            if (data.getNode() != null) {
                Tooltip tooltip = new Tooltip(
                        String.format("%.0f", data.getYValue().doubleValue())
                );
                Tooltip.install(data.getNode(), tooltip);
            }
        }
    }

    private void addLabelsToChart(Map<String, int[]> creditsData) {
        // Clear old labels
        creditsChart.getChildren().removeIf(node -> node instanceof Label);
        
        // Tìm plot area
        javafx.scene.Node plotArea = creditsChart.lookup(".chart-plot-background");
        if (plotArea == null) return;

        double plotX = plotArea.getBoundsInLocal().getMinX();
        double plotY = plotArea.getBoundsInLocal().getMinY();
        double plotWidth = plotArea.getBoundsInLocal().getWidth();
        double plotHeight = plotArea.getBoundsInLocal().getHeight();

        NumberAxis yAxis = creditsYAxis;
        double yMax = yAxis.getUpperBound();
        double yMin = yAxis.getLowerBound();
        double yRange = yMax - yMin;
        double pixelsPerUnit = plotHeight / yRange;
        double yBottom = plotY + plotHeight;

        double barWidth = plotWidth / creditsData.size();
        int barIndex = 0;

        for (String sem : creditsData.keySet()) {
            int[] credits = creditsData.get(sem);
            int newCredits = credits[0];
            int repeatCredits = credits[1];
            int total = newCredits + repeatCredits;

            double barCenterX = plotX + barIndex * barWidth + barWidth / 2;

            // Label tín chỉ tích lũy thêm (xanh lá)
            if (newCredits > 0) {
                Label newLabel = new Label(String.valueOf(newCredits));
                newLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11;");
                newLabel.setAlignment(Pos.CENTER);
                double yNewLabel = yBottom - (newCredits * 0.5 * pixelsPerUnit);
                creditsChart.getChildren().add(newLabel);
                // Dùng TranslateX/Y để đặt vị trí
                newLabel.setTranslateX(barCenterX - 20);
                newLabel.setTranslateY(yNewLabel - 10);
            }

            // Label tín chỉ học cải thiện (vàng)
            if (repeatCredits > 0) {
                Label repeatLabel = new Label(String.valueOf(repeatCredits));
                repeatLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11;");
                repeatLabel.setAlignment(Pos.CENTER);
                double yRepeatLabel = yBottom - ((newCredits + repeatCredits * 0.5) * pixelsPerUnit);
                creditsChart.getChildren().add(repeatLabel);
                repeatLabel.setTranslateX(barCenterX - 20);
                repeatLabel.setTranslateY(yRepeatLabel - 10);
            }

            // Total label (xanh dương box)
            if (total > 0) {
                Label totalLabel = new Label(String.valueOf(total));
                totalLabel.setStyle("""
                        -fx-text-fill: white;
                        -fx-font-weight: bold;
                        -fx-font-size: 10;
                        -fx-background-color: #1e88e5;
                        -fx-padding: 2 5;
                        -fx-border-radius: 2;
                        -fx-background-radius: 2;
                    """);
                totalLabel.setAlignment(Pos.CENTER);
                double yTotalLabel = yBottom - (total * pixelsPerUnit) - 20;
                creditsChart.getChildren().add(totalLabel);
                totalLabel.setTranslateX(barCenterX - 20);
                totalLabel.setTranslateY(yTotalLabel);
            }

            barIndex++;
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
        creditsCumYAxis.setAutoRanging(false);
        creditsCumYAxis.setUpperBound(160);
        creditsCumYAxis.setLowerBound(0);
        creditsCumYAxis.setTickUnit(20);    
        creditsCumYAxis.setLabel("Tín chỉ tích lũy");
    }
}
