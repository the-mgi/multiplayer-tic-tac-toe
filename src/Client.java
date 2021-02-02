import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import utility.CreateSessionData;
import utility.Operation;
import utility.Person;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;

import static utility.UtilityMethods.showError;

public class Client extends Application {
    private Stage stage;
    private GridPane playersNameSymbolAllottedGridPane;

    private static final String BACKGROUND_COLOR_BUTTON_DEFAULT_TIC = "#5465f8";
    private static final String BACKGROUND_COLOR_BUTTON_HOVER_TIC = "#6272fa";
    private static final String STYLESHEET_PATH = "./stylesheets/styles.css";

    private final SimpleStringProperty userConnectionStatus = new SimpleStringProperty("Waiting for players...");

    private final SimpleStringProperty userFullName = new SimpleStringProperty("");  // nice
    private final SimpleStringProperty topField = new SimpleStringProperty();
    private final SimpleStringProperty selectedDropdown = new SimpleStringProperty();
    private final SimpleStringProperty lobbyStatus = new SimpleStringProperty("Currently room is empty");

    private Operation myNodeSymbol;  // symbol for user to which he will be identified

    private Operation selectedStartOperation = null;

    private ClientHandler clientHandler;
    private Socket socket;

    private String sessionId;

    // Want to play among three players or MORE, change this NUMBER_OF_ROWS to the next ODD number AND
    // in ServerHandler change the NUMBER_OF_PLAYERS to the actual NUMBER_OF_PLAYERS
    private static int NUMBER_OF_ROWS = 3;

    @Override
    public void start(Stage stage) {
        new Thread(() -> {
            try {
                socket = new Socket("localhost", 8080);
            } catch (IOException e) {
                showError("Exception Occurred in ClientJeopardy start method: " + e.toString());
            }
            clientHandler = new ClientHandler(socket);
        }).start();

        this.stage = stage;

        Scene scene = getCompleteScene();
        stage.setScene(scene);
        stage.setTitle("Tic-Tac-Toe Multiplayer");
        stage.setResizable(false);
        stage.show();
    }

    private Scene getCompleteScene() {
        BorderPane borderPane = new BorderPane();

        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER);
        hBox.setPadding(new Insets(50));
        hBox.setSpacing(30);
        TextField name = getTextField("Enter Full Name", 180, 40);
        Label label = getLabel("Enter Full Name");
        userFullName.bind(name.textProperty());

        hBox.getChildren().addAll(label, name);
        borderPane.setTop(hBox);

        VBox mainVBox = new VBox();
        mainVBox.setAlignment(Pos.TOP_CENTER);
        VBox vBox = new VBox();
        vBox.setPadding(new Insets(30));
        vBox.setSpacing(50);

        Button createSession = getButton("Create Session", 200, 200);
        this.maxWidth(createSession, 200, 200);
        this.adjustProps(createSession);
        createSession.setOnAction(e -> {
            borderPane.setCenter(null);
            this.selectedStartOperation = Operation.CREATE_SESSION;
            VBox mainCreateVBox = createSessionORJoinVBox(
                    "Number Of Rows",
                    "Enter Number of rows",
                    "Select Number of Players Allowed",
                    new String[]{"2", "3", "4", "5"},
                    true
            );
            borderPane.setCenter(mainCreateVBox);
        });
        Button joinSession = getButton("Join Session", 200, 200);
        joinSession.setOnAction(e -> {
            this.selectedStartOperation = Operation.JOIN_SESSION;
            VBox mainCreateVBox = createSessionORJoinVBox(
                    "Session ID",
                    "Enter Session ID",
                    "Select From Available Sessions",
                    new String[]{"2", "3", "4", "5"},
                    false
            );
            borderPane.setCenter(mainCreateVBox);
        });
        this.maxWidth(joinSession, 200, 200);
        this.adjustProps(joinSession);

        vBox.getChildren().addAll(createSession, joinSession);
        mainVBox.getChildren().add(vBox);
        borderPane.setLeft(mainVBox);

