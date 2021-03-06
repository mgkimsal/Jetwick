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

package de.jetwick.ui;

import de.jetwick.solr.SolrTweetSearch;
import de.jetwick.ui.util.LabeledLink;
import de.jetwick.util.Helper;
import de.jetwick.util.MapEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

/**
 *
 * @author Peter Karich, peat_hal 'at' users 'dot' sourceforge 'dot' net
 */
public class UrlTrendPanel extends Panel {

    private static String TITLE = "Links found in your search results";
    private List<Entry<String, Long>> urls = new ArrayList<Entry<String, Long>>();
    private List<Boolean> filter = new ArrayList<Boolean>();
    private String cssString = "";
    private int counter = 0;

    public UrlTrendPanel(String id) {
        super(id);
        // to hide + show on ajax request
        setOutputMarkupPlaceholderTag(true);

        // TODO WICKET: update urlTitle even if we call only update(rsp)
        add(new ListView("urlTitle", filter) {

            @Override
            public void populateItem(final ListItem item) {
                boolean filteredUrl = (Boolean) item.getModelObject();
                if (filteredUrl) {
                    item.add(new LabeledLink("filterName", "< " + TITLE) {

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            onUrlClick(target, null);
                        }
                    }.add(new AttributeAppender("title", new Model("Deselect all url filters"), " ")));
                } else
                    item.add(new Label("filterName", TITLE));
            }
        });

        ListView urlListView = new ListView("urls", urls) {

            @Override
            public void populateItem(final ListItem item) {
                final Entry<String, Long> url = (Entry) item.getModelObject();

                String title = Helper.trimAll(url.getKey());
                title = Helper.htmlEntityDecode(title);

                MarkupContainer moreUrlInfo = new MarkupContainer("moreUrlInfo") {
                };
                String cl = "moreUrl" + ++counter;
                moreUrlInfo.add(new AttributeAppender("class", new Model(cl), " "));
                addUrl(cl);
                item.add(moreUrlInfo);


                Link moreUrlLink = new AjaxFallbackLink("moreUrlLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onUrlClick(target, url.getKey());
                    }
                };
                moreUrlLink.add(new Label("urlLabel", title));
                moreUrlInfo.add(moreUrlLink);

                Link link = new AjaxFallbackLink("urlLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onUrlClick(target, url.getKey());
                    }
                };
                link.add(new Label("urlLabel", title));
                moreUrlInfo.add(link);
            }
        };
        add(urlListView);

        HeaderContributor c = new HeaderContributor(new IHeaderContributor() {

            @Override
            public void renderHead(IHeaderResponse response) {
                response.renderString("<style type='text/css'>\n" + cssString + "</style>");
            }
        });
        add(c);
    }

    public void addUrl(String cssClass) {
        cssString += "." + cssClass + ":hover .url-more { display: block;}\n";
        cssString += "." + cssClass + ":hover .url-less { display: none;}";
    }

    protected void onUrlClick(AjaxRequestTarget target, String name) {
    }

    public void update(QueryResponse rsp, SolrQuery query) {
        cssString = "";
        counter = 0;
        filter.clear();
        filter.add(false);

        urls.clear();
        if (rsp != null) {
            if (query.getFilterQueries() != null)
                for (String str : query.getFilterQueries()) {
                    if (str.startsWith(SolrTweetSearch.FIRST_URL_TITLE + ":"))
                        filter.set(0, true);
                }

            List<FacetField> facetFields = rsp.getFacetFields();
            if (facetFields != null)
                for (FacetField ff : facetFields) {
                    if (SolrTweetSearch.FIRST_URL_TITLE.equals(ff.getName()) && ff.getValues() != null) {
                        for (Count cnt : ff.getValues()) {
                            // although we avoid indexing empty title -> its save to do it again ;-)
                            if (!cnt.getName().isEmpty())
                                urls.add(new MapEntry<String, Long>(cnt.getName(), cnt.getCount()));
                        }
                        break;
                    }
                }
        }
        setVisible(urls.size() > 0);
    }
}
