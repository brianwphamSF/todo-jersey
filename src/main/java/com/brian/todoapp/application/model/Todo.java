package com.brian.todoapp.application.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.searchbox.annotations.JestId;
import net.vz.mongodb.jackson.ObjectId;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.Serializable;
//import org.codehaus.jettison.json.JSONException;
//import org.codehaus.jettison.json.JSONObject;

// Model
public class Todo implements Serializable {

    @ObjectId
    public String _id;

    @ObjectId
    private String documentId;

    public String title;

    public String body;

    public boolean done;

    @JsonProperty("_id")
    public String getId() {
        return _id;
    }

    public void setId(String _id) {
        this._id = _id;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean getDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

}