package mse.mse_android.Views;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import mse.mse_android.R;
import mse.mse_android.common.Config;
import mse.mse_android.common.LogLevel;
import mse.mse_android.common.Logger;
import mse.mse_android.data.Author;
import mse.mse_android.data.Search;
import mse.mse_android.search.AuthorSearch;
import mse.mse_android.search.SearchScope;

/**
 * A placeholder fragment containing a simple view.
 */
public class SearchFragment extends Fragment {

    private Config mCfg;
    private Logger mLogger;
    private Activity mActivity;

    ArrayList<Author> mSelectedAuthors = new ArrayList<>();

    boolean firstSearch = true;

    EditText searchTextBox;
    Button btnSearch;
    ProgressBar progressBar;
    TextView tvSearchProgress;
    WebView wvSearchResults;

    ArrayList<String> previousUrl;

    public SearchFragment() {
        this.previousUrl = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        createLog();
        createConfig();
        copyResultsGif();

        View v = inflater.inflate(R.layout.fragment_search, container, false);

        this.searchTextBox = (EditText) v.findViewById(R.id.edtxtSearchText);
        this.progressBar = (ProgressBar) v.findViewById(R.id.pbSearch);
        this.tvSearchProgress = (TextView) v.findViewById(R.id.tvSearchProgress);
        this.wvSearchResults = (WebView) v.findViewById(R.id.wvSearchResults);

        this.btnSearch = (Button) v.findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                InputMethodManager inputManager = (InputMethodManager)
                        mActivity.getSystemService(mActivity.INPUT_METHOD_SERVICE);

                inputManager.hideSoftInputFromWindow((null == mActivity.getCurrentFocus()) ? null : mActivity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                File expansionFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Android" + File.separator + "obb" + File.separator + mActivity.getPackageName());
                mLogger.log(LogLevel.DEBUG, expansionFile.getAbsolutePath());
                if (!expansionFile.exists()) {
                    expansionFile.mkdirs();
                    mLogger.log(LogLevel.LOW, "Created expansion file");
                } else {
                    mLogger.log(LogLevel.DEBUG, "Expansion file already exists");
                }

                try {
                    mLogger.log(LogLevel.HIGH, expansionFile.getCanonicalPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                File deleteFile = new File(mActivity.getFilesDir() + "res");
                if (deleteFile.exists()) {
                    DeleteRecursive(deleteFile);
                    mLogger.log(LogLevel.HIGH, "Deleted file");
                }

                String searchString = searchTextBox.getText().toString();
                Search search = new Search(mActivity, mCfg, mLogger, searchString, progressBar, tvSearchProgress);
                search.setSearchScope(SearchScope.SENTENCE);

                AuthorSearch authorSearch = new AuthorSearch(mActivity, wvSearchResults, previousUrl, mCfg, mLogger, mSelectedAuthors, search, getActivity().getAssets());
                authorSearch.start();
            }
        });

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }

    private void copyResultsGif() {
        // copy the results gif across
        try {
            BufferedInputStream in = new BufferedInputStream(mActivity.getAssets().open("img/results.gif"));
            File outFile = new File(mActivity.getFilesDir() + File.separator + "img/results.gif");
            if (!outFile.exists()) {
                outFile.getParentFile().mkdirs();
                outFile.createNewFile();
            }
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public boolean goBack() {
        if (wvSearchResults.canGoBack()) {
            wvSearchResults.goBack();
            return true;
        } else {
            return false;
        }
    }

    void DeleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                DeleteRecursive(child);

        fileOrDirectory.delete();
    }

    public void clickAuthor(int groupPosition, int childPosition, long id) {
        mLogger.openLog();
        mLogger.log(LogLevel.DEBUG, "Clicked " + groupPosition + " " + childPosition + " " + id);
        mLogger.log(LogLevel.DEBUG, Author.values()[childPosition].getName());
        toggleAuthor(Author.values()[childPosition]);
        mLogger.closeLog();
    }

    private void toggleAuthor(Author author) {
        if (mSelectedAuthors.contains(author)) {
            mSelectedAuthors.remove(author);
        } else {
            mSelectedAuthors.add(author);
        }
    }

    private void createLog() {
        mLogger = new Logger(LogLevel.DEBUG, mActivity);
    }

    private void createConfig() {
        mLogger.openLog();
        mCfg = new Config(mLogger, mActivity);
        mCfg.refresh();
        mLogger.closeLog();
    }

}
