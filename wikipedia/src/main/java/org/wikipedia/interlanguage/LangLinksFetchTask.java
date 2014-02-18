package org.wikipedia.interlanguage;

import android.content.*;
import org.json.*;
import org.mediawiki.api.json.*;
import org.wikipedia.*;
import org.wikipedia.concurrency.*;

import java.util.*;

public class LangLinksFetchTask extends ApiTask<ArrayList<PageTitle>> {
    private final PageTitle title;
    public LangLinksFetchTask(Context context, PageTitle title) {
        super(
                ExecutorService.getSingleton().getExecutor(LangLinksFetchTask.class, 1),
                ((WikipediaApp)context.getApplicationContext()).getAPIForSite(title.getSite())
        );
        this.title = title;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("query")
                .param("prop", "langlinks")
                .param("titles", title.getPrefixedText())
                .param("lllimit", "500");
    }

    @Override
    public ArrayList<PageTitle> processResult(ApiResult result) throws Throwable {
        ArrayList<PageTitle> linkTitles = new ArrayList<PageTitle>();
        JSONObject pagesJSON = result.asObject()
                .optJSONObject("query")
                .optJSONObject("pages");
        String pageId = (String) pagesJSON.keys().next();
        String fullJSON = result.asObject().toString(4);
        if (!pagesJSON.optJSONObject(pageId).has("langlinks")) {
            // No links found
            return linkTitles;
        }

        JSONArray langlinksJSON = pagesJSON.optJSONObject(pageId).optJSONArray("langlinks");

        for (int i = 0; i < langlinksJSON.length(); i++) {
            JSONObject langlinkJSON = langlinksJSON.optJSONObject(i);
            PageTitle linkTitle = new PageTitle(null,
                    langlinkJSON.optString("*"),
                    Site.forLang(langlinkJSON.optString("lang")));
            linkTitles.add(linkTitle);
        }

        return linkTitles;
    }
}
