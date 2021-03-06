/**
 * Created by jakub on 20/10/2016.
 */

(function() {
    'use strict';

    angular.module('dave').controller('MarginShortfallSurplusHistoryController', MarginShortfallSurplusHistoryController);

    function MarginShortfallSurplusHistoryController($scope, $routeParams, $http, $interval, $filter, sortRecordsService, recordCountService, updateViewWindowService, showExtraInfoService) {
        BaseHistoryController.call(this, $scope, $http, $interval, sortRecordsService, recordCountService, updateViewWindowService, showExtraInfoService);
        var vm = this;
        vm.route = {
            "clearer": $routeParams.clearer,
            "pool": $routeParams.pool,
            "member": $routeParams.member,
            "clearingCcy": $routeParams.clearingCcy,
            "ccy": $routeParams.ccy
        };
        vm.defaultOrdering = ["-received"];
        vm.ordering = vm.defaultOrdering;
        vm.getTickFromRecord = getTickFromRecord;
        vm.getRestQueryUrl = getRestQueryUrl;
        vm.loadData();

        function getTickFromRecord(record) {
            var tick = {
                period: $filter('date')(record.received, "yyyy-MM-dd HH:mm:ss"),
                marginRequirement: record.marginRequirement,
                securityCollateral: record.securityCollateral,
                cashBalance: record.cashBalance,
                shortfallSurplus: record.shortfallSurplus,
                marginCall: record.marginCall            };
            return tick;
        }

        function getRestQueryUrl() {
            return '/api/v1.0/mss/history/' + vm.route.clearer + '/' + vm.route.pool + '/' + vm.route.member + '/' + vm.route.clearingCcy + '/' + vm.route.ccy;
        }
    };
})();
