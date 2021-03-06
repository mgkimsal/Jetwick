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

import de.jetwick.util.Helper;
import de.jetwick.util.StopWatch;
import de.jetwick.data.UrlEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import static de.jetwick.util.Helper.*;

/**
 *
 * @author Peter Karich, peat_hal 'at' users 'dot' sourceforge 'dot' net
 */
public class UrlExtractor extends Extractor {

    private StopWatch sw = new StopWatch("");
    private int resolveTimeout = 500;
    private List<UrlEntry> urlEntries = new ArrayList<UrlEntry>();

    public UrlExtractor setResolveTimeout(int resolveTimeout) {
        this.resolveTimeout = resolveTimeout;
        return this;
    }

    @Override
    public String toLink(String url, String title) {
        return url;
    }

    @Override
    public UrlExtractor setText(String text) {
        super.setText(text);
        return this;
    }

    public Collection<UrlEntry> getUrlEntries() {
        return urlEntries;
    }

    @Override
    public UrlExtractor run() {
        int index = 0;

        for (; (index = text.indexOf("http://", index)) >= 0; index++) {
            boolean resolveUrl = true;
            String subStr = text.substring(index);
            // this shouldn't be an url shortener:
            if (subStr.startsWith("http://www."))
                resolveUrl = false;

            // url shorteners seems to have a "domain.de" shorter or equal to 11
            // the longest was tinyurl.com the shortest is t.co
            int index2 = subStr.indexOf("/", 7);
            if (index2 < 0)
                index2 = Math.max(0, subStr.indexOf(" ", 7));

            if (index2 >= 11 + 7 || index2 < 4)
                resolveUrl = false;

            String domain = subStr.substring(0, index2);
            index2 = domain.lastIndexOf(".");
            if (index2 < 0 || domain.substring(index2).length() < 3)
                resolveUrl = false;

            StringBuilder tmpSb = new StringBuilder();
            int lastIndex = onNewRawUrl(index, tmpSb);
            
            if (lastIndex > 0) {
                String url = tmpSb.toString();
                if (resolveUrl) {
                    String newUrl = resolveOneUrl(url, resolveTimeout);
                    if (newUrl.length() > 0)
                        url = newUrl;
                }
                UrlEntry entry = new UrlEntry(index, lastIndex, url);
                sw.start();
                String str[] = getInfo(url, resolveTimeout);
                entry.setResolvedTitle(str[0]);
                entry.setResolvedSnippet(str[1]);
                sw.stop();
                entry.setResolvedDomain(Helper.extractDomain(url));

                urlEntries.add(entry);
            }
        }

        return this;
    }

    public long getTime() {
        return sw.getTime();
    }

    public UrlExtractor oldRun() {
        int index = 0;
        int offset = 0;
        sb = new StringBuilder(text);
        for (; (index = text.indexOf("http://", index)) >= 0; index++) {
            String subStr = text.substring(index);
            // this shouldn't be an url shortener:
            if (subStr.startsWith("http://www."))
                continue;
            // url shorteners seems to have a "domain.de" shorter or equal to 11
            // the longest was tinyurl.com the shortest is t.co
            int index2 = subStr.indexOf("/", HTTP.length());
            if (index2 < 0)
                index2 = Math.max(0, subStr.indexOf(" ", HTTP.length()));

            if (index2 >= 11 + HTTP.length() || index2 < 4)
                continue;

            String domain = subStr.substring(0, index2);
            index2 = domain.lastIndexOf(".");
            if (index2 < 0 || domain.substring(index2).length() < 3)
                continue;

            StringBuilder tmpSb = new StringBuilder();
            int lastIndex = onNewRawUrl(index, tmpSb);
            if (lastIndex > 0) {
                String oldUrl = tmpSb.toString();
//                long start = System.currentTimeMillis();
                String newUrl = resolveOneUrl(oldUrl, resolveTimeout);
//                long end = System.currentTimeMillis();
//                if (end - start > 1000)
//                    System.out.println("too slow:" + onNewUrl + "\t\t || " + tw.getText());

                sb.delete(index + offset, lastIndex + offset);
                sb.insert(index + offset, newUrl);
                offset += newUrl.length() - oldUrl.length();
            }
        }
        return this;
    }

    public String resolveOneUrl(String url, int timeout) {
        return Helper.getResolvedUrl(url, timeout);
    }

    public String[] getInfo(String url, int timeout) {
        return Helper.getUrlInfos(url, timeout);
    }
}
