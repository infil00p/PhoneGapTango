/*
 *
 * Copyright 2015 Adobe Systems Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/

var argscheck = require('cordova/argscheck'),
    utils = require("cordova/utils"),
    exec = require("cordova/exec");

var running = false;

var timers = {};

var listeners = [];

//Last returned pose object from native
var pose = null;

function start() {
  exec (function(a) {
    var tempListeners = listeners.slice(0);
    pose = a;
    for(var i = 0, l = tempListeners.length; i < l; i++) {
      tempListenres[i].win(pose);
    }
  } , function(e) {
      var tempListeners = listeners.slice(0);
      for (var i = 0, l = tempListeners.length; i < l; i++) {
            tempListeners[i].fail(e);
      }
    }, "Tango", "start", []);
    running = true;
}

function stop() {
  exec(null, null, "Tango", "stop", []);
  running = false;
}

function createCallbackPair(win, fail) {
  return {win: win, fail: fail};
}

// Removes a win/fail listener pair from the listeners array
function removeListeners(l) {
    var idx = listeners.indexOf(l);
    if (idx > -1) {
        listeners.splice(idx, 1);
        if (listeners.length === 0) {
            stop();
        }
    }
}

var tango = {
    watchTango: function(successCallback, errorCallback, options) {
        argscheck.checkArgs('fFO', 'tango.watchTango', arguments);
        // Default interval (1 sec)
        var frequency = (options && options.frequency && typeof options.frequency == 'number') ? options.frequency : 1000;

        // Keep reference to watch id, and report accel readings as often as defined in frequency
        var id = utils.createUUID();

        var p = createCallbackPair(function(){}, function(e) {
            removeListeners(p);
            errorCallback && errorCallback(e);
        });
        listeners.push(p);

        timers[id] = {
          timer:window.setInterval(function() {
                if (pose) {
                    successCallback(pose);
                }
            }, frequency),
          listeners:p
        };

        if (running) {
            // If we're already running then immediately invoke the success callback
            // but only if we have retrieved a value, sample code does not check for null ...
            if (pose) {
                successCallback(pose);
            } 
        } else {
            start();
        }
        return id;
    },

    /**
     * Clears the specified Tango Sensor watch.
     *
     * @param {String} id       The id of the watch returned from #watchAcceleration.
     */
    clearWatch: function(id) {
        // Stop javascript timer & remove from timer list
        if (id && timers[id]) {
            window.clearInterval(timers[id].timer);
            removeListeners(timers[id].listeners);
            delete timers[id];
        }
    }
};

module.exports = tango;
