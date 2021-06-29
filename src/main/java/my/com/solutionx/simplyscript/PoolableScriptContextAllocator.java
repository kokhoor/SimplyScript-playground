/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package my.com.solutionx.simplyscript;

import java.io.IOException;
import javax.script.ScriptException;
import stormpot.Allocator;
import stormpot.Slot;

/**
 *
 * @author kokhoor
 */
public class PoolableScriptContextAllocator implements Allocator<PoolableScriptContext> {
    ScriptGlobalObject global;

    public PoolableScriptContextAllocator(ScriptGlobalObject global) throws IOException, ScriptException {
        this.global = global;
    }
    
    @Override
    public PoolableScriptContext allocate(Slot slot) throws Exception {
        return new PoolableScriptContext(global, slot);
    }

    @Override
    public void deallocate(PoolableScriptContext poolable) throws Exception {
        poolable.deallocate();
    }
}