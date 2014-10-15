package com.brian.todoapp.application; /**
 * Created by brianpham on 10/7/14.
 */
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.*;
import org.glassfish.jersey.grizzly2.GrizzlyHttpServerFactory;
import org.glassfish.jersey.media.json.JsonJacksonModule;
import org.glassfish.jersey.server.Application;
import org.glassfish.jersey.server.ResourceConfig;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;

public class TodoServer {

    public static DB mongoDB;

    public static String todoUrl;

    private static final String TODO_CONTENT_PATH = "/todocontent";

    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
        final int port = System.getenv("PORT") != null ? Integer.valueOf(System.getenv("PORT")) : 8080;
        final URI baseUri = UriBuilder.fromUri("http://0.0.0.0/").port(port).build();
        final Application application = Application.builder(ResourceConfig.builder().packages(TodoServer.class.getPackage().getName()).build()).build();
        application.addModules(new JsonJacksonModule());
        final HttpServer httpServer = GrizzlyHttpServerFactory.createHttpServer(baseUri, application);
        httpServer.getServerConfiguration().addHttpHandler(new StaticHttpHandler("src/main/webapp"), TODO_CONTENT_PATH);

        for (NetworkListener networkListener : httpServer.getListeners()) {
            if (System.getenv("FILE_CACHE_ENABLED") == null) {
                networkListener.getFileCache().setEnabled(false);
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                httpServer.stop();
            }
        });

        MongoURI mongolabUri = new MongoURI(System.getenv("MONGOLAB_URI") != null ? System.getenv("MONGOLAB_URI") : "mongodb://127.0.0.1:27017/todo");
        Mongo m = new Mongo(mongolabUri);
        mongoDB = m.getDB(mongolabUri.getDatabase());
        if ((mongolabUri.getUsername() != null) && (mongolabUri.getPassword() != null)) {
            mongoDB.authenticate(mongolabUri.getUsername(), mongolabUri.getPassword());
        }

        todoUrl = System.getenv("TODO_URL") != null ? System.getenv("TODO_URL") : TODO_CONTENT_PATH;
        Thread.currentThread().join();
    }


}
