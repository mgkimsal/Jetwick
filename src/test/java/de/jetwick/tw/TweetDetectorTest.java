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

import de.jetwick.solr.SolrTweet;
import de.jetwick.solr.SolrUser;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Peter Karich, peat_hal 'at' users 'dot' sourceforge 'dot' net
 */
public class TweetDetectorTest {

    public TweetDetectorTest() {
    }

    @Test
    public void testRun() {
        List<SolrTweet> tweets = new ArrayList<SolrTweet>();
        tweets.add(new SolrTweet(1, "term5 term2 term3 term4!"));
        tweets.add(new SolrTweet(1, "term o"));
        tweets.add(new SolrTweet(1, "TERM term6 Gehts?"));
        TweetDetector extractor = new TweetDetector(tweets);
        extractor.setTermMaxCount(10);
        List<Entry<String, Integer>> mostFrequentTerms = extractor.run().getSortedTerms();

        assertEquals(7, mostFrequentTerms.size());
        assertEquals("term", mostFrequentTerms.get(0).getKey());
        assertEquals(2, (int) mostFrequentTerms.get(0).getValue());
    }

    @Test
    public void testUrlsInTerms() {
        List<SolrTweet> tweets = new ArrayList<SolrTweet>();
        tweets.add(new SolrTweet(1, "the god http://www.jetwick.com/hihiho/test.html <b>http</b>://<b>bit</b>.ly/9FZv5E"));
        List<Entry<String, Integer>> mostFrequentTerms = createExtractor(tweets).run().getSortedTerms();
//        System.out.println(mostFrequentTerms);
        assertEquals(1, mostFrequentTerms.size());
    }

    @Test
    public void testTermsWithRemove() {
        List<SolrTweet> tweets = new ArrayList<SolrTweet>();
        tweets.add(new SolrTweet(1, "the god"));
        tweets.add(new SolrTweet(2, "the thing"));
        tweets.add(new SolrTweet(3, "it's now"));
        List<Entry<String, Integer>> mostFrequentTerms = createExtractor(tweets).run().getSortedTerms();

        // remove "the", "it's" and "now"
        assertEquals(2, mostFrequentTerms.size());

        tweets.add(new SolrTweet(4, "p d"));
        mostFrequentTerms = createExtractor(tweets).run().getSortedTerms();
        assertEquals(2, mostFrequentTerms.size());

        tweets.add(new SolrTweet(5, "we're --"));
        mostFrequentTerms = createExtractor(tweets).run().getSortedTerms();
        assertEquals(2, mostFrequentTerms.size());

        tweets.clear();
        tweets.add(new SolrTweet(6, "c++"));
        tweets.add(new SolrTweet(7, "c#"));
        tweets.add(new SolrTweet(8, "DivaDOD:"));

        mostFrequentTerms = createExtractor(tweets).run().getSortedTerms();
        assertEquals(3, mostFrequentTerms.size());
        assertEquals("c++", mostFrequentTerms.get(0).getKey());
        assertEquals("c#", mostFrequentTerms.get(1).getKey());
        assertEquals("divadod", mostFrequentTerms.get(2).getKey());
    }

    @Test
    public void testStripNoiseFromWords() {
        assertEquals(" hi ", TweetDetector.stripNoiseFromWord("@hi@"));
        assertEquals("pet ", TweetDetector.stripNoiseFromWord("pet."));
        assertEquals(" peter ", TweetDetector.stripNoiseFromWord("@peter."));
        assertEquals(" pet er ", TweetDetector.stripNoiseFromWord("@pet,er!"));
        assertEquals(" pet er ", TweetDetector.stripNoiseFromWord("@pet,er!"));
        assertEquals(" peter_mueller", TweetDetector.stripNoiseFromWord("@<b>peter</b>_mueller"));
        assertEquals("  peter  ", TweetDetector.stripNoiseFromWord(">>peter<<"));
        assertEquals(" peter ", TweetDetector.stripNoiseFromWord("\"peter\""));
        assertEquals("don't", TweetDetector.stripNoiseFromWord("don't"));
        assertEquals("hi how are you ", TweetDetector.stripNoiseFromWord("hi\nhow\tare you?"));

        assertEquals("all things after urls should remain!",
                "  hi", TweetDetector.stripNoiseFromWord("http://blibla.de hi"));
        assertEquals("test_t   test", TweetDetector.stripNoiseFromWord("test_t https://www.stupid.de test"));
        assertEquals(" ", TweetDetector.stripNoiseFromWord("http://blibla.de"));
        assertEquals(" ", TweetDetector.stripNoiseFromWord(" "));
        assertEquals("  test", TweetDetector.stripNoiseFromWord("http:// test"));
    }

    @Test
    public void testLanguageDetection() {
        // skip the noise words and last terms for language detection:
        List<SolrTweet> tweets = new ArrayList<SolrTweet>();
        tweets.add(new SolrTweet(1, "das geht ja ab!"));
        Map<String, Integer> langs = createExtractor(tweets).run().getLanguages();
        assertEquals(3, langs.get(TweetDetector.DE).intValue());
    }

    @Test
    public void testTerms() {
        SolrUser user = new SolrUser("Peter");
        user.addOwnTweet(new SolrTweet(1, "test pest alpha", user));
        user.addOwnTweet(new SolrTweet(2, "alpha", user));

        assertEquals(3, (int) createExtractor(user.getOwnTweets()).run().getSortedTerms().size());
        assertEquals(2, (int) createExtractor(user.getOwnTweets()).run().getSortedTerms().get(0).getValue());
    }

    TweetDetector createExtractor(Collection<SolrTweet> tweets) {
        return new TweetDetector(tweets);
    }
//    @Test
//    public void testFilterLang() throws Exception {
//        Map<String, Integer> langs = new HashMap<String, Integer>();
//        langs.put(TweetDetector.EN, 100);
//        langs.put(TweetDetector.DE, 100);
//        assertEquals(2, new TweetDetector().setLanguages(langs).filterLanguages(-1).size());
//
//        langs = new HashMap<String, Integer>();
//        langs.put(TweetDetector.EN, 100);
//        langs.put(TweetDetector.DE, 3);
//        assertEquals(1, new TweetDetector().setLanguages(langs).filterLanguages(-1).size());
//
//        langs = new HashMap<String, Integer>();
//        langs.put(TweetDetector.EN, 100);
//        langs.put(TweetDetector.DE, 4);
//        assertEquals(2, new TweetDetector().setLanguages(langs).filterLanguages(-1).size());
//    }
}
