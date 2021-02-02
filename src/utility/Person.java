package utility;

import java.io.Serializable;
import java.util.UUID;

public class Person implements Serializable {
    private final String name;
    private final String uuid;

    public Person(String name) {
        this.name = name;
        this.uuid = UUID.randomUUID().toString();
    }

    public Person(String name, String uuid) {
        this.name = name;
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", uuid=" + uuid +
                '}';
    }
}
