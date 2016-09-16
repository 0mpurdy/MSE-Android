package mse.mse_android.Views;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.vending.expansion.zipfile.APKExpansionSupport;
import com.android.vending.expansion.zipfile.ZipResourceFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import mse.mse_android.R;
import mse.mse_android.common.config.Config;
import mse.mse_android.common.log.LogLevel;
import mse.mse_android.common.log.Logger;
import mse.mse_android.data.author.Author;
import mse.mse_android.data.search.Search;
import mse.mse_android.helpers.ReaderCreator;
import mse.mse_android.search.IndexStore;
import mse.mse_android.search.SearchProgressThread;
import mse.mse_android.search.SearchThread;

/**
 * A placeholder fragment containing a simple view.
 */
public class SearchFragment extends Fragment {

    private Config mCfg;
    private Logger mLogger;
    private MainActivity mActivity;

    private IndexStore indexStore;

    ArrayList<Author> mSelectedAuthors;

    boolean firstSearch = true;

    EditText searchTextBox;
    Button btnSearch;
    ImageButton btnMenu;
    ProgressBar progressBar;
    TextView tvSearchProgress;
    WebView wvSearchResults;

    ArrayList<String> previousUrl;

    ZipResourceFile mExpansionFile;

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

        // Get a ZipResourceFile representing a merger of both the main and patch files
        try {
            mExpansionFile = APKExpansionSupport.getAPKExpansionZipFile(mActivity, 10, 0);
        } catch (IOException e) {
            Log.w("[DEBUG  ]", "Failed to find expansion file", e);
        }

        checkExternalMedia();
        copyAssetToInternalStorage("files/mseStyle.css", "files/", "mseStyle.css");
        copyAssetToInternalStorage("files/bootstrap/css/bootstrap.css", "files/bootstrap/css/", "bootstrap.css");
        copyAssetToInternalStorage("files/bootstrap/js/bootstrap.js", "files/bootstrap/js/", "bootstrap.js");
        copyAssetToInternalStorage("files/jquery/jquery-1.11.3.min.js", "files/jquery/", "jquery-1.11.3.min.js");

        this.wvSearchResults = (WebView) v.findViewById(R.id.wvSearchResults);
        this.wvSearchResults.getSettings().setJavaScriptEnabled(true);
        this.wvSearchResults.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d("[URL     ]", url);