        Button ok = getButton("Ok", 80, 80);
        ok.setOnAction(e -> {
            String userFullNameValue = userFullName.getValue();
            String topFieldValue = topField.getValue();
            String selectedDropdownValue = selectedDropdown.getValue();
            if (userFullNameValue.isEmpty() || topFieldValue.isEmpty()) {
                showError("Fields must not be empty!");
                return;
            }

            boolean r = this.clientHandler.setValues(userFullNameValue, topFieldValue, selectedDropdownValue);
            if (!r) {
                showError("Number should be integer");
                return;
            }
            boolean condition = false;
            if (this.selectedStartOperation == Operation.CREATE_SESSION) {
                this.sessionId = this.clientHandler.initiateSession();
                if (!this.clientHandler.initiateSession().isEmpty()) {
                    changeScene();
                    condition = true;
                } else {
                    showError("Session Cannot be connected!");
                }
            } else if (this.selectedStartOperation == Operation.JOIN_SESSION) {
                this.sessionId = UUID.randomUUID().toString();
                Operation operation = this.clientHandler.joinSession(this.sessionId);
                if (operation == Operation.JOIN_SESSION_SUCCESS) {
                    changeScene();
                    condition = true;
                } else if (operation == Operation.JOIN_SESSION_FAILED) {
                    showError("Session cannot be joined!");
                }
            } else {
                showError("Please Select an Option");
            }
            if (condition) {
                new Thread(() -> {
                    Platform.runLater(() -> lobbyStatus.set("Players with Symbols"));
                    ArrayList<Object> list = this.clientHandler.startListeningOrders();
                    if (list != null) {
                        for (int i = 0; i < list.size(); i++) {
                            Object[] array = (Object[]) list.get(i);
                            Operation operation = (Operation) array[0];
                            System.out.println(operation);
                            Person person = (Person) array[1];
                            Node node = switch (operation) {
                                case RECTANGLE -> getRectangle(Color.BLACK);
                                case TICK -> getTick(Color.BLACK);
                                case LINE -> getLine(Color.BLACK);
                                case POLYGON -> getPolygon(Color.BLACK);
                                default -> getCircle(Color.BLACK);
                            };
                            if (person.getUuid().equalsIgnoreCase(this.sessionId)) {
                                System.out.println("passed!");
                                this.myNodeSymbol = operation;
                            }
                            int finalI = i;
                            Platform.runLater(() -> {
                                HBox box = boxRight(person.getName(), node);
                                playersNameSymbolAllottedGridPane.add(box, 0, finalI);

                            });
                        }
                    }
                }).start();
            }
        });
        this.adjustProps(ok);

        VBox okButtonBox = new VBox();
        okButtonBox.setAlignment(Pos.BOTTOM_RIGHT);
        okButtonBox.setPadding(new Insets(30));
        okButtonBox.getChildren().add(ok);
        borderPane.setBottom(okButtonBox);

