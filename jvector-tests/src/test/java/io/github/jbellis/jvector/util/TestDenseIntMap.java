/*
 * All changes to the original code are Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */

/*
 * Original license:
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.jbellis.jvector.util;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class TestDenseIntMap extends RandomizedTest {

    @Test
    public void testInsert() {
        var map = new DenseIntMap<String>(100);
        for (int i = 0; i < 3; i++) {
            Assert.assertNull(map.get(i));
            Assert.assertFalse(map.containsKey(i));

            map.put(i, "value" + i);
            Assert.assertEquals("value" + i, map.get(i));
            Assert.assertTrue(map.containsKey(i));
            Assert.assertEquals(i + 1, map.size());
        }
    }

    @Test
    public void testUpdate() {
        var map = new DenseIntMap<String>(100);
        for (int i = 0; i < 3; i++) {
            map.put(i, "value" + i);
        }
        Assert.assertEquals(3, map.size());

        for (int i = 0; i < 3; i++) {
            map.put(i, "new-value" + i);
            Assert.assertEquals("new-value" + i, map.get(i));
            Assert.assertEquals(3, map.size());
        }
    }

    @Test
    public void testRemove() {
        var map = new DenseIntMap<String>(100);
        for (int i = 0; i < 3; i++) {
            map.put(i, "value" + i);
        }
        Assert.assertEquals(3, map.size());

        for (int i = 0; i < 3; i++) {
            map.remove(i);
            Assert.assertNull(map.get(i));
            Assert.assertFalse(map.containsKey(i));
            Assert.assertEquals(3 - (i + 1), map.size());
        }
    }

    @Test
    public void testConcurrency() throws InterruptedException {
        var map = new DenseIntMap<String>(100);
        var source = new ConcurrentHashMap<Integer, String>();

        var latch = new CountDownLatch(4);
        for (int t = 0; t < 4; t++) {
            new Thread(() -> {
                try {
                    for (int i = 0; i < 1000; i++) {
                        int key = randomIntBetween(0, 500);
                        if (rarely()) {
                            source.remove(key);
                            map.remove(key);
                        } else {
                            String value = randomAsciiAlphanumOfLength(20);
                            source.put(key, value);
                            map.put(key, value);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        latch.await();

        Assert.assertEquals(source.size(), map.size());
        source.forEach((key, value) -> {
            Assert.assertTrue(map.containsKey(key));
            Assert.assertEquals(value, map.get(key));
        });
    }
}
