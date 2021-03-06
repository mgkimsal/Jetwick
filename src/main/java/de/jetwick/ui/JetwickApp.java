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

import org.apache.wicket.Page;
import org.apache.wicket.Request;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.guice.GuiceComponentInjector;
import org.apache.wicket.protocol.http.WebApplication;

import com.google.inject.Guice;
import com.google.inject.Injector;
import de.jetwick.config.Configuration;
import de.jetwick.config.DefaultModule;
import org.apache.wicket.Application;

/**
 * Application object for your web application. If you want to run this application without deploying, run the Start class.
 *
 * @author Peter Karich
 */
public class JetwickApp extends WebApplication {

    private Configuration cfg;
    private Injector injector;

    public JetwickApp() {
        injector = Guice.createInjector(new DefaultModule());
        cfg = injector.getInstance(Configuration.class);
    }

    protected GuiceComponentInjector getGuiceInjector() {
        return new GuiceComponentInjector(this, injector);
    }

    @Override
    protected void init() {
        super.init();

        getApplicationSettings().setPageExpiredErrorPage(HomePage.class);
        getApplicationSettings().setInternalErrorPage(ErrorPage.class);

        // default is <em> </em> for disabled links
        getMarkupSettings().setDefaultBeforeDisabledLink(null);
        getMarkupSettings().setDefaultAfterDisabledLink(null);

        if (isDev()) {
            getDebugSettings().setDevelopmentUtilitiesEnabled(true);
//            getRequestCycleSettings().addResponseFilter(new ServerAndClientTimeFilter());
        }

//    For HybridUrlCodingStrategy we get some problems:
//    jetty bug for characters like '#' in query it won't work
//    link in tweet is: http://localhost/twittersearch|||0.1?u=%40TelegraphNews
//        and not       http://localhost/twittersearch?u=%40TelegraphNews|||0.1
//    error whens searching for \ => tomcat has problem (empty page)
//    not produceable: wget http://localhost/jetwick-dev/twittersearch/q/java
//         and you will get an endless loop! (for users without cookies!)
//    not reproducable problem if we click on 'retweets' of the following tweet nothing happens:
//         http://localhost/twittersearch/id/25372450085
//         but if we filter first then it works!!?? (e.g. search #wicket + filter original)

//        HybridUrlCodingStrategy strategy = new HybridUrlCodingStrategy("twittersearch", HomePage.class, true) {
//
//            @Override
//            protected String getBeginSeparator() {
//                return "|||";
//            }
//        };
//        mount(strategy);

//        mount(new QueryStringUrlCodingStrategy("twittersearch", HomePage.class));
//        mount(new MixedParamUrlCodingStrategy("twittersearch", HomePage.class, new String[]{"q"}));

        // 1.5-M2.1
//        getRootRequestMapperAsCompound().add(new MountedMapper("twittersearch", HomePage.class));

        mountBookmarkablePage("about", About.class);
        mountBookmarkablePage("imprint", Imprint.class);
        addComponentInstantiationListener(getGuiceInjector());
    }

    @Override
    public Class<? extends Page> getHomePage() {
        return HomePage.class;
    }

    // enable production mode
    @Override
    public String getConfigurationType() {
        if (isDev())
            return Application.DEVELOPMENT;
        else
            return Application.DEPLOYMENT;
    }

    public boolean isDev() {
        return "development".equals(cfg.getStage());
    }

    @Override
    public Session newSession(Request request, Response response) {        
        Session session = new MySession(request, isDev());
        getGuiceInjector().inject(session);
        return session;

    }
    // remove the jsessionid! but if user agent disabled cookies (like googlebot) it cannot search!?
//    @Override
//    protected WebResponse newWebResponse(HttpServletResponse servletResponse) {
//        return (getRequestCycleSettings().getBufferResponse()
//                ? new BufferedWebResponse(servletResponse) {
//
//            @Override
//            public CharSequence encodeURL(CharSequence url) {
//                return url;
//            }
//        }
//                : new WebResponse(servletResponse));
//    }
    // encrypt url works but I want that the explicit parameters are visible!!
    // altough the request via parameters still works!
//    @Override
//    protected IRequestCycleProcessor newRequestCycleProcessor() {
//        return new WebRequestCycleProcessor() {
//
//            @Override
//            protected IRequestCodingStrategy newRequestCodingStrategy() {
//                return new CryptedUrlWebRequestCodingStrategy(new WebRequestCodingStrategy());
//            }
//        };
//    }
}
