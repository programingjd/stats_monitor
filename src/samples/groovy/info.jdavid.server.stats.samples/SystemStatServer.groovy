package info.jdavid.server.stats.samples

import groovy.transform.CompileStatic
import info.jdavid.server.stats.AbstractStatServer

import java.lang.management.ManagementFactory


@CompileStatic
public class SystemStatServer extends AbstractStatServer {

  @Override protected File getStatsFileDirectory() { return null }

  @Override protected String getStatsFileExtension() { return null }

  @Override protected int getEventPeriod() { return 10 }

  @Override protected List<String> getStatNames() {
    return [ 'millis', 'cpu' ]
  }

  @Override
  protected List<? extends Number> getStatValues() {
    final long t = System.currentTimeMillis()
    def os = ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class)
    return [ t as Number, os.systemCpuLoad as Number ]
  }

  private static SystemStatServer instance = new SystemStatServer()

  public static startServer() {
    instance.server.start()
  }

  public static stopServer() {
    instance.server.shutdown()
  }

}
