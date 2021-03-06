/*
 * Copyright 2021 SolutionX Software Sdn. Bhd. &lt;info@solutionx.com.my&gt;.
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
package my.com.solutionx.simplyscript;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.script.ScriptException;
import stormpot.Pool;
import stormpot.PoolBuilder;
import stormpot.PoolException;

/**
 *
 * @author kokhoor
 */
public class ScriptService {
    ScriptEngineInterface engine;

    Pool<PoolableScriptContext> poolContext;
    Cache<String, Object> modules = Caffeine.newBuilder()
            .maximumSize(1024)
            .build();

    Map<String, Object> app = new ConcurrentHashMap<>();
    Cache<String, Object> cache = Caffeine.newBuilder()
            .maximumSize(1024)
            .build();
    Map<String, Object> system = new ConcurrentHashMap<>();
    Map<String, Object> services = new ConcurrentHashMap<>();
    SimplyScriptClassLoader loader = null;
    Set<String> privilegedServices = new HashSet<>();

    // Map<String, String> config = null;
    private Map<String, Object> mapScriptConfig;

    public ScriptService() throws ScriptException {
        super();
    }
    
    public void init(Map<String, String> config) throws FileNotFoundException, IOException, ScriptException, PoolException, InterruptedException, ScriptServiceException, InvocationTargetException {
        // this.config = config;
        loader = new SimplyScriptClassLoader("SimplyScriptService", new URL[] {}, this.getClass().getClassLoader() );
        Thread.currentThread().setContextClassLoader(loader);
        String config_path = config.getOrDefault("config_path", "./config/");
        String working_path = config.getOrDefault("working_path", "./");
        String scripts_path = config.getOrDefault("scripts_path", "./scripts/");
        String pool_size = config.getOrDefault("pool_size", "5");
        config.put("config_path", config_path);
        config.put("scripts_path", scripts_path);
        config.put("working_path", working_path);

        String ScriptEngineClass = config.getOrDefault("engine", "my.com.solutionx.simplyscript.nashorn.ScriptEngine");
        System.out.println("Using ScriptEngineClass: " + ScriptEngineClass);
        Class<ScriptEngineInterface> scriptEngineClass;
        try {
            scriptEngineClass = (Class<ScriptEngineInterface>) Class.forName(ScriptEngineClass);
            Constructor<ScriptEngineInterface> declaredConstructor = scriptEngineClass.getDeclaredConstructor();
            this.engine = declaredConstructor.newInstance();
        } catch (ClassNotFoundException e) {
            throw new ScriptServiceException("Script Engine Class not found: " + e.getMessage(), "E_CannotFindEngineClass");
        } catch (NoSuchMethodException e) {
            throw new ScriptServiceException("Script Engine constructor not found: " + e.getMessage(), "E_CannotFindEngineConstructor");
        } catch (SecurityException e) {
            throw new ScriptServiceException("Error getting Script Engine Constructor: " + e.getMessage(), "E_ErrorGettingEngineConstructor");
        } catch (InstantiationException e) {
            throw new ScriptServiceException("Error Creating Script Engine: " + e.getMessage(), "E_ErrorCreatingEngineConstructor");
        } catch (IllegalAccessException e) {
            throw new ScriptServiceException("Error calling Script Engine Constructor: " + e.getMessage(), "E_ErrorCallingEngineConstructor");
        } catch (IllegalArgumentException e) {
            throw new ScriptServiceException("Invalid Argument sent to Script Engine Constructor: " + e.getMessage(), "E_InvalidArgumentEngineConstructor");
        } catch (InvocationTargetException e) {
            throw e;
        }

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> mapModuleConfig = mapper.readValue(new FileReader(config_path + "/scripts/module_conf.json"),
                Map.class);
        Map<String, Object> mapServiceConfig = mapper.readValue(new FileReader(config_path + "/scripts/service_conf.json"),
                Map.class);
        mapScriptConfig = new HashMap<>();
        mapModuleConfig.put("path", scripts_path + mapModuleConfig.getOrDefault("path", "modules"));
        mapServiceConfig.put("path", scripts_path + mapServiceConfig.getOrDefault("path", "services"));
        mapScriptConfig.put("module", mapModuleConfig);
        mapScriptConfig.put("service", mapServiceConfig);
        mapScriptConfig.put("config", config);
        system.put("config", mapScriptConfig);
        engine.init(this, mapScriptConfig);

        PoolableScriptContextAllocator allocator = new PoolableScriptContextAllocator(engine);
        PoolBuilder<PoolableScriptContext> poolBuilder = Pool.from(allocator);
        poolBuilder = poolBuilder.setSize(Integer.valueOf(pool_size));
        poolContext = poolBuilder.build();
/*
        ScriptContextInterface ctx = engine.getScriptContext();
        ctx.init();
*/
        List<String> preload = (List<String>)mapServiceConfig.get("preload");
        if (preload != null) {
            engine.loadServices(preload);
        }

        preload = (List<String>)mapModuleConfig.get("preload");
        if (preload != null) {
            engine.loadModules(preload);
        }
    }

