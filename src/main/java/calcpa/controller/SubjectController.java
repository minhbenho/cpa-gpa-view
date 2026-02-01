package calcpa.controller;

import calcpa.model.Course;
import calcpa.service.CourseService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.prefs.Preferences;

public class SubjectController {

	@FXML private FlowPane creditsPane;
	@FXML private FlowPane gradesPane;
	@FXML private FlowPane semestersPane;
	@FXML private Button btnSearch;
	@FXML private TableView<Course> courseTable;
	@FXML private TableColumn<Course, String> colCode;
	@FXML private TableColumn<Course, String> colName;
	@FXML private TableColumn<Course, Integer> colCredits;
	@FXML private TableColumn<Course, String> colGrade;
	@FXML private TableColumn<Course, String> colSemester;

	private final ObservableList<Course> courseList = FXCollections.observableArrayList();

	private final Map<Integer, CheckBox> creditChecks = new LinkedHashMap<>();
	private final Map<String, CheckBox> gradeChecks = new LinkedHashMap<>();
	private final Map<String, CheckBox> semesterChecks = new LinkedHashMap<>();

	private final Set<Integer> selectedCredits = new LinkedHashSet<>();
	private final Set<String> selectedGrades = new LinkedHashSet<>();
	private final Set<String> selectedSemesters = new LinkedHashSet<>();

	private static final Preferences preferences = Preferences.userNodeForPackage(SubjectController.class);
	private static final String PREF_CREDITS = "subject.selectedCredits";
	private static final String PREF_GRADES = "subject.selectedGrades";
	private static final String PREF_SEMESTERS = "subject.selectedSemesters";

	private static final String GRADE_OTHER = "OTHER";

	private static final List<Integer> CREDIT_OPTIONS = List.of(0, 1, 2, 3, 6);
	private static final List<String> GRADE_OPTIONS = List.of(
			"A+", "A", "B+", "B", "C+", "C", "D+", "D", "F", GRADE_OTHER
	);
	private static final Set<String> STANDARD_GRADES = new LinkedHashSet<>(List.of(
			"A+", "A", "B+", "B", "C+", "C", "D+", "D", "F"
	));

	@FXML
	public void initialize() {
		setupTable();
		buildCreditOptions();
		buildGradeOptions();
		reloadData();
	}

	public void reloadData() {
		buildSemesterOptions();
		loadSelections();
		applySelectionsToUI();
		applyFilters();
	}

	private void setupTable() {
		colCode.setCellValueFactory(new PropertyValueFactory<>("code"));
		colName.setCellValueFactory(new PropertyValueFactory<>("name"));
		colCredits.setCellValueFactory(new PropertyValueFactory<>("credits"));
		colGrade.setCellValueFactory(new PropertyValueFactory<>("grade"));
		colSemester.setCellValueFactory(new PropertyValueFactory<>("semester"));
		courseTable.setItems(courseList);
	}

	private void buildCreditOptions() {
		creditsPane.getChildren().clear();
		creditChecks.clear();
		for (Integer credit : CREDIT_OPTIONS) {
			CheckBox cb = new CheckBox(String.valueOf(credit));
			creditChecks.put(credit, cb);
			creditsPane.getChildren().add(cb);
		}
	}

	private void buildGradeOptions() {
		gradesPane.getChildren().clear();
		gradeChecks.clear();
		for (String grade : GRADE_OPTIONS) {
			String label = grade.equals(GRADE_OTHER) ? "Khác" : grade;
			CheckBox cb = new CheckBox(label);
			gradeChecks.put(grade, cb);
			gradesPane.getChildren().add(cb);
		}
	}

	private void buildSemesterOptions() {
		semestersPane.getChildren().clear();
		semesterChecks.clear();
		Set<String> semesters = new TreeSet<>();
		for (Course c : CourseService.getAllCourses()) {
			String semester = c.getSemester();
			if (semester != null && !semester.isBlank()) {
				semesters.add(semester.trim());
			}
		}
		for (String semester : semesters) {
			CheckBox cb = new CheckBox(semester);
			semesterChecks.put(semester, cb);
			semestersPane.getChildren().add(cb);
		}
	}

	private void loadSelections() {
		selectedCredits.clear();
		selectedGrades.clear();
		selectedSemesters.clear();

		String savedCredits = preferences.get(PREF_CREDITS, "");
		if (!savedCredits.isBlank()) {
			for (String token : savedCredits.split(",")) {
				try {
					selectedCredits.add(Integer.parseInt(token.trim()));
				} catch (NumberFormatException ignored) {
				}
			}
		}

		String savedGrades = preferences.get(PREF_GRADES, "");
		if (!savedGrades.isBlank()) {
			for (String token : savedGrades.split(",")) {
				String value = token.trim().toUpperCase();
				if (!value.isEmpty()) {
					selectedGrades.add(value);
				}
			}
		}

		String savedSemesters = preferences.get(PREF_SEMESTERS, "");
		if (!savedSemesters.isBlank()) {
			for (String token : savedSemesters.split(",")) {
				String value = token.trim();
				if (!value.isEmpty()) {
					selectedSemesters.add(value);
				}
			}
		}
	}

