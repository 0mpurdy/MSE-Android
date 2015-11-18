package mse.mse_android.search;

import android.app.Activity;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Michael on 18/11/2015.
 */
public class SearchProgressThread extends Thread {

    Activity mActivity;

    AtomicInteger progress;
    int numAuthors;

    ProgressBar progressBar;
    TextView progressLabel;

    public SearchProgressThread(Activity activity, ProgressBar progressBar, TextView progressLabel, AtomicInteger progress, int numAuthors) {
        this.mActivity = activity;
        this.progressBar = progressBar;
        this.progressLabel = progressLabel;
        this.progress = progress;
        this.numAuthors = numAuthors;
    }

    @Override
    public void run() {
        Log.d("[DEBUG   ]","Started progress");
        while (progress.get() < (1000 * numAuthors)) {


            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setProgress((progress.get() / numAuthors) / 10);
                }
            });
            SystemClock.sleep(100);
            progressBar.setProgress(100);
        }
    }

}
