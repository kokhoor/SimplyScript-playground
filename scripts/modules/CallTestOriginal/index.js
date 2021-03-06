/* global log */

({
  _setup(moduleName, args, system, path, ctx) {
    this._loggername = "modules." + moduleName;
    log.info(this, "Module name: {} my path is: {}", moduleName, path);
  },
  getLoggerName() {
    return this._loggername;
  },
  saveEmployee(args, ctx) {
    ctx.db.update(null, 'saveEmployee', {
      name: "Demo Test Employee 01",
      username: '*TESTONLY*',
      contactno: "do not contact me",
      email: "demotest01@demotest.com",
      mobileno: "12345"
    }, ctx);
    var db = ctx.db.get(null, ctx);
    var updated = db.update("saveEmployee", {
      name: "Demo Test Employee 01",
      username: '*TESTONLY*',
      contactno: "contact me!",
      email: "demotest01@demotest.com",
      mobileno: "12345"
    });
    return updated;
  },
  getEmployee(args, ctx) {
    log.info(ctx, "Can you see me, {} {}", "Now?", "and Tomorrow?");
    return ctx.db.selectOne(null, "getEmployee", {"mobileno": "kokhoor"}, ctx);
  },
  getEmployees(args, ctx) {
    var params = {
      "mobileno": ["kokhoor", "*TESTONLY*"]
    };
    console.log("Params: " + params);
    return ctx.db.selectList(null, "getEmployees", params, ctx);
  },
  test(args, ctx) {    
    ctx.call("Alert.out", {"str": "String to display"});
    print("Before get db: " + ctx + ":" + ctx.db + ":" + ctx.db.get + ":" + ctx.db.newDb);
    try {
      var db = ctx.db.get('default', ctx);
    } catch (e) {
      print("Exception: " + e.message);
      throw e;
    }
    print("Have db: " + db);
    if (db != null) {
      var cursor = db.selectCursor("getEmployees",
        {"mobileno": ["mobileno", "12345"]});
      for (const row of cursor) {
        console.log(`${row.name}`)
      }
      cursor.close();
    }
    var db2 = ctx.db.newDb('default', ctx);
    print("Have db2: " + db2);
    var db_same = ctx.db.get('default', ctx);
    print("Have db_same: " + db_same);
    var db2_same = ctx.db.newDb('default', ctx);
    print("Have db2_same (shd be different): " + db2_same);
    return "CallTest.test completed!";
  },
  test2(args, ctx) {
    return {"a": 5, "b": 6, "c": [1,2,3], "d": {"x":0, "y": 10}};
  },
  email(args, ctx) {
    return ctx.service("email").send({
      "profile": "default",
      "to": "", // comma separated email addresses
      "subject": "This is an OTP Test",
      "text": "Congratulations, you received OTP from us: 123456 from SimplyScript"
    }, ctx);
  }
});