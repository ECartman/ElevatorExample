module com.microsoft.jfx.test {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    opens com.microsoft.jfx.test to javafx.fxml;
    exports com.microsoft.jfx.test;
}
