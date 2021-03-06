package org.mobilburger.learnwords;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.mobilburger.database.DBHelper;
import org.mobilburger.database.DBHelper.Direction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import rx.functions.Action1;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,GoogleApiClient.OnConnectionFailedListener {
    private SwitchCompat mSwitchd = null;
    private Context mContext = null;
   // private LockScreenUtil mLockScreenUtil;
    private DrawerLayout mDrawerLayout;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private ActionBarDrawerToggle mDrawerToggle;
    private Toolbar mToolbar;
    private TextView mToogleTitle;
    private SQLiteOpenHelper mSqLiteOpenHelper;
    private ProgressBar mProgressbar;
    private NavigationView mNavigationView,mNavigationViewFooter;
    //private TextView titleView;
    public SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private TabLayout mTabs;
    private ProgressDialog mProgressDialog;
    private SharedPreferences preferences;
    private Intent lockServiceIntent;
    private FloatingActionButton addWordButton;
    private CreateDefaultTask createDefaultTask;
    private FirebaseAuth mAuth;

    public static String currentDictId = null;
    private ListView mStatisticList;

    private ImageView photoProfile;
    private Button loginNavButton;
    private ImageButton syncNavButton;
    private DatabaseReference mDatabase;
    private SearchView searchView;
    private Boolean isYesNoMessageDialogShow = false;
    private GoogleApiClient mGoogleApiClient;

    private BroadcastReceiver stopServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onStart();
        }
    };
    private BroadcastReceiver startServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onStart();
        }
    };
    private View.OnClickListener syncListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
            if (haveNetworkConnection()) {
                sync();
            } else {
                showErrorMessage(getString(R.string.error_no_internet_connection));
            }
        }
    };

    private View.OnClickListener logInListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            mDrawerLayout.closeDrawer(GravityCompat.START);
            if (haveNetworkConnection()) {
                gotoSignInActivity();
            } else {
                showErrorMessage(getString(R.string.error_no_internet_connection));
            }

        }
    };
    private CharSequence currentDictTitle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSqLiteOpenHelper = DBHelper.getOpenHelper(getApplicationContext());

        mTitle = mDrawerTitle = getTitle();

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        //titleView = (TextView) findViewById(R.id.dict_title);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mContext = this;
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // enable ActionBar app icon to behave as action to toggle nav drawer
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //getSupportActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,
                mToolbar,/* DrawerLayout object */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */)   {
                public void onDrawerClosed(View view) {
                    if (getSupportActionBar() != null)
                    getSupportActionBar().setTitle(mTitle);
                    invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                if (getSupportActionBar() != null)
                getSupportActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        mNavigationView =  findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);
        mNavigationViewFooter = findViewById(R.id.nav_view_footer);
        mNavigationViewFooter.setNavigationItemSelectedListener(this);

        //mLockScreenUtil = new LockScreenUtil(mContext);


        mToogleTitle = (TextView) findViewById(R.id.manage_service_title);

        mSwitchd = (SwitchCompat) findViewById(R.id.switch_locksetting);



        mProgressbar = (ProgressBar) findViewById(R.id.progress);

        mViewPager = (ViewPager) findViewById(R.id.container);

        mTabs = (TabLayout) findViewById(R.id.tabs);

        preferences = getSharedPreferences("preference", Activity.MODE_PRIVATE);

        currentDictId = preferences.getString("current_dict_id",null);




        addWordButton = (FloatingActionButton) findViewById(R.id.fab_add_word);
        addWordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addWord();
            }
        });

        registerReceiver(stopServiceReceiver, new IntentFilter("StopService"));
        registerReceiver(startServiceReceiver, new IntentFilter("StartService"));

        View header =  mNavigationView.getHeaderView(0);
        loginNavButton = header.findViewById(R.id.nav_login_button);
        syncNavButton = header.findViewById(R.id.sync_button);
        photoProfile = header.findViewById(R.id.photo_url);


        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        createViewPager();
        createNavigationMenu();

        auth();


    }

    //-------------------------------------------Implementation-------------------------------------------
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(stopServiceReceiver);
        unregisterReceiver(startServiceReceiver);
        stopServiceReceiver = null;
        startServiceReceiver = null;
    }

    @Override
    protected void onStart() {
        super.onStart();


        mSwitchd.setEnabled(!isDictionaryNotChoosen());

        boolean lockState = LockScreenUtil.getInstance(mContext).isServiceRunning(LockScreenService.class);
        if (lockState) {
            mSwitchd.setChecked(true);
            mToogleTitle.setText(R.string.stop_service);

        } else {
            mSwitchd.setChecked(false);
            mToogleTitle.setText(R.string.start_service);

        }
        mSwitchd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {


                lockServiceIntent = new Intent(mContext,LockScreenService.class);
                //startActivity(lockServiceIntent);

                if (!isChecked) {
                    setStopService();
                } else {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {

                            Intent permissionActivityIntent = new Intent(mContext, PermissionActivity.class);
                            permissionActivityIntent.setAction(PermissionActivity.PERMISSION_PHONE_STATE);
                            permissionActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                            LockScreenUtil.getInstance(mContext).createAsyncSubject()
                                    .subscribe(new Action1<Boolean>() {

                                        @Override
                                        public void call(Boolean value) {
                                            setRunService();
                                        }

                                    });
                            startActivity(permissionActivityIntent);

                        } else {
                            setRunService();
                        }
                    } else {
                        setRunService();
                    }
                }
            }
        });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Toast.makeText(this, "Google Play Services error."+connectionResult, Toast.LENGTH_SHORT).show();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem item) {


        int id = item.getItemId();
        switch (id) {
            case R.id.add:
                addDictionary();
                break;
            case R.id.action_search:
                //открываем поиск пользователей
                startActivityForResult(new Intent(this, SearchListActivity.class),125);
                break;
            case R.id.action_about_app:
                PackageManager packageManager = getPackageManager();
                String packageName = getPackageName();

                String mVersion = "version : ";
                String mVersionName = "not available"; // initialize String

                try {
                    mVersionName = packageManager.getPackageInfo(packageName, 0).versionName;
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                String message = mVersion+mVersionName+"\n";
                message+= "developer : Mark Khakimulin"+"\n";
                message+= "mark.khakimulin@gmail.com";
                AlertDialog.Builder builderAbout = new AlertDialog.Builder(this);
                builderAbout.setMessage(message).setTitle(getString(R.string.title_about_app));
                builderAbout.create().show();
                break;
            case R.id.action_help:
                AlertDialog.Builder builderHelp = new AlertDialog.Builder(this);
                builderHelp.setMessage("not ready yet").setTitle(getString(R.string.action_help));
                builderHelp.create().show();
                break;
            default:

                String dict_id = getCurrentDictId(id);
                switchVocabulary(dict_id,null);
                item.setCheckable(true);

                break;
        }
        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                Intent settingIntent = new Intent(this, SettingsActivity.class);
                settingIntent.putExtra(DBHelper.CN_ID_DICT, currentDictId);
                startActivity(settingIntent);
                break;
            case R.id.action_rename_dict:
                renameDictionary();
                break;
            case R.id.action_load_dict:
                loadDictionary();
                break;
            case R.id.action_upload_dict:
                uploadDictionary();
                break;
            case R.id.action_remove_dict:
                removeDictionary();
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {

        super.onActivityResult(requestCode,resultCode,data);

        if (resultCode == RESULT_OK && requestCode == OpenFileDialogActivity.CHOOSE_FILE) {

            if (data != null && data.getBooleanExtra("choose",true))
            {
                showProgress(true);

                JsonElement obj = null;
                JsonParser  mParser = new JsonParser();
                try {
                    obj =  mParser.parse(new FileReader(data.getStringExtra("data")));
                    new ParseDictionaryTask(currentDictId, obj).execute();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } if (resultCode == RESULT_OK && requestCode == 124) {

            auth();
            sync();

        } if (resultCode == RESULT_OK && requestCode == 125) {

            String userId = data.getStringExtra("user_id");
            String dictId = data.getStringExtra("dict_id");
            downloadUserDictionaries(userId, dictId);

        }

        else
        if (resultCode == RESULT_OK && requestCode == OpenFileDialogActivity.SAVE_FILE) {



            if (data != null && data.getBooleanExtra("choose",true))
            {
                showProgress(true);
                SaveDictionaryTask saveDictionaryTask = new SaveDictionaryTask(data.getStringExtra("data"), currentDictId);
                saveDictionaryTask.execute();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search_word);
        //final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        //searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

                @Override
                public boolean onQueryTextSubmit(String query) {
                    if (((CursorAdapter) gerCurrentFragment().getListView().getAdapter()).getFilter() != null)
                        ((CursorAdapter) gerCurrentFragment().getListView().getAdapter()).getFilter().filter(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    if (((CursorAdapter) gerCurrentFragment().getListView().getAdapter()).getFilter() != null)
                        ((CursorAdapter) gerCurrentFragment().getListView().getAdapter()).getFilter().filter(newText);
                    return true;
                }
            });
        }

        return super.onCreateOptionsMenu(menu);
    }

    private void downloadUserDictionaries(final String userId,final String dictId) {

        new Thread() {
            @Override
            public void run() {



                mDatabase.child(DBHelper.TB_USERS).child(userId).child(DBHelper.TB_DICTS).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        Boolean downloadAllDicts = dictId == null;

                        for (DataSnapshot userDictSnapshot: dataSnapshot.getChildren()) {

                            final String userDictId = userDictSnapshot.getKey();
                            HashMap values = (HashMap) userDictSnapshot.getValue();
                            Boolean isPublic = (Boolean)values.get("public");

                            final FirebaseUser user = mAuth.getCurrentUser();
                            String myUid = "";
                            if (user != null) {
                                myUid = user.getUid();
                            }
                            Boolean isMine = myUid.equalsIgnoreCase(userId);

                            Callable<String> callbackDictionaryDownloaded = new Callable<String>() {
                                @Override
                                public String call() throws Exception {
                                    hideProgressDialog();
                                    return null;
                                }

                                @Override
                                public void call(final String ownerId) {

                                    mDatabase.child(DBHelper.TB_USERS).child(ownerId).child(DBHelper.TB_WORDS).child(userDictId).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {

                                            for (DataSnapshot userWordSnapshot : dataSnapshot.getChildren()) {

                                                String wordId = userWordSnapshot.getKey();
                                                String date = (String) userWordSnapshot.child(DBHelper.CN_DATE).getValue();
                                                Long lock = (Long) userWordSnapshot.child(DBHelper.CN_LOCK).getValue();
                                                //если не нашли в локальной базе словарь с таким id то создаем его
                                                String myUid = "";
                                                if (user != null) {
                                                    myUid = user.getUid();
                                                }

                                                downloadWord(userDictId, wordId, myUid, lock.intValue(), date, new Callable<Boolean>() {
                                                    @Override
                                                    public void call(Boolean value) {
                                                        if (value) {
                                                            refreshVocabulary();
                                                        }
                                                    }

                                                    @Override
                                                    public Boolean call() throws Exception {return null;}
                                                });
                                            }
                                            hideProgressDialog();
                                        }
                                        @Override
                                        public void onCancelled (DatabaseError databaseError){
                                        }
                                    });
                                }
                            };

                            if (downloadAllDicts || userDictId.equalsIgnoreCase(dictId)){
                                if (isMine || isPublic) {
                                    showProgressDialog();
                                    downloadDictionary(userDictId, myUid, callbackDictionaryDownloaded);
                                }
                            }
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {  }
                });
            }
            private void downloadDictionary(final String dictId, final String userId,@Nullable final Callable<String> callback) {

                mDatabase.child(DBHelper.TB_DICTS).child(dictId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        HashMap values = (HashMap) dataSnapshot.getValue();
                        SQLiteDatabase dbw = mSqLiteOpenHelper.getWritableDatabase();

                        if (values != null) {
                            String title = (String) values.get("title");
                            String ownerId = (String)values.get("user_id");
                            String ownerName = (String)values.get("user_name");
                            Boolean isMine = values.get("user_id").equals(userId);//если владелец словаря совпадает с текущим пользователем то он может редактировать словарь

                            removeDictionaryFromDB(dbw,dictId);
                            addDictionaryToDB(dbw, title, isMine,ownerName,dictId);
                            dbw.close();
                            createNavigationMenu();

                            if (isDictionaryNotChoosen())
                            switchVocabulary(dictId,null);

                            callback.call(ownerId);

                            if (!isMine && !userId.equalsIgnoreCase("")) {

                                removeUserDictionaryFromFB(userId,dictId);
                                addUserDictionaryToFB(userId, dictId,getDateTime());
                            }

                        } else {
                            removeDictionaryFromDB(dbw,dictId);
                            dbw.close();
                            createNavigationMenu();
                            switchVocabulary(getNextDictId(),null);

                            if (!userId.equalsIgnoreCase("")) {
                                removeUserDictionaryFromFB(userId, dictId);
                            }

                            try {
                                callback.call();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                });
            }

            private void downloadWord(final String dictId,final String wordId,final String userId,final int lock,final String date,@Nullable final Callable<Boolean> callback) {

                mDatabase.child(DBHelper.TB_WORDS).child(dictId).child(wordId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        HashMap values = (HashMap) dataSnapshot.getValue();
                        SQLiteDatabase dbw = mSqLiteOpenHelper.getWritableDatabase();
                        if (values != null) {
                            String word = (String) values.get("word");
                            String translate = (String) values.get("translate");
                            Boolean isMine = values.get("user_id").equals(userId);//если владелец словаря совпадает с текущим пользователем то он может редактировать словарь
                            String myDate = getDateTime();
                            if (addWordToDB(dbw, word, translate, dictId, !isMine ? 0 : lock, !isMine ? myDate : date, wordId) != null) {
                                dbw.close();

                                callback.call(true);

                                if (!isMine && !userId.equalsIgnoreCase("")) {
                                    addUserWordToFB(userId,dictId,wordId, 0, myDate);
                                }
                            }
                        } else {
                            if (removeWordFromDB(dbw,dictId,wordId)) {
                                dbw.close();
                                callback.call(true);
                                if (!userId.equalsIgnoreCase("")) {
                                    removeUserWordFromFB(userId, dictId, wordId);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                });
            }

        }.run();

    }

    public CharSequence getCurrentDictTitle() {

        String title = "";
        SQLiteDatabase db = mSqLiteOpenHelper.getReadableDatabase();
        Cursor cursor = db.query(DBHelper.TB_DICTS,new String[]{DBHelper.CN_TITLE},"_id = ?",new String[]{currentDictId},null,null,null);
        if (cursor.moveToNext()) {
            title = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return title;
    }

    interface Callable<V> extends java.util.concurrent.Callable {
        void call(V value);

        @Override
        V call() throws Exception;
    }

    private void uploadUserDictionaries(@Nullable final String dictId) {
        new Thread() {
            @Override
            public void run() {

                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null ) {

                    SQLiteDatabase dbr = mSqLiteOpenHelper.getReadableDatabase();
                    String[] args = null;
                    String selection = null;
                    if (dictId != null) {
                        args = new String[]{dictId};
                        selection = "_id = ?";
                    }
                    Cursor cursor = dbr.query(DBHelper.TB_DICTS, null, selection, args, null, null, null);

                    //Boolean isMine = true;
                    while (cursor.moveToNext()) {
                        String dict_id = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ID));
                        String title = cursor.getString(cursor.getColumnIndex(DBHelper.CN_TITLE));
                        Boolean isMine = cursor.getInt(cursor.getColumnIndex(DBHelper.CN_EDITABLE)) > 0;

                        if (isMine) {
                            addDictionaryToFB(user.getUid(), dict_id, title,user.getDisplayName());
                        }
                        addUserDictionaryToFB(user.getUid(), dict_id, getDateTime());

                        selection = "dict_id = ?";
                        args = new String[]{dict_id};
                        Cursor wordCursor = dbr.query(DBHelper.TB_WORDS, null, selection, args, null, null, null);

                        while (wordCursor.moveToNext()) {
                            final String word_id = wordCursor.getString(wordCursor.getColumnIndex(DBHelper.CN_ID));
                            final String word = wordCursor.getString(wordCursor.getColumnIndex(DBHelper.CN_WORD));
                            final String translate = wordCursor.getString(wordCursor.getColumnIndex(DBHelper.CN_TRANSLATE));
                            final int lock = wordCursor.getInt(wordCursor.getColumnIndex(DBHelper.CN_LOCK));
                            final String date = wordCursor.getString(wordCursor.getColumnIndex(DBHelper.CN_DATE));
                            if (isMine) {
                                addWordToFB(user.getUid(), dict_id, word_id, word, translate);
                            }
                            addUserWordToFB(user.getUid(), dict_id, word_id, lock, date);
                        }
                        wordCursor.close();
                    }
                    cursor.close();
                }
            }
        }.run();
    }

    public boolean isYesNoMessageDialogShow() {
        return isYesNoMessageDialogShow;
    }

    private void auth() {

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            //startActivityForResult(new Intent(this, SignInActivity.class),124);
            loginNavButton.setText(getString(R.string.no_auth));
            loginNavButton.setOnClickListener(logInListener);
            syncNavButton.setOnClickListener(logInListener);
            photoProfile.setImageResource(R.drawable.profile_no_photo);

        } else {
            loginNavButton.setText(user.getDisplayName());
            loginNavButton.setOnClickListener(logInListener);
            syncNavButton.setOnClickListener(syncListener);
            Picasso.with(MainActivity.this).load(user.getPhotoUrl()).fit().placeholder(R.drawable.profile_no_photo).into(photoProfile);
        }

    }

    private void sync() {
        final FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            uploadUserDictionaries(null);
            downloadUserDictionaries(user.getUid(), null);
        }
    }

    public void gotoSignInActivity() {

        startActivityForResult(new Intent(this, SignInActivity.class),124);
    }

    public CreateDefaultTask getLoadDefaultTask() {
        return createDefaultTask;
    }

    public void createDefaultTask(WordStatListFragment fragment) {
        createDefaultTask = new CreateDefaultTask(fragment);
        createDefaultTask.run();
    }

    void hideFabButtons() {
        addWordButton.setVisibility(View.INVISIBLE);
    }

    void showFabButtons() {
        addWordButton.setVisibility(View.VISIBLE);
    }

    void setRunService () {
        mContext.startService(lockServiceIntent);
        mToogleTitle.setText(R.string.stop_service);
    }

    void setStopService() {

        Intent broadcastIntent = new Intent("PreStopService");
        sendBroadcast(broadcastIntent);
        mContext.stopService(lockServiceIntent);
        mToogleTitle.setText(R.string.start_service);

    }

    private  void createViewPager() {

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        // Set up the ViewPager with the sections adapter.
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mTabs.setupWithViewPager(mViewPager);

    }

    public ListFragment gerFragment(int position) {
        return (ListFragment) mSectionsPagerAdapter.instantiateItem(mViewPager, position);
    }

    public ListFragment gerCurrentFragment() {
        return (ListFragment) mSectionsPagerAdapter.instantiateItem(mViewPager, mViewPager.getCurrentItem());
    }

    private Boolean isDictionaryNotChoosen() {
        return currentDictId == null;

    }

    public void removeDictionary() {

        if (isDictionaryNotChoosen()) {

            showErrorMessage(getString(R.string.error_no_dict_choose));
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(R.string.confirm_remove_dict_message).setTitle(R.string.confirm_remove_dict_title);
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                Boolean isMine = isMineDictionary(currentDictId);

                SQLiteDatabase db = mSqLiteOpenHelper.getWritableDatabase();

                if (removeDictionaryFromDB(db,currentDictId)) {

                    db.close();
                    String new_dict_id = getNextDictId();

                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        //db = mSqLiteOpenHelper.getReadableDatabase();
                        if (isMine)
                        {
                            removeDictionaryFromFB(currentDictId);
                        }
                        removeUserDictionaryFromFB(user.getUid(),currentDictId);
                    }
                    switchVocabulary(new_dict_id, new Callable() {
                        @Override
                        public void call(Object value) {

                            createNavigationMenu();
                        }

                        @Override
                        public Object call() throws Exception {return null;}
                    });

                }
            }
        });
        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
        builder.create().show();
    }

    public void renameDictionary() {


        if (isDictionaryNotChoosen()) {

            showErrorMessage(getString(R.string.error_no_dict_choose));
            return;
        }

        final AlertDialog.Builder createProjectAlert = new AlertDialog.Builder(this);
        createProjectAlert.setTitle(getString(R.string.action_rename_dict));
        View dialogView = getLayoutInflater().inflate(R.layout.edit_dictionary_dialog, null);
        final EditText dictionaryName = (EditText) dialogView.findViewById(R.id.edit_dictionary);

        dictionaryName.setText(getCurrentDictTitle());

        createProjectAlert.setView(dialogView)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null);

        final AlertDialog mAlertDialog  = createProjectAlert.create();
        mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {

            //Это сделано для того чтобы не закрывалось окно диалога при нажатии на кнопку ОК
            @Override
            public void onShow(DialogInterface dialog) {

                Button b = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        if (dictionaryName.getText().length() == 0) {
                            dictionaryName.setError(getString(R.string.error_dict_is_empty));
                            dictionaryName.requestFocus();
                            return;
                        }

                        if (!isMineDictionary(currentDictId)) {
                            Toast.makeText(mContext,getString(R.string.error_dictionary_is_not_yours),Toast.LENGTH_SHORT).show();

                        } else {

                            SQLiteDatabase db = mSqLiteOpenHelper.getWritableDatabase();

                            String newTitle = dictionaryName.getText().toString();
                            String localDictId = addDictionaryToDB(db, newTitle, true,"", currentDictId);
                            if (localDictId != null) {
                                db.close();
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    addDictionaryToFB(user.getUid(), currentDictId, newTitle,user.getDisplayName());
                                    addUserDictionaryToFB(user.getUid(), currentDictId, getDateTime());
                                }
                                createNavigationMenu();
                            }
                        }
                        mAlertDialog.dismiss();
                    }
                });
            }
        });
        mAlertDialog.show();
    }

    public void addDictionary(String title) {
        AlertDialog.Builder createProjectAlert = new AlertDialog.Builder(this);
        createProjectAlert.setTitle(getString(R.string.new_dict_title));

        View view = getLayoutInflater().inflate(R.layout.create_dictionary_dialog, null);
        final EditText dict_name = view.findViewById(R.id.dictionary_name);
        dict_name.setText(title);

        createProjectAlert.setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        SQLiteDatabase db = mSqLiteOpenHelper.getWritableDatabase();
                        String title = dict_name.getText().toString();
                        String localDictId = addDictionaryToDB(db, title, true, "", null);
                        if (localDictId != null) {
                            db.close();

                            switchVocabulary(localDictId, null);

                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                addDictionaryToFB(user.getUid(), currentDictId, title, user.getDisplayName());
                                addUserDictionaryToFB(user.getUid(), currentDictId, getDateTime());
                            }
                            createNavigationMenu();
                        }

                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        createProjectAlert.show();
    }

    public void addDictionary() {
        addDictionary("");
    }

    public void addWord() {


        if (isDictionaryNotChoosen()) {

            showErrorMessage(getString(R.string.error_no_dict_choose));
            return;
        }

        final AlertDialog.Builder createProjectAlert = new AlertDialog.Builder(this);
        createProjectAlert.setTitle(getString(R.string.add_word_title));
        View dialogView = getLayoutInflater().inflate(R.layout.edit_word_dialog, null);
        final EditText wordText;
        final EditText translationText;
        if (((WordStatListFragment)gerCurrentFragment()).TAG == Direction.WORD_TO_TRANSLATE.toString()) {
            wordText = (EditText) dialogView.findViewById(R.id.edit_word);
            translationText = (EditText) dialogView.findViewById(R.id.edit_tranclation);
        } else {
            wordText = (EditText) dialogView.findViewById(R.id.edit_tranclation);
            translationText = (EditText) dialogView.findViewById(R.id.edit_word);
         }

        createProjectAlert.setView(dialogView)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null);

        final AlertDialog mAlertDialog  = createProjectAlert.create();
        mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {

            //Это сделано для того чтобы не закрывалось окно диалога при нажатии на кнопку ОК
            @Override
            public void onShow(DialogInterface dialog) {

                Button b = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        if (wordText.getText().length() == 0) {
                            wordText.setError(getString(R.string.error_empty_text_word));
                            wordText.requestFocus();
                            return;
                        }
                        if (translationText.getText().length() == 0) {
                            translationText.setError(getString(R.string.error_empty_text_translate));
                            translationText.requestFocus();
                            return;
                        }

                        if (!isMineDictionary(currentDictId)) {
                            Toast.makeText(mContext,getString(R.string.error_dictionary_is_not_yours),Toast.LENGTH_SHORT).show();

                        } else {

                            String word = wordText.getText().toString().trim();
                            String translate = translationText.getText().toString().trim();
                            String date = getDateTime();
                            SQLiteDatabase db = mSqLiteOpenHelper.getWritableDatabase();
                            String wordId = addWordToDB(db, word, translate, currentDictId, 0, getDateTime(), null);
                            if (wordId != null) {
                                db.close();
                                refreshVocabulary();
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    addWordToFB(user.getUid(), currentDictId, wordId, word, translate);
                                    addUserWordToFB(user.getUid(), currentDictId, wordId, 0, date);
                                }
                            }
                        }
                        mAlertDialog.dismiss();
                    }
                });
            }
        });
        mAlertDialog.show();
    }

    public void loadDictionary() {

        if (isDictionaryNotChoosen()) {

            showErrorMessage(getString(R.string.error_no_dict_choose));
            return;
        }
        if (!isMineDictionary(currentDictId)) {
            Toast.makeText(mContext,getString(R.string.error_dictionary_is_not_yours),Toast.LENGTH_SHORT).show();

        } else {
            Intent intent = new Intent(this, OpenFileDialogActivity.class);
            intent.setAction(OpenFileDialogActivity.DOWNLOAD_DICTIONARY);
            startActivityForResult(intent, OpenFileDialogActivity.CHOOSE_FILE);
        }
    }

    public void uploadDictionary() {

        if (isDictionaryNotChoosen()) {

            showErrorMessage(getString(R.string.error_no_dict_choose));
            return;
        }
        Intent intent = new Intent(this,OpenFileDialogActivity.class);
        intent.setAction(OpenFileDialogActivity.UPLOAD_DICTIONARY);
        startActivityForResult(intent,OpenFileDialogActivity.SAVE_FILE);
    }

    void setCurrentDictId(Callable callback) throws Exception {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("current_dict_id",currentDictId);
        editor.apply();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            setCurrentDictionaryToFB(user.getUid(),currentDictId);
        }
        if (callback != null) callback.call(currentDictId);
    }

    private void switchVocabulary(String id,@Nullable Callable callable) {


        if (!(id ==null || currentDictId==null) && !currentDictId.equalsIgnoreCase(id))
        {
            if (LockScreenUtil.getInstance(mContext).isServiceRunning(LockScreenService.class)) {

                //Переводим переключатель выкл. Сервис остановится сам
                mSwitchd.setChecked(false);
                mToogleTitle.setText(R.string.start_service);

                Toast.makeText(this,getString(R.string.dictionary_changed),Toast.LENGTH_LONG).show();
            }
        }

        currentDictId = id;

        try {
            setCurrentDictId(callable);
        } catch (Exception e) {
            e.printStackTrace();
        }

        refreshVocabulary();
        mSwitchd.setEnabled(!isDictionaryNotChoosen());
    }

    public void refreshVocabulary(Boolean redraw) {

        LoadWordsTask loadWords = new LoadWordsTask(Direction.WORD_TO_TRANSLATE,(WordStatListFragment)gerFragment(0),redraw);
        loadWords.run();
        LoadWordsTask loadWords1 = new LoadWordsTask(Direction.TRANSLATE_TO_WORD,(WordStatListFragment)gerFragment(1),redraw);
        loadWords1.run();
    }

    public void refreshVocabulary() {
        refreshVocabulary(false);
    }

    public void refreshVocabulary(WordStatListFragment fragment ) {
        LoadWordsTask loadWords = new LoadWordsTask(fragment.mDirection,fragment);
        loadWords.run();
    }

    private void createNavigationMenu() {
       new Thread( new CreateNavigationMenu()).run();
    }

    private class CreateNavigationMenu implements Runnable {

        @Override
        public void run() {
            Menu menu = mNavigationView.getMenu();
            for (int i = 0;i<menu.size();i++) {
                if (menu.findItem(i) != null) {
                    menu.removeItem(i);
                }
            }

            SQLiteDatabase db = mSqLiteOpenHelper.getReadableDatabase();
            if (db == null) {
                return;
            }
            Cursor cursor = db.query(DBHelper.TB_DICTS,null,null,null,null,null,DBHelper.CN_ID);
            int order = 0;

            while (cursor.moveToNext()) {

                createNavigationItem(menu,order,
                        cursor.getString(cursor.getColumnIndex(DBHelper.CN_ID)),
                        cursor.getString(cursor.getColumnIndex(DBHelper.CN_TITLE)),
                        cursor.getInt(cursor.getColumnIndex(DBHelper.CN_EDITABLE)),
                        cursor.getString(cursor.getColumnIndex(DBHelper.CN_USER_NAME)));
                order++;
            }
            cursor.close();
            db.close();

        }
        private MenuItem createNavigationItem(Menu menu,int order,String id,String dictName,int editable,String userName) {


            MenuItem menuItem = menu.findItem(order);
            if (menuItem == null) {

                menuItem = menu.add(R.id.my_dict,order,10+order, dictName);

            }

            if (editable == 0) dictName+=" ("+userName+")";
            menuItem.setTitle(dictName);

            if (id.equalsIgnoreCase(currentDictId)) {

                menuItem.setCheckable(true);
                menuItem.setChecked(true);
            }

            return menuItem;
        }
    }


