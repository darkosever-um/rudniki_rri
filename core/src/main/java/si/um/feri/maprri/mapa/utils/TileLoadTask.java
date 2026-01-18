package si.um.feri.maprri.mapa.utils;

class TileLoadTask implements Runnable, Comparable<TileLoadTask> {
    private final double distance;
    private final Runnable task;

    public TileLoadTask(double distance, Runnable task) {
        this.distance = distance;
        this.task = task;
    }

    @Override
    public void run() { task.run(); }

    @Override
    public int compareTo(TileLoadTask o) {
        return Double.compare(this.distance, o.distance);
    }
}
