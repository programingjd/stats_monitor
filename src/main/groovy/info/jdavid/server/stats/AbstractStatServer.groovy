package info.jdavid.server.stats

import com.squareup.okhttp.Headers
import groovy.transform.CompileStatic
import info.jdavid.ok.rest.RestServer
import info.jdavid.ok.server.Dispatcher
import info.jdavid.ok.server.HttpServer
import info.jdavid.ok.server.MediaTypes
import info.jdavid.ok.server.Response
import info.jdavid.ok.server.StatusLines
import okio.Buffer

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@CompileStatic
public abstract class AbstractStatServer {

  /**
   * Returns the directory where the stats are saved.
   * Stats are saved in one file for each day in that directory.
   * This is called before the constructor, so it should not use any variable from the implementing class.
   * @return the directory or null to keep the default (the current directory).
   */
  protected abstract File getStatsFileDirectory()

  /**
   * Returns the extension to use for the stats files.
   * This is needed so that multiple stats can be saved in the same directory.
   * @return the extension (without the '.'), or null to keep the default (stats).
   */
  protected abstract String getStatsFileExtension()

  /**
   * Returns the ordered list of stat names.
   * This is called before the constructor, so it should not use any variable from the implementing class.
   * @return the list of column names.
   */
  protected abstract List<String> getStatNames()

  /**
   * Returns the looping period (time between each loop execution).
   * This is called before the constructor, so it should not use any variable from the implementing class.
   * @return the event period in seconds.
   */
  protected abstract int getEventPeriod()

  /**
   * Returns the list of stat values at the current time.
   * The order of the list should be consistent with the order of the list of stat names.
   * @return the stat values.
   */
  protected abstract List<? extends Number> getStatValues()


  private static class FixedThreadPoolDispatcher implements Dispatcher {
    private ExecutorService mExecutors = null;
    @Override public void start() { mExecutors = Executors.newFixedThreadPool(1); }
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

  private static final DateFormat format = new SimpleDateFormat('yyyyMMdd')

  private static class StatsEventSource extends Response.SSE.DefaultEventSource {
    public final AtomicBoolean stopped = new AtomicBoolean(false);
    public StatsEventSource(final AbstractStatServer server) {
      Thread.start {
        while (!stopped.get()) {
          long t = System.currentTimeMillis()
          List<Number> values = server.getStatValues()
          if (values != null) values.join(',').with {
            write(it)
            final File f = new File(server.dir, "${format.format(new Date())}.${server.ext}")
            if (!f.exists() && !f.createNewFile()) {
              println("Cannot create file: " + f.canonicalPath)
              println("Please fix the permissions")
            }
            f.append(it)
            f.append('\n')
          }
          final wait = server.period * 1000 - (System.currentTimeMillis() - t)
          if (wait > 0) Thread.sleep(wait)
        }
      }
    }
  }

  private final StatsEventSource eventSource = new StatsEventSource(this)

  protected final RestServer server = new RestServer() {
    @Override void shutdown() {
      eventSource.stopped.set(true)
      super.shutdown()
    }
  }.with {
    port(8080).dispatcher(new FixedThreadPoolDispatcher())
    return it
  }

  private static File getDefaultFileDirectory() {
    return new File('.').canonicalFile.parentFile
  }

  private final File dir = (getStatsFileDirectory() ?: getDefaultFileDirectory()).with {
    if (!exists() && !dir.mkdirs()) throw new IllegalArgumentException()
    it
  }
  private final String ext = getStatsFileExtension() ?: 'stats'
  private final String names = getStatNames().join(',')

  private final int period = getEventPeriod()

  public AbstractStatServer() {
    if (dir == null || !dir.isDirectory()) throw new IllegalArgumentException()
    server.get('/sse') { final Buffer body, final Headers headers, final List<String> captures ->
      return new Response.SSE(5, eventSource) as Response;
    }
    server.get('/names') { final Buffer body, final Headers headers, final List<String> captures ->
      return new Response.Builder().
        statusLine(StatusLines.OK).
        contentType(MediaTypes.CSV).
        body(names).
        build()
    }
    server.get('/values') { final Buffer body, final Headers headers, final List<String> captures ->
      Calendar cal = Calendar.getInstance()
      final File f2 = new File(dir, "${format.format(cal.getTime())}.${ext}")
      cal.add(Calendar.DATE, -1)
      final File f1 = new File(dir, "${format.format(cal.getTime())}.${ext}")
      final List<String> names = getStatNames()
      final int count = names.size()
      final List list = []
      final Closure processLine = {
        final String[] split = (it as String).split(',')
        if (split.size() != count) return false
        Map map = [:]
        for (int i=0; i<count; ++i) {
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
      return new Response.Builder().
        statusLine(StatusLines.OK).
        header("Access-Control-Allow-Origin", "*").
        header("Access-Control-Allow-Methods", "GET").
        header("Access-Control-Allow-Headers", "Content-Type, Accept").
        contentType(MediaTypes.JSON).
        body(new JSONResponseBody(list)).
        build()
    }
  }

}
