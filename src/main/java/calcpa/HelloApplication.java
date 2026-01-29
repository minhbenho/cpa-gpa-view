package calcpa;

import calcpa.model.Course;
import calcpa.service.CourseService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        CourseService.loadData();

        FXMLLoader loader = new FXMLLoader(
                HelloApplication.class.getResource("main.fxml")
        );

        Scene scene = new Scene(loader.load(), 800, 600);
        stage.setScene(scene);
        stage.setTitle("CPA App");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