	private void applySelectionsToUI() {
		for (Map.Entry<Integer, CheckBox> entry : creditChecks.entrySet()) {
			entry.getValue().setSelected(selectedCredits.contains(entry.getKey()));
		}
		for (Map.Entry<String, CheckBox> entry : gradeChecks.entrySet()) {
			entry.getValue().setSelected(selectedGrades.contains(entry.getKey()));
		}
		for (Map.Entry<String, CheckBox> entry : semesterChecks.entrySet()) {
			entry.getValue().setSelected(selectedSemesters.contains(entry.getKey()));
		}
	}

	private void updateSelectionsFromUI() {
		selectedCredits.clear();
		selectedGrades.clear();
		selectedSemesters.clear();

		for (Map.Entry<Integer, CheckBox> entry : creditChecks.entrySet()) {
			if (entry.getValue().isSelected()) {
				selectedCredits.add(entry.getKey());
			}
		}
		for (Map.Entry<String, CheckBox> entry : gradeChecks.entrySet()) {
			if (entry.getValue().isSelected()) {
				selectedGrades.add(entry.getKey());
			}
		}
		for (Map.Entry<String, CheckBox> entry : semesterChecks.entrySet()) {
			if (entry.getValue().isSelected()) {
				selectedSemesters.add(entry.getKey());
			}
		}
	}

	private void saveSelections() {
		preferences.put(PREF_CREDITS, joinIntegers(selectedCredits));
		preferences.put(PREF_GRADES, String.join(",", selectedGrades));
		preferences.put(PREF_SEMESTERS, String.join(",", selectedSemesters));
	}

	private String joinIntegers(Set<Integer> values) {
		List<String> tokens = new ArrayList<>();
		for (Integer v : values) {
			tokens.add(String.valueOf(v));
		}
		return String.join(",", tokens);
	}

	@FXML
	private void onSearch() {
		updateSelectionsFromUI();
		saveSelections();
		applyFilters();
	}

	private void applyFilters() {
		Map<String, List<Course>> byCode = new LinkedHashMap<>();
		for (Course c : CourseService.getAllCourses()) {
			String code = c.getCode();
			if (code == null || code.isBlank()) continue;
			byCode.computeIfAbsent(code.trim(), k -> new ArrayList<>()).add(c);
		}

		List<Course> filtered = new ArrayList<>();
		for (List<Course> attempts : byCode.values()) {
			List<Course> scopedAttempts = filterBySelectedSemesters(attempts);
			if (scopedAttempts.isEmpty()) continue;

			Course best = pickBestGrade(scopedAttempts);
			if (best == null) continue;

			if (matchesCredits(best) && matchesGrades(best)) {
				filtered.add(best);
			}
		}
		courseList.setAll(filtered);
	}

	private Course pickBestGrade(List<Course> attempts) {
		Course best = null;
		int bestRank = Integer.MIN_VALUE;
		for (Course c : attempts) {
			int rank = gradeRank(c.getGrade());
			if (best == null || rank > bestRank) {
				best = c;
				bestRank = rank;
			}
		}
		return best;
	}

	private int gradeRank(String grade) {
		if (grade == null) return 0;
		String value = grade.trim().toUpperCase();
		return switch (value) {
			case "A+" -> 9;
			case "A" -> 8;
			case "B+" -> 7;
			case "B" -> 6;
			case "C+" -> 5;
			case "C" -> 4;
			case "D+" -> 3;
			case "D" -> 2;
			case "F" -> 1;
			default -> 0;
		};
	}

	private boolean matchesCredits(Course c) {
		if (selectedCredits.isEmpty()) return true;
		return selectedCredits.contains(c.getCredits());
	}

	private List<Course> filterBySelectedSemesters(List<Course> attempts) {
		if (selectedSemesters.isEmpty()) return attempts;
		List<Course> filtered = new ArrayList<>();
		for (Course c : attempts) {
			String semester = c.getSemester();
			if (semester != null && selectedSemesters.contains(semester.trim())) {
				filtered.add(c);
			}
		}
		return filtered;
	}

	private boolean matchesGrades(Course c) {
		if (selectedGrades.isEmpty()) return true;
		String grade = c.getGrade();
		String value = grade == null ? "" : grade.trim().toUpperCase();
		boolean isStandard = STANDARD_GRADES.contains(value);
		if (isStandard) {
			return selectedGrades.contains(value);
		}
		return selectedGrades.contains(GRADE_OTHER);
	}
}