        return new Scene(borderPane, 800, 800);
    }

    private VBox createSessionORJoinVBox(String textLabelTop, String placeholderText, String textLabelBottom, String[] comboBoxValues, boolean wants) {
        VBox mainCreateVBox = new VBox();
        mainCreateVBox.setPadding(new Insets(10));
        mainCreateVBox.setAlignment(Pos.TOP_CENTER);

        HBox numberOfRowsBox = new HBox();
        numberOfRowsBox.setAlignment(Pos.CENTER);
        numberOfRowsBox.setPadding(new Insets(10));
        numberOfRowsBox.setSpacing(30);
        Label rowsLabel = getLabel(textLabelTop);

        numberOfRowsBox.getChildren().add(rowsLabel);

        HBox playersAllowed = new HBox();
        playersAllowed.setSpacing(30);
        playersAllowed.setPadding(new Insets(10));
        playersAllowed.setAlignment(Pos.CENTER);
        Label numberOfPlayers = getLabel(textLabelBottom);
        ComboBox<String> comboBox = getStringComboBox(comboBoxValues, selectedDropdown);
        playersAllowed.getChildren().addAll(numberOfPlayers, comboBox);

        Node node;
        if (wants) {
            node = getStringComboBox(new String[]{"3", "4", "5"}, topField);
            mainCreateVBox.getChildren().addAll(numberOfRowsBox, playersAllowed);
        } else {
            TextField textField = getTextField(placeholderText, 100, 40);
            topField.bind(textField.textProperty());
            node = textField;
            mainCreateVBox.getChildren().addAll(numberOfRowsBox);
        }
        numberOfRowsBox.getChildren().add(node);
        return mainCreateVBox;
    }

    private ComboBox<String> getStringComboBox(String[] args, SimpleStringProperty simpleStringProperty) {
        ComboBox<String> comboBox = new ComboBox<>();
        for (String str : args) {
            comboBox.getItems().add(str);
        }
        comboBox.setValue(args[0]);
        simpleStringProperty.bind(comboBox.valueProperty());
        this.adjustWidthHeight(comboBox, 100, 40);
        return comboBox;
    }

    private Label getLabel(String text) {
        Label label = new Label(text);
        label.getStylesheets().add(STYLESHEET_PATH);
        label.getStyleClass().add("font-size");
        return label;
    }

    private void maxWidth(Button createSession, int height, int width) {
        createSession.setMaxHeight(height);
        createSession.setMaxWidth(width);
    }

    private Button getButton(String text, int width, int height) {
        Button button = new Button(text);
        button.getStylesheets().add(STYLESHEET_PATH);
        button.getStyleClass().addAll("button", "add-radius");
        this.adjustWidthHeight(button, width, height);
        return button;
    }

    private TextField getTextField(String placeholderText, int width, int height) {
        TextField textField = new TextField();
        textField.setPromptText(placeholderText);
        textField.setEditable(true);
        this.adjustWidthHeight(textField, width, height);
        return textField;
    }

    private void adjustWidthHeight(Control node, int width, int height) {
        node.setMinWidth(width);
        node.setMinHeight(height);
    }

    private void changeScene() {
        stage.setTitle("Tic-Tac-Toe, Client: " + userFullName.get());
        stage.setResizable(true);
        stage.setScene(new Scene(getGridPane()));
        stage.show();
    }

    private BorderPane getGridPane() {
        BorderPane borderPane = new BorderPane();
        borderPane.setPadding(new Insets(50));

        GridPane gridPane = new GridPane();

        HBox hBoxZero = new HBox();
        hBoxZero.setAlignment(Pos.CENTER);
        hBoxZero.setPadding(new Insets(10));
        Label label = new Label();
        label.setTextFill(Color.RED);
        label.textProperty().bindBidirectional(userConnectionStatus);
        label.getStylesheets().add(STYLESHEET_PATH);
        label.getStyleClass().add("font-size");

        hBoxZero.getChildren().add(label);
        borderPane.setTop(hBoxZero);

        for (int i = 0; i < Client.NUMBER_OF_ROWS; i++) {
            HBox hBox = new HBox();
            hBox.setSpacing(10);
            hBox.setPadding(new Insets(5));
            for (int j = 0; j < Client.NUMBER_OF_ROWS; j++) {
                Button button = buttonWithProps(i, j);
                hBox.getChildren().add(button);
            }
            gridPane.add(hBox, 0, i);
        }

        borderPane.setCenter(gridPane);

        GridPane rightGridPane = new GridPane();
        rightGridPane.getStylesheets().add(STYLESHEET_PATH);
        rightGridPane.getStyleClass().addAll("right-side", "add-radius");
        rightGridPane.setPadding(new Insets(20));
        rightGridPane.setVgap(10);
        playersNameSymbolAllottedGridPane = rightGridPane;

        Label noUsers = getLabel("");
        noUsers.textProperty().bindBidirectional(lobbyStatus);
        noUsers.setTextFill(Color.WHITE);

        HBox hBox = new HBox();
        hBox.getChildren().add(noUsers);
        hBox.setAlignment(Pos.CENTER);
        hBox.setMinWidth(250);

        rightGridPane.add(hBox, 0, 0);
        borderPane.setRight(rightGridPane);
        return borderPane;
    }

    private HBox boxRight(String playerName, Node node) {
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER);
        hBox.setPadding(new Insets(8));
        hBox.setSpacing(10);
        hBox.getStylesheets().add(STYLESHEET_PATH);
        hBox.getStyleClass().addAll("hBox-right", "add-radius");

        Label image = new Label();
        image.setGraphic(node);
        Label name = getLabel(playerName);
        hBox.getChildren().addAll(image, name);
        return hBox;
    }

    /**
     * Provides the single button of BOARD, with the styles and properties
     *
     * @param i row-value of button position on board
     * @param j column-value of button position on board
     * @return Board single button
     * @see #adjustProps
     */
    private Button buttonWithProps(int i, int j) {
        Button button = getButton("", 150, 150);
        button.setId(i + "-" + j);
//        button.disableProperty().bindBidirectional(disableAllButtons);
        button.setDisable(false);
        this.adjustProps(button);
        button.setOnAction(e -> {
            button.setGraphic(switch (this.myNodeSymbol) {
                case RECTANGLE -> getRectangle(Color.WHITE);
                case TICK -> getTick(Color.WHITE);
                case LINE -> getLine(Color.WHITE);
                case POLYGON -> getPolygon(Color.WHITE);
                default -> getCircle(Color.WHITE);
            });
        });
        return button;
    }

    /**
     * @param button button to add style properties to
     */
    private void adjustProps(Button button) {
        button.setCursor(Cursor.HAND);
        button.setStyle("-fx-background-color: " + BACKGROUND_COLOR_BUTTON_DEFAULT_TIC + ";");
        button.getStylesheets().add(STYLESHEET_PATH);
        button.getStyleClass().add("font-size");
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: " + BACKGROUND_COLOR_BUTTON_HOVER_TIC + ";"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: " + BACKGROUND_COLOR_BUTTON_DEFAULT_TIC + ";"));
    }

    private Node getCircle(Color color) {
        Circle circle = new Circle();
        circle.setRadius(30);
        circle.setStroke(color);
        circle.setFill(Color.rgb(200, 200, 200, 0.0));
        circle.setStrokeWidth(4);
        return circle;
    }

    private Region getTick(Color color) {
        Region region = new Region();
        region.setMaxWidth(40);
        region.setMaxHeight(20);
        region.setPrefHeight(10);
        region.setRotate(-45);
        region.getStylesheets().add(STYLESHEET_PATH);
        region.getStyleClass().add("region");
        return region;
    }

    private Rectangle getRectangle(Color color) {
        Rectangle rectangle = new Rectangle(50, 50);
        rectangle.setStroke(color);
        rectangle.setFill(Color.rgb(200, 200, 200, 0.0));
        rectangle.setStrokeWidth(4);
        rectangle.setArcHeight(10);
        rectangle.setArcWidth(10);
        return rectangle;
    }

    private Line getLine(Color color) {
        Line line = new Line(0, 0, 50, 0);
        line.setStroke(color);
        line.setStrokeWidth(4);
        return line;
    }

    private Polygon getPolygon(Color color) {
        Polygon parallelogram = new Polygon();
        parallelogram.getPoints().addAll(30.0, 0.0, 130.0, 0.0, 100.00, 50.0, 0.0, 50.0);
        parallelogram.setFill(Color.TRANSPARENT);
        parallelogram.setStroke(color);
        parallelogram.setStrokeWidth(4);
        return parallelogram;
    }

    public static void main(String[] args) {
        launch(args);
    }

}

