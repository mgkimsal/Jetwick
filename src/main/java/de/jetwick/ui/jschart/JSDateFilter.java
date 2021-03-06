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

package de.jetwick.ui.jschart;

import de.jetwick.solr.SolrTweetSearch;
import de.jetwick.ui.util.FacetHelper;
import de.jetwick.ui.util.LabeledLink;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.behavior.AttributeAppender;
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
public class JSDateFilter extends Panel {

    private List<FacetHelper> facetList = new ArrayList<FacetHelper>();
    private String dtKey = SolrTweetSearch.DATE;
    private final float MAX_HEIGHT_IN_PX = 50.0f;
    private long max = 1;
    private long totalHits = 0;

    public JSDateFilter(String id) {
        super(id);

        final String dtVal = "Date Filter";
        List<String> dateFilterList = new ArrayList<String>();
        dateFilterList.add(dtKey);

        // TODO WICKET update dateFilter even if we call only update(rsp)
        ListView dateFilter = new ListView("dateFilterParent", dateFilterList) {

            @Override
            public void populateItem(final ListItem item) {
                String filter = getFilterName(dtKey);
                if (filter != null) {
                    item.add(new LabeledLink("dateFilter", "View results for all days") {

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            onFacetChange(target, dtKey, null);
                        }
                    }.add(new AttributeAppender("title", new Model("Remove all filters from '" + dtVal + "'"), " ")));
                } else {
                    String str = "";
                    if (totalHits > 0)
                        str = "Select a date to filter results";
                    Label label = new Label("dateFilter", str);
                    label.add(new AttributeAppender("class", new Model("gray"), " "));
                    item.add(label);
                }
            }
        };
        add(dateFilter);

        ListView items = new ListView("items", facetList) {

            @Override
            public void populateItem(final ListItem item) {
                float zoomer = MAX_HEIGHT_IN_PX / max;
                final FacetHelper entry = (FacetHelper) item.getModelObject();

                Label bar = new Label("itemSpan");
                AttributeAppender app = new AttributeAppender("title", new Model(entry.count + " tweets"), " ");
                bar.add(app).add(new AttributeAppender("style", new Model("height:" + (int) (zoomer * entry.count) + "px"), " "));
                final boolean selected = isAlreadyFiltered(entry.getFilter());
                Link link = new /*Indicating*/AjaxFallbackLink("itemLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        JSDateFilter.this.onFacetChange(target, entry.getFilter(), !selected);
                    }
                };
                link.add(app);
                Label label = new Label("itemLabel", entry.displayName);
                link.add(bar).add(label);
                if (entry.count == 0) {
                    link.setEnabled(false);
                    link.add(new AttributeAppender("class", new Model("gray"), " "));
                }

                if (selected)
                    link.add(new AttributeAppender("class", new Model("filter-rm"), " "));
                else
                    link.add(new AttributeAppender("class", new Model("filter-add"), " "));
                item.add(link);
            }
        };

        add(items);
    }

    protected void onFacetChange(AjaxRequestTarget target, String filter, Boolean selected) {
    }

    protected boolean isAlreadyFiltered(String filter) {
        return false;
    }

    public String getFilterName(String name) {
        return name;
    }

    public void update(QueryResponse rsp) {
        facetList.clear();
        if (rsp == null)
            return;

        totalHits = rsp.getResults().getNumFound();
        Integer count;
        Map<String, Integer> facetQueries = null;
        if (rsp != null) {
            facetQueries = rsp.getFacetQuery();

            // exclude smaller zero?
            count = getFacetQueryCount(facetQueries, SolrTweetSearch.FILTER_ENTRY_LATEST_DT);
            if (count == null)
                count = 0;
            facetList.add(new FacetHelper(dtKey, SolrTweetSearch.FILTER_VALUE_LATEST_DT, "last 8h", count));

            List<FacetField> dateFacets = rsp.getFacetDates();
            if (dateFacets != null) {
                for (FacetField ff : dateFacets) {
                    if (ff.getValues() != null && dtKey.equals(ff.getName())) {
                        Collections.reverse(ff.getValues());
                        for (Count cnt : ff.getValues()) {
                            String name = cnt.getName();
                            String display = "";
                            String filter = "[" + cnt.getName() + " TO " + cnt.getName() + "/DAY" + ff.getGap() + "]";
                            // ignore year and time
                            int index = name.indexOf("T");
                            if (index > 0)
                                display = name.substring(5, index);

                            facetList.add(new FacetHelper(dtKey, filter, display, cnt.getCount()));
                        }
                    }
                }
            }
        }

        count = getFacetQueryCount(facetQueries, SolrTweetSearch.FILTER_ENTRY_OLD_DT);
        if (count == null)
            count = 0;
        facetList.add(new FacetHelper(dtKey, SolrTweetSearch.FILTER_VALUE_OLD_DT, "older", count));

        max = 1;
        for (FacetHelper h : facetList) {
            if (h.count > max)
                max = h.count;
        }
    }

    public List<FacetHelper> getFacetList() {
        return facetList;
    }

    public static Integer getFacetQueryCount(Map<String, Integer> facetQueries, String entry) {
        if (facetQueries != null)
            return facetQueries.get(entry);
        return null;
    }
//    private List<Object[]> entryList = new ArrayList<Object[]>();
//    private long max = 1;
//
//    public JSDateFilter(String id) {
//        super(id);
//
//        ListView items = new ListView("items", entryList) {
//
//            @Override
//            public void populateItem(final ListItem item) {
//                float zoomer = MAX_HEIGHT_IN_PX / max;
//                final Object[] entry = (Object[]) item.getModelObject();
//                String strValue = (String) entry[0];
//                Integer count = (Integer) entry[1];
//                Label bar = new Label("itemSpan");
//
//                AttributeAppender app = new AttributeAppender("title", new Model(count + " entries"), " ");
//                bar.add(app).add(new AttributeAppender("style", new Model("height:" + (int) (zoomer * count) + "px"), " "));
//                Link link = new IndicatingAjaxFallbackLink("itemLink") {
//
//                    @Override
//                    public void onClick(AjaxRequestTarget target) {
//                        //TODO
//                    }
//                };
//                link.add(app);
//                Label label = new Label("itemLabel", strValue);
//                link.add(bar).add(label);
//                if (count == 0) {
//                    link.setEnabled(false);
//                    link.add(new AttributeAppender("class", new Model("gray"), " "));
//                }
//
////                if (selected)
////                    link.add(new AttributeAppender("class", new Model("filter-rm"), " "));
////                else
////                    link.add(new AttributeAppender("class", new Model("filter-add"), " "));
//
//                item.add(link);
//            }
//        };
//
//        add(items);
//    }
//
//    public void update(Map<String, Integer> map) {
//        entryList.clear();
//        max = 1;
//        for (Entry<String, Integer> e : map.entrySet()) {
//            entryList.add(new Object[]{e.getKey(), e.getValue()});
//            if (e.getValue() > max)
//                max = e.getValue();
//        }
//    }
}
