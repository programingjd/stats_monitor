(function() {

  angular.
    module('app', []).
    controller('GroupCtrl', ['$http', '$q', '$scope', GroupCtrl]).
    directive(
      'chart',
      function() {
        return {
          restrict: 'E',
          template: '<div></div>',
          transclude: true,
          controller: ['$element', '$attrs', '$http', '$scope', Chart],
          controllerAs: 'chart'
        }
      }
  );

  function GroupCtrl($http, $q, $scope) {
    var promise = $http.get('http://localhost:8080/groups.json');
    promise.then(
      function(response) {
        var promises = [];
        var groups = [];
        var names = [];
        response.data.forEach(function(it) {
          names.push(it);
          promises.push($http.get('http://localhost:8080/' + it + '/names.json'));
        });
        $q.all(promises).then(function(values) {
          for (var i=0; i<values.length; ++i) {
            groups.push({ name: names[i], values: values[i].data });
            $scope.groups = groups;
            $scope.groupIndex1 = '0';
            $scope.groupIndex2 = '1';
          }
        });
      }
    );
  }

  function Chart($element, $attrs, $http, $scope) {
    var seriesOptions = [];
    $attrs.$observe('group', function() { loadData(); });
    //$scope.$watch('groupIndex', function() {
    //  loadData();
    //});
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

    var chart = new Highcharts.StockChart(conf); //new Highcharts.Chart(conf);

    var eventSource = undefined;

    function loadData() {
      if (eventSource) eventSource.close();
      var groupIndexStr = $attrs['group'];  // $scope.groupIndex;
      var groups = $scope.groups;
      if (groupIndexStr !== undefined && groups) {
        var groupIndex = parseInt(groupIndexStr);
        var group = groups[groupIndex].name;
        var names = groups[groupIndex].values;
        var series = names.slice();
        var times = series.shift();

        var promise = $http.get('http://localhost:8080/' + group + '/values.json');
        promise.then(
          function (response) {
            var arr = [];
            series.forEach(function() { arr.push([]); });
            response.data.forEach(function (o) {
              for (var i=0; i<series.length; ++i) {
                var key = series[i];
                arr[i].push([o[times], o[key]]);
              }
            });
            (function() {
              var n = chart.series.length;
              var i;
              var cur;
              var navigator;
              for (i=n-1; i>-1; --i) {
                cur = chart.series[i];
                if (cur.name.toLowerCase() == 'navigator') {
                  navigator = cur;
                }
                else {
                  cur.remove(false);
                }
              }
              for (i=0; i<series.length; ++i) {
                chart.addSeries({ name: series[i], data: arr[i]}, false);
              }
              if (navigator) navigator.setData(arr[0]);
              chart.redraw();
            }());

            eventSource = new EventSource('http://localhost:8080/' + group + '/sse');
            var onMessage = function (e) {
              var split = e.data.split(',');
              var x = parseInt(split[0]);
              var y = parseFloat(split[1]);
              console.log('sse: ', [x, y]);
              chart.series[0].addPoint([x, y], true);
            };
            var onOpen = function () {
              eventSource.addEventListener('message', onMessage);
            };
            eventSource.addEventListener('open', onOpen);
            eventSource.addEventListener('error', function (err) {
              console.log(err);
              // uncomment to prevent auto reconnects
              //eventSource.removeEventListener('open', onOpen);
              //eventSource.removeEventListener('message', onMessage);
              //eventSource.close();
            })
          },
          function (err) {
            console.log(err);
          }
        );
      }
    }
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
        },
        minRange: 60000,
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
