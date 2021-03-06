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

package de.jetwick.tw.cmd;

import de.jetwick.data.UrlEntry;
import de.jetwick.solr.SolrTweet;
import de.jetwick.solr.SolrUser;
import de.jetwick.tw.TweetDetector;
import de.jetwick.util.MyDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Peter Karich, peat_hal 'at' users 'dot' sourceforge 'dot' net
 */
public class TermCreateCommandTest {

    public TermCreateCommandTest() {
    }

    void execute(Collection<SolrTweet> tweets) {
        // remove executor since we only have one remaining command?
        new SerialCommandExecutor(tweets).add(new TermCreateCommand()).execute();
    }

    public static void execute(SolrTweet tw) {
        new TermCreateCommand(false).execute(tw);
    }

    @Test
    public void testQuality() throws SolrServerException {
        SolrTweet tw1 = createSolrTweet(1L, "@lwr32 #JAVA! "
                + "#COFFEE! #JAVA! #COFFEE! #JAVA! #COFFEE! #JAVA! #COFFEE! #JAVA! #COFFEE! #JAVA! #COFFEE! #JAVA! #COFFEE! #JAVA!", "usera");
        SolrTweet tw2 = createSolrTweet(2L, "@meggytron JAH-VA! java java java java "
                + "java java java. /Dante's Peak #requirescaffeine mashup", "userb");
        SolrTweet tw3 = createSolrTweet(3L, "@ierinleker ...JAVA JAVA JAVA JAVA JAVA "
                + "JAVA JAVA http://twitpic.com/2kk65u", "userc");
        SolrTweet tw4 = createSolrTweet(4L, "java", "userd");

        execute(Arrays.asList(tw1, tw2, tw3, tw4));

        assertTrue(tw4.getQuality() > tw3.getQuality());
        // both tweets have 7 java terms
        assertEquals(tw3.getQuality(), tw2.getQuality());
        assertTrue(tw2.getQuality() > tw1.getQuality());
    }

    @Test
    public void testQuality2() {
        String[] tweetsAsStr = new String[]{
            "Fernsehen entut Werder Bremen vs FC Twente Enschede http://watchlivefree.blogspot.com/2010/11/fernsehen-werder-bremen-vs-fc-twente.html",
            "Fernsehen taek Werder Bremen vs FC Twente Enschede http://watchlivefree.blogspot.com/2010/11/fernsehen-werder-bremen-vs-fc-twente.html",
            "Fernsehen stream Werder Bremen vs FC Twente Enschede http://watchlivefree.blogspot.com/2010/11/fernsehen-werder-bremen-vs-fc-twente.html",
            "Fernsehen live Werder Bremen vs FC Twente Enschede http://watchlivefree.blogspot.com/2010/11/fernsehen-werder-bremen-vs-fc-twente.html",
            "Televisie kijken Werder Bremen vs FC Twente Enschede http://watchlivefree.blogspot.com/2010/11/fernsehen-werder-bremen-vs-fc-twente.html",
            "Fernsehen Werder Bremen vs FC Twente Enschede http://watchlivefree.blogspot.com/2010/11/fernsehen-werder-bremen-vs-fc-twente.html",
            "Fernsehen kijken Werder Bremen vs FC Twente Enschede http://watchlivefree.blogspot.com/2010/11/fernsehen-werder-bremen-vs-fc-twente.html",
            "Fernsehen Televisie Werder Bremen vs FC Twente Enschede http://watchlivefree.blogspot.com/2010/11/fernsehen-werder-bremen-vs-fc-twente.html",
            "Fernsehen Werder Bremen vs FC Twente Enschede http://watchlivefree.blogspot.com/2010/11/fernsehen-werder-bremen-vs-fc-twente.html#1live",
            "Fernsehen Werder Bremen vs FC Twente Enschede http://watchlivefree.blogspot.com/2010/11/fernsehen-werder-bremen-vs-fc-twente.html#1"
        };
        List<SolrTweet> list = new ArrayList();
        int counter = 0;
        SolrUser user = new SolrUser("sakilamahipallb");
        for (String tw : tweetsAsStr) {
            counter++;
            list.add(new SolrTweet(counter, tw, user));
        }
        execute(list);

        counter = 0;
        for (SolrTweet tw : list) {
            assertTrue("tweet:" + tw, tw.getQuality() < SolrTweet.QUAL_LOW);
            if (tw.getQuality() < SolrTweet.QUAL_SPAM)
                counter++;
        }
        // a lot of those tweets are spam - not only bad!
        assertTrue(counter > 7);

        user = new SolrUser("user2");
        SolrTweet tw1 = new SolrTweet(1L, "E Grant Rd / N Swan Rd Accident no injury (Tue 3:24 PM)  http://tinyurl.com/5hwubc", user);
        SolrTweet tw2 = new SolrTweet(2L, "N Columbus Bl / E Grant Rd Accident no injury (Tue 3:26 PM)  http://tinyurl.com/658t96", user);
        execute(Arrays.asList(tw1, tw2));
        assertTrue("tweet:" + tw1, tw1.getQuality() < SolrTweet.QUAL_MAX);
        assertTrue("tweet:" + tw1, tw1.getQuality() > SolrTweet.QUAL_SPAM);
        assertTrue("tweet:" + tw2, tw2.getQuality() < SolrTweet.QUAL_MAX);
        assertTrue("tweet:" + tw2, tw2.getQuality() > SolrTweet.QUAL_SPAM);

        user = new SolrUser("user2");
        tw1 = new SolrTweet(1L, "Werder verliert sein Heimspiel gegen Twente http://goo.gl/fb/fKFEi #werder #svw", user);
        tw2 = new SolrTweet(2L, "Werder Bremen verliert gegen Twente Enschede http://goo.gl/fb/O8maL #werder #svw", user);
        execute(Arrays.asList(tw1, tw2));
        assertTrue("tweet:" + tw1, tw1.getQuality() == SolrTweet.QUAL_MAX);
        assertTrue("tweet:" + tw2, tw2.getQuality() < SolrTweet.QUAL_MAX);
        assertTrue("tweet:" + tw2, tw2.getQuality() > SolrTweet.QUAL_SPAM);
    }

