package com.brian.todoapp.application.controller; /**
 * Created by brianpham on 10/7/14.
 */
import com.brian.todoapp.application.TodoServer;
import com.brian.todoapp.application.model.Todo;
import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.resource.factory.MessageFactory;
import com.twilio.sdk.resource.instance.Message;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.JestResultHandler;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.*;
import io.searchbox.indices.CreateIndex;
import net.vz.mongodb.jackson.JacksonDBCollection;
import org.apache.http.message.BasicNameValuePair;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.apache.http.NameValuePair;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

@Path("/")
public class TodoController {

    private static final String TITLE_TYPE_NAME = "titles";
    private static final String TODO_INDEX_NAME = "todo";

    public SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

    // Setup the JestClient for ElasticSearch io.searchbox
    public JestClient jestClient() {
        String connectionUrl;

        if (System.getenv("SEARCHBOX_URL") != null) {
            // Heroku
            connectionUrl = System.getenv("SEARCHBOX_URL");

        } /*else if (System.getenv("VCAP_SERVICES") != null) {
            // CloudFoundry
            Map result = new ObjectMapper().readValue(System.getenv("VCAP_SERVICES"), HashMap.class);
            connectionUrl = (String) ((Map) ((Map) ((List)
                    result.get("searchly-n/a")).get(0)).get("credentials")).get("uri");
        }*/ else {
            // generic, check your dashboard
            //connectionUrl = "http://site:your-api-key@your-url.searchly.com";
            connectionUrl = "http://localhost:9200";
            //connectionUrl = "https://site:4200f46417dd5b88d9ae842a712e627d@bofur-us-east-1.searchly.com";
        }

        // Configuration
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(new HttpClientConfig
                .Builder(connectionUrl)
                .multiThreaded(true)
                .build());
        return factory.getObject();
    }

