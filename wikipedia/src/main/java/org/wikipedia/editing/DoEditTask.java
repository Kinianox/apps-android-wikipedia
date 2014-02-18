package org.wikipedia.editing;

import android.content.*;
import android.util.*;
import org.json.*;
import org.mediawiki.api.json.*;
import org.wikipedia.*;
import org.wikipedia.concurrency.*;

public class DoEditTask extends ApiTask<EditingResult> {
    private final PageTitle title;
    private final String sectionWikitext;
    private final int sectionID;
    private final String editToken;

    public DoEditTask(Context context, PageTitle title, String sectionWikitext, int sectionID, String editToken) {
        super(
                ExecutorService.getSingleton().getExecutor(DoEditTask.class, 1),
                ((WikipediaApp)context.getApplicationContext()).getAPIForSite(title.getSite())
        );
        this.title = title;
        this.sectionWikitext = sectionWikitext;
        this.sectionID = sectionID;
        this.editToken = editToken;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("edit")
                .param("title", title.getPrefixedText())
                .param("section", String.valueOf(sectionID))
                .param("text", sectionWikitext)
                .param("token", editToken);
    }

    @Override
    protected ApiResult makeRequest(RequestBuilder builder) {
        return builder.post(); // Editing requires POST requests
    }

    @Override
    public EditingResult processResult(ApiResult result) throws Throwable {
        JSONObject resultJSON = result.asObject();
        Log.d("Wikipedia", resultJSON.toString(4));
        if (resultJSON.has("error")) {
            JSONObject errorJSON = resultJSON.optJSONObject("error");
            throw new EditingException(errorJSON.optString("code"), errorJSON.optString("info"));
        }
        JSONObject edit = resultJSON.optJSONObject("edit");
        String status = edit.optString("result");
        if (status.equals("Success")) {
            return new SuccessEditResult();
        } else if (status.equals("Failure")) {
            if (edit.has("captcha")) {
                return new CaptchaEditResult(
                        edit.optJSONObject("captcha").optString("id")
                );
            }
            if (edit.has("code") && edit.optString("code").startsWith("abusefilter-")) {
                return new AbuseFilterEditResult(edit);
            }
        }
        // Handle other type of return codes here
        throw new RuntimeException("Failure happens");
    }
}
