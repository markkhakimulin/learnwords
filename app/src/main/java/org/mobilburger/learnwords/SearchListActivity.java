package org.mobilburger.learnwords;

import android.app.Activity;
import android.app.ProgressDialog;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.solver.widgets.Snapshot;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.database.ChangeEventListener;
import com.firebase.ui.database.FirebaseArray;
import com.firebase.ui.database.FirebaseListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.mobilburger.database.DBHelper;

import java.util.HashMap;
import java.util.concurrent.Callable;

/**
 * Created by hakimulin on 20.11.2017.
 */

public class SearchListActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {

    public Toolbar mToolbar;
    public SearchView searchView;
    public DatabaseReference mDatabase;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_list_activity);

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getSupportFragmentManager().addOnBackStackChangedListener(this);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.search_container, new UserDictionariesListFragment(), UserDictionariesListFragment.TAG);
        //ft.addToBackStack(UserDictionariesListFragment.TAG);
        ft.commit();

    }
    @Override
    public void onBackStackChanged() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        UserDictionariesListFragment fragment = (UserDictionariesListFragment)getSupportFragmentManager().findFragmentByTag(UserDictionariesListFragment.TAG);
        ((FirebaseCustomAdapter)fragment.getListAdapter()).startListening();
    }
    @Override
    public void onBackPressed() {
        if(getSupportFragmentManager().getBackStackEntryCount() > 0){
            getSupportFragmentManager().popBackStackImmediate();
        }
        else{
            super.onBackPressed();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //adapter.stopListening();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        getMenuInflater().inflate(R.menu.menu_search, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search_user);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint(getString(R.string.action_search_user));
        searchView.setOnQueryTextListener((UserDictionariesListFragment)getSupportFragmentManager().findFragmentByTag(UserDictionariesListFragment.TAG));
        searchView.setIconified(true);
        searchView.requestFocus();
        searchView.onActionViewExpanded();

        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            searchView.clearFocus();
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        searchView.setOnQueryTextListener(null);
    }

    public void onUserSelected(String userId) {

        searchView.setOnQueryTextListener(null);
        searchView.setIconified(false);
        searchView.clearFocus();
        searchView.onActionViewCollapsed();

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        DictionariesListFragment fragment = new DictionariesListFragment();
        Bundle bundle = new Bundle();
        bundle.putString("user_id",userId);
        fragment.setArguments(bundle);
        //ft.remove(getSupportFragmentManager().findFragmentByTag(UserDictionariesListFragment.TAG));
        ft.replace(R.id.search_container, fragment, DictionariesListFragment.TAG);
        ft.addToBackStack(DictionariesListFragment.TAG);
        ft.commit();
    }

    public void onUserDictionarySelected(String userId,String dictionaryId) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("user_id",userId);
        resultIntent.putExtra("dict_id", dictionaryId);
        setResult(RESULT_OK, resultIntent);
        finish();
    }



   public static class UserDictionariesListFragment extends ListFragment implements AdapterView.OnItemClickListener,SearchView.OnQueryTextListener {

        public static String TAG = "UserDictionariesListFragment";
        private SearchListActivity mContext;
        public DatabaseReference mDatabase;
        public Callable hideProgress = new Callable() {
            @Override
            public Object call() throws Exception {
                hideProgressDialog();
                return null;
            }
        };

       private ProgressBar mProgressDialog;

       public class UsersAdapter extends FirebaseCustomAdapter {

            public UsersAdapter(Context context, Query ref,Callable callable) {
                super(context,ref,callable);
            }

            @Override
            protected void populateView(View v, Object model, int position) {
                HashMap<String,String> object = (HashMap) model;
                final TextView textview =  v.findViewById(R.id.title);
                final ImageView photoProfile = v.findViewById(R.id.photo_url);
                textview.setText(object.get("name"));
                if (object.get("photo_url") != null)
                Picasso.with(mContext).load(object.get("photo_url")).fit().placeholder(R.drawable.profile_no_photo).into(photoProfile);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
        }

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);

            try {
                mContext = (SearchListActivity) context;
            } catch (ClassCastException e) {
                throw new ClassCastException(context.toString());
            }
        }

        @Override
        public void onDetach() {
            super.onDetach();
            mContext = null;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_user_list, container,false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            mDatabase = FirebaseDatabase.getInstance().getReference();
            mProgressDialog = view.findViewById(android.R.id.empty);
            showProgressDialog();
            setListAdapter(new UsersAdapter(getContext(), mDatabase.child("users").orderByChild("date").limitToFirst(30),hideProgress));
            getListView().setOnItemClickListener(this);
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
             String userId = ((UsersAdapter) getListAdapter()).getKey(position);
             mContext.onUserSelected(userId);
        }

        @Override
        public boolean onQueryTextSubmit(String constraint) {

            Query query;
            if (constraint != null && constraint.length() > 0) {
                query = mDatabase.child("users").orderByChild("name").startAt(constraint).endAt("~").limitToFirst(30);
            } else {
                query = mDatabase.child("users").orderByValue().limitToFirst(30);
            }
            showProgressDialog();
            setListAdapter(new UsersAdapter(getContext(), query,hideProgress));
            return true;
        }

        @Override
        public boolean onQueryTextChange(String constraint) {
            Query query;
            if (constraint != null && constraint.length() > 0) {
                query = mDatabase.child("users").orderByChild("name").startAt(constraint).endAt("~").limitToFirst(30);
            } else {
                query = mDatabase.child("users").orderByValue().limitToFirst(30);
            }
            showProgressDialog();
            setListAdapter(new UsersAdapter(getContext(), query,hideProgress));
            return false;
        }
       public void showProgressDialog() {
           mProgressDialog.setIndeterminate(true);
           mProgressDialog.setVisibility(View.VISIBLE);
       }

       public void hideProgressDialog() {
           mProgressDialog.setVisibility(View.GONE);
       }
    }

   public static class DictionariesListFragment extends ListFragment implements AdapterView.OnItemClickListener{

       public static String TAG = "DictionariesListFragment";
       private String  mUserId;
       public DatabaseReference mDatabase;
       private ProgressBar mProgressDialog;
       private FirebaseAuth mAuth;
       private SearchListActivity mContext;

       public class DictionaryAdapter extends FirebaseCustomAdapter {

            public DictionaryAdapter(Context context, Query ref,Callable callable) {
               super(context,ref,callable);
            }

           @Override
           public View getView(final int position, View view, ViewGroup viewGroup) {
               if (view == null) {
                   view = LayoutInflater.from(mContext).inflate(R.layout.user_dictionary_item, viewGroup, false);
               }

               ImageButton setupButton = (ImageButton)view.findViewById(R.id.setup_button);
               setupButton.setOnClickListener(new View.OnClickListener(){
                   @Override
                   public void onClick(View v) {
                       String dictId = ((DictionaryAdapter)getListAdapter()).getKey(position);
                       mContext.onUserDictionarySelected(mUserId,dictId);
                   }
               });


               String model = getKey(position);

               // Call out to subclass to marshall this model into the provided view
               populateView(view, model, position);
               return view;
           }

           @Override
           protected void populateView(final View v, final Object dict_id, final int position) {
               final TextView textview = (TextView) v.findViewById(R.id.title);
               mDatabase.child("dictionaries").child(dict_id.toString()).addListenerForSingleValueEvent(new ValueEventListener() {
                   @Override
                   public void onDataChange(DataSnapshot dataSnapshot) {
                       if (dataSnapshot.getValue() != null) {

                           HashMap value = (HashMap) dataSnapshot.getValue();
                           String userName = (String)value.get("user_name");
                           textview.setText((String)value.get("title")+" ("+userName+")");
                       } else {
                           textview.setText("removed"+" ("+dict_id.toString()+")");
                       }
                   }
                   @Override
                   public void onCancelled(DatabaseError databaseError) {}
               });
           }
       }

        @Override
        public void onResume() {
            super.onResume();
        }

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);

            try {
                mContext = (SearchListActivity) context;
            } catch (ClassCastException e) {
                throw new ClassCastException(context.toString());
            }
        }

        @Override
        public void onDetach() {
            super.onDetach();
            mContext = null;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            Bundle bundle = getArguments();
            if (bundle != null) {
                String userId = bundle.getString("user_id");
                if (userId != null) {
                    mUserId = userId;
                }
            }

            return inflater.inflate(R.layout.fragment_user_list, container,false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            mProgressDialog = view.findViewById(android.R.id.empty);
            showProgressDialog();
            mDatabase = FirebaseDatabase.getInstance().getReference();
            mAuth = FirebaseAuth.getInstance();

            setListAdapter(new DictionaryAdapter(mContext, mDatabase.child(DBHelper.TB_USERS).child(mUserId).child(DBHelper.TB_DICTS).orderByChild("public").equalTo(true), new Callable() {
                @Override
                public Object call() throws Exception {
                    hideProgressDialog();
                    return null;
                }
            }));

            getListView().setOnItemClickListener(this);
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
            //String dictId = ((DictionaryAdapter)getListAdapter()).getKey(position);
            //mContext.onUserDictionarySelected(mUserId,dictId);
        }
       public void showProgressDialog() {
           mProgressDialog.setIndeterminate(true);
           mProgressDialog.setVisibility(View.VISIBLE);
       }

       public void hideProgressDialog() {
           mProgressDialog.setVisibility(View.GONE);
       }

   }

   static abstract class FirebaseCustomAdapter extends FirebaseListAdapter<Object> {

        protected Callable onDatachanged;
        protected SearchListActivity mContext;
        FirebaseCustomAdapter(Context context, Query ref,Callable onDatachanged) {

            super(context, Object.class, R.layout.user_item, ref);
            this.onDatachanged = onDatachanged;
            this.mContext = (SearchListActivity) context;

        }

        String getKey(int position) {
            return mSnapshots.get(position).getKey();
        }

       @Override
       public void onDataChanged() {
            if (onDatachanged != null) try {
                onDatachanged.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
       }

   }



}