    @Test
    public void testUrlTitleQuality() {
        String url1 = "http://watchlivefree.blogspot.different.domain.com",
                url2 = "http://watchlivefree.blogspot.com";
        String[] tweetsAsStr = new String[]{
            "blap notspamword " + url1,
            "blup secondnotspamword " + url2};

        SolrUser user = new SolrUser("user1");
        SolrTweet tw1 = new SolrTweet(1L, tweetsAsStr[0], user);
        tw1.getUrlEntries().add(new UrlEntry(5, 123, url1).setResolvedTitle("identical title"));
        SolrTweet tw2 = new SolrTweet(2L, tweetsAsStr[1], user);
        tw2.getUrlEntries().add(new UrlEntry(5, 123, url2).setResolvedTitle("identical title"));

        execute(Arrays.asList(tw1, tw2));

        assertTrue("tweet:" + tw1, tw1.getQuality() < 90);
        assertTrue("tweet:" + tw2, tw2.getQuality() < 90);
    }

    @Test
    public void testExecute() {
        SolrTweet tw = new SolrTweet(1L, "java lava", new SolrUser("tmp"));
        execute(tw);
        assertEquals(2, tw.getTextTerms().size());

        SolrUser u = new SolrUser("peter");
        tw = new SolrTweet(1L, "java lava", u);
        SolrTweet tw2 = new SolrTweet(2L, "peter java", u);
        execute(tw);
        assertEquals(2, tw.getTextTerms().size());
        assertEquals(2, tw2.getTextTerms().size());
    }

    SolrTweet createSolrTweet(long id, String twText, String user) {
        return new SolrTweet(id, twText, new SolrUser(user)).setCreatedAt(new MyDate(id).toDate());
    }

    @Test
    public void testTermDetection() {
        SolrUser user = new SolrUser("Peter");
        user.addOwnTweet(new SolrTweet(1, "term1 term2 term1", user));
        SolrTweet tw = new SolrTweet(2, "term3 not term2 important term3", user);
        user.addOwnTweet(tw);

        execute(tw);
        Collection<Entry<String, Integer>> coll = tw.getTextTerms().entrySet();
        assertEquals(3, (int) coll.size());
        int counter = 0;
        int counter2 = 0;
        for (Entry<String, Integer> e : coll) {
            if (e.getKey().equals("term1"))
                counter++;

            if (e.getKey().equals("not"))
                counter2++;
        }
        assertEquals(0, counter);
        assertEquals(0, counter2);
    }

    @Test
    public void testTermDetection2() {
        SolrUser user = new SolrUser("Peter");
        SolrTweet tw1 = new SolrTweet(1, "#term1 #term1", user);
        user.addOwnTweet(tw1);
        SolrTweet tw2 = new SolrTweet(2, "term1", user);
        user.addOwnTweet(tw2);

        execute(tw2);

        // two tweets with 'term1'
//        assertEquals(2, (int) extractor.run().getSortedTerms().get(0).getValue());
        assertEquals(1, (int) tw2.getTextTerms().size());
    }

