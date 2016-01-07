package info.jdavid.server.stats.samples

import okhttp3.OkHttpClient
import okhttp3.Request.Builder
import okhttp3.Response
import groovy.transform.CompileStatic
import info.jdavid.server.stats.AbstractStatServer
import okio.Buffer
import okio.Sink
import okio.Timeout

import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit


@CompileStatic
public class StatServer extends AbstractStatServer {

  private static OkHttpClient client = new OkHttpClient.Builder().connectTimeout(2, TimeUnit.SECONDS).build()
  private static Map<String, okhttp3.Request> requests = [
    google: new Builder().url('http://google.com').build(),
    yahoo: new Builder().url('http://yahoo.com').build(),
    bing: new Builder().url('http://bing.com').build(),
  ]

  private static Map<String, ? extends Map<String, ?>> groups = [
    system: [
      names: [ 'millis', 'cpu' ],
      period: 1,
      values: { ->
        final long t = System.currentTimeMillis()
        //noinspection UnnecessaryQualifiedReference
        def os = ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class)
        return [t as Number, os.systemCpuLoad as Number]
      }
    ],
    net: [
      names: [ 'millis', 'google', 'yahoo', 'bing' ],
      period: 10,
      values: { ->
        final long t = System.currentTimeMillis()
        final Map map = [:]
        requests.each { key, value ->
          final long t0 = System.currentTimeMillis()
          try {
            final Response response = client.newCall(value).execute()
            if (response.isSuccessful()) {
              response.body().source().readAll(new DummySink())
              map[(key)] = System.currentTimeMillis() - t0
            }
            else {
              map[(key)] = 0
            }
          }
          catch (ignore) {
            map[(key)] = 0
          }
        }
        return [t as Number, map['google'] as Number, map['yahoo'] as Number, map['bing'] as Number]
      }
    ]
  ]

  @Override protected File getStatsFileDirectory() { null }

  @Override protected List<String> getGroupKeys() { groups.collect { it.key } }

  @Override protected int getEventPeriod(final String key) { groups[key]['period'] as int }

  @Override protected List<String> getStatNames(final String key) { groups[key]['names'] as List<String> }

  @Override protected List<? extends Number> getStatValues(final String key) {
    (groups[key]['values'] as Closure<List<? extends Number>>)()
  }

  private static class DummySink implements Sink {
    @Override public void write(final Buffer source, final long byteCount) {}
    @Override public void flush() {}
    @Override public Timeout timeout() { Timeout.NONE }
    @Override void close() {}
  }

  private static StatServer instance = new StatServer()

  public static startServer() {
    instance.start()
  }

  @SuppressWarnings("GroovyUnusedDeclaration")
  public static stopServer() {
    instance.shutdown()
  }

  public static void main(String[] args) {
    startServer()
  }

}
