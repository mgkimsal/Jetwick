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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Peter Karich, peat_hal 'at' users 'dot' sourceforge 'dot' net
 */
public class NavigationPanel extends Panel {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private Link prev;
    private Link next;
    private int hitsPerPage;
    private int page = 0;
    private long hits = 0;

    /**
     *  for test only
     */
    public NavigationPanel(String id) {
        super(id);
        next = new Link("next") {

            @Override
            public void onClick() {
                page++;
                onPageChange(null, page);
            }
        };
        add(next);

        prev = new Link("pre") {

            @Override
            public void onClick() {
                page--;
                onPageChange(null, page);
            }
        };
        add(prev);
    }

    public NavigationPanel(String id, int hitsPerPage) {
        this(id);
        this.hitsPerPage = hitsPerPage;
    }

    public void onPageChange(AjaxRequestTarget target, int page) {
    }

    public void updateVisibility() {
        next.setVisible(isNextPossible());
        prev.setVisible(isPreviousPossible());
    }

    boolean isNextPossible() {
        return page + 1 < (float) hits / hitsPerPage;
    }

    boolean isPreviousPossible() {
        return page - 1 >= 0;
    }

    public void setHits(long hits) {
        this.hits = hits;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setHitsPerPage(int hitsPerPage) {
        this.hitsPerPage = hitsPerPage;
    }
}
