package info.jdavid.server.stats

import okhttp3.Headers
import groovy.transform.CompileStatic
import info.jdavid.ok.rest.RestServer
import info.jdavid.ok.server.Dispatcher
import info.jdavid.ok.server.HttpServer
import info.jdavid.ok.server.MediaTypes
import info.jdavid.ok.server.Response
import info.jdavid.ok.server.StatusLines
import okio.Buffer
import okio.Okio
import okio.Source
import okio.Timeout

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@CompileStatic
public abstract class AbstractStatServer extends RestServer {

  /**
   * Returns the directory where the stats are saved.<br>
   * Stats are saved in one file for each day in that directory.<br>
   * @return the directory or null to keep the default (the current directory).
   */
  protected abstract File getStatsFileDirectory()

  /**
   * Returns the list of statistical group (keys).<br>
   * You can separate statistics into groups. Each group has its own key (String). The key is used as the
   * file name extension for the stats files.<br>
   * @return the list of group keys, or null to keep the default (one group with the key: "stats").
   */
  protected abstract List<String> getGroupKeys()

  /**
   * Returns the ordered list of stat names for the group with the specified key.<br>
   * @param key the group key.
   * @return the list of column names.
   */
  protected abstract List<String> getStatNames(final String key)

  /**
   * Returns the looping period (time between each loop execution) for the group with the specified key.<br>
   * @param key the group key.
   * @return the event period in seconds, or -1 if you want to use the default (60 secs).
   */
  protected abstract int getEventPeriod(final String key)

  /**
   * Returns the list of stat values at the current time for the group with the specified key.
   * The order of the list should be consistent with the order of the list of stat names.
   * @param key the group key.
   * @return the stat values.
   */
  protected abstract List<? extends Number> getStatValues(final String key)


  private static final DateFormat format = new SimpleDateFormat('yyyyMMdd')
  private static final String keyRegex = '[0-9a-zA-Z]+'

  private static File getDefaultFileDirectory() { new File('.').canonicalFile.parentFile }

  private Map<String, StatsEventSource> eventSources = [:]

  @Override protected Dispatcher createDefaultDispatcher() { new FixedThreadPoolDispatcher() }

  @Override protected void setup() {
    super.setup()
    final File dir = (getStatsFileDirectory() ?: getDefaultFileDirectory()).with {
      if ((!exists() && !mkdirs()) || !isDirectory()) throw new IllegalArgumentException()
      return it
    }
    final List<String> keys = (getGroupKeys() ?: ['stats']).findAll {
      if (it ==~ keyRegex) return true
      throw new IllegalArgumentException("Invalid key: $it")
    }
    eventSources.putAll(keys.collectEntries { [ (it): new StatsEventSource(this, dir, it) ] })

    get('/groups[.](json|csv)') { final Buffer body, final Headers headers,
                                         final List<String> captures ->
      final Response.Builder builder = new Response.Builder().
        statusLine(StatusLines.OK).
        header("Access-Control-Allow-Origin", "*").
        header("Access-Control-Allow-Methods", "GET").
        header("Access-Control-Allow-Headers", "Content-Type, Accept")
      String format = captures[0]
      if (format == 'json') {
        builder.body(new JSONResponseBody(getGroupKeys()))
      }
      else if (format == 'csv') {
        builder.body(new CSVResponseBody(getGroupKeys().join(',')))
      }
      else {
        throw new RuntimeException()
      }
      return builder.build()
    }
    get("/(${keyRegex})/sse") { final Buffer body, final Headers headers,
                                       final List<String> captures ->
      final String key = captures[0]
      return keys.contains(key) ?
             new Response.SSE(Math.ceil(getEventPeriod(key) * 0.65) as int,
                              eventSources[key]) as Response :
             new Response.Builder().statusLine(StatusLines.NOT_FOUND).noBody().build()
    }
    get("/(${keyRegex})/names[.](json|csv)") { final Buffer body,
                                                      final Headers headers,
                                                      final List<String> captures ->
      final String key = captures[0]
      if (!keys.contains(key)) {
        return new Response.Builder().statusLine(StatusLines.NOT_FOUND).noBody().build()
      }
      final Response.Builder builder = new Response.Builder().
        statusLine(StatusLines.OK).
        header("Access-Control-Allow-Origin", "*").
        header("Access-Control-Allow-Methods", "GET").
        header("Access-Control-Allow-Headers", "Content-Type, Accept")
      String format = captures[1]
      if (format == 'json') {
        builder.body(new JSONResponseBody(getStatNames(key)))
      }
      else if (format == 'csv') {
        builder.body(new CSVResponseBody(getStatNames(key).join(',')))
      }
      else {
        throw new RuntimeException()
      }
      return builder.build()
    }
    get("/(${keyRegex})/values[.](json|csv)") { final Buffer body,
                                                       final Headers headers,
                                                       final List<String> captures ->
      final String key = captures[0]
      if (!keys.contains(key)) {
        return new Response.Builder().statusLine(StatusLines.NOT_FOUND).noBody().build()
      }
      final Calendar cal = Calendar.getInstance()
      final File f2 = new File(dir, "${format.format(cal.getTime())}.${key}")
      cal.add(Calendar.DATE, -1)
      final File f1 = new File(dir, "${format.format(cal.getTime())}.${key}")
      final Response.Builder builder = new Response.Builder().
        statusLine(StatusLines.OK).
        header("Access-Control-Allow-Origin", "*").
        header("Access-Control-Allow-Methods", "GET").
        header("Access-Control-Allow-Headers", "Content-Type, Accept")
      String format = captures[1]
      if (format == 'json') {
        final List<String> names = getStatNames(key)
        final int count = names.size()
        final List list = []
        final Closure processLine = {
          final String[] split = (it as String).split(',')
          if (split.size() != count) return false
          Map map = [:]
          for (int i = 0; i < count; ++i) {
            String s = split[i]
            if (s.indexOf('.') == -1) {
              map[(names.get(i))] = s as long
            }
            else {
              map[(names.get(i))] = s as float
            }
          }
          return list.add(map)
        }
        if (f1.canRead()) f1.eachLine(processLine)
        if (f2.canRead()) f2.eachLine(processLine)
        return builder.contentType(MediaTypes.JSON).body(new JSONResponseBody(list)).build()
      }
      else if (format == 'csv') {
        if (f1.canRead()) {
          final long len1 = f1.length()
          final Source source1 = Okio.source(f1)
          final long len2 = f2.length()
          final Source source2 = Okio.source(f2)
          final Source combined = Okio.buffer(new CombinedSource(source1, source2))
          return builder.contentType(MediaTypes.CSV).body(new CSVResponseBody(len1 + len2, combined)).build()
        }
        else {
          final long len = f2.length()
          final Source source = Okio.buffer(Okio.source(f2))
          return builder.contentType(MediaTypes.CSV).body(new CSVResponseBody(len, source)).build()
        }
      }
      else {
        throw new RuntimeException()
      }
    }
  }

