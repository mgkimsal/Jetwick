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

package de.jetwick.solr;

import de.jetwick.tw.Twitter4JTweet;
import de.jetwick.tw.TwitterSearch;
import de.jetwick.util.MyDate;
import de.jetwick.data.UrlEntry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import twitter4j.Tweet;
import static org.junit.Assert.*;

/**
 *
 * @author Peter Karich, peat_hal 'at' users 'dot' sourceforge 'dot' net
 */
public class SolrTweetSearchTest extends MyAbstractSolrTestCase {

    private SolrTweetSearch twSearch;

    public SolrTweetSearch getTweetSearch() {
        return twSearch;
    }

    @Override
    public String getSolrHome() {
        return "twindex";
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        EmbeddedSolrServer server = new EmbeddedSolrServer(h.getCoreContainer(), h.getCore().getName());
        twSearch = new SolrTweetSearch(server);
        // add a small waiting because otherwise we would get 'Error opening new searcher. exceeded limit of maxWarmingSearchers=1'
        // for maxWarmingSearchers lower than 2 (which means too few newSearcher)
        Thread.sleep(100);
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testSearch() throws Exception {
        SolrUser fromUser = new SolrUser("peter");
        SolrTweet tw1 = new SolrTweet(1L, "this is a test!", fromUser);

        SolrUser otherUser = new SolrUser("otherUser");
        SolrTweet tw2 = new SolrTweet(2L, "java is cool and stable!", otherUser);
        twSearch.update(tw1, false);
        twSearch.update(tw2, true);

        assertEquals(1, twSearch.search("test").size());
        assertEquals(1, twSearch.search("java").size());
    }

    @Test
    public void testHashtags() throws SolrServerException {
        // # is handled as digit so that we can search for java to get java and #java results (the same applies to @)
        twSearch.update(createTweet(1L, "is cool and stable! #java", "peter2"));
        assertEquals(1, twSearch.search("java").size());
        assertEquals(1, twSearch.search("#java").size());

        twSearch.deleteAll();
        twSearch.commit();
        assertEquals(0, twSearch.search("java").size());
        assertEquals(0, twSearch.search("#java").size());
        twSearch.update(createTweet(1L, "is cool and stable! java", "peter2"));
        assertEquals(1, twSearch.search("java").size());
        assertEquals(0, twSearch.search("#java").size());
    }

    @Test
    public void testSearchAnchors() throws Exception {
        SolrUser peter = new SolrUser("peter");
        SolrTweet tw1 = new SolrTweet(1L, "peter #java is cool!", peter);
        SolrUser peter2 = new SolrUser("peter2");
        SolrTweet tw2 = new SolrTweet(2L, "@peter java is cool!", peter2);
        twSearch.update(tw1, false);
        twSearch.update(tw2, true);

        assertEquals(1, twSearch.search("#java").size());
        assertEquals(1, twSearch.search("@peter").size());
    }

    @Test
    public void testCamelCase() throws Exception {
        SolrTweet tw1 = new SolrTweet(1L, "peter iBood is cool!", new SolrUser("peter1"));
        SolrTweet tw2 = new SolrTweet(2L, "ibood is cool!", new SolrUser("peter2"));
        SolrTweet tw3 = new SolrTweet(3L, "peter iBOOD is cool!", new SolrUser("peter3"));
        SolrTweet tw4 = new SolrTweet(4L, "Ibood is cool!", new SolrUser("peter4"));
        SolrTweet tw5 = new SolrTweet(5L, "iBOOD.com", new SolrUser("peter5"));

        twSearch.update(tw1, false);
        twSearch.update(tw2, false);
        twSearch.update(tw3, false);
        twSearch.update(tw5, false);
        twSearch.update(tw4, true);

        assertEquals(4, twSearch.search("ibood").size());//1,2,3,4 -> missing: 5
        assertEquals(3, twSearch.search("iBood").size());//1,3,5   -> missing: 2,4
        assertEquals(3, twSearch.search("bood").size()); //1,3,5   -> ok
        assertEquals(1, twSearch.search("iBood.com").size()); //1  -> ok        
        assertEquals(0, twSearch.search("ibood.com*").size()); //   -> missing: 5

        //TODO if lowercase filter factory is before worddelemiter 
        // ibood and iBood would match 5 but then bood wouldn't match!
    }

    @Test
    public void testSearchJavaScript() throws Exception {
        // keepwords.txt
        SolrUser peter = new SolrUser("peter1");
        SolrTweet tw1 = new SolrTweet(1L, "peter JavaScript is cool!", peter);
        SolrUser peter2 = new SolrUser("peter2");
        SolrTweet tw2 = new SolrTweet(2L, "java is cool!", peter2);
        SolrTweet tw3 = new SolrTweet(3L, "peter javascript is cool!", new SolrUser("peter3"));
        twSearch.update(tw1, false);
        twSearch.update(tw2, false);
        twSearch.update(tw3, true);

        assertEquals(1, twSearch.search("java").size());
        assertEquals("peter2", twSearch.search("java").iterator().next().getScreenName());
        assertEquals(2, twSearch.search("javascript").size());
        Iterator<SolrUser> iter = twSearch.search("javascript").iterator();
        assertEquals("peter1", iter.next().getScreenName());
        assertEquals("peter3", iter.next().getScreenName());
    }

    @Test
    public void testSorting() throws SolrServerException {
        MyDate day = new MyDate();
        MyDate day2 = day.clone().plusDays(1);
        twSearch.update(createSolrTweet(day, "java is a test!", "peter"), false);
        twSearch.update(createSolrTweet(day2, "java is cool and stable!", "peter2"), true);
        SolrQuery q = new TweetQuery("java").setSort("dt desc");
        List<SolrUser> res = new ArrayList<SolrUser>();
        twSearch.search(res, q);
        assertEquals(2, res.size());
        assertEquals(day2.getTime(), (long) res.get(0).getOwnTweets().iterator().next().getTwitterId());

        q = new TweetQuery("java").setSort("dt asc");
        res.clear();
        twSearch.search(res, q);
        assertEquals(day.getTime(), (long) res.get(0).getOwnTweets().iterator().next().getTwitterId());
    }

    @Test
    public void testLoc() throws SolrServerException {
        SolrUser user = new SolrUser("peter");
        user.setLocation("TEST");
        SolrTweet tw;
        user.addOwnTweet(tw = new SolrTweet(1L, "test tweet text"));
        twSearch.update(tw, false);
        user.addOwnTweet(tw = new SolrTweet(2L, "test tweet text2"));
        twSearch.update(tw, true);
        List<SolrUser> res = new ArrayList<SolrUser>();
        twSearch.search(res, new SolrQuery().setFilterQueries("loc:TEST"));
        assertEquals(1, res.size());
        assertEquals(2, res.get(0).getOwnTweets().size());

        user = new SolrUser("peter");
        tw = new SolrTweet(3L, "test tweet text");
        tw.setLocation("TEST3");
        user.addOwnTweet(tw);
        twSearch.update(tw, false);

        tw = new SolrTweet(4L, "test tweet text");
        tw.setLocation("TEST4");
        user.addOwnTweet(tw);
        twSearch.update(tw, true);
        res = new ArrayList<SolrUser>();
        twSearch.search(res, new SolrQuery().setFilterQueries("loc:TEST3"));
        assertEquals(1, res.size());
        assertEquals(1, res.get(0).getOwnTweets().size());
    }

    @Test
    public void testDelete() throws Exception {
        // do not throw exception
        twSearch.delete(Collections.EMPTY_LIST);

        SolrUser otherUser = new SolrUser("otherUser");
        SolrTweet tw2 = new SolrTweet(2L, "java is cool and stable!", otherUser);
        twSearch.update(tw2, false);
        twSearch.commit();
        assertEquals(1, twSearch.search("java").size());

        twSearch.delete(Arrays.asList(tw2));
        twSearch.commit();
        assertEquals(0, twSearch.search("java").size());
    }

    @Test
    public void testDeleteUsers() throws Exception {
        // do not throw exception
        twSearch.deleteUsers(Collections.EMPTY_LIST);

        SolrUser peter = new SolrUser("peter");
        SolrTweet tw1 = new SolrTweet(1L, "java is cool and stable!", peter);

        SolrUser karsten = new SolrUser("karsten");
        SolrTweet tw2 = new SolrTweet(2L, "oracle is bad!", karsten);

        twSearch.update(tw1, false);
        twSearch.update(tw2, true);
        assertEquals(1, twSearch.search("java").size());
        assertEquals(1, twSearch.search("oracle").size());

        twSearch.deleteUsers(Arrays.asList("peter"));
        twSearch.commit();
        assertEquals(0, twSearch.search("java").size());
        assertEquals(1, twSearch.search("oracle").size());
    }

    @Test
    public void testGetReplies() {
        SolrUser usera = new SolrUser("usera");
        SolrTweet tw = new SolrTweet(1L, "this is a Test ", usera);
        SolrUser userb = new SolrUser("userb");
        SolrTweet tw2 = new SolrTweet(2L, "this is a Test ", userb);
        tw2.addReply(tw);
        twSearch.update(tw, true);
        twSearch.update(tw2, true);

        assertEquals(0, twSearch.searchReplies(1L, true).size());
        assertEquals(0, twSearch.searchReplies(2L, true).size());
        assertEquals(0, twSearch.searchReplies(1L, false).size());
        assertEquals(1, twSearch.searchReplies(2L, false).size());
        assertEquals(1L, (long) twSearch.searchReplies(2L, false).iterator().next().getTwitterId());
    }

    @Test
    public void testGetRetweets() {
        SolrUser usera = new SolrUser("usera");
        SolrTweet tw = new SolrTweet(1L, "this is a Test ", usera);
        SolrUser userb = new SolrUser("userb");
        SolrTweet tw2 = new SolrTweet(2L, "rt @usera: this is a Test ", userb);
        tw.addReply(tw2);
        twSearch.update(tw, false);
        twSearch.update(tw2, true);

        assertEquals(1, twSearch.searchReplies(1L, true).size());
        assertEquals(0, twSearch.searchReplies(2L, true).size());
        assertEquals(0, twSearch.searchReplies(1L, false).size());
        assertEquals(0, twSearch.searchReplies(2L, false).size());
        assertEquals(2L, (long) twSearch.searchReplies(1L, true).iterator().next().getTwitterId());
    }

    @Test
    public void testSimilarQuery() {
        SolrQuery q = new TweetQuery().createSimilarQuery(new SolrTweet(1L, "Test test jAva http://blabli"));
        assertTrue(q.getQuery().contains("test"));
        assertTrue(q.getQuery().contains("java"));
        assertFalse(q.getQuery().contains("http"));
        q = new TweetQuery().createSimilarQuery(new SolrTweet(1L, "RT @user: test"));
        assertFalse(q.getQuery().contains("user"));
    }

    @Test
    public void testNewUpdate() {
        List<Tweet> list = new ArrayList<Tweet>();

        list.add(createTweet(1L, "text", "usera"));
        list.add(createTweet(2L, "RT @usera: text", "userb"));

        list.add(createTweet(3L, "text2", "usera"));
        list.add(createTweet(4L, "hey I read your text", "userb").setInReplyToStatusId(3L));

        Collection<SolrTweet> res = twSearch.update(list, new Date(0));
        assertEquals(4, res.size());

        assertEquals(1, twSearch.findByTwitterId(1L).getReplyCount());
        assertEquals(1, twSearch.findByTwitterId(1L).getRetweetCount());

        assertEquals(0, twSearch.findByTwitterId(2L).getReplyCount());
        assertEquals(0, twSearch.findByTwitterId(2L).getRetweetCount());

        assertEquals(1, twSearch.findByTwitterId(3L).getReplyCount());
        assertEquals(0, twSearch.findByTwitterId(3L).getRetweetCount());

        assertEquals(0, twSearch.findByTwitterId(4L).getReplyCount());
        assertEquals(0, twSearch.findByTwitterId(4L).getRetweetCount());
    }

    @Test
    public void testConnectTwitterId() throws Exception {
        // A has replies B and C
        // C has replies D

        // store A and D
        twSearch.privateUpdate(Arrays.asList(createTweet(1L, "A", "u1"),
                createTweet(4L, "D", "u4").setInReplyToStatusId(3L)));

        twSearch.update(createTweet(3L, "C", "u3").setInReplyToStatusId(1L));

        // now check if C was properly connected with A and D
        SolrTweet twC = twSearch.findByTwitterId(3L);
        assertEquals(1, twC.getReplyCount());

        SolrTweet twA = twSearch.findByTwitterId(1L);
        assertEquals(1, twA.getReplyCount());

        // now check if B was properly connected with A
        twSearch.update(createTweet(2L, "B", "u2").setInReplyToStatusId(1L));

        twA = twSearch.findByTwitterId(1L);
        assertEquals(2, twA.getReplyCount());
    }

    @Test
    public void testAttach() throws Exception {
        Twitter4JTweet tw = new Twitter4JTweet(1, "test", "peter");
        twSearch.update(tw);

        tw = new Twitter4JTweet(2, "test2", "peter");
        twSearch.update(tw);

        assertEquals(2, twSearch.findBSolrUserName("peter").getOwnTweets().size());
    }

    @Test
    public void testDoNotSaveSecondUser() {
        Twitter4JTweet fTweet = new Twitter4JTweet(5, "@peter @karsten bla bli", "peter");
        twSearch.update(fTweet);

        assertNull(twSearch.findBSolrUserName("karsten"));
        assertNotNull(twSearch.findBSolrUserName("peter"));
    }

    @Test
    public void testDoSaveDuplicate() {
        twSearch.update(new Twitter4JTweet(4, "@peter bla bli", "peter"));
        twSearch.update(new Twitter4JTweet(5, "@peter bla bli", "karsten"));

        assertNotNull(twSearch.findBSolrUserName("karsten"));
        assertNotNull(twSearch.findBSolrUserName("peter"));
    }

    @Test
    public void testIdVsName() {
        Twitter4JTweet fTweet = new Twitter4JTweet(5, "@karsten bla bli", "peter");
        twSearch.update(fTweet);

        fTweet = new Twitter4JTweet(6, "@peter bla bli", "karsten");
        twSearch.update(fTweet);
        assertNotNull(twSearch.findBSolrUserName("karsten"));
    }

    @Test
    public void testNoDuplicateUser2() {
        Twitter4JTweet fTweet = new Twitter4JTweet(1, "@karsten bla bli", "peter");
        twSearch.update(fTweet);

        fTweet = new Twitter4JTweet(2, "@Karsten bla bli", "Peter");
        twSearch.update(fTweet);
    }

    @Test
    public void testNoDuplicateTweet() {
        Twitter4JTweet fTweet = new Twitter4JTweet(123, "@karsten bla bli", "peter");
        twSearch.update(fTweet);
        twSearch.update(fTweet);

        assertEquals(1, twSearch.countAll());
        assertEquals(1, twSearch.findBSolrUserName("peter").getOwnTweets().size());
    }

    @Test
    public void testUpdateTweetsWhichIsInfluencedFromActivationDepth() throws Exception {
        Twitter4JTweet tw1 = new Twitter4JTweet(1L, "tweet1", "peter");
        Twitter4JTweet tw2 = new Twitter4JTweet(2L, "tweet2", "peter");

        twSearch.update(tw1);
        twSearch.update(tw2);

        assertEquals(2, twSearch.findBSolrUserName("peter").getOwnTweets().size());

        tw1 = new Twitter4JTweet(1L, "tweet1", "peter");
        twSearch.update(tw1);

        assertEquals(2, twSearch.findBSolrUserName("peter").getOwnTweets().size());
    }

    @Test
    public void testUpdateAndRemove() throws Exception {
        Twitter4JTweet tw1 = new Twitter4JTweet(1L, "@karsten hajo", "peter");
        tw1.setCreatedAt(new MyDate().minusDays(2).toDate());

        twSearch.update(tw1);
        assertEquals(1, twSearch.countAll());
        assertEquals("@karsten hajo", twSearch.search("hajo").iterator().next().getOwnTweets().iterator().next().getText());
        assertEquals(1, twSearch.findBSolrUserName("peter").getOwnTweets().size());

        Collection<SolrTweet> res = twSearch.update(Arrays.asList(new Twitter4JTweet(2L, "test", "peter")),
                new MyDate().minusDays(1).toDate());
        assertEquals(1, res.size());
        assertEquals(1, twSearch.countAll());
        assertEquals(0, twSearch.search("hajo").size());
        assertEquals(1, twSearch.findBSolrUserName("peter").getOwnTweets().size());
    }

    @Test
    public void testDoubleUpdateShouldIncreaseReplies() throws Exception {
        twSearch.privateUpdate(Arrays.asList(createTweet(1L, "bla bli blu", "userA"),
                createTweet(2L, "RT @userA: bla bli blu", "userC")));

        assertEquals(1, twSearch.findByTwitterId(1L).getReplyCount());
        assertEquals(1, twSearch.findByTwitterId(1L).getRetweetCount());

        twSearch.privateUpdate(Arrays.asList(
                new Twitter4JTweet(3L, "RT @userA: bla bli blu", "userC"),
                new Twitter4JTweet(4L, "RT @userA: bla bli blu", "userD")));

        assertEquals(2, twSearch.findByTwitterId(1L).getReplyCount());
        assertEquals(2, twSearch.findByTwitterId(1L).getRetweetCount());

        assertEquals(0, twSearch.findByTwitterId(2L).getReplyCount());
        assertEquals(0, twSearch.findByTwitterId(3L).getReplyCount());
        assertEquals(0, twSearch.findByTwitterId(4L).getReplyCount());

        twSearch.privateUpdate(Arrays.asList(
                new Twitter4JTweet(5L, "RT @userA: bla bli blu", "userE")));

        assertEquals(3, twSearch.findByTwitterId(1L).getReplyCount());
        assertEquals(3, twSearch.findByTwitterId(1L).getRetweetCount());
    }

    @Test
    public void testConnectTweets() throws Exception {
        // A has reply B        
        twSearch.privateUpdate(Arrays.asList(createTweet(1L, "bla bli blu", "userA"),
                createTweet(2L, "RT @userA: bla bli blu", "userC")));
        assertEquals(1, twSearch.findByTwitterId(1L).getReplyCount());

        twSearch.update(createTweet(3L, "@userXY see this nice fact: RT @userA: bla bli blu", "userB"));

        assertEquals(2, twSearch.findByTwitterId(1L).getReplyCount());
    }

    @Test
    public void testProcessToUser() throws Exception {
        twSearch.update(new Twitter4JTweet(1L, "@userA bla bli blu", "userB"));
        twSearch.update(new Twitter4JTweet(2L, "RT @userB: @userA bla bli blu", "userA"));
        assertEquals(2, twSearch.countAll());
        assertEquals(1, twSearch.findByTwitterId(1L).getReplyCount());
        assertEquals(1, twSearch.findByTwitterId(1L).getRetweetCount());
        assertEquals(0, twSearch.findByTwitterId(2L).getReplyCount());
        assertEquals(0, twSearch.findByTwitterId(2L).getRetweetCount());
    }

    @Test
    public void testDoNotAllowSelfRetweets() throws Exception {
        twSearch.update(new Twitter4JTweet(1L, "bla bli blu", "userA"));
        twSearch.update(new Twitter4JTweet(2L, "RT @userA: bla bli blu", "userA"));

        assertEquals(0, twSearch.findByTwitterId(1L).getReplyCount());
    }

    @Test
    public void testDoNotAddDuplicateRetweets() throws Exception {
        twSearch.update(new Twitter4JTweet(1L, "bla bli blu", "userA"));
        assertEquals(0, twSearch.findByTwitterId(1L).getReplyCount());

        twSearch.update(new Twitter4JTweet(2L, "RT @userA: bla bli blu", "userB"));
        assertEquals(1, twSearch.findByTwitterId(1L).getRetweetCount());

        twSearch.update(new Twitter4JTweet(3L, "RT @userA: bla bli blu", "userB"));
        assertEquals(1, twSearch.findByTwitterId(1L).getRetweetCount());
    }

    @Test
    public void testDoNotAddOldTweets() {
        Twitter4JTweet tw = new Twitter4JTweet(2L, "RT @userA: bla bli blu", "userB");
        tw.setCreatedAt(new MyDate().minusDays(2).toDate());
        assertEquals(0, twSearch.update(Arrays.asList(tw),
                new MyDate().minusDays(1).toDate()).size());
    }

    @Test
    public void testAddOldTweetsForJetwickSource() {
        Twitter4JTweet tw = new Twitter4JTweet(2L, "RT @userA: bla bli blu", "userB");
        tw.setSource(TwitterSearch.SOURCE);
        tw.setCreatedAt(new MyDate().minusDays(2).toDate());
        assertEquals(1, twSearch.update(tw).size());
    }

    @Test
    public void testDontRemoveOldIfJetwicked() throws Exception {
        Twitter4JTweet tw2 = new Twitter4JTweet(2L, "RT @userA: bla bli blu", "userB");
        tw2.setSource(TwitterSearch.SOURCE);
        tw2.setCreatedAt(new MyDate().minusDays(2).toDate());
        assertEquals(1, twSearch.update(tw2).size());
        assertNotNull(twSearch.findByTwitterId(2L).getUpdatedAt());

        Twitter4JTweet tw3 = new Twitter4JTweet(3L, "another tweet grabbed from search", "userB");
        Collection<SolrTweet> res = twSearch.update(Arrays.asList(tw3), new MyDate().minusDays(1).toDate());
        assertEquals(1, res.size());
        // TODO
//        assertEquals(0, res.getDeletedTweets().size());
    }

    @Test
    public void testComplexUpdate() throws Exception {
        Twitter4JTweet tw1 = createTweet(1L, "bla bli blu", "userA");
        tw1.setCreatedAt(new MyDate().minusDays(2).toDate());

        Twitter4JTweet tw2 = createTweet(2L, "rt @usera: bla bli blu", "userB");
        tw2.setCreatedAt(new MyDate().minusDays(2).plusMinutes(1).toDate());

        Twitter4JTweet tw3 = createTweet(3L, "rt @usera: bla bli blu", "userC");
        tw3.setCreatedAt(new MyDate().minusDays(2).plusMinutes(1).toDate());

        Twitter4JTweet tw4 = createTweet(4L, "rt @usera: bla bli blu", "userD");
        tw4.setCreatedAt(new MyDate().minusDays(2).plusMinutes(1).toDate());

        Collection<SolrTweet> res = twSearch.privateUpdate(Arrays.asList(tw1, tw2, tw3, tw4));
        assertEquals(1, twSearch.findBSolrUserName("usera").getOwnTweets().size());
        assertEquals(3, twSearch.findByTwitterId(1L).getReplyCount());
        assertEquals(4, res.size());

        Twitter4JTweet tw100 = new Twitter4JTweet(100L, "newtext", "usera");
        Twitter4JTweet tw101 = new Twitter4JTweet(101L, "newtext two", "usera");
        Twitter4JTweet tw102 = new Twitter4JTweet(102L, "newbla one", "userd");
        Twitter4JTweet tw103 = new Twitter4JTweet(103L, "newbla two", "userd");
        Twitter4JTweet tw104 = new Twitter4JTweet(104L, "rt @usera: newtext two", "userc");
        tw104.setCreatedAt(new MyDate(tw101.getCreatedAt()).plusMinutes(1).toDate());

        res = twSearch.update(Arrays.asList(tw100, tw101, tw102, tw103, tw104), new MyDate().minusDays(1).toDate());
        assertEquals(5, twSearch.countAll());
        assertEquals(1, twSearch.findByTwitterId(101L).getReplyCount());
        assertEquals(5, res.size());
        assertEquals(5, twSearch.countAll());

        // no tweet exists with that string
        assertEquals(0, twSearch.search("bla bli blu").size());
    }

    @Test
    public void testDoNotThrowQueryParserException() {
        Twitter4JTweet tw = createTweet(1L, "rt @jenny2s: -- Earth, Wind & Fire - September  (From \"Live In Japan\")"
                + " http://www.youtube.com/watch?v=hy-huQAMPQA via @youtube --- HAPPY SEPTEMBER !!", "usera");
        twSearch.update(tw);
    }

    @Test
    public void testUpdateList() {
        assertEquals(1, twSearch.privateUpdate(Arrays.asList(new Twitter4JTweet(1L, "test", "peter"),
                new Twitter4JTweet(1L, "test", "peter"))).size());
        assertNotNull(twSearch.findByTwitterId(1L));
    }

    @Test
    public void testQueryChoices() throws SolrServerException {
        twSearch.setTermMinFrequency(0);
        twSearch.privateUpdate(Arrays.asList(createTweet(1L, "obama obama", "usera"),
                createTweet(2L, "bluest obama obama", "usera"),
                createTweet(3L, "bluest bluest obama", "usera"),
                createTweet(4L, "obama bluest again and again", "usera")));

        assertEquals(3L, twSearch.search(new SolrQuery().addFilterQuery("tag:bluest")).getResults().getNumFound());

        Collection<String> coll = twSearch.getQueryChoices(null, "obama");
        assertEquals(0, coll.size());
//        assertTrue(coll.contains("obama"));

        coll = twSearch.getQueryChoices(null, "obama ");
        assertEquals(1, coll.size());
        assertTrue(coll.contains("obama bluest"));
    }

    @Test
    public void testQueryChoicesWithoutDateRestrictions() throws SolrServerException {
        twSearch.setTermMinFrequency(0);
        twSearch.privateUpdate(Arrays.asList(createTweet(new MyDate().minusDays(1).minusMinutes(3), "obama obama", "usera"),
                createTweet(new MyDate().minusDays(1).minusMinutes(2), "bluest obama obama", "usera"),
                createTweet(new MyDate().minusDays(1).minusMinutes(1), "bluest bluest obama", "usera"),
                createTweet(new MyDate().minusDays(1), "obama bluest again and again", "usera")));

        assertEquals(3L, twSearch.search(new SolrQuery().addFilterQuery("tag:bluest")).getResults().getNumFound());

        Collection<String> coll = twSearch.getQueryChoices(new TweetQuery("").addFilterQuery(SolrTweetSearch.FILTER_ENTRY_LATEST_DT), "obama ");
        assertEquals(1, coll.size());
        assertTrue(coll.contains("obama bluest"));
    }

    @Test
    public void testFindOrigin() {
        twSearch.privateUpdate(Arrays.asList(createTweet(1L, "text", "usera"),
                createTweet(2L, "RT @usera: text", "userb"),
                createTweet(3L, "RT @usera: text", "userc"),
                createTweet(4L, "new text", "userd")));

        SolrQuery q = twSearch.createFindOriginQuery(null, "text", 1);
        assertEquals(2, q.getFilterQueries().length);
        assertEquals(SolrTweetSearch.RT_COUNT + ":[1 TO *]", q.getFilterQueries()[1]);

        // too high minResults
        int minResults = 3;
        q = twSearch.createFindOriginQuery(null, "text", minResults);
        assertEquals(1, q.getFilterQueries().length);

        // no retweets for 'new text'
        q = twSearch.createFindOriginQuery(null, "new text", 2);
        assertEquals(1, q.getFilterQueries().length);
    }

    @Test
    public void testTags() throws SolrServerException {
        twSearch.privateUpdate(Arrays.asList(createTweet(1L, "Beitrag des @WDR zum Thema #Atomkraft: "
                + "Investitionsruinen im Wert von 360 Milliarden Euro."
                + "Milliardengrab Atomkraft http://www.wdr.de/themen/wirtschaft/wirtschafts... #atom", "micha_koester"),
                createTweet(2L, "Morgenlektüre: Öko-Aktivist Sweeney über Uranabbau - "
                + "\"Der Castor beginnt in Australien\" @tazonline http://ow.ly/2zWS9 #atom #gruene", "micha_koester"),
                createTweet(3L, "third tweet", "micha_koester")));

        QueryResponse rsp = twSearch.search(new TweetQuery("#atom"));
        assertEquals(1, rsp.getFacetField("tag").getValues().size());

        rsp = twSearch.search(new SolrQuery().addFilterQuery("tag:atom"));
        assertEquals(2, twSearch.collectTweets(rsp).size());
    }

    @Test
    public void testReadUrlEntries() throws IOException {
        SolrTweet tw = new SolrTweet(1L, "text", new SolrUser("peter"));
        List<UrlEntry> entries = new ArrayList<UrlEntry>();

        UrlEntry urlEntry = new UrlEntry(0, 20, "http://test.de/bla");
        urlEntry.setResolvedDomain("");
        urlEntry.setResolvedTitle("");
        entries.add(urlEntry);

        urlEntry = new UrlEntry(5, 17, "");
        urlEntry.setResolvedDomain("");
        urlEntry.setResolvedTitle("");
        entries.add(urlEntry);

        urlEntry = new UrlEntry(2, 18, "http://fulltest.de/bla");
        urlEntry.setResolvedDomain("resolved-domain.de");
        urlEntry.setResolvedTitle("ResolvedTitel");
        entries.add(urlEntry);

        tw.setUrlEntries(entries);
        SolrInputDocument iDoc = twSearch.createDoc(tw);
        SolrDocument doc = new SolrDocument();
        for (Entry<String, SolrInputField> entry : iDoc.entrySet()) {
            doc.put(entry.getKey(), entry.getValue().getValue());
        }

        SolrTweet tw2 = twSearch.readDoc(doc, null);
        assertEquals(1, tw2.getUrlEntries().size());
        Iterator<UrlEntry> iter = tw2.getUrlEntries().iterator();
        urlEntry = iter.next();
        assertEquals("http://fulltest.de/bla", urlEntry.getResolvedUrl());
        assertEquals("resolved-domain.de", urlEntry.getResolvedDomain());
        assertEquals("ResolvedTitel", urlEntry.getResolvedTitle());
        assertEquals(2, urlEntry.getIndex());
        assertEquals(18, urlEntry.getLastIndex());
    }

    @Test
    public void testGetMoreTweets() throws IOException {
        // fill index with 2 tweets and 1 user
        SolrTweet tw2;
        twSearch.update(Arrays.asList(
                createSolrTweet(1L, "test", "peter"),
                tw2 = createSolrTweet(2L, "text", "peter")));
        twSearch.commit();

        Map<Long, SolrTweet> alreadyExistingTw = new LinkedHashMap<Long, SolrTweet>();
        alreadyExistingTw.put(2L, tw2);
        Map<String, SolrUser> users = new LinkedHashMap<String, SolrUser>();
        SolrUser u = new SolrUser("peter");
        users.put("peter", u);

        // return the tweet (1L) which is not already in the map!
        twSearch.fetchMoreTweets(alreadyExistingTw, users);
        assertEquals(1, u.getOwnTweets().size());
        assertEquals(1L, (long) u.getOwnTweets().iterator().next().getTwitterId());
    }

    SolrTweet createSolrTweet(MyDate dt, String twText, String user) {
        return new SolrTweet(dt.getTime(), twText, new SolrUser(user)).setCreatedAt(dt.toDate());
    }

    SolrTweet createSolrTweet(long id, String twText, String user) {
        return new SolrTweet(id, twText, new SolrUser(user)).setCreatedAt(new Date(id));
    }

    Twitter4JTweet createTweet(long id, String twText, String user) {
        return new Twitter4JTweet(id, twText, user).setCreatedAt(new Date(id));
    }

    Twitter4JTweet createTweet(MyDate dt, String twText, String user) {
        return new Twitter4JTweet(dt.getTime(), twText, user).setCreatedAt(dt.toDate());
    }
}
