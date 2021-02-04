package utility;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

public class UtilityMethods {
    private static Label getLabel(String string) {
        Label label = new Label(string);
        label.setPrefWidth(400);
        label.setPadding(new Insets(15, 15, 15, 15));
        return label;
    }

    public static void showError(String errorStatement) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            if (errorStatement.equals("") || errorStatement.contains("SocketException")) {
                alert.getDialogPane().setContent(getLabel("Server Not Running"));
                alert.showAndWait();
                Platform.exit();
            } else {
                alert.getDialogPane().setContent(getLabel(errorStatement));
                alert.show();
            }
        });
    }

    public static void showConfirmation(String confirmationStatement) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.getDialogPane().setContent(getLabel(confirmationStatement));
            alert.showAndWait();
            Platform.exit();
        });
    }
}
