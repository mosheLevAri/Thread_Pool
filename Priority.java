package il.co.lird.FS133.Projects.ThreadPool;

public enum Priority {
    LOW(1), MEDIUM(2), HIGH(3);
    private final int value;

    Priority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}