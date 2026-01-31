package calcpa.service;

import calcpa.model.Course;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Service quản lý thứ tự hiển thị tùy chỉnh của các môn học theo từng kỳ.
 * Dữ liệu được lưu vào file order.txt để duy trì qua các lần khởi động.
 */
public class CourseOrderService {
    private static final String ORDER_FILE = "src/main/resources/course_order.txt";
    
    // Map lưu trữ thứ tự: semester -> (courseCode -> displayOrder)
    private static final Map<String, Map<String, Integer>> orderMap = new HashMap<>();
    
    /**
     * Tải thứ tự tùy chỉnh từ file
     */
    public static void loadOrder() {
        orderMap.clear();
        File file = new File(ORDER_FILE);
        if (!file.exists()) {
            return;
        }
        
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // Format: semester|courseCode|displayOrder
                String[] parts = line.split("\\|");
                if (parts.length == 3) {
                    String semester = parts[0];
                    String courseCode = parts[1];
                    int displayOrder = Integer.parseInt(parts[2]);
                    
                    orderMap.putIfAbsent(semester, new HashMap<>());
                    orderMap.get(semester).put(courseCode, displayOrder);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Lưu thứ tự tùy chỉnh ra file
     */
    public static void saveOrder() {
        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(ORDER_FILE), StandardCharsets.UTF_8))) {
            pw.println("# Thứ tự hiển thị môn học: semester|courseCode|displayOrder");
            
            // Sắp xếp theo semester để dễ đọc
            List<String> semesters = new ArrayList<>(orderMap.keySet());
            Collections.sort(semesters);
            
            for (String semester : semesters) {
                Map<String, Integer> courseOrders = orderMap.get(semester);
                for (Map.Entry<String, Integer> entry : courseOrders.entrySet()) {
                    pw.println(semester + "|" + entry.getKey() + "|" + entry.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Áp dụng thứ tự tùy chỉnh cho danh sách môn học
     */
    public static void applyOrder(List<Course> courses, String semester) {
        if (!orderMap.containsKey(semester)) {
            // Nếu chưa có thứ tự tùy chỉnh, khởi tạo theo thứ tự hiện tại
            initializeOrder(courses, semester);
            return;
        }
        
        Map<String, Integer> courseOrders = orderMap.get(semester);
        for (Course course : courses) {
            Integer order = courseOrders.get(course.getCode());
            if (order != null) {
                course.setDisplayOrder(order);
            }
        }
        
        // Sắp xếp theo displayOrder
        courses.sort(Comparator.comparingInt(Course::getDisplayOrder));
    }
    
    /**
     * Khởi tạo thứ tự cho các môn chưa có
     */
    private static void initializeOrder(List<Course> courses, String semester) {
        Map<String, Integer> courseOrders = orderMap.computeIfAbsent(semester, k -> new HashMap<>());
        
        for (int i = 0; i < courses.size(); i++) {
            Course course = courses.get(i);
            if (!courseOrders.containsKey(course.getCode())) {
                courseOrders.put(course.getCode(), i);
                course.setDisplayOrder(i);
            }
        }
    }
    
    /**
     * Cập nhật thứ tự sau khi kéo thả
     */
    public static void updateOrder(List<Course> courses, String semester) {
        Map<String, Integer> courseOrders = orderMap.computeIfAbsent(semester, k -> new HashMap<>());
        courseOrders.clear();
        
        for (int i = 0; i < courses.size(); i++) {
            Course course = courses.get(i);
            course.setDisplayOrder(i);
            courseOrders.put(course.getCode(), i);
        }
        
        saveOrder();
    }
    
    /**
     * Xóa thứ tự của một môn học
     */
    public static void removeOrder(String semester, String courseCode) {
        if (orderMap.containsKey(semester)) {
            orderMap.get(semester).remove(courseCode);
            saveOrder();
        }
    }
}
