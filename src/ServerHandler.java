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

    private CreateSessionData createSessionData;

    private int currentCount = 0;
    private int currentActivePlayer;

    private final ArrayList<ObjectOutputStream> objectOutputStreams = new ArrayList<>();
    private final ArrayList<ObjectInputStream> objectInputStreams = new ArrayList<>();
    private final ArrayList<Person> people = new ArrayList<>();

    public ServerSession(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, CreateSessionData createSessionData) {
        this.objectInputStreams.add(objectInputStream);
        this.objectOutputStreams.add(objectOutputStream);
        this.people.add(createSessionData.getPerson());

        this.currentCount += 1;

        this.createSessionData = createSessionData;
    }

    public void addStreams(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream, Person person) {
        if (this.currentCount < this.createSessionData.getNumberOfPlayersAllowed()) {
            this.objectOutputStreams.add(objectOutputStream);
            this.objectInputStreams.add(objectInputStream);
            this.sendObject(Operation.JOIN_SESSION_SUCCESS, this.objectOutputStreams.get(this.currentCount));
            this.people.add(person);
            if (this.currentCount == (this.createSessionData.getNumberOfPlayersAllowed() - 1)) {
                this.sendAllPlayersNamesSymbols();
            }
            this.currentCount += 1;
            return;
        }
        this.sendObject(Operation.JOIN_SESSION_FAILED, this.objectOutputStreams.get(this.currentCount));
    }

    private void sendAllPlayersNamesSymbols() {
        Operation[] shapes = new Operation[]
                {Operation.RECTANGLE, Operation.TICK, Operation.LINE, Operation.POLYGON, Operation.CIRCLE};
        ArrayList<Object> finalArray = new ArrayList<>();
        for (int i = 0; i < this.createSessionData.getNumberOfPlayersAllowed(); i++) {
            Object[] data = new Object[]{shapes[i], people.get(i)};
            finalArray.add(data);
        }
        this.sendObjectToAll(finalArray);
    }

    private void sendObjectToAll(Object object) {
        for (int i = 0; i < this.createSessionData.getNumberOfPlayersAllowed(); i++) {
            this.sendObject(object, this.objectOutputStreams.get(i));
        }
    }

    private void sendObject(Object object, ObjectOutputStream objectOutputStream) {
        try {
            objectOutputStream.writeObject(object);
        } catch (IOException ex) {
            System.out.println("Error Occurred in sendObject in ClientHandler: " + ex.toString());
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
