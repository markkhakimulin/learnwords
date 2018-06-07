package org.mobilburger.learnwords;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.app.ActionBar;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.annotation.Nullable;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import org.mobilburger.database.DBHelper;

import java.io.Serializable;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity {

    private static String currentDictId;
    private SharedPreferences prefs;
    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }



    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentDictId = getIntent().getStringExtra(DBHelper.CN_ID_DICT);
        prefs =  getSharedPreferences("preference", Activity.MODE_PRIVATE);

        getFragmentManager().beginTransaction().replace(android.R.id.content,new GeneralPreferenceFragment() ).commit();
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }


    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {

        private SharedPreferences prefs;
        private FirebaseAuth mAuth;
        private SettingsActivity mContext;

        public GeneralPreferenceFragment() {

        }

               @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_empty);
            setHasOptionsMenu(true);
            mAuth = FirebaseAuth.getInstance();

        }
        private Preference.OnPreferenceClickListener resetListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                SharedPreferences.Editor editor = prefs.edit();
                editor.remove(currentDictId + DBHelper.CN_PUBLIC);
                editor.remove(currentDictId + DBHelper.CN_EMPTY_ANSWER_IS_WRONG);
                editor.remove(currentDictId + DBHelper.CN_WORDS_PER_LESSON);
                editor.remove(currentDictId + DBHelper.CN_WORDS_STUDY);
                editor.remove(currentDictId + DBHelper.CN_WRONG_ANSWERS_TO_SKIP);
                editor.remove(currentDictId + DBHelper.CN_RIGHT_ANSWER_PERCENT);
                editor.remove(currentDictId + DBHelper.CN_WRONG_ANSWER_PERCENT);
                editor.remove(currentDictId + DBHelper.CN_USE_TIPS);
                editor.apply();
                Toast.makeText(getActivity(),getString(R.string.settings_toast_reset),Toast.LENGTH_LONG).show();
                closeActivity();
                return true;
            }

        };
        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            prefs = getActivity().getSharedPreferences("preference", Activity.MODE_PRIVATE);

            PreferenceScreen preferenceScreen = this.getPreferenceScreen();
            PreferenceCategory preferenceCategory = new PreferenceCategory(preferenceScreen.getContext());
            preferenceCategory.setTitle(getString(R.string.pref_general));
            preferenceScreen.addPreference(preferenceCategory);

            SwitchPreference preferencePrivacy = new SwitchPreference(preferenceScreen.getContext());
            preferencePrivacy.setKey(currentDictId + DBHelper.CN_PUBLIC);
            preferencePrivacy.setTitle(getString(R.string.pref_general_public));
            preferencePrivacy.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

            Boolean isPrivacy = prefs.getBoolean(currentDictId+DBHelper.CN_PUBLIC,getResources().getBoolean(R.bool.privacy));
            preferencePrivacy.setChecked(isPrivacy);
            if (isPrivacy) {
                preferencePrivacy.setSummary(getString(R.string.pref_general_public_summary));
            } else {
                preferencePrivacy.setSummary(getString(R.string.pref_general_non_public_summary));
            }
            preferenceCategory.addPreference(preferencePrivacy);

            SwitchPreference preference = new SwitchPreference(preferenceScreen.getContext());
            preference.setKey(currentDictId + DBHelper.CN_EMPTY_ANSWER_IS_WRONG);
            preference.setTitle(getString(R.string.pref_general_empty_is_wrong));
            preference.setSummary(getString(R.string.pref_general_empty_is_wrong_summary));
            preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
            preferenceCategory.addPreference(preference);

            preference.setChecked(prefs.getBoolean(currentDictId+DBHelper.CN_EMPTY_ANSWER_IS_WRONG,getResources().getBoolean(R.bool.empty_answer_is_wrong)));


            EditTextPreference preferenceEditText = new EditTextPreference (preferenceScreen.getContext());
            preferenceEditText.setKey(currentDictId+DBHelper.CN_WORDS_STUDY);
            preferenceEditText.setDefaultValue(Integer.toString(getResources().getInteger(R.integer.words_study)));
            preferenceEditText.setTitle(getString(R.string.pref_general_study_word));
            preferenceEditText.setSummary(prefs.getString(currentDictId+DBHelper.CN_WORDS_STUDY,Integer.toString(getResources().getInteger(R.integer.words_study))));
            preferenceEditText.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
            preferenceCategory.addPreference(preferenceEditText);

            EditTextPreference preferenceEditText1 = new EditTextPreference (preferenceScreen.getContext());
            preferenceEditText1.setKey(currentDictId+DBHelper.CN_WORDS_PER_LESSON);
            preferenceEditText1.setDefaultValue(Integer.toString(getResources().getInteger(R.integer.words_per_lesson)));
            preferenceEditText1.setTitle(getString(R.string.pref_general_words_per_unlock));
            preferenceEditText1.setSummary(prefs.getString(currentDictId+DBHelper.CN_WORDS_PER_LESSON,Integer.toString(getResources().getInteger(R.integer.words_per_lesson))));
            preferenceEditText1.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
            preferenceCategory.addPreference(preferenceEditText1);

            EditTextPreference preferenceEditText2 = new EditTextPreference (preferenceScreen.getContext());
            preferenceEditText2.setKey(currentDictId+DBHelper.CN_WRONG_ANSWERS_TO_SKIP);
            preferenceEditText2.setDefaultValue(Integer.toString(getResources().getInteger(R.integer.wrong_answers_to_skip)));
            preferenceEditText2.setTitle(getString(R.string.pref_general_wrong_answers_to_skip));
            preferenceEditText2.setSummary(prefs.getString(currentDictId+DBHelper.CN_WRONG_ANSWERS_TO_SKIP,Integer.toString(getResources().getInteger(R.integer.wrong_answers_to_skip))));
            preferenceEditText2.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
            preferenceCategory.addPreference(preferenceEditText2);

            ListPreference  preferenceList = new ListPreference  (preferenceScreen.getContext());
            preferenceList.setKey(currentDictId+DBHelper.CN_RIGHT_ANSWER_PERCENT);
            preferenceList.setDefaultValue(Integer.toString(getResources().getInteger(R.integer.right_answer_percent)));
            preferenceList.setEntries(getResources().getStringArray(R.array.pref_general_right_answer_list_entries));
            preferenceList.setEntryValues(R.array.pref_general_right_answer_list_values);
            preferenceList.setTitle(getString(R.string.pref_general_right_answer));
            preferenceList.setSummary(prefs.getString(currentDictId+DBHelper.CN_RIGHT_ANSWER_PERCENT,Integer.toString(getResources().getInteger(R.integer.right_answer_percent))));
            preferenceList.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
            preferenceCategory.addPreference(preferenceList);

            ListPreference  preferenceList1 = new ListPreference  (preferenceScreen.getContext());
            preferenceList1.setKey(currentDictId+DBHelper.CN_WRONG_ANSWER_PERCENT);
            preferenceList1.setDefaultValue(Integer.toString(getResources().getInteger(R.integer.wrong_answer_percent)));
            preferenceList1.setEntries(getResources().getStringArray(R.array.pref_general_wrong_answer_list_entries));
            preferenceList1.setEntryValues(R.array.pref_general_wrong_answer_list_values);
            preferenceList1.setTitle(getString(R.string.pref_general_wrong_answer));
            preferenceList1.setSummary(prefs.getString(currentDictId+DBHelper.CN_WRONG_ANSWER_PERCENT,Integer.toString(getResources().getInteger(R.integer.wrong_answer_percent))));
            preferenceList1.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
            preferenceCategory.addPreference(preferenceList1);

            SwitchPreference preference1 = new SwitchPreference(preferenceScreen.getContext());
            preference1.setKey(currentDictId + DBHelper.CN_USE_TIPS);
            preference1.setTitle(getString(R.string.pref_general_use_tips));
            preference1.setSummary(getString(R.string.pref_general_use_tips_summary));
            preference1.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
            preferenceCategory.addPreference(preference1);

            preference1.setChecked(prefs.getBoolean(currentDictId+DBHelper.CN_USE_TIPS,getResources().getBoolean(R.bool.use_tips)));


            Preference preferenceReset = new Preference (preferenceScreen.getContext());
            preferenceReset.setTitle(getString(R.string.pref_general_reset_settings));
            preferenceReset.setSummary(getString(R.string.pref_general_reset_settings_summary));
            preferenceReset.setOnPreferenceClickListener(resetListener);
            preferenceCategory.addPreference(preferenceReset);
        }

        void closeActivity() {
            getActivity().getFragmentManager().beginTransaction().remove(this).commit();
            getActivity().finish();
        }

        private Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                String stringValue = value.toString();

                SharedPreferences.Editor editor = prefs.edit();
                if (preference instanceof SwitchPreference) {


                    if (preference.getKey().equals(currentDictId + DBHelper.CN_EMPTY_ANSWER_IS_WRONG)) {

                        preference.setSummary(getString(R.string.pref_general_empty_is_wrong_summary));
                        editor.putBoolean(currentDictId + DBHelper.CN_EMPTY_ANSWER_IS_WRONG, (Boolean) value);
                    }

                    if (preference.getKey().equals(currentDictId + DBHelper.CN_USE_TIPS)) {

                        preference.setSummary(getString(R.string.pref_general_use_tips_summary));
                        editor.putBoolean(currentDictId + DBHelper.CN_USE_TIPS, (Boolean) value);
                    }
                    if (preference.getKey().equals(currentDictId + DBHelper.CN_PUBLIC)) {

                        editor.putBoolean(currentDictId + DBHelper.CN_PUBLIC, (Boolean) value);
                        if ((Boolean) value) {
                            preference.setSummary(getString(R.string.pref_general_public_summary));
                        } else {
                            preference.setSummary(getString(R.string.pref_general_non_public_summary));
                        }
                        if (mAuth.getCurrentUser() != null)
                        Toast.makeText(getActivity(),getString(R.string.settings_toast_sync),Toast.LENGTH_LONG).show();
                    }

                } else if (preference instanceof EditTextPreference) {


                    if (Integer.valueOf(stringValue) == 0) {
                        preference.setSummary(getString(R.string.not_using));
                    } else {
                        preference.setSummary(stringValue);
                    }

                    if (preference.getKey().equals(currentDictId + DBHelper.CN_WORDS_STUDY)) {
                        editor.putString(currentDictId + DBHelper.CN_WORDS_STUDY, (String) value);
                    }
                    if (preference.getKey().equals(currentDictId + DBHelper.CN_WORDS_PER_LESSON)) {
                        editor.putString(currentDictId + DBHelper.CN_WORDS_PER_LESSON, (String) value);
                    }
                    if (preference.getKey().equals(currentDictId + DBHelper.CN_WRONG_ANSWERS_TO_SKIP)) {
                        editor.putString(currentDictId + DBHelper.CN_WRONG_ANSWERS_TO_SKIP, (String) value);
                    }

                } else if (preference instanceof ListPreference) {

                    preference.setSummary(stringValue);

                    if (preference.getKey().equals(currentDictId + DBHelper.CN_RIGHT_ANSWER_PERCENT)) {
                        editor.putString(currentDictId + DBHelper.CN_RIGHT_ANSWER_PERCENT,(String) value);
                    }
                    if (preference.getKey().equals(currentDictId + DBHelper.CN_WRONG_ANSWER_PERCENT)) {
                        editor.putString(currentDictId + DBHelper.CN_WRONG_ANSWER_PERCENT, (String) value);
                    }
                }

                editor.apply();
                return true;
            }
        };
        @Override
        public void onAttach(Context context) {
            super.onAttach(context);

            try {
                mContext = (SettingsActivity) context;
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
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}

