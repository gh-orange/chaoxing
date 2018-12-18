package pers.cz.chaoxing.util;

public enum InfoType {
    PLAYER(0),
    HOMEWORK(1),
    EXAM(2);

    private final int id;

    InfoType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
