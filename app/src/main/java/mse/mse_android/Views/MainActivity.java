package mse.mse_android.Views;

import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import mse.mse_android.R;
import mse.mse_android.common.Config;
import mse.mse_android.common.LogLevel;
import mse.mse_android.common.Logger;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // set the view
        setContentView(R.layout.activity_main);

        // add the sprint 1 fragment
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.activity_main, new SearchFragment(), "fragment_game").commit();
        }

        setContentView(R.layout.activity_main);
    }
}
