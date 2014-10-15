/**
 * Created by brianpham on 10/7/14.
 */
function loadNotDoneTodos() {
    $.ajax("/todos", {
        contentType: "application/json",
        success: function(data) {
            $("#todosNotDone").children().remove();
            $.each(data, function(index, item) {
                if (item.done == false) {
                    $("#todosNotDone").append($("<li>")
                        .text(item.title + ' ' + item.body
                    ), $("<button id='update' value='" + item.id + "' onclick='updateTodo(\"" + item.id + "\", \"" + item.title + "\", \"" + item.body + "\", \"" + item.done + "\")'/>").text("Update"),
                        $("<button id='delete' value='" + item.id + "' onclick='deleteTodo(\"" + item.id + "\", \"" + item.title + "\", \"" + item.body + "\", \"" + item.done + "\")'/>").text("Delete"));
                }
            });
        }
    });
}

function loadDoneTodos() {
    $.ajax("/todos", {
        contentType: "application/json",
        success: function(data) {
            $("#todosDone").children().remove();
            $.each(data, function(index, item) {
                if (item.done == true) {
                    $("#todosDone").append($("<li>")
                        .text(item.title + ' ' + item.body
                    ), $("<button id='update' value='" + item.id + "' onclick='updateTodo(\"" + item.id + "\", \"" + item.title + "\", \"" + item.body + "\", \"" + item.done + "\")'/>").text("Update")
                    , $("<button id='delete' value='" + item.id + "' onclick='deleteTodo(\"" + item.id + "\", \"" + item.title + "\", \"" + item.body + "\", \"" + item.done + "\")'/>").text("Delete"));
                }
            });
        }
    });
}

function addTodo() {
    $.ajax({
        url: "/todo",
        type: 'post',
        dataType: 'json',
        contentType: 'application/json',
        data: JSON.stringify(
            {
                title:$("#title").val(),
                body:$("#body").val(),
                done:"false"
            }
        ),
        success: loadNotDoneTodos()
    });
}

function updateTodo(id, title, body, done) {
    //alert("title and body: " + title + ' ' + body);
    $.ajax({
        url: "/update",
        type: 'put',
        dataType: 'json',
        contentType: 'application/json',
        data: JSON.stringify(
            {
                _id:id,
                documentId:null,
                title:title,
                body:body,
                done:done,
                id:id
            }
        ),
        success: function() {
            loadDoneTodos();
            loadNotDoneTodos();
        }
    })
}

function deleteTodo(id, title, body, done) {
    //alert("title and body: " + title + ' ' + body);
    $.ajax({
        url: "/delete",
        type: 'delete',
        dataType: 'json',
        contentType: 'application/json',
        data: JSON.stringify(
            {
                _id:id,
                documentId:null,
                title:title,
                body:body,
                done:done,
                id:id
            }
        ),
        success: function() {
            loadDoneTodos();
            loadNotDoneTodos();
        }
    })
}

function searchTodo() {
    /*window.location = "/search?query="
        + encodeURIComponent(
            document.getElementById("searchQuery")
                .value
        );
    */
    $.ajax("/search?query="
        + encodeURIComponent(
            document.getElementById("searchQuery")
                .value), {
        contentType: "application/json",
        success: function(data) {
            $("#onQuery").children().remove();
            $.each(data, function(index, item) {
                $("#onQuery").append($("<li>")
                    .text(item.title + ' ' + item.body
                ));
            });
        }
    });
}

$(function() {
    //$("body").append('<form action="<spring:url value=\'/search\' htmlEscape=\'true\'/>" method="get" class="navbar-search pull-left"><input type="text" placeholder="Search" class="search-query span2" name="q"></form>');

    $("body").append('<input id="searchQuery"/>');
    $("body").append('<button id="onSearch">Search Todo List</button>');
    $("body").append('<br/>');
    $("body").append('<br/>');

    $("body").append('<input id="title"/>');
    $("body").append('<input id="body"/>');
    $("body").append('<button id="submit">Add on Todo!</button>');
    $("body").append('<br/>');

    $("body").append('<h4>Search results:</h4>');
    $("body").append('<ul id="onQuery"></ul>');

    searchTodo();

    $("body").append('<h4>Todos Done:</h4>');
    $("body").append('<ul id="todosDone"></ul>');

    loadDoneTodos();

    $("body").append('<h4>Todos Not Done:</h4>');
    $("body").append('<ul id="todosNotDone"></ul>');

    loadNotDoneTodos();

    $("#submit").click(addTodo);
    $("#todo").keyup(function(key) {
        if (key.which == 13) {
            addTodo();
        }
    });

    $("#onSearch").click(searchTodo);

    $("#update").click(updateTodo);

    $("#delete").click(deleteTodo);

});