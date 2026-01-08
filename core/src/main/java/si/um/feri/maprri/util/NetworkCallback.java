package si.um.feri.maprri.util;

public interface NetworkCallback<T> {
    void onSuccess(T result);
    void onError(Throwable t);
}
