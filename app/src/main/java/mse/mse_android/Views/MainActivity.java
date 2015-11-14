package mse.mse_android.Views;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;

import java.util.ArrayList;

import mse.mse_android.R;

public class MainActivity extends Activity {

    SearchFragment searchFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // set the view
        setContentView(R.layout.activity_main);

        searchFragment = new SearchFragment();

        // add the sprint 1 fragment
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.activity_main, searchFragment, "fragment_game").commit();
        }

        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && searchFragment.goBack()) {
            Log.d(this.getClass().getName(), "back button pressed");
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }
}
