package org.wikipedia.search;

import org.json.*;
import org.mediawiki.api.json.*;
import org.wikipedia.*;
import org.wikipedia.concurrency.*;

import java.util.*;

public class SearchArticlesTask extends ApiTask<List<PageTitle>> {
    private final String prefix;
    private final Site site;

    public SearchArticlesTask(Api api, Site site, String prefix) {
        super(ExecutorService.getSingleton().getExecutor(SearchArticlesTask.class, 4), api);
        this.prefix = prefix;
        this.site = site;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("opensearch").param("search", prefix).param("limit", "12");
    }

    @Override
    public List<PageTitle> processResult(ApiResult result) throws Throwable {
        JSONArray searchResults = result.asArray().optJSONArray(1);

        ArrayList<PageTitle> pageTitles = new ArrayList<PageTitle>();
        for (int i = 0; i < searchResults.length(); i++) {
            pageTitles.add(new PageTitle(null, searchResults.optString(i), site));
        }

        return pageTitles;
    }
}
