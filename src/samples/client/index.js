(function() {

  angular.
    module('app', []).
    directive(
      'chart',
      function() {
        return {
          restrict: 'E',
          template: '<div></div>',
          transclude: true,
          controller: ['$element', '$http', Chart],
          controllerAs: 'chart'
        }
      }
  );

  function Chart($element, $http) {
    var seriesOptions = [
      { name: 'cpu', data: [] }
    ];
    var conf = theme();
    conf.series = seriesOptions;
    conf.chart.renderTo = $element.find('div')[0];

    conf.chart.events = {
      load: loadData
    };

    conf.rangeSelector.buttons = [
      {
        count: 5,
        type: 'minute',
        text: '5M'
      },
      {
        count: 30,
        type: 'minute',
        text: '30M'
      },
      {
        count: 1,
        type: 'hour',
        text: '1H'
      },
      {
        count: 3,
        type: 'hour',
        text: '3H'
      },
      {
        count: 1,
        type: 'day',
        text: '1D'
      },
      {
        type: 'all',
        text: 'All'
      }
    ];
    conf.rangeSelector.inputEnabled = false;
    conf.rangeSelector.selected = 0;

    var chart = new Highcharts.StockChart(conf);

    function loadData() {
      var promise = $http.get('http://localhost:8080/values');
      promise.then(
        function(response) {
          console.log('ok');
          var series1 = chart.series[0];
          response.data.forEach(function(o) {
            var x = o['millis'];
            var y = o['cpu'];
            console.log('load: ', [x,y]);
            series1.addPoint([x,y]);
          });
        },
        function(err) { console.log(err); }
      );
    }

    var eventSource = new EventSource('http://localhost:8080/sse');
    var onMessage = function(e) {
      var split = e.data.split(',');
      var x = parseInt(split[0]);
      var y = parseFloat(split[1]);
      console.log('sse: ', [x, y]);
      chart.series[0].addPoint([x, y], true);
    };
    var onOpen = function() {
      eventSource.addEventListener('message', onMessage);
    };
    eventSource.addEventListener('open', onOpen);
    eventSource.addEventListener('error', function(err) {
      console.log(err);
      eventSource.removeEventListener('open', onOpen);
      eventSource.removeEventListener('message', onMessage);
      eventSource.close();
    })
  }

  function theme() {
    return {
      chart: {
        animation: false,
        backgroundColor: '#3e3e40',
        plotBorderColor: '#606063'
      },
      colors: [
        "#2b908f", "#f45b5b", "#90ee7e", "#7798bf", "#aaeeee", "#ff0066", "#eeaaee",
        "#55bf3b", "#df5353", "#7798bf", "#aaeeee"
      ],
      title: {
      style: {
        color: '#e0e0e3'
      }
    },
      subtitle: {
        style: {
          color: '#e0e0e3'
        }
      },
      xAxis: {
        labels: {
          style: {
            color: '#e0e0e3'
          }
        },
        gridLineColor: '#707073',
          lineColor: '#707073',
          minorGridLineColor: '#505053',
          tickColor: '#707073',
          title: {
          style: {
            color: '#a0a0a3'
          }
        }
      },
      yAxis: {
        labels: {
          style: {
            color: '#e0e0e3'
          }
        },
        gridLineColor: '#707073',
          lineColor: '#707073',
          minorGridLineColor: '#505053',
          tickColor: '#707073',
          tickWidth: 1,
          plotLines: [{
          value: 0,
          width: 2,
          color: '#c0c0c0'
        }],
          title: {
          style: {
            color: '#a0a0a3'
          }
        }
      },
      plotOptions: {
        series: {
          animation: false,
          dataLabels: {
            color: '#b0b0b3'
          },
          marker: {
            lineColor: '#333'
          },
          boxplot: {
            fillColor: '#505053'
          },
          errorbar: {
            color: '#fff'
          }
        }
      },
      legend: {
        itemStyle: {
          color: '#e0e0e3'
        },
        itemHoverStyle: {
          color: '#fff'
        },
        itemHiddenStyle: {
          color: '#606063'
        }
      },
      tooltip: {
        backgroundColor: 'rgba(0,0,0.85)',
          style: {
          color: '#f0f0f0'
        }
      },
      labels: {
        style: {
          color: '#707073'
        }
      },
      drilldown: {
        activeAxisLabelStyle: {
          color: '#f0f0f3'
        },
        activeDataLabelStyle: {
          color: '#f0f0f3'
        }
      },
      navigation: {
        buttonOptions: {
          symbolStroke: '#ddd',
            theme: {
            fill: '#505053'
          }
        }
      },
      rangeSelector: {
        buttonTheme: {
          fill: '#505053',
            stroke: '#000',
            style: {
            color: '#ccc'
          },
          states: {
            hover: {
              fill: '#707073',
                stroke: '#000',
                style: {
                color: '#fff'
              }
            },
            select: {
              fill: '#000003',
                stroke: '#000',
                style: {
                color: '#fff'
              }
            }
          }
        },
        inputBoxBorderColor: '#505053',
          inputStyle: {
          backgroundColor: '#333',
            color: '#c0c0c0'
        }
      },
      navigator: {
        handles: {
          backgroundColor: '#666',
            borderColor: '#aaa'
        },
        outlineColor: '#ccc',
          maskFill: 'rgba(255, 255, 255, 0.1)',
          series: {
          color: '#7798bf',
            lineColor: '#a6c7ed'
        },
        xAxis: {
          labels: {
            style: {
              color: '#fff'
            }
          },
          gridLineColor: '#505053'
        }
      },
      scrollbar: {
        barBackgroundColor: '#808083',
          barBorderColor: '#808083',
          buttonArrowColor: '#ccc',
          buttonBackgroundColor: '#606063',
          buttonBorderColor: '#606063',
          rifleColor: '#fff',
          trackBackgroundColor: '#404043',
          trackBorderColor: '#404043'
      },
      legendBackgroundColor: 'rgba(0, 0, 0, 0.5)',
      background2: '#505053',
      dataLabelsColor: '#b0b0b3',
      textColor: '#c0c0c0',
      constrastTextColor: '#f0f0f3',
      maskColor: 'rgba(255, 255, 255, 0.3)'
    }
  }

})();
