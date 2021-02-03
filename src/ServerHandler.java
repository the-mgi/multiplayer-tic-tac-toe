import utility.CreateSessionData;
import utility.Operation;
import utility.Person;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class ServerHandler {
    static final HashMap<String, ServerSession> serverSessions = new HashMap<>();

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(8080);
            while (true) {
                Socket socket = serverSocket.accept();

                ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());

                while (true) {
                    Operation operation = (Operation) objectInputStream.readObject(); // What client wants to do
                    if (operation == Operation.CREATE_SESSION) {
                        CreateSessionData createSessionData = (CreateSessionData) objectInputStream.readObject();
                        ServerSession serverSession = new ServerSession(objectInputStream, objectOutputStream, createSessionData);
                        serverSessions.put(createSessionData.getPerson().getUuid(), serverSession);
                        new Thread((serverSession)).start();
                        break;
                    } else if (operation == Operation.JOIN_SESSION) {
                        String string = (String) objectInputStream.readObject();
                        Person person = (Person) objectInputStream.readObject();
                        if (serverSessions.containsKey(string)) {  // either the key the server sent is right or wrong!!
                            serverSessions.get(string).addStreams(objectInputStream, objectOutputStream, person);
                            break;
                        } else {
                            objectOutputStream.writeObject(Operation.JOIN_SESSION_FAILED);
                        }
                    } else {
                        socket.close();
                    }
                }
            }
        } catch (IOException | ClassCastException | ClassNotFoundException | NullPointerException ex) {
            System.out.println("Error occurred in main in ServerHandler: " + ex.toString());
            ex.printStackTrace();
        }
    }
}

class ServerSession implements Runnable {

    private final CreateSessionData createSessionData;

    private int currentCount = 0;
    private int currentActivePlayer = 0;
    private final Operation[][] boardPositions;

    // here we can wrap these arrays and person in class but i am lazy!!
    private final ArrayList<ObjectOutputStream> objectOutputStreams = new ArrayList<>();
    private final ArrayList<ObjectInputStream> objectInputStreams = new ArrayList<>();
    private final ArrayList<Person> people = new ArrayList<>();
    private final Operation[] shapes = new Operation[]
            {Operation.RECTANGLE, Operation.LINE, Operation.POLYGON, Operation.CIRCLE, Operation.TICK};

    public ServerSession(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, CreateSessionData createSessionData) {
        this.objectInputStreams.add(objectInputStream);
        this.objectOutputStreams.add(objectOutputStream);
        this.people.add(createSessionData.getPerson());

        this.currentCount += 1;

        this.createSessionData = createSessionData;

        this.boardPositions = new Operation[this.createSessionData.getNumberOfRows()][this.createSessionData.getNumberOfRows()];
    }

    public void addStreams(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, Person person) {
        if (this.currentCount < this.createSessionData.getNumberOfPlayersAllowed()) {
            this.objectOutputStreams.add(objectOutputStream);
            this.objectInputStreams.add(objectInputStream);
            this.sendObject(new Object[]{Operation.JOIN_SESSION_SUCCESS, this.createSessionData.getNumberOfRows()}, this.objectOutputStreams.get(this.currentCount));
            this.people.add(person);
            if (this.currentCount == (this.createSessionData.getNumberOfPlayersAllowed() - 1)) {
                this.sendAllPlayersNamesSymbols();
            }
            this.currentCount += 1;
            return;
        }
        this.sendObject(Operation.JOIN_SESSION_FAILED, this.objectOutputStreams.get(this.currentCount));
    }

    private void sendObject(Object object, ObjectOutputStream objectOutputStream) {
        try {
            objectOutputStream.writeObject(object);
        } catch (IOException ex) {
            System.out.println("Error Occurred in sendObject in ClientHandler: " + ex.toString());
        }
    }

    private Object readObject(int currentActivePlayer) {
        Object object = null;
        try {
            object = this.objectInputStreams.get(currentActivePlayer).readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException ex) {
            System.out.println("Error Occurred in readObject in ServerSession: " + ex.toString());
        }
        return object;
    }

