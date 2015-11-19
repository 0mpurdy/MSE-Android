package mse.mse_android.Views;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
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
import java.util.concurrent.atomic.AtomicInteger;

import mse.mse_android.R;
import mse.mse_android.common.Config;
import mse.mse_android.common.LogLevel;
import mse.mse_android.common.Logger;
import mse.mse_android.data.Author;
import mse.mse_android.data.Search;
import mse.mse_android.search.AuthorSearchThread;
import mse.mse_android.search.IndexStore;
import mse.mse_android.search.SearchProgressThread;
import mse.mse_android.search.SearchScope;
import mse.mse_android.search.SearchThread;

/**
 * A placeholder fragment containing a simple view.
 */
public class SearchFragment extends Fragment {

    private Config mCfg;
    private Logger mLogger;
    private Activity mActivity;

    private IndexStore indexStore;

    ArrayList<Author> mSelectedAuthors;

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
        searchTextBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                search();
                return false;
            }
        });
        this.progressBar = (ProgressBar) v.findViewById(R.id.pbSearch);
        this.tvSearchProgress = (TextView) v.findViewById(R.id.tvSearchProgress);
        this.wvSearchResults = (WebView) v.findViewById(R.id.wvSearchResults);

        mSelectedAuthors = new ArrayList<>();
        mSelectedAuthors.add(Author.JND);

        this.btnSearch = (Button) v.findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search();
            }
        });

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }

    private void search() {
        mLogger.log(LogLevel.INFO, "Started Search ... ");

        InputMethodManager inputManager = (InputMethodManager)
                mActivity.getSystemService(mActivity.INPUT_METHOD_SERVICE);

        inputManager.hideSoftInputFromWindow((null == mActivity.getCurrentFocus()) ? null : mActivity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

//                File expansionFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Android" + File.separator + "obb" + File.separator + mActivity.getPackageName());
//                mLogger.log(LogLevel.DEBUG, expansionFile.getAbsolutePath());
//                if (!expansionFile.exists()) {
//                    expansionFile.mkdirs();
//                    mLogger.log(LogLevel.LOW, "Created expansion file");
//                } else {
//                    mLogger.log(LogLevel.DEBUG, "Expansion file already exists");
//                }
//
//                try {
//                    mLogger.log(LogLevel.HIGH, expansionFile.getCanonicalPath());
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//                File deleteFile = new File(mActivity.getFilesDir() + "res");
//                if (deleteFile.exists()) {
//                    DeleteRecursive(deleteFile);
//                    mLogger.log(LogLevel.HIGH, "Deleted file");
//                }

        if (firstSearch) {
            indexStore = new IndexStore(mCfg, mActivity.getAssets());
            firstSearch = false;
        }

        String searchString = searchTextBox.getText().toString();
        Search search = new Search(mActivity, mCfg, mLogger, searchString);

        mLogger.log(LogLevel.INFO, "Searched for: " + searchString);

        AtomicInteger progress = new AtomicInteger();

        SearchProgressThread searchProgressThread = new SearchProgressThread(mActivity, progressBar, tvSearchProgress, progress, mSelectedAuthors.size());
        searchProgressThread.start();

        // start the thread to search
        SearchThread searchThread = new SearchThread(mCfg, mLogger, mActivity, wvSearchResults, mSelectedAuthors, indexStore, search, progress);
        searchThread.start();
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

    public void goToLocation(String location) {
        wvSearchResults.loadUrl(location);
    }

    void DeleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                DeleteRecursive(child);

        fileOrDirectory.delete();
    }

    public void clickAuthor(int groupPosition, int childPosition, long id) {
        mLogger.openLog();
        int i = -1;
        int j = -1;
        while (j < childPosition && i < Author.values().length) {
            i++;
            if (Author.values()[i].isSearchable()) j++;
        }
        if (j<Author.values().length) toggleAuthor(Author.values()[i]);
        mLogger.log(LogLevel.DEBUG, "Clicked " + groupPosition + " " + childPosition + " " + id);
        mLogger.log(LogLevel.DEBUG, Author.values()[i].getName());
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
