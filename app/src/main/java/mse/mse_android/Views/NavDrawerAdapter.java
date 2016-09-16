//package mse.mse_android.Views;
//
//import java.util.ArrayList;
//
//import android.app.Activity;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.BaseExpandableListAdapter;
//import android.widget.TextView;
//
//import mse.mse_android.R;
//import mse.mse_android.data.author.Author;
//
///**
// * Adapted from: https://github.com/PrashamTrivedi/DrawerLayoutTest
// */
//public class NavDrawerAdapter extends BaseExpandableListAdapter {
//
//    private final Activity mActivity;
//
//    private ArrayList<String> groupItem, tempChild;
//    private ArrayList<Object> Childtem = new ArrayList<Object>();
//    private LayoutInflater minflater;
//    private Activity activity;
//
//    private boolean[] selectedAuthors;
//
//    public NavDrawerAdapter(Activity mActivity, ArrayList<String> grList, ArrayList<Object> childItem) {
//        this.mActivity = mActivity;
//        groupItem = grList;
//        this.Childtem = childItem;
//        selectedAuthors = new boolean[Author.values().length];
//        for (int i=0; i<selectedAuthors.length; i++) selectedAuthors[i] = false;
//        selectedAuthors[0] = true;
//    }
//
//    public void clickAuthor(int authorIndex) {
//        selectedAuthors[authorIndex] = !selectedAuthors[authorIndex];
//    }
//
//    public void setInflater(LayoutInflater mInflater, Activity act) {
//        this.minflater = mInflater;
//        activity = act;
//    }
//
//    @Override
//    public Object getChild(int groupPosition, int childPosition) {
//        return null;
//    }
//
//    @Override
//    public long getChildId(int groupPosition, int childPosition) {
//        return 0;
//    }
//
//    @Override
//    public View getChildView(int groupPosition, final int childPosition,
//                             boolean isLastChild, View convertView, ViewGroup parent) {
//        LayoutInflater inflater = mActivity.getLayoutInflater();
//        tempChild = (ArrayList<String>) Childtem.get(groupPosition);
//        if (convertView == null) {
//            convertView = inflater.inflate(R.layout.drawer_list_item, parent, false);
//        }
//        TextView text = (TextView) convertView.findViewById(R.id.drawer_list_item_text);
//        text.setText(tempChild.get(childPosition));
//        if (groupPosition == 0 && selectedAuthors[childPosition]) {
//            text.setBackgroundColor(mActivity.getResources().getColor(R.color.childItemSelected));
//        } else {
//            text.setBackgroundColor(mActivity.getResources().getColor(R.color.childItemBackground));
//        }
//        convertView.setTag(tempChild.get(childPosition));
//        return convertView;
//    }
//
//
//    @Override
//    public int getChildrenCount(int groupPosition) {
//        return ((ArrayList<String>) Childtem.get(groupPosition)).size();
//    }
//
//    @Override
//    public Object getGroup(int groupPosition) {
//        return null;
//    }
//
//    @Override
//    public int getGroupCount() {
//        return groupItem.size();
//    }
//
//    @Override
//    public void onGroupCollapsed(int groupPosition) {
//        super.onGroupCollapsed(groupPosition);
//    }
//
//    @Override
//    public void onGroupExpanded(int groupPosition) {
//        super.onGroupExpanded(groupPosition);
//    }
//
//    @Override
//    public long getGroupId(int groupPosition) {
//        return 0;
//    }
//
//    @Override
//    public View getGroupView(int groupPosition, boolean isExpanded,
//                             View convertView, ViewGroup parent) {
//        LayoutInflater inflater = mActivity.getLayoutInflater();
//        if (convertView == null) {
//            convertView = inflater.inflate(R.layout.drawer_group_item, parent, false);
//        }
//        ((TextView) convertView).setText(groupItem.get(groupPosition));
//        convertView.setTag(groupItem.get(groupPosition));
//        return convertView;
//    }
//
//    @Override
//    public boolean hasStableIds() {
//        return false;
//    }
//
//    @Override
//    public boolean isChildSelectable(int groupPosition, int childPosition) {
//        return true;
//    }
//
//}