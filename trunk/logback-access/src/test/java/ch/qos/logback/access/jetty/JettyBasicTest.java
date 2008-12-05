package ch.qos.logback.access.jetty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.qos.logback.access.spi.AccessEvent;
import ch.qos.logback.access.spi.Util;
import ch.qos.logback.access.testUtil.NotifyingListAppender;
import ch.qos.logback.core.testUtil.RandomUtil;

public class JettyBasicTest {

  static RequestLogImpl REQYEST_LOG_IMPL;
  static JettyFixture JETTY_FIXTURE;

  static int RANDOM_SERVER_PORT = RandomUtil.getRandomServerPort();
  
  @BeforeClass
  static public void startServer() throws Exception {
    // System.out.println("*** JettyBasicTest.startServer called");
    REQYEST_LOG_IMPL = new RequestLogImpl();
    JETTY_FIXTURE = new JettyFixture(REQYEST_LOG_IMPL, RANDOM_SERVER_PORT);
    JETTY_FIXTURE.start();
  }

  @AfterClass
  static public void stopServer() throws Exception {
    // System.out.println("*** JettyBasicTest.stopServer called");
    if (JETTY_FIXTURE != null) {
      JETTY_FIXTURE.stop();
    }
  }

  @Test
  public void getRequest() throws Exception {
    URL url = new URL("http://localhost:" + RANDOM_SERVER_PORT + "/");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setDoInput(true);

    String result = Util.readToString(connection.getInputStream());

    assertEquals("hello world", result);

    NotifyingListAppender listAppender = (NotifyingListAppender) REQYEST_LOG_IMPL
        .getAppender("list");
    listAppender.list.clear();
  }

  @Test
  public void eventGoesToAppenders() throws Exception {
    URL url = new URL(JETTY_FIXTURE.getUrl());
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setDoInput(true);

    String result = Util.readToString(connection.getInputStream());

    assertEquals("hello world", result);

    NotifyingListAppender listAppender = (NotifyingListAppender) REQYEST_LOG_IMPL
        .getAppender("list");
    synchronized (listAppender) {
      listAppender.wait(100);
    }

    assertTrue(listAppender.list.size() > 0);
    AccessEvent event = (AccessEvent) listAppender.list.get(0);
    assertEquals("127.0.0.1", event.getRemoteHost());
    assertEquals("localhost", event.getServerName());
    listAppender.list.clear();
  }

  @Test
  public void postContentConverter() throws Exception {
    URL url = new URL(JETTY_FIXTURE.getUrl());
    String msg = "test message";

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    // this line is necessary to make the stream aware of when the message is
    // over.
    connection.setFixedLengthStreamingMode(msg.getBytes().length);
    ((HttpURLConnection) connection).setRequestMethod("POST");
    connection.setDoOutput(true);
    connection.setDoInput(true);
    connection.setUseCaches(false);
    connection.setRequestProperty("Content-Type", "text/plain");

    PrintWriter output = new PrintWriter(new OutputStreamWriter(connection
        .getOutputStream()));
    output.print(msg);
    output.flush();
    output.close();

    // StatusPrinter.print(requestLogImpl.getStatusManager());

    NotifyingListAppender listAppender = (NotifyingListAppender) REQYEST_LOG_IMPL
        .getAppender("list");

    synchronized (listAppender) {
      listAppender.wait(100);
    }

    @SuppressWarnings("unused")
    AccessEvent event = (AccessEvent) listAppender.list.get(0);

    // we should test the contents of the requests
    // assertEquals(msg, event.getRequestContent());
  }
}