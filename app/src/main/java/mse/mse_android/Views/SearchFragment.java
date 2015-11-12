package mse.mse_android.Views;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

import mse.mse_android.R;
import mse.mse_android.common.Config;
import mse.mse_android.common.ILogger;
import mse.mse_android.common.LogLevel;
import mse.mse_android.common.Logger;
import mse.mse_android.data.Author;
import mse.mse_android.data.Search;
import mse.mse_android.search.AuthorSearch;
import mse.mse_android.search.IndexStore;

/**
 * A placeholder fragment containing a simple view.
 */
public class SearchFragment extends Fragment {

    Config cfg;
    Logger logger;
    Activity mActivity;

    boolean firstSearch = true;

    EditText searchTextBox;
    Button btnSearch;
    ProgressBar progressBar;
    TextView tvSearchProgress;
    WebView wvSearchResults;

    public SearchFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_search, container, false);

        this.searchTextBox = (EditText) v.findViewById(R.id.edtxtSearchText);

        this.btnSearch = (Button) v.findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (firstSearch) {
                    logger = new Logger(LogLevel.DEBUG, mActivity);
                    logger.openLog();
                    cfg = new Config(logger, mActivity);
                    cfg.refresh();
                } else {
                    logger.openLog();
                }

                String searchString = searchTextBox.getText().toString();
                Search search = new Search(mActivity, cfg, logger, searchString, progressBar, tvSearchProgress);

                Author authorToSearch = Author.AJG;
                ArrayList<Author> authorsToSearch = new ArrayList<>();
                authorsToSearch.add(authorToSearch);

                IndexStore indexStore = new IndexStore(cfg);

                AuthorSearch authorSearch = new AuthorSearch(mActivity, wvSearchResults, cfg, logger, authorsToSearch, indexStore, search, getActivity().getAssets());
                authorSearch.start();
            }
        });

        this.progressBar = (ProgressBar) v.findViewById(R.id.pbSearch);
        this.tvSearchProgress = (TextView) v.findViewById(R.id.tvSearchProgress);
        this.wvSearchResults = (WebView) v.findViewById(R.id.wvSearchResults);

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }
}
