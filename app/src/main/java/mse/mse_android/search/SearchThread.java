package mse.mse_android.search;

import android.app.Activity;
import android.util.Log;
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
import mse.mse_android.data.IResult;
import mse.mse_android.data.Search;
import mse.mse_android.helpers.HtmlHelper;
import mse.mse_android.helpers.ReaderCreator;
import mse.mse_android.search.IndexStore;

/**
 * Created by Michael Purdy on 17/11/2015.
 *
 * This is the thread that performs the search
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

        // for each author to be searched
        for (Author nextAuthor : authorsToSearch) {

            if (!nextAuthor.isSearchable()) continue;

            AuthorIndex nextAuthorIndex = indexStore.getIndex(logger, nextAuthor);

            AuthorSearchCache nextAsc = new AuthorSearchCache(cfg, nextAuthorIndex, search);

            AuthorSearchThread nextAuthorSearchThread = new AuthorSearchThread(cfg, nextAsc, progress);
            singleSearchThreads.add(nextAuthorSearchThread);

            nextAuthorSearchThread.start();

        } // end searching each author


        // write the results

        // try to open and write to the results file
        File resultsFile = new File(ReaderCreator.getResultsFileLocation());
        if (!resultsFile.exists()) {
            resultsFile.getParentFile().mkdirs();
            try {
                resultsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        PrintWriter pwResults = null;
        try {

            logger.log(LogLevel.DEBUG, "Writing Results: " + resultsFile.getAbsolutePath());
            pwResults = new PrintWriter(resultsFile);
            pwResults.println(HtmlHelper.getResultsHeader("../../mseStyle.css"));

            // join all the threads
            for (SingleSearchThread nextThread : singleSearchThreads) {
                try {
                    nextThread.join();

                    AuthorSearchCache asc = ((AuthorSearchThread) nextThread).getAsc();

                    // write the author header
                    pwResults.println(HtmlHelper.getAuthorResultsHeader(asc.author, asc.printableSearchWords()));

                    // write all the results / errors
                    for (IResult result : nextThread.getResults()) {
                        pwResults.println(result.getBlock());
                    }

                    HtmlHelper.closeAuthorContainer(pwResults);

                    // write the number of results for the author
                    pwResults.println(HtmlHelper.getSingleAuthorResults(asc.getAuthorName(), asc.numAuthorResults));

                    // write the log
                    for (LogRow logRow : nextThread.getLog()) {
                        logger.log(logRow);
                    }

                    // add the number of search results to the total
                    search.addAuthorSearchResults(nextThread.getNumberOfResults());

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            pwResults.println("\n\t\t<div class=\"spaced\">Number of total results: " + search.getNumTotalResults() + "</div>");
            pwResults.println(HtmlHelper.getHtmlFooter("\t</div>"));

        } catch (FileNotFoundException fnfe) {

            logger.log(LogLevel.HIGH, "Could not find results file.");

        } finally {
            if (pwResults != null) pwResults.close();
        }

        progress.set(1000 * authorsToSearch.size() + 1);

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String url = "file:///" + ReaderCreator.getResultsFileLocation();
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