    private void sendAllPlayersNamesSymbols() {
        ArrayList<Object> finalArray = new ArrayList<>();
        for (int i = 0; i < this.createSessionData.getNumberOfPlayersAllowed(); i++) {
            Object[] data = new Object[]{this.shapes[i], people.get(i)};
            finalArray.add(data);
        }
        this.sendObjectToAll(finalArray);
        this.sendMovePermissions(this.currentActivePlayer);
    }

    private void sendObjectToAll(Object object) {
        for (int i = 0; i < this.createSessionData.getNumberOfPlayersAllowed(); i++) {
            this.sendObject(object, this.objectOutputStreams.get(i));
        }
    }

    private void sendMovePermissions(int currentActivePlayer) {
        for (int i = 0; i < this.objectOutputStreams.size(); i++) {
            boolean permission = false;
            if (i == currentActivePlayer) {
                permission = true;
            }
            this.sendObject(permission, this.objectOutputStreams.get(i));
        }
        this.listeningForCurrentPlayersMove();
    }

    private void listeningForCurrentPlayersMove() {
        String move = (String) this.readObject(this.currentActivePlayer);
        String[] array = move.split("-");
        int rowValue = Integer.parseInt(array[0]);
        int colValue = Integer.parseInt(array[1]);
        this.boardPositions[rowValue][colValue] = this.shapes[this.currentActivePlayer];

        Object[] sendData = new Object[]{this.shapes[this.currentActivePlayer], move};
        this.sendObjectToAll(sendData);

        if (this.currentActivePlayer == this.createSessionData.getNumberOfPlayersAllowed() - 1) {
            this.currentActivePlayer = 0;
        } else {
            this.currentActivePlayer += 1;
        }
        boolean result = checkWinningState(rowValue, colValue, this.shapes[this.currentActivePlayer]);


        this.sendMovePermissions(this.currentActivePlayer);


    }

    private boolean checkWinningState(int rowValue, int columnValue, Operation operation) {
        return rowCondition(rowValue, operation) ||
                columnCondition(columnValue, operation) ||
                diagonalCondition(rowValue, columnValue, operation) ||
                antiDiagonalCondition(operation);
    }

    private boolean rowCondition(int row, Operation operation) {
        for (int columnIndex = 0; columnIndex < this.createSessionData.getNumberOfRows(); columnIndex++) {
            if (!(this.boardPositions[row][columnIndex] == operation)) {
                return false;
            }
        }
        return true;
    }

    private boolean columnCondition(int column, Operation operation) {
        for (int rowIndex = 0; rowIndex < this.createSessionData.getNumberOfRows(); rowIndex++) {
            if (!(this.boardPositions[column][rowIndex] == operation)) {
                return false;
            }
        }
        return true;
    }

    private boolean diagonalCondition(int row, int column, Operation operation) {
        if (row == column) {
            for (int index = 0; index < this.createSessionData.getNumberOfRows(); index++) {
                if (!(this.boardPositions[index][index] == operation)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean antiDiagonalCondition(Operation operation) {
        for (int index = this.createSessionData.getNumberOfRows() - 1; index >= 0; index--) {
            if (!(this.shapes[index] == operation)) {
                return false;
            }
        }
        return true;
    }

    private void sendWinningReportToAll(int winnerIndex) {
        for (int i = 0; i < this.createSessionData.getNumberOfPlayersAllowed(); i++) {
            boolean winner = false;
            if (i == winnerIndex) {
                winner = true;
            }
            this.sendObject(winner, this.objectOutputStreams.get(i));
        }
    }

    @Override
    public void run() {
    }

    @Override
    public String toString() {
        return "ServerSession{" +
                "createSessionData=" + createSessionData +
                ", currentCount=" + currentCount +
                ", currentActivePlayer=" + currentActivePlayer +
                '}';
    }
}
