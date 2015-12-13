package mse.mse_android.search;

import android.app.Activity;
import android.webkit.WebView;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import mse.mse_android.common.Config;
import mse.mse_android.common.ILogger;
import mse.mse_android.common.LogLevel;
import mse.mse_android.common.LogRow;
import mse.mse_android.data.Author;
import mse.mse_android.data.AuthorIndex;
import mse.mse_android.data.Search;
import mse.mse_android.search.IndexStore;

/**
 * Created by Michael on 17/11/2015.
 */
public class SearchThread extends Thread {

    private Activity mActivity;
    private WebView mWebView;

    private Config cfg;
    private ILogger logger;
    private IndexStore indexStore;
    private Search search;
    ArrayList<Author> authorsToSearch;

    ArrayList<SingleSearchThread> singleSearchThreads;
    ArrayList<ArrayList<LogRow>> searchLogs;

    // the progress of the current search (0 - 1000)
    private AtomicInteger progress;

    public SearchThread(Config cfg, ILogger logger, Activity activity, WebView webView, ArrayList<Author> authorsToSearch, IndexStore indexStore, Search search, AtomicInteger progress) {
        this.cfg = cfg;
        this.logger = logger;
        this.mActivity = activity;
        this.mWebView = webView;
        this.authorsToSearch = authorsToSearch;
        this.indexStore = indexStore;
        this.search = search;
        this.progress = progress;

        singleSearchThreads = new ArrayList<>();

        searchLogs = new ArrayList<>();
//        this.progress.set(0);
    }

    @Override
    public void run() {

        if (authorsToSearch.contains(Author.BIBLE)) {

            // search the bible

        }

        // for each author to be searched
        for (Author nextAuthor : authorsToSearch) {

            if (!nextAuthor.isSearchable()) continue;

//            ArrayList<LogRow> searchLog = new ArrayList<>();
//            searchLogs.add(searchLog);

            AuthorIndex nextAuthorIndex = indexStore.getIndex(logger, nextAuthor);

            AuthorSearchCache nextAsc = new AuthorSearchCache(cfg, nextAuthorIndex, search);

            AuthorSearchThread nextAuthorSearchThread = new AuthorSearchThread(mActivity, cfg, nextAsc, progress);
            singleSearchThreads.add(nextAuthorSearchThread);

            nextAuthorSearchThread.start();

//            searchAuthor(resultText, nextAuthor, search, indexStore);
//            resultText.add("Number of results for " + nextAuthor.getName() + ": " + search.getNumAuthorResults());
//            search.clearAuthorValues();

        } // end searching each author


        // write the results

        // try to open and write to the results file
        File resultsFile = new File(mActivity.getFilesDir() + File.separator + cfg.getResultsFileName());
        if (!resultsFile.exists()) {
            resultsFile.getParentFile().mkdirs();
            try {
                resultsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        PrintWriter pwResults = null;
        try  {

            pwResults = new PrintWriter(resultsFile);

            pwResults.println(getHtmlHeader());

            // join all the threads
            for (SingleSearchThread nextThread : singleSearchThreads) {
                try {
                    nextThread.join();

                    for (String resultLine : nextThread.getResults()) {
                        pwResults.println(resultLine);
                    }

                    for (LogRow logRow : nextThread.getLog()) {
                        logger.log(logRow);
                    }

                    search.addAuthorSearchResults(nextThread.getNumberOfResults());

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            pwResults.println("\n\t<p>\n\t\tNumber of total results: " + search.getNumTotalResults() + "\n\t</p>");
            pwResults.println(getHtmlFooter());

        } catch (FileNotFoundException fnfe) {
            logger.log(LogLevel.HIGH, "Could not find results file.");
        } finally {
            if (pwResults != null) pwResults.close();
        }

//        search.setProgress("Done", 1.0);

        progress.set(1000 * authorsToSearch.size() + 1);

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String url = "file:///" + mActivity.getFilesDir() + File.separator + cfg.getResultsFileName();
                mWebView.loadUrl(url);
            }
        });

        logger.closeLog();

    }

    private String getHtmlHeader() {
        return "<!DOCTYPE html>" +
                "\n\n<html>" +
                "\n\n<head>" +
                "\n\t<link rel=\"stylesheet\" type=\"text/css\" href=\"../../mseStyle.css\" />" +
                "\n\t<title>Search Results</title>" +
                "\n</head>" +
                "\n<body>" +
                "\t<p><img src=\"../../img/results.gif\"></p>";
    }

    private String getHtmlFooter() {
        return "\n</body>\n\n</html>";
    }

}
