package calcpa.controller;

import calcpa.service.CourseService;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;

public class MainController {

    // Singleton đơn giản để các controller con có thể gọi reloadApp()
    private static MainController instance;

    public MainController() {
        instance = this;
    }

    public static MainController getInstance() {
        return instance;
    }

    // Root nodes của từng trang
    @FXML private Node pageDashboard;
    @FXML private Node pageDashboardCredit;
    @FXML private Node pageSemester;
    @FXML private Node pageSubject;
    @FXML private Node pageSetting;

    // Controller của các trang (theo pattern fx:include + fx:id)
    @FXML private DashboardController pageDashboardController;
    @FXML private DashboardCreditController pageDashboardCreditController;
    @FXML private SemesterController pageSemesterController;
    @FXML private SubjectController pageSubjectController;
    @FXML private SettingController pageSettingController;

    @FXML
    public void initialize() {
        hideAllPages();
        showPage(pageDashboard);
    }

    private void hideAllPages() {
        hide(pageDashboard);
        hide(pageDashboardCredit);
        hide(pageSemester);
        hide(pageSubject);
        hide(pageSetting);
    }

    private void hide(Node page) {
        if (page == null) return;
        page.setVisible(false);
        page.setManaged(false);
    }

    private void showPage(Node page) {
        if (page == null) return;
        page.setVisible(true);
        page.setManaged(true);
    }

    @FXML
    private void showDashboard() {
        hideAllPages();
        showPage(pageDashboard);
    }

    @FXML
    private void showDashboardCredit() {
        hideAllPages();
        showPage(pageDashboardCredit);
    }

    @FXML
    private void showSemester() {
        hideAllPages();
        showPage(pageSemester);
    }

    @FXML
    private void showSubject() {
        hideAllPages();
        showPage(pageSubject);
    }

    @FXML
    private void showSetting() {
        hideAllPages();
        showPage(pageSetting);
    }

    /**
     * Nút Reload dùng để:
     * - Đọc lại data từ file CSV
     * - Báo cho các trang con cập nhật lại UI
     */
    @FXML
    public void reloadApp() {
        // Load lại data gốc
        CourseService.loadData();

        // Báo từng trang tự reload UI nếu có controller
        if (pageDashboardController != null) {
            pageDashboardController.reloadData();
        }
        if (pageDashboardCreditController != null) {
            pageDashboardCreditController.reloadData();
        }
        if (pageSemesterController != null) {
            pageSemesterController.reloadData();
        }
        // Subject / Setting hiện chưa dùng data nên tạm thời không cần làm gì
//        Alert alert = new Alert(Alert.AlertType.INFORMATION);
//        alert.setTitle("Thông báo");
//        alert.setHeaderText(null);
//        alert.setContentText("Đã reload");
//        alert.showAndWait();
    }
}
