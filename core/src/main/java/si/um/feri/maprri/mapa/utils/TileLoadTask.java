package si.um.feri.maprri.mapa.utils;

class TileLoadTask implements Runnable, Comparable<TileLoadTask> {
    private final double priority;
    private final Runnable task;

    public TileLoadTask(double priority, Runnable task) {
        this.priority = priority;
        this.task = task;
    }

    @Override
    public void run() {
        task.run();
    }

    @Override
    public int compareTo(TileLoadTask o) {
        return Double.compare(this.priority, o.priority);
    }
}
