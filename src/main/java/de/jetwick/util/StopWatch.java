/**
 * Copyright (C) 2010 Peter Karich <jetwick_@_pannous_._info>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.jetwick.util;

/**
 *
 * @author Peter Karich, peat_hal 'at' users 'dot' sourceforge 'dot' net
 */
public class StopWatch {

    private long lastTime = 0;
    private long time = 0;
    private String name = "";

    public StopWatch(String name) {
        this.name = name;
    }

    public void start() {
        lastTime = System.currentTimeMillis();
    }

    public StopWatch stop() {
        if (lastTime < 0)
            return this;
        time += System.currentTimeMillis() - lastTime;
        lastTime = -1;
        return this;
    }

    public long getTime() {
        return time;
    }

    @Override
    public String toString() {
        return name + " time:" + time / 1000f;
    }
}
