/**
 * Created by jakub on 20/10/2016.
 */

(function() {
    'use strict';

    angular.module('dave').controller('MarginShortfallSurplusLatestController', MarginShortfallSurplusLatestController);

    function MarginShortfallSurplusLatestController($scope, $routeParams, $http, $interval, $filter) {
        var vm = this;
        vm.initialLoad= true;
        vm.pageSize = 20;
        vm.recordCount = 0;
        vm.viewWindow = [];
        vm.updateViewWindow = updateViewWindow;
        vm.errorMessage = "";
        vm.filter = filter;
        vm.filterQuery = "";
        vm.sortRecords = sortRecords;
        vm.route = {
            "clearer": "*",
            "pool": "*",
            "member": "*",
            "clearingCcy": "*"
        };

        processRouting();

        var currentPage = 1;
        var refresh = $interval(loadData, 60000);
        var sourceData = [];
        var ordering = ["clearer", "pool", "member", "clearingCcy", "ccy"];
        var restQueryUrl = '/api/v1.0/mss/latest/' + vm.route.clearer + '/' + vm.route.pool + '/' + vm.route.member + '/' + vm.route.clearingCcy;

        loadData();

        ////////////////////

        function loadData(){
            $http.get(restQueryUrl).success(function(data) {
                processData(data);
                vm.errorMessage = "";
                vm.initialLoad = false;
            }).error(function(data, status, headers, config) {
                vm.errorMessage = "Server returned status " + status;
                vm.initialLoad = false;
            });
        }

        function processRouting()
        {
            if ($routeParams.clearer) { vm.route.clearer = $routeParams.clearer } else { vm.route.clearer = "*" }
            if ($routeParams.pool) { vm.route.pool = $routeParams.pool } else { vm.route.pool = "*" }
            if ($routeParams.member) { vm.route.member = $routeParams.member } else { vm.route.member = "*" }
            if ($routeParams.clearingCcy) { vm.route.clearingCcy = $routeParams.clearingCcy } else { vm.route.clearingCcy = "*" }
        }

        function processData(data) {
            var index;

            for (index = 0; index < data.length; ++index) {
                data[index].functionalKey = data[index].clearer + '-' + data[index].pool + '-' + data[index].member + '-' + data[index].clearingCcy + '-' + data[index].ccy;
            }

            sourceData = data;
            filter();
            updateViewWindow(currentPage);
        }

        function sortRecords(column) {
            if (ordering[0] == column)
            {
                ordering = ["-" + column, "clearer", "pool", "member", "clearingCcy", "ccy"];
            }
            else {
                ordering = [column, "clearer", "pool", "member", "clearingCcy", "ccy"];
            }

            updateViewWindow(currentPage);
        }

        function updateViewWindow(page) {
            currentPage = page;
            vm.viewWindow = $filter('orderBy')($filter('spacedFilter')(sourceData, vm.filterQuery), ordering).slice(currentPage*vm.pageSize-vm.pageSize, currentPage*vm.pageSize);
        }

        function filter() {
            vm.recordCount = $filter('spacedFilter')(sourceData, vm.filterQuery).length;
            updateViewWindow(currentPage);
        }

        $scope.$on("$destroy", function() {
            if (refresh != null) {
                $interval.cancel(refresh);
            }
        });
    };
})();