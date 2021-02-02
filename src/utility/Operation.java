package utility;

import java.io.Serializable;

public enum Operation implements Serializable {
    CREATE_SESSION,
    JOIN_SESSION,
    JOIN_SESSION_SUCCESS,
    JOIN_SESSION_FAILED,
    RECTANGLE,
    TICK,
    LINE,
    POLYGON,
    CIRCLE
}
