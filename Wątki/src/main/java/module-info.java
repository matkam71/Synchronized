module com.example.lista6 {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.dlsc.formsfx;

    opens com.example.lista6 to javafx.fxml;
    exports com.example.lista6;
}