package calcpa.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.scene.Node;

public class MainController {

    @FXML private Node pageDashboard;
    @FXML private Node pageSemester;
    @FXML private Node pageSubject;
    @FXML private Node pageSetting;

    @FXML
    public void initialize() {
        hideAllPages();
        showPage(pageDashboard);
    }

    private void hideAllPages() {
        hide(pageDashboard);
        hide(pageSemester);
        hide(pageSubject);
        hide(pageSetting);
    }

    private void hide(Node page) {
        page.setVisible(false);
        page.setManaged(false);
    }

    private void showPage(Node page) {
        page.setVisible(true);
        page.setManaged(true);
    }

    @FXML
    private void showDashboard() {
        hideAllPages();
        showPage(pageDashboard);
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
}