//------------------------------------------------------database FIREBASE----------------------------------------------------------
    private String addDictionaryToFB(String userId, String dict_id,String title,String userName) {

        HashMap<String, Object> values = new HashMap<>();
        values.put("title", title);
        values.put("user_id", userId);
        values.put("user_name", userName);

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put(DBHelper.TB_DICTS+"/" + dict_id, values);
        mDatabase.updateChildren(childUpdates);
        return dict_id;
    }

    private String setCurrentDictionaryToFB(String userId, String dict_id) {

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put(DBHelper.TB_USERS+"/"+userId+"/current_dict_id",dict_id);
        mDatabase.updateChildren(childUpdates);
        return dict_id;
    }

    private String addUserDictionaryToFB(String userId, String dict_id,String date) {

        SharedPreferences preferences = getSharedPreferences("preference", Activity.MODE_PRIVATE);
        Boolean isPrivacy = preferences.getBoolean(dict_id + DBHelper.CN_PUBLIC, getResources().getBoolean(R.bool.privacy));

        HashMap<String, Object> values = new HashMap<>();
        values.put(DBHelper.CN_DATE, date);
        values.put(DBHelper.CN_PUBLIC, isPrivacy);

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put(DBHelper.TB_USERS+"/"+userId+"/"+DBHelper.TB_DICTS+"/"+dict_id, values);

        mDatabase.updateChildren(childUpdates);
        return dict_id;
    }

    private String addWordToFB(String userId, String dict_id,String word_id,String word,String translate) {

        HashMap<String, Object> values = new HashMap<>();
        values.put("word", word);
        values.put("translate", translate );
        values.put("user_id", userId );

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put(DBHelper.TB_WORDS+"/"+dict_id+"/"+ word_id, values);

        mDatabase.updateChildren(childUpdates);
        return word_id;
    }

    private String addUserWordToFB(String userId, String dict_id,String word_id,int lock,String date) {

        HashMap<String, Object> user_values = new HashMap<>();
        user_values.put("lock", lock);
        user_values.put("date", date );

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put(DBHelper.TB_USERS+"/"+userId+"/"+DBHelper.TB_WORDS+"/"+dict_id+"/"+word_id, user_values);

        mDatabase.updateChildren(childUpdates);

        return word_id;
    }

    private void removeDictionaryFromFB(String dict_id) {
        mDatabase.child(DBHelper.TB_DICTS+"/"+dict_id).removeValue();
        mDatabase.child(DBHelper.TB_WORDS+"/"+dict_id).removeValue();
    }

    private void removeUserDictionaryFromFB(String userId,String dict_id) {
        mDatabase.child(DBHelper.TB_USERS+"/" + userId + "/"+DBHelper.TB_DICTS+"/"+dict_id).removeValue();
        mDatabase.child(DBHelper.TB_USERS+"/" + userId + "/"+DBHelper.TB_WORDS+"/"+dict_id).removeValue();
    }

    private void removeWordFromFB(String dict_id,String word_id) {
        mDatabase.child(DBHelper.TB_WORDS+"/"+dict_id+"/"+word_id).removeValue();
    }

    private void removeUserWordFromFB(String userId,String dict_id,String word_id) {
        mDatabase.child(DBHelper.TB_USERS+"/" + userId + "/"+DBHelper.TB_WORDS+"/"+dict_id+"/"+word_id).removeValue();
    }


