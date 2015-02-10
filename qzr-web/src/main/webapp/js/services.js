'use strict';

/* Services */

var qzrServices = angular.module('qzr.services', ['ngResource']);

qzrServices.factory('qzrSvc', ['$http', function ($http) {

    // Server base URL
    var RESOURCES = '/api/quizzes/health/';

    // Utility function to retrieve the HREF for a HATEOAS-style response.
    function getKeyResourceHref(resource) {
        for (var i = 0; i < resource.links.length; i++) {
            if (resource.links[i].rel !== 'self')
                return resource.links[i].href;
        }
        return null;
    }

    // Create our service.
    var svc = {

        initialised: false,
        eventListeners: [],
        socket: null,
        stompClient: null,
        isConnecting: false,
        isConnected: false,
        restInterfaces: {},

        model: {
            questions: [],
            knowns: [],
            hrmax: null,
            events: []
        },

        connect: function () {
            // If for some reason this gets called twice, we try to avoid connecting again.
            if (svc.isConnecting || svc.isConnected) return;

            svc.isConnecting = true;

            // Assumes that service is on same server and that we have been consistent with ports.
            svc.socket = new SockJS('/drools');
            svc.stompClient = Stomp.over(svc.socket);
            svc.stompClient.connect({}, function (frame) {
                svc.isConnected = true;
                svc.isConnecting = false;
                console.log('Connected to /drools : ' + frame);
                svc.stompClient.subscribe('/queue/agendaevents/', svc.newEvent);
            });

        },

        newEvent: function (data) {
            var event = JSON.parse(data.body);
            svc.model.events.push(event);
        },

        disconnect: function () {
            svc.stompClient.disconnect();
            svc.setConnected(false);
            console.log("Disconnected Drools working memory event listener client");
        },

        loadResources: function () {

            // Load our REST interface links from the server.
            $http.get(RESOURCES).success(function (data) {
                for (var i = 0; i < data.links.length; i++) {
                    if (data.links[i].rel !== "self") {
                        svc.restInterfaces[data.links[i].rel] = data.links[i].href;
                    }
                }

                // Then load the associated REST interfaces
                console.log('Loading from ' + svc.restInterfaces['questions'] + '...');
                $http.get(svc.restInterfaces['questions']).success(function (questions) {
                    svc.model.questions = questions;

                    $http.get(svc.restInterfaces['hrmax']).success(function (hrmax) {
                        svc.model.hrmax = hrmax;
                        $http.get(svc.restInterfaces['knowns']).success(function (knowns) {
                            svc.model.knowns = knowns;
                        });
                    }).error(function () {
                        svc.model.hrmax = null;
                    });
                }).error(function () {
                    throw Error('Failed to retrieve ' + svc.restInterfaces['questions']);
                })
            }).error(function () {
                throw Error('Failed to retrieve ' + RESOURCES);
            });
        },


        init: function () {
            if (!svc.initialised) {
                svc.loadResources();
            }
            svc.connect();
        },

        answer: function (question, answerValue) {
            console.log("Answering : " + question.key + "=" + answerValue);

            var answer = {key: question.key, value: answerValue};
            $http.put(getKeyResourceHref(question), answer).success(svc.loadResources);
        },

        skip: function (question) {
            console.log("Skipping : " + question.key);

            $http.post(getKeyResourceHref(question)).success(svc.loadResources);
        },

        retractAnswer: function (question) {
            console.log("Retracting : " + question.key);

            $http.delete(getKeyResourceHref(question)).success(svc.loadResources);
        }
    };

    return svc;

}]);