                if (url.contains("\\")) url = url.replace("\\", "/");
                if (!url.contains("android_asset")) {
                    // if trying to load a file that is an asset load the asset
                    if (isAssetFolder(url)) {
                        String ident = "files/";
                        if (url.contains(ident)) {
                            url = url.substring(url.indexOf(ident));
                        } else {
                            url = url.substring(url.indexOf(':') + 1);
                        }

                        //todo fix horrible hack
                        if (url.contains("files//")) {
                            url = url.replace("files//", "files/");
                        }

                        url = "file:///android_asset/" + url;
                        Log.d("[A_URL ]", url);
                        wvSearchResults.loadUrl(url);
                    } else if (!url.startsWith("data:") && url.startsWith("mse:")) {
                        url = url.substring(4);
                        Log.d("[E_URL  ]", url);
                        try {
                            InputStream in = mExpansionFile.getInputStream(url);

                            byte[] buffer = new byte[in.available()];
                            in.read(buffer);
                            in.close();
                            String data = new String(buffer);
                            if (data.contains("../../bootstrap"))
                                data = data.replace("../../bootstrap", "/bootstrap");
                            if (data.contains("../mseStyle.css"))
                                data = data.replace("../mseStyle.css", Environment.getExternalStorageDirectory() + "/mseStyle.css");
                            wvSearchResults.loadData(data, "text/html", "UTF-8");
                        } catch (IOException ioe) {
                            String data = "<p>" + ioe.getMessage() + "</p>";
                            wvSearchResults.loadData(data, "text/html", "UTF-8");
                        }
                    }
                }
                return false;
            }
        });

        WebSettings mWebSettings = wvSearchResults.getSettings();
        mWebSettings.setBuiltInZoomControls(true);
        wvSearchResults.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        wvSearchResults.setScrollbarFadingEnabled(false);

        if (mExpansionFile != null) {
            try {
                InputStream in = mExpansionFile.getInputStream("target_a/index.html");
                byte[] buffer = new byte[in.available()];
                in.read(buffer);
                in.close();
                String data = new String(buffer);
                data = data.replace("file:///android_asset/", "mse:");
                wvSearchResults.loadData(data, "text/html", "UTF-8");
            } catch (IllegalArgumentException | IllegalStateException | IOException e) {
                Log.w("[DEBUG  ]", "Failed to update data source for media player", e);
            }
        }

        mSelectedAuthors = new ArrayList<>();
        mSelectedAuthors.add(Author.BIBLE);


        this.btnMenu = (ImageButton) v.findViewById(R.id.menuButton);
        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuClick();
            }
        });

        this.btnSearch = (Button) v.findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search();
            }
        });

        return v;
    }

    private boolean isAssetFolder(String url) {
        return url.contains("bible/") || url.contains("hymns/") || url.contains("jnd/");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (MainActivity) activity;
    }

    private void menuClick() {
        mActivity.openDrawer();
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
        Search search = new Search(mCfg.getSearchType(), searchString);

        mLogger.log(LogLevel.INFO, "Searched for: " + searchString);

        AtomicInteger progress = new AtomicInteger();

        SearchProgressThread searchProgressThread = new SearchProgressThread(mActivity, progressBar, tvSearchProgress, progress, mSelectedAuthors.size());
        searchProgressThread.start();

        // start the thread to search
        SearchThread searchThread = new SearchThread(mCfg, mLogger, mActivity, wvSearchResults, mSelectedAuthors, indexStore, search, progress);
        searchThread.start();
    }

    private void copyAssetToExternalStorage(String assetLocation, String externalStorageFolder, String externalStorageName) {
        try {
            BufferedInputStream in = new BufferedInputStream(mActivity.getAssets().open(assetLocation));
            File folder = getExternalStorageDir(externalStorageFolder);
            File outFile = new File(folder, externalStorageName);
            if (!outFile.exists()) {
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

    private void copyAssetToInternalStorage(String assetLocation, String internalStorageFolder, String internalStorageName) {
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try {
            in = new BufferedInputStream(mActivity.getAssets().open(assetLocation));
            File folder = getInternalStorageDir(internalStorageFolder);
            if (folder.exists() || folder.mkdirs()) {
                File outFile = new File(folder, internalStorageName);
                mLogger.log(LogLevel.DEBUG, "Copying asset: " + assetLocation + " to " + outFile.getAbsolutePath());
                if (!outFile.exists()) {
                    outFile.createNewFile();
                }
                out = new BufferedOutputStream(new FileOutputStream(outFile));
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
            } else {
                Log.e("[ERROR  ]", "Could not create folders in internal storage: " + folder.getAbsolutePath());
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public File getExternalStorageDir(String fileName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStorageDirectory(), fileName);
        if (!file.mkdirs()) {
            Log.e("[ERROR  ]", "Directory not created: " + file.getAbsolutePath());
        }
        return file;
    }

    public File getInternalStorageDir(String fileName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(mActivity.getFilesDir(), fileName);
        return file;
    }

    private void checkExternalMedia() {
        boolean eSAvail = false;
        boolean esWrite = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // Can read and write the media
            eSAvail = esWrite = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // Can only read the media
            eSAvail = true;
            esWrite = false;
        } else {
            // Can't read or write
            eSAvail = esWrite = false;
        }
        Log.d("[DEBUG   ]", "External media available: " + eSAvail + " : " + "External media writeable: " + esWrite);
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
        if (j < Author.values().length) toggleAuthor(Author.values()[i]);
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
        mLogger = new Logger(LogLevel.DEBUG, mActivity.getFilesDir() + File.separator + Logger.DEFAULT_LOG);
    }

    private void createConfig() {
        mLogger.openLog();
        mCfg = new Config(mLogger, mActivity.getFilesDir().toString(), mActivity.getFilesDir() + File.separator + "config.txt", true);
//        mCfg = new Config(mLogger, mActivity.getFilesDir() + File.separator + "config.txt");
        mCfg.refresh();
        mLogger.closeLog();
        ReaderCreator.mCfg = mCfg;
    }

}
