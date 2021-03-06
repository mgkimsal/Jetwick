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

import java.util.Date;

/**
 * I know I could use jodatime but this is smaller
 *
 * @author Peter Karich, peat_hal 'at' users 'dot' sourceforge 'dot' net
 */
public class MyDate implements Cloneable {

    private long time;
    public static long ONE_MINUTE = 60 * 1000L;
    public static long ONE_HOUR = 60 * ONE_MINUTE;
    public static long ONE_DAY = 24 * ONE_HOUR;

    public MyDate() {
        this(new Date());
    }

    public MyDate(Date date) {
        time = date.getTime();
    }

    public MyDate(long time) {
        this.time = time;
    }

    public MyDate(MyDate date) {
        time = date.getTime();
    }

    public long getTime() {
        return time;
    }

    public MyDate plusDays(int days) {
        time += days * ONE_DAY;
        return this;
    }

    public MyDate minusDays(int days) {
        return plusDays(-days);
    }

    public MyDate minusMinutes(int minutes) {
        return plusMinutes(-minutes);
    }

    public MyDate plusMinutes(int minutes) {
        time += minutes * ONE_MINUTE;
        return this;
    }

    public MyDate minus(MyDate date) {
        time -= date.getTime();
        return this;
    }

    public long toDays() {
        return time / ONE_DAY;
    }

    public Date toDate() {
        return new Date(time);
    }

    @Override
    public MyDate clone() {
        return new MyDate(this);
    }

    @Override
    public String toString() {
        return new Date(time).toString();
    }
}
