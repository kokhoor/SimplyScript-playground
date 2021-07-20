/*
 * (line 1)
 * (line 2).
 */
package my.com.solutionx.simplyscript;

/*
 * Copyright 2021 kokhoor.
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

import java.io.File;
import org.ini4j.Profile.Section;
import org.ini4j.Wini;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author kokhoor
 */
public class ScriptEngineTest {
    @Test
    public void testScriptEngine() throws Exception {
        String ini_filename = System.getProperty("config", "config.ini");
        if (ini_filename == null || ini_filename.length() == 0) {
            ini_filename = "config.ini";
        }
        Wini ini = new Wini(new File(ini_filename));
        Section iniMain = ini.get("main");
        ScriptEngine engine = new ScriptEngine();
        engine.init(iniMain);
        Assert.assertNotNull(engine);
        engine.action("Alert.test");
        engine.action("Alert.out", "abcdefg");
        engine.action("CallTest.test", "abcdefg");
    }
}
