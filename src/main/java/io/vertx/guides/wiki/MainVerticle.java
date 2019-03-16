package io.vertx.guides.wiki;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jingshouyan
 * #date 2019/3/15 21:34
 */
@Slf4j
public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    Future<Void> steps = prepareDatabase().compose(v -> startHttpServer());
    steps.setHandler(startFuture.completer());
  }


  private static final String SQL_CREATE_PAGES_TABLE = "create table if not exists Pages (Id integer identity primary key,Name varchar(255) unique, Content clob)";
  private static final String SQL_GET_PAGE = "select Id, Content from Pages where Name = ?";
  private static final String SQL_CREATE_PAGE = "insert into Pages values (NULL, ?, ?)";
  private static final String SQL_SAVE_PAGE = "update Pages set Content = ? where Id = ?";
  private static final String SQL_ALL_PAGES = "select Name from Pages";
  private static final String SQL_DELETE_PAGE = "delete from Pages where Id = ?";

  private JDBCClient dbClient;

  private Future<Void> prepareDatabase() {
    Future<Void> future = Future.future();
    dbClient = JDBCClient.createShared(vertx, new JsonObject()
      .put("url", "jdbc:hsqldb:file:db/wiki")
      .put("driver_class", "org.hsqldb.jdbcDriver")
      .put("max_pool_size", 30));
    dbClient.getConnection(ar -> {
      if (ar.failed()) {
        log.error("Could not open a database connection", ar.cause());
        future.fail(ar.cause());
      } else {
        SQLConnection connection = ar.result();
        connection.execute(SQL_CREATE_PAGES_TABLE, create -> {
          connection.close();
          if (create.failed()) {
            log.error("Database preparation error", create.cause());
            future.fail(create.cause());
          } else {
            future.complete();
          }
        });
      }
    });
    return future;
  }

  private FreeMarkerTemplateEngine templateEngine;
  private Future<Void> startHttpServer() {
    Future<Void> future = Future.future();
    HttpServer server = vertx.createHttpServer();
    Router router = Router.router(vertx);
    templateEngine = FreeMarkerTemplateEngine.create(vertx);
    server.requestHandler(router)
      .listen(18080,ar -> {
        if(ar.succeeded()) {
          log.info("HTTP server running on port 18080");
          future.complete();
        } else {
          log.error("Could not start a HTTP server", ar.cause());
          future.fail(ar.cause());
        }
      });

    return future;
  }
}