//------------------------------------------------------database SQL---------------------------------------------------------------

    private String addDictionaryToDB(SQLiteDatabase db, String title,Boolean isMine,String userName,@Nullable String local_id) {


        SharedPreferences preferences = getSharedPreferences("preference", Activity.MODE_PRIVATE);
        Boolean isPrivacy = preferences.getBoolean(local_id + DBHelper.CN_PUBLIC, getResources().getBoolean(R.bool.privacy));

        ContentValues cv = new ContentValues();
        cv.put(DBHelper.CN_ID, local_id);
        cv.put(DBHelper.CN_TITLE, title.trim());
        cv.put(DBHelper.CN_EDITABLE, isMine?1:0);
        cv.put(DBHelper.CN_USER_NAME, userName);
        //cv.put(DBHelper.CN_PUBLIC, isPrivacy?1:0);

        if (local_id != null && db.update(DBHelper.TB_DICTS,cv,"_id = ?",new String[]{local_id}) > 0) {
            return local_id;
        }

        if (local_id == null) {
            local_id = DBHelper.UUID();
            cv.put(DBHelper.CN_ID,local_id);
        }

        db.insert(DBHelper.TB_DICTS, null, cv);

        return  local_id;
    }

    private String addWordToDB(SQLiteDatabase db,String word,String translate,String dict_id,int lock,String date,@Nullable String word_id)    {


        ContentValues cv = new ContentValues();
        cv.put(DBHelper.CN_ID, word_id);
        cv.put(DBHelper.CN_WORD, word.trim());
        cv.put(DBHelper.CN_TRANSLATE, translate.trim());
        cv.put(DBHelper.CN_ID_DICT, dict_id);
        cv.put(DBHelper.CN_DATE, date);
        cv.put(DBHelper.CN_LOCK, lock);

        if (word_id != null && db.update(DBHelper.TB_WORDS, cv, "dict_id = ? and _id = ?", new String[]{dict_id, word_id}) > 0) {
            return word_id;
        }
        if (word_id == null) {
            word_id = DBHelper.UUID();
            cv.put(DBHelper.CN_ID,word_id);
        }

        db.insert(DBHelper.TB_WORDS, null, cv);

        return word_id;
    }

    private Boolean removeDictionaryFromDB(SQLiteDatabase db,String dict_id) {


        Boolean deleteWords = db.delete(DBHelper.TB_WORDS,"dict_id = ?",new String[]{dict_id}) > 0;
        Boolean deleteDict = db.delete(DBHelper.TB_DICTS, "_id = ?", new String[]{dict_id})> 0;

        return deleteDict || deleteWords;

    }

    private String getNextDictId() {
        String next_dict_id = null;
        SQLiteDatabase db = mSqLiteOpenHelper.getReadableDatabase();
        Cursor cursor = db.query(DBHelper.TB_DICTS, new String[]{DBHelper.CN_ID}, null, null, null, null, DBHelper.CN_ID);
        if (cursor.moveToNext()) {
            next_dict_id = cursor.getString(0);
            cursor.close();
        }
        return next_dict_id;
    }

    private Boolean removeAllWordsFromDB(SQLiteDatabase db, String dict_id) {

        if (!isMineDictionary(dict_id)) {
            showErrorMessage(getString(R.string.error_dictionary_is_not_yours));
            return false;
        }
        db.delete(DBHelper.TB_WORDS,"dict_id = ?",new String[]{dict_id});
        return true;
    }

    private Boolean removeWordFromDB(SQLiteDatabase db,String dict_id,String word_id) {


        if (!isMineDictionary(dict_id)) {
            showErrorMessage(getString(R.string.error_dictionary_is_not_yours));
            return false;
        }
        return db.delete(DBHelper.TB_WORDS,"dict_id = ? and _id = ?",new String[]{dict_id,word_id}) > 0;
    }

    private Boolean isMineDictionary(String dict_id) {

        SQLiteDatabase db = mSqLiteOpenHelper.getReadableDatabase();
        Cursor cursor = db.query(DBHelper.TB_DICTS,null,"_id = ?", new String[]{dict_id},null,null,null);
        if (cursor.getCount() == 0) {
            cursor.close();
            return true;
        }
        if (cursor.moveToNext()) {
            Boolean isMine = cursor.getInt(cursor.getColumnIndex(DBHelper.CN_EDITABLE)) == 1;
            cursor.close();
            return isMine;
        }
        return false;
    }

    private String getCurrentDictId(int menuNumber) {

        SQLiteDatabase db = mSqLiteOpenHelper.getReadableDatabase();
        Cursor cursor = db.query(DBHelper.TB_DICTS,new String[]{DBHelper.CN_ID},null, null,null,null,DBHelper.CN_ID);
        if (cursor.getCount() == 0) {
            cursor.close();
            return null;
        }
        int count = 0;
        while (cursor.moveToNext()) {

            if (count == menuNumber) {
                String id = cursor.getString(0);
                cursor.close();
                return id;
            }
            count++;
        }
        return null;
    }

    void showDefaultListDictionaryDialog(ArrayList<DataSnapshot> data) {


        List<DefaultListDictionaryData> dataList = new ArrayList<>();

        dataList.add(new DefaultListDictionaryData("0",getString(R.string.add_empty_dictionary_title),""));
        for (DataSnapshot userDictSnapshot: data) {
            HashMap value = (HashMap) userDictSnapshot.getValue();
            String title = (String)value.get("title");
            String user_id = (String)value.get("user_id");


            dataList.add(new DefaultListDictionaryData(userDictSnapshot.getKey(),title,user_id));
        }


        final DefaultListDictionaryAdapter adapter = new DefaultListDictionaryAdapter(this,R.layout.default_dictionary_item, dataList);
        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(getResources().getString(R.string.action_add_dict)).setSingleChoiceItems(
            adapter, 0, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    DefaultListDictionaryData data = adapter.getItem(which);

                    switch (data.id) {
                        case "0" :
                            dialog.dismiss();
                            addDictionary(getString(R.string.default_dict_name));
                            break;
                        default:
                            dialog.dismiss();
                            downloadUserDictionaries(data.user_id, data.id);

                            break;

                    }


                }
            });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                createEmptyDefaultDictionary();
            }
        });
        builder.create().show();
    }

    public class DefaultListDictionaryAdapter extends ArrayAdapter<DefaultListDictionaryData> {

        public DefaultListDictionaryAdapter(@NonNull Context context, int resource, @NonNull List<DefaultListDictionaryData> objects) {
            super(context, resource, objects);
        }


        @Override
        public @NonNull View getView(int position, @Nullable View convertView,
                                     @NonNull ViewGroup parent) {

            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.default_dictionary_item,null);
            }
            ((TextView) convertView.findViewById(R.id.title)).setText(getItem(position).title);
            return convertView;
        }
    }

    public class DefaultListDictionaryData {

        public String id,title,user_id;

        public DefaultListDictionaryData(String id,String title,String user_id){
            this.id = id;
            this.title = title;
            this.user_id = user_id;
        }

    }

    void createEmptyDefaultDictionary() {
        SQLiteDatabase db = mSqLiteOpenHelper.getWritableDatabase();
        if (db == null) {
            return;
        }
        String dict_id = addDictionaryToDB(db,getString(R.string.default_dict_name),true,"",null);
        if (!dict_id.equalsIgnoreCase("")) {
            db.close();
        }

        switchVocabulary(dict_id, new Callable() {
            @Override
            public void call(Object value) {
                createNavigationMenu();
            }
            @Override
            public Object call() throws Exception {
                return null;
            }
        });


    }

    //-----------------------------------------------------------async------------------------------------------------------------------

    public class ParseDictionaryTask extends AsyncTask<Void, Integer, Boolean> {

        protected String errorMessage = "";
        protected String dictId = null;
        protected JsonElement obj = null;

        ParseDictionaryTask(String id, JsonElement jsonElement) {
            obj = jsonElement;
            dictId = id;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            JsonArray dict = null;

            if (obj == null) return false;

            if (obj.isJsonArray()) {
                dict = (JsonArray) obj;
            } else {
                return false;
            }

            SQLiteDatabase db = mSqLiteOpenHelper.getWritableDatabase();
            if (removeAllWordsFromDB(db,dictId)) {
                db.close();
            } else {
                return false;
            }

            int maxValue = dict.size();
            db = mSqLiteOpenHelper.getWritableDatabase();
            String date = getDateTime();
            for (float i = 0; i < maxValue;i++) {

                JsonArray wordArr = (JsonArray) dict.get((int)i);

                String word ,translate;
                try {
                    word = wordArr.get(0).getAsString();
                } catch (IndexOutOfBoundsException e) {
                    errorMessage = e.getMessage();
                    return false;
                }

                try {
                    translate = wordArr.get(1).getAsString();
                } catch (IndexOutOfBoundsException e) {
                    errorMessage = e.getMessage();
                    return false;
                }

                if (!word.equalsIgnoreCase("") && !translate.equalsIgnoreCase("")) {

                    if (word.contains("/") && translate.contains("/")) {
                        errorMessage = "Ambiguous meaning of words : "+word + " , "+translate;
                        return false;
                    } else
                    if (word.contains("/")) {

                        String[] words = word.split("/");

                        for (String subWord:words) {
                            addWordToDB(db,subWord,translate,dictId,0,date,null);
                        }

                    } else
                    if (translate.contains("/")) {

                        String[] words = translate.split("/");

                        for (String subWord:words) {
                            addWordToDB(db,word,subWord,dictId,0,date,null);
                        }

                    } else {
                        addWordToDB(db,word,translate,dictId,0,date,null);
                    }
                    publishProgress(Math.round(i/maxValue*100));
                }
            }

            db.close();
            return true;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            setProgressPercent(progress[0]);
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            showProgress(false);

            onCancelled();

            if (success) {

                Toast.makeText(mContext,getString(R.string.dictionary_downloaded),Toast.LENGTH_LONG).show();
                createNavigationMenu();
                createViewPager();

                switchVocabulary(dictId, new Callable() {
                    @Override
                    public void call(Object value) {
                        uploadUserDictionaries(dictId);
                    }

                    @Override
                    public Object call() throws Exception {return null;}
                });

            }
        }

    }

    public class SaveDictionaryTask extends AsyncTask<Void, Integer, Boolean> {

        private String errorMessage = "",mFilePath = "",mDictName = "";
        private String dictId = null;

        SaveDictionaryTask(String filePath, String addId ) {
            mFilePath = filePath;
            dictId = addId;
        }

        @Override
        protected Boolean doInBackground(Void... params) {


            JSONArray list = new JSONArray();
            SQLiteDatabase db = mSqLiteOpenHelper.getReadableDatabase();
            Cursor cursor = db.query(DBHelper.TB_WORDS,new String[]{"word","translate"},"dict_id = ?",new String[]{dictId},null,null,null);
            while (cursor.moveToNext()) {
                JSONArray obj = new JSONArray();
                obj.put(cursor.getString(cursor.getColumnIndex(DBHelper.CN_WORD)));
                obj.put(cursor.getString(cursor.getColumnIndex(DBHelper.CN_TRANSLATE)));
                list.put(obj);
                setProgressPercent(cursor.getPosition()/cursor.getCount()* 100);
            }

            Writer output;
            File file = new File(mFilePath);

            try {
                output = new BufferedWriter(new FileWriter(file));
                output.write(list.toString());
                output.close();

            } catch (IOException e) {
                errorMessage = e.getMessage();
                return false;
            }
            return true;
        }



        @Override
        protected void onProgressUpdate(Integer... progress) {
            setProgressPercent(progress[0]);
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            showProgress(false);

            onCancelled();

            if (success) {

                Toast.makeText(mContext,getString(R.string.dictionary_uploaded),Toast.LENGTH_LONG).show();


            } else {

                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setMessage(errorMessage).setTitle(R.string.error_title);
                builder.create().show();

            }
        }

        @Override
        protected void onCancelled() {

        }


    }

    public class LoadWordsTask implements Runnable {

        WordStatListFragment mFragment;
        Direction mDirection;
        Boolean mRedraw;

        public LoadWordsTask(Direction direction,WordStatListFragment cursorCallback,Boolean redraw) {
            mDirection = direction;
            mFragment = cursorCallback;
            mRedraw = redraw;

        }

        public LoadWordsTask(Direction direction,WordStatListFragment cursorCallback) {
            this(direction,cursorCallback,false);
        }

        @Override
        public void run() {

            //SQLiteOpenHelper mSqLiteOpenHelper = DBHelper.getOpenHelper(getApplication());
            SQLiteDatabase db = mSqLiteOpenHelper.getReadableDatabase();
            if (db == null) {
                return;
            }
            //String[] args = {mDirection.toString(),Integer.toString(currentDictId)};
            //Cursor cursor = db.rawQuery(DBHelper.getWordsQuery("w.direction = ? and w.dict_id  = ?"),args);
            Cursor cursor = db.rawQuery(DBHelper.getWordsQuery(currentDictId,mDirection.toString(),null),null);
            if (mFragment != null && cursor.getCount() > 0) {
                mFragment.updateCursor(cursor,mRedraw);
                db.close();
            } else {
                mFragment.updateCursor(null,mRedraw);
                cursor.close();
            }
        }
    }

    public class CreateDefaultTask implements Runnable {

        WordStatListFragment context;
        ArrayList<DataSnapshot> data;

        public CreateDefaultTask(WordStatListFragment direction) {
            context = direction;
            data = new ArrayList<>();
        }

            /*SQLiteDatabase db = mSqLiteOpenHelper.getWritableDatabase();
            if (db == null) {
                return;
            }
            String dict_id = addDictionaryToDB(db,getString(R.string.default_dict_name),true,"",null);
            if (!dict_id.equalsIgnoreCase("")) {
                db.close();
                showProgress(true);

                InputStream inputStream = getResources().openRawResource(R.raw.default_dict);
                JsonParser  mParser = new JsonParser();
                try {
                    JsonElement obj = mParser.parse(new InputStreamReader(inputStream, "UTF-8"));

                    new ParseDictionaryTask(dict_id,obj).execute();

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }*/

        @Override
        public void run() {
            mDatabase.child("users").child("KkQaNQpCglOqFptVS63SK5eztdm2").child("dictionaries").orderByChild("public").equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    if (dataSnapshot.getValue() != null) {

                        final int total = (int) dataSnapshot.getChildrenCount();

                        for (DataSnapshot userDictSnapshot: dataSnapshot.getChildren()) {
                            mDatabase.child("dictionaries").child(userDictSnapshot.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {

                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {

                                    data.add(dataSnapshot);
                                    if (data.size() == total) {
                                        showDefaultListDictionaryDialog(data);
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }
                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    showDefaultListDictionaryDialog(data);
                }
            });

        }
    }

    //-----------------------------------------------------------services------------------------------------------------------------------
    private void showProgress(final boolean show) {

        mProgressbar.setVisibility(show ? View.VISIBLE : View.GONE);
        setProgressPercent(0);

    }

    public static String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    public void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    public void setProgressPercent(int progress) {
        mProgressbar.setProgress(progress);
    }

    private void showErrorMessage(String errorMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(errorMessage).setTitle(R.string.error_title);
        builder.create().show();
    }

    public void showYesNoMessageDialog(String title, String message,final Callable positiveCallback,final Callable negativeCallback) {

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(message).setTitle(title);
        builder.setCancelable(true);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    dialog.dismiss();
                    if (positiveCallback != null)
                    positiveCallback.call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    dialog.dismiss();
                    if (negativeCallback != null)
                    negativeCallback.call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                isYesNoMessageDialogShow = false;
            }
        });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                isYesNoMessageDialogShow = false;
            }
        });
        builder.create().show();
        isYesNoMessageDialogShow = true;
    }

    private boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }
    //---------------------------------------------------------fragments-------------------------------------------------------------------

    public class SectionsPagerAdapter extends FragmentPagerAdapter{

        int mCount = 2;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {


            WordStatListFragment wlf;
            Bundle bundle = new Bundle();
            switch (position) {
                case 0:
                    wlf = new WordStatListFragment();
                    bundle.putSerializable(DBHelper.CN_DIRECTION,Direction.WORD_TO_TRANSLATE);
                    wlf.setArguments(bundle);
                    wlf.TAG = Direction.WORD_TO_TRANSLATE.toString();
                    return wlf;
                case 1:
                    wlf = new WordStatListFragment();
                    bundle.putSerializable(DBHelper.CN_DIRECTION,Direction.TRANSLATE_TO_WORD);
                    wlf.setArguments(bundle);
                    wlf.TAG = Direction.TRANSLATE_TO_WORD.toString();
                    return wlf;

            }
            return null;
        }

        @Override
        public int getCount() {
            return mCount;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.direction_word_to_translate);
                case 1:
                    return getString(R.string.direction_translate_to_word);
            }
            return null;
        }
    }

    public static  class WordStatListFragment extends ListFragment implements AdapterView.OnItemClickListener, AbsListView.MultiChoiceModeListener,AbsListView.OnScrollListener{

        public Direction mDirection;
        //public SearchWordAdapter mAdapter;
        public String TAG;
        private MainActivity mCallback;
        private FirebaseAuth mAuth;
        private WordStatListFragment mContext;


        public class SearchWordAdapter extends CursorAdapter {

            LayoutInflater lInflater;
            SQLiteDatabase db;
            FilterQueryProvider fp;
            Context mContext;

            SearchWordAdapter(Context context, Cursor cursor) {
                super(context,cursor,FLAG_REGISTER_CONTENT_OBSERVER);
                mContext = context;
                lInflater = LayoutInflater.from(context);
                fp = new FilterQueryProvider() {
                    @Override
                    public Cursor runQuery(CharSequence constraint) {

                        SQLiteOpenHelper mSqLiteOpenHelper = DBHelper.getOpenHelper(mContext);
                        db = mSqLiteOpenHelper.getReadableDatabase();

                        if (db == null) {
                            return null;
                        }
                        String[] args = {"%"+constraint.toString()+"%"};
                        if (mDirection == Direction.WORD_TO_TRANSLATE) {
                            return db.rawQuery(DBHelper.getWordsQuery(currentDictId,mDirection.toString(),"w.word like ?"),args);
                        } else {
                            return db.rawQuery(DBHelper.getWordsQuery(currentDictId,mDirection.toString(),"w.translate like ?"),args);
                        }

                    }
                };
                setFilterQueryProvider(fp);
            }
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                LayoutInflater inflater = LayoutInflater.from(context);
                View view = null;

                if (cursor.getInt(cursor.getColumnIndex(DBHelper.CN_LOCK)) == 1) {
                    view = inflater.inflate(R.layout.wordstat_item_lock, parent, false);
                } else {
                    view = inflater.inflate(R.layout.wordstat_item, parent, false);
                }
                return view;
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {

                String word,translate;
                if (!cursor.isAfterLast()) {


                    if (mDirection == Direction.WORD_TO_TRANSLATE) {
                        word = cursor.getString(cursor.getColumnIndex(DBHelper.CN_WORD));
                        translate = cursor.getString(cursor.getColumnIndex(DBHelper.CN_TRANSLATE));
                    } else {
                        word = cursor.getString(cursor.getColumnIndex(DBHelper.CN_TRANSLATE));
                        translate = cursor.getString(cursor.getColumnIndex(DBHelper.CN_WORD));
                    }
                    int right = cursor.getInt(cursor.getColumnIndex(DBHelper.CN_RIGHT));
                    int wrong = cursor.getInt(cursor.getColumnIndex(DBHelper.CN_WRONG));
                    float completely = cursor.getFloat(cursor.getColumnIndex(DBHelper.CN_COMPLETELY));
                    ((TextView) view.findViewById(R.id.word)).setText(word);
                    ((TextView) view.findViewById(R.id.translate)).setText(translate);
                    ((TextView) view.findViewById(R.id.right)).setText(String.format("%s",right));
                    ((TextView) view.findViewById(R.id.wrong)).setText(Integer.toString(wrong));
                    ((TextView) view.findViewById(R.id.completely)).setText(Float.toString(completely)+"%");
                }
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
                mCallback = (MainActivity) context;
            } catch (ClassCastException e) {
                throw new ClassCastException(context.toString());
            }
        }

        @Override
        public void onDetach() {
            super.onDetach();
            mCallback = null;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                mDirection = (Direction) savedInstanceState.getSerializable(DBHelper.CN_DIRECTION);
            }
            return inflater.inflate(R.layout.fragment_wordstat_list, container,false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            mContext = this;

            mAuth = FirebaseAuth.getInstance();

            if (savedInstanceState != null) {
                mDirection = (Direction) savedInstanceState.getSerializable(DBHelper.CN_DIRECTION);
            } else {
                Bundle bundle = getArguments();
                mDirection = (Direction) bundle.getSerializable(DBHelper.CN_DIRECTION);
            }

            if (mCallback != null) {
                if (mCallback.getLoadDefaultTask() == null && !mCallback.isYesNoMessageDialogShow() && currentDictId == null) {

                    mCallback.showYesNoMessageDialog(getString(R.string.error_dictionary_not_found_title)
                            , getString(R.string.error_dictionary_not_found_message)
                            , new Callable() {
                                @Override
                                public void call(Object value) {

                                }

                                @Override
                                public Object call() throws Exception {
                                    if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                                        mCallback.gotoSignInActivity();
                                    } else {
                                        mCallback.sync();
                                    }
                                    return null;
                                }
                            }, new Callable() {
                                @Override
                                public void call(Object value) {

                                }

                                @Override
                                public Object call() throws Exception {

                                    mCallback.showYesNoMessageDialog(getString(R.string.load_default_dictionary_title),
                                            getString(R.string.load_default_dictionary_message),
                                            new Callable() {
                                                @Override
                                                public void call(Object value) {
                                                }

                                                @Override
                                                public Object call() throws Exception {
                                                    loadDefaultVocabularyTask();
                                                    return null;
                                                }
                                            }, new Callable() {
                                                @Override
                                                public void call(Object value) {

                                                }

                                                @Override
                                                public Object call() throws Exception {
                                                    mCallback.createEmptyDefaultDictionary();
                                                    return null;
                                                }
                                            });


                                    return null;
                                }
                            });

                } else {
                    if (!mCallback.isYesNoMessageDialogShow())
                    mCallback.refreshVocabulary(this);//запускаем асинхронный таск в активити
                }
            }

            getListView().setOnScrollListener(this);


        }



        public void loadDefaultVocabularyTask() {
            mCallback.createDefaultTask(this);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putSerializable(DBHelper.CN_DIRECTION,mDirection);
            super.onSaveInstanceState(outState);
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            final int lastItem = firstVisibleItem + visibleItemCount;
            if(lastItem == totalItemCount && totalItemCount != visibleItemCount){
                ((MainActivity ) getActivity()).hideFabButtons();
            } else {
                ((MainActivity ) getActivity()).showFabButtons();
            }

        }

         public void updateCursor(Cursor cursor,Boolean redraw) {

            if (getListView().getAdapter() == null || redraw)
            {
                setListAdapter(new SearchWordAdapter(getActivity(), cursor));
                getListView().setOnItemClickListener(this);
                getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
                getListView().setMultiChoiceModeListener(this);

            } else {
                ((CursorAdapter)getListView().getAdapter()).swapCursor(cursor);
                ((CursorAdapter)getListView().getAdapter()).notifyDataSetChanged();
            }


        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            int checkedCount = getListView().getCheckedItemCount();
            mode.setTitle(Integer.toString(checkedCount));// +" "+getString(R.string.word_list_action_mode_title));
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_main_action_mode, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_remove_words:
                    deleteSelectedItems();
                    mode.finish();
                  return true;
                case R.id.action_clear_wordstats:
                    clearSelectedItems();
                    mode.finish();
                    return true;
                case R.id.action_lock_words:
                    lockSelectedItems(true);
                    mode.finish();
                    return true;
                case R.id.action_unlock_words:
                    lockSelectedItems(false);
                    mode.finish();
                    return true;
                default:
                return false;
            }
        }

        private void clearSelectedItems() {

            if (!mCallback.isMineDictionary(currentDictId)) {
                Toast.makeText(mCallback, getString(R.string.error_dictionary_is_not_yours), Toast.LENGTH_LONG).show();
            } else {
                mCallback.showProgressDialog();
                SQLiteOpenHelper mSqLiteOpenHelper = DBHelper.getOpenHelper(getActivity());
                SQLiteDatabase db = mSqLiteOpenHelper.getWritableDatabase();
                if (db == null) {
                    return;
                }
                SparseBooleanArray sparseBooleanArray = getListView().getCheckedItemPositions();
                int checked = 0;
                for (int i = 0; i < getListView().getCount(); i++) {
                    if (sparseBooleanArray.get(i)) {
                        Cursor cursor = (Cursor) getListView().getItemAtPosition(i);
                        cursor.moveToPosition(i);
                        db.delete(DBHelper.TB_STATS, "word_id = ? and dict_id = ?", new String[]{cursor.getString(cursor.getColumnIndex(DBHelper.CN_ID)), currentDictId});
                        checked++;
                    }
                }
                db.close();

                Toast.makeText(getActivity(), getResources().getString(R.string.word_list_action_clear_items_complete) + checked, Toast.LENGTH_SHORT).show();

                mCallback.hideProgressDialog();
                if (checked > 0) {
                    mCallback.refreshVocabulary();

                }
            }
        }

        private void lockSelectedItems(Boolean lock) {

            if (!mCallback.isMineDictionary(currentDictId)) {
                Toast.makeText(mCallback, getString(R.string.error_dictionary_is_not_yours), Toast.LENGTH_LONG).show();
            } else {
                mCallback.showProgressDialog();
                SQLiteOpenHelper mSqLiteOpenHelper = DBHelper.getOpenHelper(getActivity());
                SQLiteDatabase db = mSqLiteOpenHelper.getWritableDatabase();
                if (db == null) {
                    return;
                }
                SparseBooleanArray sparseBooleanArray = getListView().getCheckedItemPositions();
                int checked = 0;
                FirebaseUser user = mAuth.getCurrentUser();
                for (int i = 0; i < getListView().getCount(); i++) {
                    if (sparseBooleanArray.get(i)) {
                        Cursor cursor = (Cursor) getListView().getItemAtPosition(i);
                        cursor.moveToPosition(i);
                        ContentValues cv = new ContentValues();
                        cv.put(DBHelper.CN_LOCK, lock ? 1 : 0);
                        String word_id = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ID));
                        String date = cursor.getString(cursor.getColumnIndex(DBHelper.CN_DATE));
                        Boolean updated = db.update(DBHelper.TB_WORDS, cv, "_id = ? and dict_id = ?", new String[]{word_id, currentDictId}) > 0;
                        checked++;
                        if (user != null && updated) {
                            mCallback.addUserWordToFB(user.getUid(), currentDictId, word_id, lock ? 1 : 0, date);
                        }
                    }
                }
                db.close();

                Toast.makeText(getActivity(), getResources().getString(lock ? R.string.word_list_action_locked_items_complete : R.string.word_list_action_unlocked_items_complete) + checked, Toast.LENGTH_SHORT).show();

                mCallback.hideProgressDialog();
                if (checked > 0) {
                    mCallback.refreshVocabulary(true);

                }
            }
        }

        private void deleteSelectedItems() {



            if (!mCallback.isMineDictionary(currentDictId)) {
                Toast.makeText(mCallback, getString(R.string.error_dictionary_is_not_yours), Toast.LENGTH_LONG).show();
            } else {

                mCallback.showProgressDialog();
                SQLiteOpenHelper mSqLiteOpenHelper = DBHelper.getOpenHelper(getActivity());
                SQLiteDatabase db = mSqLiteOpenHelper.getWritableDatabase();
                if (db == null) {
                    return;
                }
                SparseBooleanArray sparseBooleanArray = getListView().getCheckedItemPositions();
                int checked = 0;
                FirebaseUser user = mAuth.getCurrentUser();
                for(int i = 0; i < getListView().getCount();i++) {
                    if (sparseBooleanArray.get(i)) {
                        Cursor cursor = (Cursor) getListView().getItemAtPosition(i);

                        cursor.moveToPosition(i);
                        String word_id = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ID));
                        Boolean removed = mCallback.removeWordFromDB(db, currentDictId, word_id);

                        if (user != null && removed) {
                            mCallback.removeWordFromFB(currentDictId, word_id);
                            mCallback.removeUserWordFromFB(user.getUid(), currentDictId, word_id);
                        }
                        checked++;
                    }
                }
                db.close();

                Toast.makeText(getActivity(),getResources().getString(R.string.word_list_action_delete_items_complete)+checked,Toast.LENGTH_SHORT).show();

                mCallback.hideProgressDialog();
                if (checked >0 ) {
                    mCallback.refreshVocabulary();

                }
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {


            AlertDialog.Builder createProjectAlert = new AlertDialog.Builder(getActivity());
            createProjectAlert.setTitle(getString(R.string.edit_word_title));
            View dialogView = getActivity().getLayoutInflater().inflate(R.layout.edit_word_dialog, null);
            final EditText wordText = (EditText) dialogView.findViewById(R.id.edit_word);
            final EditText translationText = (EditText) dialogView.findViewById(R.id.edit_tranclation);
            final Cursor cursor = ((CursorAdapter) getListAdapter()).getCursor();
            cursor.moveToPosition(position);
            final String word = cursor.getString(cursor.getColumnIndex(DBHelper.CN_WORD));
            final String translate = cursor.getString(cursor.getColumnIndex(DBHelper.CN_TRANSLATE));
            wordText.setText(word);
            translationText.setText(translate);
            createProjectAlert.setView(dialogView)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            String dict_id = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ID_DICT));
                            String word_id = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ID));
                            int lock = cursor.getInt(cursor.getColumnIndex(DBHelper.CN_LOCK));
                            String date = cursor.getString(cursor.getColumnIndex(DBHelper.CN_DATE));
                            String word = wordText.getText().toString().trim();
                            String translate = translationText.getText().toString();


                            if (!mCallback.isMineDictionary(dict_id)) {
                                Toast.makeText(mCallback, getString(R.string.error_dictionary_is_not_yours), Toast.LENGTH_LONG).show();
                            } else {

                                SQLiteOpenHelper mSqLiteOpenHelper = DBHelper.getOpenHelper(mCallback);
                                SQLiteDatabase db = mSqLiteOpenHelper.getWritableDatabase();

                                if (mCallback.addWordToDB(db, word, translate, dict_id, lock, date, word_id) != null) {

                                    if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                                        mCallback.addWordToFB(mAuth.getUid(), dict_id, word_id, word, translate);
                                        mCallback.addUserWordToFB(mAuth.getUid(), dict_id, word_id, lock, date);
                                    }
                                    mCallback.refreshVocabulary();
                                    Toast.makeText(mCallback, getString(R.string.word_updated_toast), Toast.LENGTH_LONG).show();
                                }
                                db.close();
                            }
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
            createProjectAlert.show();

        }

    }


}