    // Using Jackson for MongoDB
    private JacksonDBCollection<Todo, String> getJacksonDBCollection() {
        return JacksonDBCollection.wrap(TodoServer.mongoDB.getCollection(Todo.class.getSimpleName().toLowerCase()), Todo.class, String.class);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String index() {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<script type='text/javascript' src='" + TodoServer.todoUrl + "/jquery-1.7.min.js'></script>\n" +
                "<script type='text/javascript' src='" + TodoServer.todoUrl + "/index.js'></script>\n" +
                "</head>\n" +
                "</html>";
    }

    @Path("todos")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Todo> listTodos() {
        return getJacksonDBCollection().find().toArray();
    }

    @Path("search")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Todo> searchData(@Context UriInfo info) throws Exception {

        // First, search the query
        String queryStr = info.getQueryParameters().getFirst("query");

        searchSourceBuilder.query(
                QueryBuilders.queryString(
                        "(title:"
                                + queryStr
                                + ")^2 OR body:"
                                + queryStr
                )
        );

        Search search = new Search.Builder(
                searchSourceBuilder.toString())
                .addIndex(TODO_INDEX_NAME)
                .addType(TITLE_TYPE_NAME).build();

        // Save the query into a result
        JestResult result = jestClient().execute(search);
        List<Todo> todos = result.getSourceAsObjectList(Todo.class);

        jestClient().shutdownClient();

        Todo fromSearch, fromSource;

        // Put search results in from search and from source
        List<Todo> searchResults = new ArrayList<Todo>();

        for (int i = 0; i < todos.size(); i++) {
            fromSearch = todos.get(i);
            for (int j = 0; j < listTodos().size(); j++) {
                fromSource = listTodos().get(j);
                if (fromSearch.getTitle().equals(fromSource.getTitle())
                       && fromSearch.getBody().equals(fromSource.getBody()) ) {
                    searchResults.add(fromSearch);
                }
            }
            System.out.println(todos.get(i));
        }

        return searchResults;
    }

    @Path("todo")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void addTodo(final Todo todo) throws Exception {
        // Write to MongoDB
        getJacksonDBCollection().save(todo);

        // Create index over to searchbox.io
        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();
        settings.put("number_of_shards", 3);
        settings.put("number_of_replicas", 0);
        jestClient().execute(new CreateIndex.Builder(TODO_INDEX_NAME).settings(settings.build().getAsMap()).build());

        // Index data over to searchbox.io
        Index index = new Index.Builder(todo).index(TODO_INDEX_NAME).type(TITLE_TYPE_NAME).build();
        jestClient().executeAsync(index, new JestResultHandler<JestResult>() {
            @Override
            public void completed(JestResult result) {
                todo.setDocumentId((String) result.getValue("_documentId"));
                System.out.println("Index inserted");
            }

            @Override
            public void failed(Exception ex) {

            }
        });

        jestClient().shutdownClient();
    }

    // Supposedly searches queries for updating or deleting
    // in io.searchbox (now not used anywhere)
    public String queryModifier(Todo todo) {
        return searchSourceBuilder.query(
                QueryBuilders.queryString(
                        "_id:" + todo.getId()
                        + " AND documentId:" + todo.getDocumentId()
                        + " AND title:" + todo.getTitle()
                        + " AND body:" + todo.getBody()
                        //+ " AND done:" + todo.getDone()
                        + " AND id:" + todo.getId()
                )
        ).toString();
    }

    @Path("update")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void updateTodo(final Todo todo) throws Exception {

        // If task is not done
        if (!todo.getDone()) {
            todo.setDone(true);

            // Send a text with Twilio's API as task has been done
            TwilioRestClient twilioRestClient = new TwilioRestClient("ACCOUNT_SID", "AUTH_TOKEN");
            List<NameValuePair> pairs = new ArrayList<NameValuePair>();
            pairs.add(new BasicNameValuePair("Body", todo.getTitle() + " has been marked as done."));

            // Receiving and sending phone numbers
            pairs.add(new BasicNameValuePair("To", "+14159905196"));
            pairs.add(new BasicNameValuePair("From", "+16506845329"));

            MessageFactory messageFactory = twilioRestClient.getAccount().getMessageFactory();
            Message message = messageFactory.create(pairs);
            System.out.println(message.getSid());
        }
        // If task was supposedly done
        else {
            todo.setDone(false);
        }

        // Update the record in MongoDB by ID
        getJacksonDBCollection().updateById(todo.getId(), todo);

        /** IGNORED below (what was tried before any other attempts)
         * This snippet below is supposed to update the query in io.searchbox in
         * conjunction with the record from MongoDB. It doesn't function as
         * supposed, so instead we compare values in our search, at least
         * at this time.
         */
        /*

        String script = "{\n" +
                "    \"script\" : \"ctx._source.tags += tag\",\n" +
                "    \"params\" : {\n" +
                "        \"_id\" : \"" + null + "\"\n" +
                "        \"documentId\" : \"" + null + "\"\n" +
                "        \"title\" : \"" + todo.getTitle() + "\"\n" +
                "        \"body\" : \"" + todo.getBody() + "\"\n" +
                "        \"done\" : \"" + todo.getDone() + "\"\n" +
                "        \"id\" : \"" + null + "\"\n" +
                "    }\n" +
                "}";

        System.out.println(queryModifier(todo));

        jestClient().execute(new Update.Builder(script).index(TODO_INDEX_NAME).type(TITLE_TYPE_NAME).id("1").build());

        jestClient().shutdownClient();
        */
    }

    @Path("delete")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void deleteTodo(final Todo todo) throws Exception {

        // Remove the record from MongoDB by ID
        getJacksonDBCollection().removeById(todo.getId());

        /** IGNORED below (what was tried before any other attempts)
         * As the update snippet from above, the io.searchbox record currently cannot be deleted.
         */
        /*
        Delete delete = new Delete.Builder(todo.getDocumentId()).index(TODO_INDEX_NAME).type(TITLE_TYPE_NAME).build();
        jestClient().execute(delete);

        jestClient().shutdownClient();
        */
    }

}