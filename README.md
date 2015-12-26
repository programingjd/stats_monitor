![jcenter](https://img.shields.io/badge/_jcenter_-_1.2.0-6688ff.png?style=flat)

# stats_monitor
This module includes the following:
 - An abstract server implementation that generates statistical data and serves them over http, but also in
   real time with server side events.<br/>
   The server is implemented in groovy, using [okrest](https://github.com/programingjd/okrest).<br/>
   It saves the data in files. One file for each day of statistical data.

 - Example implementations of the server (the part that actually generates the statistical data).

 - An example implementation of a client that reads those data (both historical and real-time) and plots them
   on a chart.<br/>
   The client is a very simple [angular.js](https://angularjs.org) page.
   It uses [highchart/highstock](http://www.highcharts.com/products/highstock) to generate the chart.

## Download ##

The maven artifacts are on [Bintray](https://bintray.com/programingjd/maven/info.jdavid.stats.monitor/view)
and [jcenter](https://bintray.com/search?query=info.jdavid.stats.monitor).

[Download](https://bintray.com/artifact/download/programingjd/maven/info/jdavid/stats/monitor/stats_monitor/1.2.0/stats_monitor-1.2.0.jar) the latest jar.

__Maven__

Include [those settings](https://bintray.com/repo/downloadMavenRepoSettingsFile/downloadSettings?repoPath=%2Fbintray%2Fjcenter)
 to be able to resolve jcenter artifacts.
```
<dependency>
  <groupId>info.jdavid.stats.monitor</groupId>
  <artifactId>stats_monitor</artifactId>
  <version>1.2.0</version>
</dependency>
```
__Gradle__

Add jcenter to the list of maven repositories.
```
repositories {
  jcenter()
}
```
```
dependencies {
  compile 'info.jdavid.stats.monitor:stats_monitor:1.2.0'
}
```

## Usage ##

You specify what your server does by overriding the `handle` method.

Here's a very simple example:
  - For a **GET** request to **/ok**, we return a **200 OK** with the content `"ok"`.
  - For any other request, we return a **404 Not Found** with no content.

```java
new HttpServer() {
  @Override
  protected Response handle(final String method, final String path,
                            final Headers requestHeaders,
                            final Buffer requestBody) {
    final Response.Builder builder = new Response.Builder();
    if ("GET".equals(method) && "/ok".equals(path) {
      builder.statusLine(StatusLines.OK).body("ok");
    }
    else {
      builder.statusLine(StatusLines.NOT_FOUND).noBody();
    }
    return builder.build();
  }
};
```

To start the server, you simply call `start()`. It defaults to port 8080, but you can change that easily
with the `port(int)` method. It also defaults to all the ip addresses on the local machine, but you can also
change that easily with the `hostname(String)` method.


```java
new HttpServer() { ... }.hostname("localhost").port(80).start();
```

Requests are handled by a dispatcher. The default implementation uses a cached thread pool.
You can change the dispatcher with the `dispatcher(Dispatcher)` method.

Here's an example that sets a dispatcher with a single thread executor rather than a cached thread pool.

```java
final HttpServer server = new HttpServer() { ... }.dispatcher(
  new Dispatcher() {
    private ExecutorService mExecutors = null;
    @Override
    public void start() {
      mExecutors = Executors.newSingleThreadExecutor();
    }
    @Override
    public void dispatch(final HttpServer.Request request) {
      mExecutors.execute(new Runnable() {
        @Override public void run() { request.serve(); }
      });
    }
    @Override
    public void shutdown() {
      try {
        mExecutors.awaitTermination(5, TimeUnit.SECONDS);
      }
      catch (final InterruptedException ignore) {}
      mExecutors.shutdownNow();
    }
  }
);
```

You can find more examples in the ***samples*** directory.
These include examples for implementing Server Side Events (SSE).