    @Test
    public void testTermDetection3() {
        SolrTweet tw = new SolrTweet(1L, "A Year Without Rain "
                + "A Year Without Rain A Year Without Rain A Year Without Rain "
                + "A Year Without Rain A Year Without Rain A Year Without Rain", new SolrUser("peter"));
        execute(tw);
        assertEquals(2, tw.getTextTerms().size());
    }

    @Test
    public void testOtherTweets() {
        SolrUser u = new SolrUser("peter");
        SolrTweet tw1 = new SolrTweet(1L, "A Year Without Rain Will Give Us desert xyz", u);
        SolrTweet tw2 = new SolrTweet(2L, "A Year Without Rain Will Give Us really fat desert", u);
        SolrTweet tw3 = new SolrTweet(3L, "great hui desert", u);
        tw1.setQuality(100);
        tw2.setQuality(89);
        execute(tw1);
        // unchanged
        assertEquals(89, tw2.getQuality());
        assertTrue(tw1.getQuality() < 100);

        tw1.setQuality(100);
        StringFreqMap tFreq = new StringFreqMap();
        StringFreqMap lFreq = new StringFreqMap();
        new TermCreateCommand().checkSpamInExistingTweets(tw1, tFreq, lFreq);
        // without tw1
        assertEquals(6, (int) lFreq.get(TweetDetector.EN));
        assertEquals(1, (int) lFreq.get(TweetDetector.DE));

        assertEquals(4, (int) tw1.getLanguages().get(TweetDetector.EN));

        // without tw1
        assertEquals(2, (int) tFreq.get("desert"));
        assertEquals(1, (int) tFreq.get("hui"));
        assertNull(tFreq.get("xyz"));

        assertEquals(1, (int) tw2.getTextTerms().get("fat"));
    }

    @Test
    public void testLanguageDetection2() {
        SolrUser user = new SolrUser("peter");
        SolrTweet tw1 = new SolrTweet(0, "this is lastwordIsNotRecognizedBecauseItCouldBeStrippedOut", user);
        execute(tw1);
        assertEquals(2, tw1.getLanguages().get(TweetDetector.EN).intValue());
        assertEquals(TweetDetector.UNKNOWN_LANG, tw1.getLanguage());

        // now the language is detected because a lot noise NOISE_WORDS were found
        SolrTweet tw = new SolrTweet(2, "viele ist dort deutscher Tweet!");
        user.addOwnTweet(tw);
        execute(tw);
        assertEquals(TweetDetector.DE, tw.getLanguage());

        user = new SolrUser("peter");
        user.addOwnTweet(tw1);
        user.addOwnTweet(tw = new SolrTweet(3L, "Togos with @munckytown on lunch break. "
                + "Hall and Oates \"kiss on my list\" is playing... groovy"));
        execute(tw);
        assertEquals(TweetDetector.EN, tw.getLanguage());

        user = new SolrUser("peter");
        user.addOwnTweet(tw = new SolrTweet(4L, "@ibood Bedankt voor de code! :-)"));
        execute(tw);
        // only de and en are known so detect as unknown!
        assertEquals(TweetDetector.UNKNOWN_LANG, tw.getLanguage());

        // now detect the nl language 
        user.addOwnTweet(tw = new SolrTweet(5L, "@MrDeek Klinkt goed toch, een bestek set is altijd leuk om te krijgen of te geven!"));
        execute(tw);
        assertEquals(TweetDetector.NL, tw.getLanguage());
    }

    @Test
    public void testLanguageDetection3() {
        SolrTweet tw = new SolrTweet();
        tw.getLanguages().inc("de", 1);
        StringFreqMap otherLanguages = new StringFreqMap();
        assertEquals(TweetDetector.UNKNOWN_LANG, new TermCreateCommand().detectLanguage(tw, otherLanguages));

        tw = new SolrTweet();
        tw.getLanguages().inc("de", 2);
        otherLanguages = new StringFreqMap().set("de", 1);
        assertEquals("de", new TermCreateCommand().detectLanguage(tw, otherLanguages));

        tw = new SolrTweet();
        tw.getLanguages().inc(TweetDetector.UNKNOWN_LANG, 2);
        tw.getLanguages().inc("de", 2);
        otherLanguages = new StringFreqMap().set("de", 1);
        assertEquals("de", new TermCreateCommand().detectLanguage(tw, otherLanguages));

        tw = new SolrTweet();
        tw.getLanguages().inc(TweetDetector.UNKNOWN_LANG, 2);
        tw.getLanguages().inc("de", 2);
        tw.getLanguages().inc("en", 2);
        otherLanguages = new StringFreqMap().set("de", 1).set("en", 1);
        assertEquals(TweetDetector.UNKNOWN_LANG, new TermCreateCommand().detectLanguage(tw, otherLanguages));
    }
}
