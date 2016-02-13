package com.uservoice.uservoicesdk.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewCompat;
import android.util.TypedValue;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import com.uservoice.uservoicesdk.C0621R;
import com.uservoice.uservoicesdk.activity.TopicActivity;
import com.uservoice.uservoicesdk.dialog.ArticleDialogFragment;
import com.uservoice.uservoicesdk.dialog.SuggestionDialogFragment;
import com.uservoice.uservoicesdk.model.Article;
import com.uservoice.uservoicesdk.model.BaseModel;
import com.uservoice.uservoicesdk.model.Suggestion;
import com.uservoice.uservoicesdk.model.Topic;
import java.util.Locale;

public class Utils {
    @SuppressLint({"SetJavaScriptEnabled"})
    public static void displayArticle(WebView webView, Article article, Context context) {
        String styles = "iframe, img { width: 100%; }";
        if (isDarkTheme(context)) {
            webView.setBackgroundColor(ViewCompat.MEASURED_STATE_MASK);
            styles = styles + "body { background-color: #000000; color: #F6F6F6; } a { color: #0099FF; }";
        }
        String html = String.format("<html><head><meta charset=\"utf-8\"><link rel=\"stylesheet\" type=\"text/css\" href=\"http://cdn.uservoice.com/stylesheets/vendor/typeset.css\"/><style>%s</style></head><body class=\"typeset\" style=\"font-family: sans-serif; margin: 1em\"><h3>%s</h3>%s</body></html>", new Object[]{styles, article.getTitle(), article.getHtml()});
        webView.setWebChromeClient(new WebChromeClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setPluginState(PluginState.ON);
        webView.loadUrl(String.format("data:text/html;charset=utf-8,%s", new Object[]{Uri.encode(html)}));
    }

    public static boolean isDarkTheme(Context context) {
        TypedValue tv = new TypedValue();
        float[] hsv = new float[3];
        context.getTheme().resolveAttribute(16842806, tv, true);
        Color.colorToHSV(context.getResources().getColor(tv.resourceId), hsv);
        if (hsv[2] > 0.5f) {
            return true;
        }
        return false;
    }

    @SuppressLint({"DefaultLocale"})
    public static String getQuantityString(View view, int id, int count) {
        return String.format("%,d %s", new Object[]{Integer.valueOf(count), view.getContext().getResources().getQuantityString(id, count)});
    }

    public static void displayInstantAnswer(View view, BaseModel model) {
        TextView title = (TextView) view.findViewById(C0621R.id.uv_title);
        TextView detail = (TextView) view.findViewById(C0621R.id.uv_detail);
        View suggestionDetails = view.findViewById(C0621R.id.uv_suggestion_details);
        ImageView image = (ImageView) view.findViewById(C0621R.id.uv_icon);
        if (model instanceof Article) {
            Article article = (Article) model;
            image.setImageResource(C0621R.drawable.uv_article);
            title.setText(article.getTitle());
            if (article.getTopicName() != null) {
                detail.setVisibility(0);
                detail.setText(article.getTopicName());
            } else {
                detail.setVisibility(8);
            }
            suggestionDetails.setVisibility(8);
        } else if (model instanceof Suggestion) {
            Suggestion suggestion = (Suggestion) model;
            image.setImageResource(C0621R.drawable.uv_idea);
            title.setText(suggestion.getTitle());
            detail.setVisibility(0);
            detail.setText(suggestion.getForumName());
            if (suggestion.getStatus() != null) {
                View statusColor = suggestionDetails.findViewById(C0621R.id.uv_suggestion_status_color);
                TextView status = (TextView) suggestionDetails.findViewById(C0621R.id.uv_suggestion_status);
                int color = Color.parseColor(suggestion.getStatusColor());
                suggestionDetails.setVisibility(0);
                status.setText(suggestion.getStatus().toUpperCase(Locale.getDefault()));
                status.setTextColor(color);
                statusColor.setBackgroundColor(color);
                return;
            }
            suggestionDetails.setVisibility(8);
        }
    }

    public static void showModel(FragmentActivity context, BaseModel model) {
        showModel(context, model, null);
    }

    public static void showModel(FragmentActivity context, BaseModel model, String deflectingType) {
        if (model instanceof Article) {
            new ArticleDialogFragment((Article) model, deflectingType).show(context.getSupportFragmentManager(), "ArticleDialogFragment");
        } else if (model instanceof Suggestion) {
            new SuggestionDialogFragment((Suggestion) model, deflectingType).show(context.getSupportFragmentManager(), "SuggestionDialogFragment");
        } else if (model instanceof Topic) {
            Intent intent = new Intent(context, TopicActivity.class);
            intent.putExtra("topic", (Topic) model);
            context.startActivity(intent);
        }
    }
}
