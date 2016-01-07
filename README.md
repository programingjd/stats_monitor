![jcenter](https://img.shields.io/badge/_jcenter_-_2.0.0-6688ff.png?style=flat)

# stats_monitor
This module includes the following:
 - An abstract server implementation that generates statistical data and serves them over http, but also in
   real time with server side events.<br/>
   The server is implemented in groovy, using [okrest](https://github.com/programingjd/okrest).<br/>
   It saves the data in files. One file for each day of statistical data.

 - An example implementation of the server (the part that actually generates the statistical data).

 - An example implementation of a client that reads those data (both historical and real-time) and plots them
   on a chart.<br/>
   The client is a very simple [angular.js](https://angularjs.org) page.
   It uses [highchart/highstock](http://www.highcharts.com/products/highstock) to generate the chart.

## Download ##

The maven artifacts are on [Bintray](https://bintray.com/programingjd/maven/info.jdavid.stats.monitor/view)
and [jcenter](https://bintray.com/search?query=info.jdavid.stats.monitor).

[Download](https://bintray.com/artifact/download/programingjd/maven/info/jdavid/stats/monitor/stats_monitor/2.0.0/stats_monitor-2.0.0.jar) the latest jar.

__Maven__

Include [those settings](https://bintray.com/repo/downloadMavenRepoSettingsFile/downloadSettings?repoPath=%2Fbintray%2Fjcenter)
 to be able to resolve jcenter artifacts.
```
<dependency>
  <groupId>info.jdavid.stats.monitor</groupId>
  <artifactId>stats_monitor</artifactId>
  <version>2.0.0</version>
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
  compile 'info.jdavid.stats.monitor:stats_monitor:2.0.0'
}
```

## Usage ##

You need to override the following methods:
  - `getStatValues(String)` to return the list of values for a record.

Here's an example with two groups:
  - "group1" with "prop1A", with records every 10 secs.
  - "group2" with "prop2A" and "prop2B", with records every 30 secs.


```groovy
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

  @Override
  protected File getStatsFileDirectory() { null }

  @Override
  protected List<String> getGroupKeys() { groups.collect { it.key } }

  @Override
  protected int getEventPeriod(final String key) {
   groups[key]['period'] as int
  }

  @Override
  protected List<String> getStatNames(final String key) {
    groups[key]['names'] as List<String>
  }

  @Override
  protected List<? extends Number> getStatValues(final String key) {
    (groups[key]['values'] as Closure<List<? extends Number>>)()
  }

}
```