package pers.cz.chaoxing.util;

public enum InfoType {
    Video(0),
    Homework(1);

    private final int id;

    InfoType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
