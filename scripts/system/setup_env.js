(function() {

function ctxObject(localContext) {
  this.localContext = localContext;
  this.callDepth = -1;
}

ctxObject.prototype = {
  cache(key, value) {
    if (arguments.length > 1) { // is set
      this._services['cache'][key] = value;
    } else { // is get
      return this._services['cache'][key];
    }
  },
  call(action, args) {
    var idx = action.lastIndexOf(".");
    if (idx === -1)
      throw new Error("Invalid action format. Expected XXX.YYY");

    var module = action.substring(0, idx);
    var method = action.substring(idx+1);

    var objModule = this.localContext.module(module);
    if (objModule == null)
      throw new Error("Module not found: " + module);

    this.callDepth += 1;
    try {
      var preCall = this.callDepth <= 0 ? this.localContext.system("preCall") : this.localContext.system("preInnerCall");
      if (preCall != null) {
        for (var i=0; i<preCall.length; i++) {
          try {
            preCall[i](this, null, action, args);
          } catch (e) {
          }
        }
      }

      var ret = objModule[method](args, this);

      var postCall = this.callDepth <= 0 ? this.localContext.system("postCall") : this.localContext.system("postInnerCall");
      if (postCall != null) {
        for (var i=0; i<postCall.length; i++) {
          try {
            postCall[i](this, null, action, args);
          } catch (e) {
          }
        }
      }

      return ret;
    } catch (e) {
      var postCall = this.callDepth <= 0 ? this.localContext.system("postCall") : this.localContext.system("postInnerCall");
      if (postCall != null) {
        for (var i=0; i<postCall.length; i++) {
          try {
            postCall[i](this, e, action, args);
          } catch (e) {
          }
        }
      }
      throw e;
    } finally {
      this.callDepth -= 1;
    }
  },
  service(name) {
    return this.localContext.service(name);
  }
};

ctxObject.serviceSetup = function(serviceName, system) {
  if (this._config.service.deny != null) {
    if (serviceName in this._config.service.deny) {
      return null;
    }
  }

  if (this._config.service.allow != null) {
    if (!(serviceName in this._config.service.allow)) {
      return null;
    }
  }

  var setupData = null;
  var service = load(`${this._config.service.path}/${serviceName}/index.js`);
  if (service == null)
    return null;

  if ("setup" in service) {
    setupData = service.setup();
  }
  if (setupData == null)
    return service;

  if (setupData.contextPrototypes != null) {
    for (var key in setupData.contextPrototypes) {
      ctxObject.prototype[key] = setupData.contextPrototypes[key];
    }
  }

  if (setupData.callbacks != null) {
    var keys = ["preCall", "postCall", "preInnerCall", "postInnerCall"];
    for (var i=0; i<keys.length; i++) {
      var call = setupData.callbacks[keys[i]];
      if (call != null) {
        var call_array = system[keys[i]];
        if (call_array == null)
          call_array = system[keys[i]] = [];
        call_array.push(call);
      }
    }
  }
  return service;
};

ctxObject.moduleSetup = function (moduleName) {
  if (this._config.module.deny != null) {
    if (moduleName in this._config.module.deny) {
      return null;
    }
  }

  if (this._config.module.allow != null) {
    if (!(moduleName in this._config.module.allow)) {
      return null;
    }
  }

  var module = load(`${this._config.module.path}/${moduleName}/index.js`);
  return module;
};

function array_to_map(obj) {
  var map = {};
  for (var i=0; i<obj.length; i++) {
    map[obj[i]] = true;
  }
  return map;
}

ctxObject.config = function (objs) {
  function config_by_type(type, obj) {
    if (obj.allow === "*")
      obj.allow = null;

    if (obj.allow != null) {
      obj.allow = array_to_map(obj.allow);
    }

    if (obj.deny != null) {
      obj.deny = array_to_map(obj.deny);
    }

    ctxObject._config[type] = obj;
  }

  if (!ctxObject._config) {
    ctxObject._config = {
      service: {},
      module: {}
    };
  }

  config_by_type("service", objs['service']);
  config_by_type("module", objs['module']);
};

return ctxObject;

});