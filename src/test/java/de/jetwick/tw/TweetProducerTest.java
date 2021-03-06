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

package de.jetwick.tw;

import com.google.inject.Inject;
import de.jetwick.data.TagDao;
import de.jetwick.data.YTag;
import de.jetwick.hib.HibTestClass;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Peter Karich, peat_hal 'at' users 'dot' sourceforge 'dot' net
 */
public class TweetProducerTest extends HibTestClass {

    @Inject
    private TagDao tagDao;

    public TweetProducerTest() {
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testUpdateTag() {
        // make sure that updateTag is in a transaction
        getInstance(TweetProducer.class).updateTagInTA(new YTag("test"), 6);
        assertTrue(tagDao.findByName("test").getQueryInterval() < 10 * YTag.DEFAULT_Q_I);
    }

    @Test
    public void testFIFO() {
        Queue q = new LinkedBlockingDeque();
        q.add("test");
        q.add("pest");
        assertEquals("test", q.poll());

        q = new LinkedBlockingQueue();
        q.add("test");
        q.add("pest");
        assertEquals("test", q.poll());

        Stack v = new Stack();
        v.add("test");
        v.add("pest");
        assertEquals("pest", v.pop());
    }
}
