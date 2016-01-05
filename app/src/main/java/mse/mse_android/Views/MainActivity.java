package mse.mse_android.Views;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import mse.mse_android.R;
import mse.mse_android.common.Config;
import mse.mse_android.common.LogLevel;
import mse.mse_android.common.Logger;
import mse.mse_android.data.Author;
import mse.mse_android.helpers.ReaderCreator;

public class MainActivity extends Activity {

    // navigation drawer items
    private DrawerLayout mDrawerLayout;
    private ExpandableListView mDrawerList;
    private NavDrawerAdapter mNavDrawerAdapter;

    private String[] mAuthorList;

    private ArrayList<String> groupItem;
    private ArrayList<Object> childItem;

    SearchFragment searchFragment;

    public MainActivity() {
    }

//    private static class XAPKFile {
//        public final boolean mIsMain;
//        public final int mFileVersion;
//        public final long mFileSize;
//
//        XAPKFile(boolean isMain, int fileVersion, long fileSize) {
//            mIsMain = isMain;
//            mFileVersion = fileVersion;
//            mFileSize = fileSize;
//        }
//    }
//
//    private static final XAPKFile[] xAPKS = {
//            new XAPKFile(
//                    true, // true signifies a main file
//                    2, // the version of the APK that the file was uploaded
//                    // against
//                    139402018L // the length of the zipfile in bytes right click on you expansion file and get the size in bytes, size must be same as zip size
//            ),
//
//    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ReaderCreator.mActivity = this;

        // set the view
        setContentView(R.layout.activity_main);

        // set the group and child lists
        setGroupOptions();
        setChildOptions();

        // add the nav drawer
        initDrawer();

        searchFragment = new SearchFragment();

        // add the sprint 1 fragment
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.content_frame, searchFragment, "fragment_game").commit();
        }

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

    private void initDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ExpandableListView) findViewById(R.id.left_drawer);

        // set up the drawer's list view with items and click listener
        mNavDrawerAdapter = new NavDrawerAdapter(this, groupItem, childItem);
        mDrawerList.setAdapter(mNavDrawerAdapter);
        mDrawerList.setOnChildClickListener(new DrawerItemClickListener());
    }

    private void setGroupOptions(){
        groupItem = new ArrayList<>();
        groupItem.add("Select Author");
        groupItem.add("Library");
    }

    private void setChildOptions() {

        childItem = new ArrayList<>();

        // add options for selecting author
        ArrayList<String> child = new ArrayList<>();
        for (Author nextAuthor : Author.values()) {
            if (nextAuthor.isSearchable())
                child.add(nextAuthor.getName());
        }
//        child.addAll(getAllAuthorNames());
        childItem.add(child);

        // add items to library
        child = new ArrayList<>();
        for (Author nextAuthor : Author.values()) {
            if (nextAuthor.isSearchable()) child.add(nextAuthor.getName());
        }
        childItem.add(child);
    }

//    private ArrayList<String> getAllAuthorNames() {
//        ArrayList<String> authorNames = new ArrayList<>();
//        for (Author nextAuthor : Author.values()) {
//            authorNames.add(nextAuthor.getName());
//        }
//        return authorNames;
//    }

    private class DrawerItemClickListener implements ExpandableListView.OnChildClickListener {

        @Override
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
            // TODO change to author.values.length when all authors added
            if (groupPosition == 0) {
                searchFragment.clickAuthor(groupPosition, childPosition, id);
                mNavDrawerAdapter.clickAuthor(childPosition);
                parent.setItemChecked(childPosition + 1, !parent.isItemChecked(childPosition + 1));
            } else {

                String location = null;
                int i = -1;
                int j = -1;
                while (j < childPosition && i < Author.values().length) {
                    i++;
                    if (Author.values()[i].isSearchable()) j++;
                }
                if (j<Author.values().length) location = Author.values()[i].getTargetPath(Author.values()[i].getContentsName()) + ".htm";

                searchFragment.goToLocation("file:///android_asset/" + location);

                mDrawerLayout.closeDrawers();
            }
            return false;
        }

    }

    public void openDrawer() {
        mDrawerLayout.openDrawer(GravityCompat.START);
    }

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
//
//        setContentView(R.layout.activity_main);
//
////        mTitle = mDrawerTitle = getTitle();
//        mPlanetTitles = getResources().getStringArray(R.array.planets_array);
//        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
//        mDrawerList = (ListView) findViewById(R.id.left_drawer);
//
//        // set a custom shadow that overlays the main content when the drawer opens
//        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
//        // set up the drawer's list view with items and click listener
//        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
//                R.layout.drawer_list_item, mPlanetTitles));
//        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
//    }

//    /* The click listner for ListView in the navigation drawer */
//    private class DrawerItemClickListener implements ListView.OnItemClickListener {
//        @Override
//        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//            clickAuthor(position);
//        }
//    }
//
//    private void clickAuthor(int position) {
//        Log.d("[DEBUG ]", "This is a debug");
//    }

}
