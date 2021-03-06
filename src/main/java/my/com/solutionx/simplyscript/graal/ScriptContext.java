/*
 * Copyright 2021 SolutionX Software Sdn Bhd &lt;info@solutionx.com.my&gt;.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package my.com.solutionx.simplyscript.graal;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.script.ScriptException;
import my.com.solutionx.simplyscript.ScriptContextInterface;
import my.com.solutionx.simplyscript.ScriptService;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

/**
 *
 * @author SolutionX Software Sdn Bhd &lt;info@solutionx.com.my&gt;
 */
public class ScriptContext implements ScriptContextInterface{
    WeakReference<ScriptEngine> global;
    Map<String, Object> request = new HashMap<>();
    Map<String, Object> externalReq = null;
    final Context ctx;

    ScriptContext(ScriptEngine global, HostAccess hostAccess, ClassLoader classLoader) {
        this.global = new WeakReference<>(global);
        ctx = Context.newBuilder("js").hostClassLoader(classLoader)
                .engine(global.engine)
                .allowAllAccess(true)
                .allowValueSharing(true)
                .allowHostAccess(hostAccess)
                .allowIO(true).build();
    }

    @Override
    public void init() throws ScriptException {
        ctx.eval(this.global.get().initScript);
    }

    /*
    public Value ctxConstructor() {
        return global.get().ctxObject();
    }
    */

    public Object app(String key) {
        return global.get().app(key);
    }
    
    public Object app(String key, Object value) {
        return global.get().app(key, value);
    }

    public Object cache(String key) {
        return global.get().cache(key);
    }
    
    public void cache(String key, Object value) {
        global.get().cache(key, value);
    }

    public Object system(String key) {
        return global.get().system(key);
    }
    
    public Object system(String key, Object value) {
        return global.get().system(key, value);
    }

    public Object module(String key, Object ctx) {
        return global.get().modules().get(key, (String k) -> {
            Value ctxObject = global.get().ctxObject();
            Value setupScript = ctxObject.getMember("moduleSetup");
            return setupScript.execute(key, global.get().system(), ctx);
        });
    }

    public boolean isPrivileged(String uniqueid) {
        ScriptService scriptService = global.get().scriptService.get();
        return scriptService.isPrivilegedService(uniqueid);
    }

    public Object service(String key, Object ctx) throws ScriptException {
        Value obj = (Value)global.get().service(key);
        if (obj == null) {
            Value ctxObject = global.get().ctxObject();
            Value setupScript = ctxObject.getMember("serviceSetup");
            String uuid = UUID.randomUUID().toString();
            ScriptService scriptService = global.get().scriptService.get();
            Map<String, Object> mapScriptConfig = scriptService.getScriptConfig();
            Map<String, Object> mapServiceConfig = (Map<String, Object>) mapScriptConfig.get("service");
            List lstPrivileged = (List)mapServiceConfig.get("privilegedServices");
            if (lstPrivileged != null && lstPrivileged.contains(key)) {
                scriptService.addPrivilegedService(uuid);
            }
/*
            if (v != null && !v.isNull()) {
                for (long i=0; i<v.getArraySize(); i++) {
                }
            }
*/
            Object ret = setupScript.execute(key, global.get().system(), uuid, ctx);
            if (ret == null) //  || ret.getClass() == Undefined.class)
                throw new RuntimeException("Service cannot be setup: " + key);
            obj = (Value) ret;
            global.get().service(key, obj);
        }
        return obj;
    }

    public Object req(String key) {
        if (externalReq != null)
            return externalReq.get(key);
        return request.get(key);
    }

    public Object req(String key, Object value) {
        if (externalReq != null)
            return externalReq.put(key, value);
        return request.put(key, value);
    }

    public void addClasspath(String path) throws MalformedURLException {
        global.get().addClasspath(path);
    }

    public void recycle() {
        request.clear();
    }

    public void cleanup() {
        request.clear();
        request = null;
        global.clear();
        global = null;
    }

    @Override
    public void setRequest(Map<String, Object> mapReq) {
        externalReq = mapReq;
    }
}