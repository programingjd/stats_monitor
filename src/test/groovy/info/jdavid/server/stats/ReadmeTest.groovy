package info.jdavid.server.stats

import groovy.transform.CompileStatic
import org.junit.Test


class ReadmeTest {

  @Test @CompileStatic
  public void testReadme() {
    final Map<String, ? extends Map<String, ?>> groups = [
      group1: [
        names: [ 'millis', 'prop1A' ],
        period: 10,
        values: { ->
          final long t = System.currentTimeMillis()
          final value = Math.random()
          return [ t as Number, value as Number ]
        }
      ],
      group2: [
        names: [ 'millis', 'prop2A', 'prop2B' ],
        period: 30,
        values: { ->
          final long t = System.currentTimeMillis()
          final double valueA = Math.sin(t / 10)
          final double valueB = Math.log1p(t % 10)
          return [t as Number, valueA as Number, valueB as Number]
        }
      ]
    ]
    new AbstractStatServer() {
      @Override protected File getStatsFileDirectory() { null }
      @Override protected List<String> getGroupKeys() { groups.collect { it.key } }
      @Override protected int getEventPeriod(final String key) { groups[key]['period'] as int }
      @Override protected List<String> getStatNames(final String key) { groups[key]['names'] as List<String> }
      @Override protected List<? extends Number> getStatValues(final String key) {
        (groups[key]['values'] as Closure<List<? extends Number>>)()
      }
    }
  }

}
