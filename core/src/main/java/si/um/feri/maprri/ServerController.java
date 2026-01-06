package si.um.feri.maprri;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import si.um.feri.maprri.models.Mine;
import si.um.feri.maprri.util.NetworkCallback;

import java.util.ArrayList;
import java.util.List;

public class ServerController {
    String path;
    public ServerController(String path){
        this.path = path;
    }

    public  List<Mine> getAllMines(NetworkCallback<List<Mine>> callback){
        List<Mine> mines = new ArrayList<>();
        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
        request.setUrl(path + "/mine/");
        request.setTimeOut(5000); //5s

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                String result = httpResponse.getResultAsString();

                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        if (statusCode >= 200 && statusCode < 300) {
                            try {
                                List<Mine> mines = Mine.getMineListFromJson(result);
                                callback.onSuccess(mines);
                            } catch (Exception e) {
                                callback.onError(e);
                            }
                        } else {
                            callback.onError(new Exception("Server error: " + statusCode));
                        }
                    }
                });
            }

            @Override
            public void failed(Throwable throwable) {
                System.out.println("GETTING MINES FAILED: " + throwable.toString());
            }

            @Override
            public void cancelled() {
                System.out.println("CANCELLED");
            }
        });
        return mines;
    }

    public Mine getMine(NetworkCallback<Mine> callback, String mineId){
        Mine mine = new Mine();
        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
        String url = path + "/mine/get/" + mineId;
        System.out.println("URL: " + url);
        request.setUrl(url);
        request.setTimeOut(5000); //5s

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                String result = httpResponse.getResultAsString();

                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        if (statusCode >= 200 && statusCode < 300) {
                            try {
                                Mine mine = Mine.getMineFromJson(result);
                                callback.onSuccess(mine);
                            } catch (Exception e) {
                                callback.onError(e);
                            }
                        } else {
                            callback.onError(new Exception("Server error: " + statusCode));
                        }
                    }
                });
            }

            @Override
            public void failed(Throwable throwable) {
                System.out.println("GETTING ONE MINE FAILED: " + throwable.toString());
            }

            @Override
            public void cancelled() {
                System.out.println("CANCELLED");
            }
        });
        return mine;
    }
}
