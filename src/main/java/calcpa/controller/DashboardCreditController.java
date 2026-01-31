package calcpa.controller;

import calcpa.model.Course;
import calcpa.service.CourseService;
import calcpa.service.GpaService;
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
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

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
            
            // Thêm labels trực tiếp vào bar nodes
            addLabelsToBarNodes(creditsData);
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

    private List<Label> currentLabels = new ArrayList<>();
    private Map<String, int[]> lastCreditsData = null;
    private boolean listenersAdded = false;
    
    private void addLabelsToBarNodes(Map<String, int[]> creditsData) {
        lastCreditsData = creditsData;
        
        Platform.runLater(() -> {
            Platform.runLater(() -> {
                // Try different selectors
                javafx.scene.Node plotContent = creditsChart.lookup(".chart-plot-background");
                if (plotContent == null) {
                    plotContent = creditsChart.lookup(".chart-content");
                }
                if (plotContent == null) {
                    plotContent = creditsChart.lookup(".plot-content");
                }
                
                if (plotContent == null) {
                    System.out.println("ERROR: Could not find plot area. Available nodes:");
                    printNodeTree(creditsChart, 0);
                    return;
                }
                
                System.out.println("Found plot area: " + plotContent.getClass().getName());
                
                // Get the parent pane of the plot area
                javafx.scene.Parent parent = plotContent.getParent();
                if (!(parent instanceof javafx.scene.layout.Pane)) {
                    System.out.println("ERROR: Parent is not a Pane: " + parent.getClass().getName());
                    return;
                }
                
                javafx.scene.layout.Pane plotPane = (javafx.scene.layout.Pane) parent;
                
                // Clear old labels
                plotPane.getChildren().removeAll(currentLabels);
                currentLabels.clear();
                
                System.out.println("Adding labels for " + creditsData.size() + " semesters");
                
                List<String> semesters = new ArrayList<>(creditsData.keySet());
                
                for (int i = 0; i < semesters.size(); i++) {
                    String semester = semesters.get(i);
                    int[] credits = creditsData.get(semester);
                    int newCredits = credits[0];
                    int repeatCredits = credits[1];
                    int total = newCredits + repeatCredits;
                    
                    System.out.println("Semester " + semester + ": new=" + newCredits + ", repeat=" + repeatCredits);
                    
                    // Lấy bar nodes
                    XYChart.Data<String, Number> newData = creditsChart.getData().get(0).getData().get(i);
                    XYChart.Data<String, Number> repeatData = creditsChart.getData().get(1).getData().get(i);
                    
                    javafx.scene.Node newBarNode = newData.getNode();
                    javafx.scene.Node repeatBarNode = repeatData.getNode();
                    
                    // Add label cho new credits (xanh lá - phần dưới)
                    if (newBarNode != null && newCredits > 0) {
                        javafx.geometry.Bounds bounds = newBarNode.getBoundsInParent();
                        Label label = createValueLabel(String.valueOf(newCredits));
                        positionLabelAtCenter(label, bounds, plotPane);
                        currentLabels.add(label);
                    }
                    
                    // Add label cho repeat credits (vàng - phần trên)
                    if (repeatBarNode != null && repeatCredits > 0) {
                        javafx.geometry.Bounds bounds = repeatBarNode.getBoundsInParent();
                        Label label = createValueLabel(String.valueOf(repeatCredits));
                        positionLabelAtCenter(label, bounds, plotPane);
                        currentLabels.add(label);
                    }
                    
                    // Add total label (hộp xanh dương trên cùng)
                    if (repeatBarNode != null && total > 0) {
                        javafx.geometry.Bounds bounds = repeatBarNode.getBoundsInParent();
                        Label label = createTotalLabel(String.valueOf(total));
                        positionLabelAboveBar(label, bounds, plotPane);
                        currentLabels.add(label);
                    }
                }
                
                System.out.println("Total labels added: " + currentLabels.size());
                
                // Add resize listener only once
                if (!listenersAdded) {
                    listenersAdded = true;
                    creditsChart.widthProperty().addListener((obs, old, newVal) -> {
                        if (lastCreditsData != null && Math.abs(newVal.doubleValue() - old.doubleValue()) > 5) {
                            addLabelsToBarNodes(lastCreditsData);
                        }
                    });
                    creditsChart.heightProperty().addListener((obs, old, newVal) -> {
                        if (lastCreditsData != null && Math.abs(newVal.doubleValue() - old.doubleValue()) > 5) {
                            addLabelsToBarNodes(lastCreditsData);
                        }
                    });
                }
            });
        });
    }
    
    private void printNodeTree(javafx.scene.Node node, int depth) {
        String indent = "  ".repeat(depth);
        System.out.println(indent + node.getClass().getSimpleName() + " - " + node.getId() + " - " + node.getStyleClass());
        if (node instanceof javafx.scene.Parent) {
            for (javafx.scene.Node child : ((javafx.scene.Parent) node).getChildrenUnmodifiable()) {
                printNodeTree(child, depth + 1);
            }
        }
    }
    
    private Label createValueLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
        label.setMouseTransparent(true);
        return label;
    }
    
    private Label createTotalLabel(String text) {
        Label label = new Label(text);
        label.setStyle("""
                -fx-text-fill: white;
                -fx-font-weight: bold;
                -fx-font-size: 11px;
                -fx-background-color: #1e88e5;
                -fx-padding: 3 7;
                -fx-background-radius: 3;
            """);
        label.setMouseTransparent(true);
        return label;
    }
    
    private void positionLabelAtCenter(Label label, javafx.geometry.Bounds barBounds, javafx.scene.layout.Pane plotPane) {
        // Force layout to get label size
        plotPane.getChildren().add(label);
        label.applyCss();
        label.layout();
        
        // Calculate center position
        double centerX = barBounds.getMinX() + barBounds.getWidth() / 2;
        double centerY = barBounds.getMinY() + barBounds.getHeight() / 2;
        
        // Position label (accounting for label's own size)
        label.setLayoutX(centerX - label.getWidth() / 2);
        label.setLayoutY(centerY - label.getHeight() / 2);
        
        // Ensure label is on top
        label.toFront();
        
        System.out.println("Label '" + label.getText() + "' positioned at: " + label.getLayoutX() + ", " + label.getLayoutY() + 
                           " (bar bounds: " + barBounds.getMinX() + ", " + barBounds.getMinY() + ", " + barBounds.getWidth() + ", " + barBounds.getHeight() + ")");
    }
    
    private void positionLabelAboveBar(Label label, javafx.geometry.Bounds barBounds, javafx.scene.layout.Pane plotPane) {
        // Force layout to get label size
        plotPane.getChildren().add(label);
        label.applyCss();
        label.layout();
        
        // Calculate position above bar
        double centerX = barBounds.getMinX() + barBounds.getWidth() / 2;
        double topY = barBounds.getMinY() - label.getHeight() - 5;
        
        // Position label
        label.setLayoutX(centerX - label.getWidth() / 2);
        label.setLayoutY(topY);
        
        // Ensure label is on top
        label.toFront();
        
        System.out.println("Total label '" + label.getText() + "' positioned at: " + label.getLayoutX() + ", " + label.getLayoutY());
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
