package com.opnfi.risk.util;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.mongo.HashSaltStyle;
import io.vertx.ext.auth.mongo.MongoAuth;
import io.vertx.ext.mongo.MongoClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserManagerVerticle extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(UserManagerVerticle.class);
    private static final String DEFAULT_DB_NAME = "OpnFi-Risk";
    private static final String DEFAULT_CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DEFAULT_USER_COLLECTION_NAME = "user";
    private static final String DEFAULT_SALT = "OpnFiRisk";
    private MongoClient mongo;

    @Override
    public void start(Future<Void> fut) throws Exception {
        LOG.info("Starting {} with configuration: {}", UserManagerVerticle.class.getSimpleName(), config().encodePrettily());
        Future<String> chainFuture = Future.future();

        Future<Void> connectDbFuture = Future.future();
        connectDb(connectDbFuture.completer());

        connectDbFuture.compose(v -> {
            LOG.info("Connected to MongoDB");
            Future<CompositeFuture> initDbFuture = Future.future();
            initDb(initDbFuture.completer());
            return initDbFuture;
        }).compose(v -> {
            LOG.info("Initialized MongoDB");
            Future<String> executeCommandFuture = Future.future();
            System.out.println("Step A");
            executeCommand(executeCommandFuture.completer());
            System.out.println("Step B");
            return executeCommandFuture;
        }).compose(v -> {
            LOG.info("Command executed");
            System.out.println("Step BA");
            chainFuture.complete();
            System.out.println("Step BB");
        }, chainFuture);

        chainFuture.setHandler(ar -> {
            if (ar.succeeded()) {
                fut.complete();
            } else {
                LOG.error("Unable to deploy {}", UserManagerVerticle.class.getSimpleName());
                fut.fail(chainFuture.cause());
            }
            vertx.close();
        });
    }

    private void connectDb(Handler<AsyncResult<Void>> completer) {
        JsonObject config = new JsonObject();
        config.put("db_name", config().getJsonObject("http").getJsonObject("auth").getString("db_name", UserManagerVerticle.DEFAULT_DB_NAME));
        config.put("useObjectId", true);
        config.put("connection_string", config().getJsonObject("http").getJsonObject("auth").getString("connection_string", UserManagerVerticle.DEFAULT_CONNECTION_STRING));

        mongo = MongoClient.createShared(vertx, config);
        completer.handle(Future.succeededFuture());
    }

    private void initDb(Handler<AsyncResult<CompositeFuture>> completer) {
        System.out.println("Step J");
        mongo.getCollections(ar -> {
            if (ar.succeeded()) {
                System.out.println("Step JJ");
                if (!ar.result().contains(UserManagerVerticle.DEFAULT_USER_COLLECTION_NAME)) {
                    System.out.println("Step JJJ");
                    List<Future> futures = new ArrayList<>();
                    Future<Void> createCollectionFuture = Future.future();
                    mongo.createCollection(UserManagerVerticle.DEFAULT_USER_COLLECTION_NAME, createCollectionFuture.completer());
                    JsonArray indexes = new JsonArray();
                    JsonObject key = new JsonObject().put("username", 1);
                    indexes.add(new JsonObject().put("key", key).put("name", "username_index").put("unique", true));
                    JsonObject command = new JsonObject()
                            .put("createIndexes", UserManagerVerticle.DEFAULT_USER_COLLECTION_NAME)
                            .put("indexes", indexes);
                    Future<JsonObject> createIndexFuture = Future.future();
                    mongo.runCommand("createIndexes", command, createIndexFuture.completer());

                    CompositeFuture.all(futures).setHandler(completer);
                } else {
                    System.out.println("Step JJ*");
                    completer.handle(Future.succeededFuture());
                }
            } else {
                System.out.println("Step J*");
                LOG.error("Failed to get collection list", ar.cause());
                completer.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    private void executeCommand(Handler<AsyncResult<String>> completer) {
        String command = System.getProperty("cmd", "");
        switch (command.toLowerCase()) {
            case "insert":
                this.executeInsert(completer);
                break;
            case "delete":
                this.executeDelete(completer);
                break;
            case "list":
                this.executeList(completer);
                break;
            default:
                completer.handle(Future.failedFuture("Command not provided"));
                break;
        }
    }

    private void executeInsert(Handler<AsyncResult<String>> completer) {
        JsonObject authProperties = new JsonObject();
        MongoAuth authProvider = MongoAuth.create(mongo, authProperties);
        authProvider.getHashStrategy().setSaltStyle(HashSaltStyle.EXTERNAL);
        authProvider.getHashStrategy().setExternalSalt(config().getJsonObject("http").getJsonObject("auth").getString("salt", UserManagerVerticle.DEFAULT_SALT));
        String userName = System.getProperty("userName");
        String userPassword = System.getProperty("userPassword");
        if (userName == null || userName.isEmpty()) {
            completer.handle(Future.failedFuture("User name not provided"));
            return;
        }
        if (userPassword == null || userPassword.isEmpty()) {
            completer.handle(Future.failedFuture("User password not provided"));
            return;
        }
        System.out.println("Step 2");
        authProvider.insertUser(userName, userPassword, Collections.emptyList(), Collections.emptyList(), res -> {
            if (res.succeeded())
            {
                System.out.println("Step 21");
                completer.handle(Future.succeededFuture());
            }
            else
            {
                System.out.println("Step 22");
                completer.handle(Future.failedFuture(res.cause()));
            }
        });
        System.out.println("Step 3");
    }

    private void executeDelete(Handler<AsyncResult<String>> completer) {
        String userName = System.getProperty("userName");
        if (userName == null || userName.isEmpty()) {
            completer.handle(Future.failedFuture("User name not provided"));
            return;
        }
        JsonObject query = new JsonObject().put("username", userName);
        System.out.println("Step *");
        mongo.removeDocument(UserManagerVerticle.DEFAULT_USER_COLLECTION_NAME, query, ar -> {
            if (ar.succeeded()) {
                System.out.println("Step *");
                LOG.info("Record for user {} removed from the database", userName);
                completer.handle(Future.succeededFuture());
            } else {
                System.out.println("Step *#");
                completer.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    private void executeList(Handler<AsyncResult<String>> completer) {
        JsonObject query = new JsonObject();
        mongo.find(UserManagerVerticle.DEFAULT_USER_COLLECTION_NAME, query, ar -> {
            LOG.info("Users records stored in the database:");
            if (ar.succeeded()) {
                ar.result().forEach(json -> System.out.println(json.encodePrettily()));
                completer.handle(Future.succeededFuture());
            } else {
                completer.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    @Override
    public void stop() throws Exception {
        LOG.info("{} is being stopped", UserManagerVerticle.class.getSimpleName());
        this.mongo.close();
    }

}