class ClientHandler {
    private String username;
    private String topField;
    private int selectedValue;

    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;

    public ClientHandler(Socket socket) {
        if (socket != null) {
            try {
                this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                this.objectInputStream = new ObjectInputStream(socket.getInputStream());
            } catch (IOException ex) {
                showError("Exception Occurred in ClientThread Constructor: " + ex.toString());
            }
            return;
        }
        showError("");
    }

    public String initiateSession() {
        this.sendObject(Operation.CREATE_SESSION);
        CreateSessionData data = new CreateSessionData(this.username, Operation.CREATE_SESSION, this.selectedValue, Integer.parseInt(this.topField));
        this.sendObject(data);
        System.out.println(data.getPerson().getUuid());
        return data.getPerson().getUuid();
    }

    public Operation joinSession(String id) throws ClassCastException {
        this.sendObject(Operation.JOIN_SESSION);
        this.sendObject(this.topField);
        Person person = new Person(this.username, id);
        this.sendObject(person);
        return (Operation) this.readObject();
    }

    private void sendObject(Object object) {
        try {
            this.objectOutputStream.writeObject(object);
        } catch (IOException ex) {
            showError("Error Occurred in sendObject in ClientHandler: " + ex.toString());
        }
    }

    private Object readObject() {
        try {
            return this.objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            showError("Error Occurred in readObject in ClientHandler: " + ex.toString());
        }
        return null;
    }

    public boolean setValues(String username, String topField, String selectedValue) {
        this.username = username;
        this.topField = topField;
        try {
            this.selectedValue = Integer.parseInt(selectedValue);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public ArrayList<Object> startListeningOrders() {
        try {
            ArrayList<Object> data = (ArrayList<Object>) this.objectInputStream.readObject();
            if (data == null) {
                showError("");
            }
            return data;
        } catch (IOException | ClassNotFoundException | ClassCastException ex) {
            showError("Error Occurred in startListeningOrders in ClientHandler: " + ex.toString());
            return null;
        }
    }
}
