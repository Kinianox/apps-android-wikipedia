package org.wikipedia;

import android.app.*;
import android.content.*;
import android.os.*;
import android.support.v4.app.Fragment;
import android.view.*;
import android.widget.*;
import org.wikipedia.history.*;
import org.wikipedia.login.*;
import org.wikipedia.savedpages.*;
import org.wikipedia.settings.*;

public class NavDrawerFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static final int[] ACTION_ITEMS_TEXT = {
            R.string.nav_item_history,
            R.string.nav_item_saved_pages,
            R.string.nav_item_preferences,
            R.string.nav_item_login
    };
    private static final int[] ACTION_ITEM_IMAGES = {
            android.R.drawable.ic_menu_recent_history,
            android.R.drawable.ic_menu_save,
            android.R.drawable.ic_menu_preferences,
            android.R.drawable.ic_menu_add
    };

    private ListView navList;
    private NavListAdapter adapter;
    private WikipediaApp app;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_navdrawer, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        navList = (ListView) getView().findViewById(R.id.nav_list);
        adapter = new NavListAdapter();
        app = (WikipediaApp)getActivity().getApplicationContext();

        setupDynamicItems();

        navList.setAdapter(adapter);
        navList.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent();
        switch ((Integer)view.getTag()) {
            case R.string.nav_item_history:
                intent.setClass(this.getActivity(), HistoryActivity.class);
                getActivity().startActivity(intent);
                break;
            case R.string.nav_item_saved_pages:
                intent.setClass(this.getActivity(), SavedPagesActivity.class);
                startActivity(intent);
                break;
            case R.string.nav_item_preferences:
                intent.setClass(this.getActivity(), SettingsActivity.class);
                startActivity(intent);
                break;
            case R.string.nav_item_login:
                intent.setClass(this.getActivity(), LoginActivity.class);
                startActivityForResult(intent, LoginActivity.REQUEST_CODE_LOGIN);
                break;
            case R.string.nav_item_logout:
                doLogout();
                break;
            default:
                throw new RuntimeException("Unknown ID clicked!");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Okay, so this is really, really stupid, but
        // sometimes if the previous activity was destroyed before the callback is done
        // onActivityResult may be called *before* the onCreate, onActivityAttach, etc are called
        // This, of course, is fucking stupid. However, in this particular case (updating the login status)
        // we can just ignore it if that is the case and keep going.
        // BUGS! GRR!
        if (getView() != null) {
            if (resultCode == LoginActivity.LOG_IN_SUCCESSFUL) {
                setupDynamicItems();
                ((NavListAdapter)navList.getAdapter()).notifyDataSetChanged();
            }
        }
    }

    private void setupDynamicItems() {
        // Do login / logout swap
        if (app.getUserInfoStorage().isLoggedIn()) {
            ACTION_ITEMS_TEXT[3] = R.string.nav_item_logout;
        } else {
            ACTION_ITEMS_TEXT[3] = R.string.nav_item_login;
        }
    }

    private void doLogout() {
        final ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getString(R.string.logging_out_progress));
        progressDialog.setIndeterminate(true);

        new LogoutTask(app, app.getPrimarySite()) {
            @Override
            public void onBeforeExecute() {
                progressDialog.show();
            }

            @Override
            public void onFinish(Boolean result) {
                progressDialog.dismiss();
                setupDynamicItems();
                ((NavListAdapter)navList.getAdapter()).notifyDataSetChanged();
            }
        }.execute();
    }

    private class NavListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return ACTION_ITEMS_TEXT.length;
        }

        @Override
        public Object getItem(int position) {
            return ACTION_ITEMS_TEXT[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.item_nav_item, parent, false);
            }
            TextView navText = (TextView)convertView.findViewById(R.id.nav_item_text);
            ImageView navImage = (ImageView)convertView.findViewById(R.id.nav_item_image);
            navText.setText(ACTION_ITEMS_TEXT[position]);
            navImage.setImageResource(ACTION_ITEM_IMAGES[position]);
            convertView.setTag(ACTION_ITEMS_TEXT[position]);

            return convertView;
        }
    }
}