    public ScriptEngineInterface engine() {
        return engine;
    }

    public Map<String, Object> app() {
        return app;
    }
    
    public Cache<String, Object> cache() {
        return cache;
    }

    public Map<String, Object> system() {
        return system;
    }

    public Map<String, Object> services() {
        return services;
    }

    public Cache<String, Object> modules() {
        return modules;
    }

    public Object app(String key) {
        return app.get(key);
    }
    
    public Object app(String key, Object value) {
        if (value == null)
            return app.remove(key);
        return app.put(key, value);
    }

    public Object cache(String key) {
        return cache.getIfPresent(key);
    }
    
    public void cache(String key, Object value) {
        if (value == null) {
            cache.invalidate(key);
            return;
        }
        cache.put(key, value);
    }

    public Object system(String key) {
        return system.get(key);
    }
    
    public Object system(String key, Object value) {
        if (value == null)
            return system.remove(key);
        return system.put(key, value);
    }

    public Object service(String key) {
        return services.get(key);
    }
    
    public Object service(String key, Object value) {
        if (value == null)
            return services.remove(key);
        return services.put(key, value);
    }

    public Object module(String key) {
        return modules.getIfPresent(key);
    }
    
    public void module(String key, Object value) {
        if (value == null) {
            modules.invalidate(key);
            return;
        }
        modules.put(key, value);
    }
    
    public Object getService(String name) throws ScriptException, PoolException, InterruptedException {
        return engine.getService(name);
    }
    
    public Pool<PoolableScriptContext> getScriptContextPool() {
        return poolContext;
    }

    public Object action(String action) throws ScriptException, PoolException, InterruptedException {
        return action(action, null);
    }

    public Object action(String action, Object args) throws ScriptException, PoolException, InterruptedException {
        return action(action, args, null);
    }

    public Object action(String action, Object args, Map<String, Object> mapReq) throws ScriptException, PoolException, InterruptedException {
        return engine.action(action, args, mapReq);
    }

    public String actionReturnString(String action) throws ScriptException, PoolException, InterruptedException, JsonProcessingException {
        return actionReturnString(action, null, null);        
    }

    public String actionReturnString(String action, Object args) throws ScriptException, PoolException, InterruptedException, JsonProcessingException {
        return actionReturnString(action, args, null);        
    }

    public String actionReturnString(String action, Object args, Map<String, Object> mapReq) throws ScriptException, PoolException, InterruptedException, JsonProcessingException {
        return engine.actionReturnString(action, args, mapReq);
    }

    public void addClasspath(String strFile) throws MalformedURLException {
        File file = new File(strFile);
        String[] files;
        int i;
        files = SimpleFileFilter.fileOrFiles(file);
        if (files != null) {
            for (i = 0; i < files.length; i++) {
                file = new File(file.getParent() + File.separatorChar + files[i]);
                // Check to see if we have proper access.
                if (!file.exists()) {
                    throw new IllegalArgumentException("Repository " + file.getAbsolutePath() + " doesn't exist!");
                } else if (!file.canRead()) {
                    throw new IllegalArgumentException("Do not have read access for file " + file.getAbsolutePath());
                }

                // Check that it is a directory or zip/jar file
                if (file.isDirectory()) {
                    continue;
                }

                loader.addURL(file.toURL());
            }
        } else if (file.exists() && file.isDirectory()) {
            loader.addURL(file.toURL());
        } else {
            System.out.println("Ignored : " + file.getPath());
        }
    }
    
    public ClassLoader getClassLoader() {
        return loader;
    }
    
    public void reload() throws IOException, FileNotFoundException, ScriptException, PoolException, InterruptedException, ScriptServiceException, InvocationTargetException {
        if (poolContext != null)
            poolContext.shutdown();
        poolContext = null;

        if (engine != null)
            engine.shutdown();
        engine = null;

        try {
            if (loader != null)
                loader.close();
        } catch (IOException ex) {
        }
        loader = null;

        if (modules != null)
            modules.invalidateAll();

        if (app != null)
            app.clear();

        if (cache != null)
            cache.invalidateAll();

        if (system != null) {
            system.clear();
        }

        if (services != null) {
            services.clear();
        }

        init((Map<String, String>) mapScriptConfig.get("config"));
    }

    public Map<String, Object> getScriptConfig() {
        return mapScriptConfig;
    }
    
    public void addPrivilegedService(String uniqueid) {
        privilegedServices.add(uniqueid);
    }

    public void removePrivilegedService(String uniqueid) {
        privilegedServices.remove(uniqueid);
    }

    public void clearPrivilegedService() {
        privilegedServices.clear();
    }
    
    public boolean isPrivilegedService(String uniqueid) {
        return privilegedServices.contains(uniqueid);
    }
}