  @Override public final void shutdown() {
    eventSources.each { it.value.stopped.set(true) }
    super.shutdown()
  }

  private static class StatsEventSource extends Response.SSE.DefaultEventSource {
    public final AtomicBoolean stopped = new AtomicBoolean(false)
    public StatsEventSource(final AbstractStatServer server, final File dir, final String key) {
      final long p = server.getEventPeriod(key)
      final long period = p > 0 ? p : 60L
      Thread.start {
        while (!stopped.get()) {
          long t = System.currentTimeMillis()
          final List<Number> values = server.getStatValues(key)
          if (values != null) values.join(',').with {
            write(it)
            final File f = new File(dir, "${format.format(new Date())}.${key}")
            if (!f.exists() && !f.createNewFile()) {
              println("Cannot create file: " + f.canonicalPath)
              println("Please fix the permissions")
            }
            f.append(it)
            f.append('\n')
          }
          final wait = period * 1000 - (System.currentTimeMillis() - t)
          if (wait > 0) Thread.sleep(wait)
        }
      }
    }
  }

  private static class CombinedSource implements Source {
    private final Source source1
    private final Source source2
    private first = true
    public CombinedSource(final Source source1, final Source source2) {
      this.source1 = source1
      this.source2 = source2
    }
    @Override long read(final Buffer sink, final long byteCount) throws IOException {
      if (first) {
        final long read = source1.read(sink, byteCount)
        if (read == -1L) {
          first = false
          return source2.read(sink, byteCount)
        }
        else {
          return read
        }
      }
      else {
        return source2.read(sink, byteCount)
      }
    }
    @Override Timeout timeout() { Timeout.NONE }
    @Override void close() throws IOException {
      try { source1.close() } finally { source2.close() }
    }
  }

  private static class FixedThreadPoolDispatcher implements Dispatcher {
    private ExecutorService mExecutors = null;
    @Override public void start() { mExecutors = Executors.newFixedThreadPool(1); }
    @SuppressWarnings("UnnecessaryQualifiedReference")
    @Override public void dispatch(final HttpServer.Request request) {
      mExecutors.execute(new Runnable() { @Override public void run() { request.serve(); } });
    }
    @Override public void shutdown() {
      try {
        mExecutors.awaitTermination(5, TimeUnit.SECONDS);
      }
      catch (final InterruptedException ignore) {}
      mExecutors.shutdownNow();
    }
  }

}
