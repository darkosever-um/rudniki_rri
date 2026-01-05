package si.um.feri.maprri;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;

public class ServerController {
    String path;
    public ServerController(String path){
        this.path = path;
    }

    public void getAllMines(){
        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
        request.setUrl(path + "/");
        request.setTimeOut(5000); //5s
        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                String result = httpResponse.getResultAsString();
                System.out.println("CODE:" + statusCode);
                System.out.println("RESULT: " + result);
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
    }
}
