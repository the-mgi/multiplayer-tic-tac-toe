package utility;

import java.io.Serializable;
import java.util.UUID;

/**
 * serves as an object that holds the data
 */
public class CreateSessionData implements Serializable {
    private final Person person;
    private final Operation operation;
    private final int numberOfPlayersAllowed;
    private final int numberOfRows;

    public CreateSessionData(String name, Operation operation, int numberOfPlayersAllowed, int numberOfRows) {
        this.person = new Person(name);
        this.operation = operation;
        this.numberOfPlayersAllowed = numberOfPlayersAllowed;
        this.numberOfRows = numberOfRows;
    }

    public Person getPerson() {
        return person;
    }

    public Operation getOperation() {
        return operation;
    }

    public int getNumberOfRows() {
        return numberOfRows;
    }

    public int getNumberOfPlayersAllowed() {
        return numberOfPlayersAllowed;
    }

    @Override
    public String toString() {
        return "CreateSessionData{" +
                "person=" + person +
                ", operation=" + operation +
                ", numberOfPlayersAllowed=" + numberOfPlayersAllowed +
                ", numberOfRows=" + numberOfRows +
                '}';
    }
}
