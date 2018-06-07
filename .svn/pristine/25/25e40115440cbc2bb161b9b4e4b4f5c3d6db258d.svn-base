package org.mobilburger.learnwords;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mobilburger.learnwords.OpenFileDialogActivity.FileArrayAdapter.TYPE_BUTTON;
import static org.mobilburger.learnwords.OpenFileDialogActivity.FileArrayAdapter.TYPE_ROW;

public class OpenFileDialogActivity extends ListActivity {

    public static final int CHOOSE_FILE = 3;
    public static final int SAVE_FILE = 4;
    private static final int PERMISSION_REQUEST = 1;

    private File currentDir;
    private FileArrayAdapter adapter;
    public static String UPLOAD_DICTIONARY = "upload";
    public static String DOWNLOAD_DICTIONARY = "download";
    String action;


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        action = getIntent().getAction();
        if (action.equals(DOWNLOAD_DICTIONARY)) {

            if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST);
            } else {
                fill(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
            }
        }
        if (action.equals(UPLOAD_DICTIONARY)) {
            if (ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST);
            } else {
                fill(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fill(Environment.getExternalStoragePublicDirectory("/storage/"));
            } else {
                Toast.makeText(this, getString(R.string.error_no_permission_allowed), Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void fill(File f)
    {
        File[]dirs = f.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {

                return !name.toLowerCase().startsWith(".");
            }
        });
        setTitle(getString(R.string.directory_actual)+f.getName());
        List<Option>dir = new ArrayList<>();
        List<Option>fls = new ArrayList<>();
        try{
            for(File ff: dirs)
            {
                if(ff.isDirectory())
                    dir.add(new Option(ff.getName(),getString(R.string.folder),ff.getAbsolutePath()));
                else
                {
                    fls.add(new Option(ff.getName(),getString(R.string.file_size)+ff.length(),ff.getAbsolutePath()));
                }
            }
        }catch(Exception e)
        {

        }
        Collections.sort(dir);
        Collections.sort(fls);
        dir.addAll(fls);

        if(!f.getName().equalsIgnoreCase("sdcard"))
            dir.add(0,new Option("...",getString(R.string.parent_directory),f.getParent()));
        if(action.equals(UPLOAD_DICTIONARY))
            dir.add(0,new Option("uploading",getString(R.string.press_to_save),f.getPath()));
        adapter = new FileArrayAdapter(OpenFileDialogActivity.this,R.layout.file_browser,dir,action,f.getPath());
        setListAdapter(adapter);

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        // TODO Auto-generated method stub
        super.onListItemClick(l, v, position, id);
        Option o = adapter.getItem(position);
        if(o.getData().equalsIgnoreCase(getString(R.string.folder)) || o.getData().equalsIgnoreCase(getString(R.string.parent_directory))){

            currentDir = new File(o.getPath());
            fill(currentDir);
        }
        else
        {
            onFileClick(o);
        }
    }

    private void onFileClick(final Option o)
    {

        if (o.getName().equalsIgnoreCase("uploading")) {
            AlertDialog.Builder createProjectAlert = new AlertDialog.Builder(this);
            createProjectAlert.setTitle(getString(R.string.new_dict_title));

            View view = getLayoutInflater().inflate(R.layout.create_dictionary_dialog, null);
            final EditText dict_name = (EditText) view.findViewById(R.id.dictionary_name);

            createProjectAlert.setView(view)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int id) {

                            String dict_name_text = dict_name.getText().toString();

                            if (!dict_name_text.endsWith(".json")) {

                                dict_name_text = dict_name_text.concat(".json");
                            }
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("data", o.getPath()+"/"+dict_name_text);
                            resultIntent.putExtra("choose", true);
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
            createProjectAlert.show();
        } else

        if (o.getName().endsWith(".json")) {

            Intent resultIntent = new Intent();
            resultIntent.putExtra("data", o.getPath());
            resultIntent.putExtra("choose", true);
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }



    public class FileArrayAdapter extends ArrayAdapter<Option> {

        public static final int TYPE_ROW = 0;
        public static final int TYPE_BUTTON = 1;
        private Context c;
        private int id;
        private List<Option> items;

        public FileArrayAdapter(Context context, int textViewResourceId,
                                List<Option> objects, String action, String path) {
            super(context, textViewResourceId, objects);
            c = context;
            id = textViewResourceId;
            items = objects;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).getType();
        }

        public Option getItem(int i) {
            return items.get(i);
        }
        @Override
        public long getItemId(int position) {
            return super.getItemId(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            final Option o = items.get(position);

            int listViewItemType = getItemViewType(position);

            if (v == null) {

                LayoutInflater vi = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                if (listViewItemType == TYPE_BUTTON) {
                    v = vi.inflate(R.layout.save_dict_button_browser, null);
                } else if (listViewItemType == TYPE_ROW) {
                    v = vi.inflate(id, null);
                }
            }
            if (o != null) {
                TextView t1 = (TextView) v.findViewById(R.id.TextView01);
                TextView t2 = (TextView) v.findViewById(R.id.TextView02);

                if (t1 != null)
                    t1.setText(o.getName());
                if (t2 != null)
                    t2.setText(o.getData());

            }
            return v;
        }
    }

    public class Option implements Comparable<Option>{
        protected String name;
        protected String data;
        protected String path;

        public Option(String n,String d,String p)
        {
            name = n;
            data = d;
            path = p;
        }
        public String getName()
        {
            if (name.equals("sdcard0")) return getString(R.string.sdcard0);
            if (name.equals("sdcard1")) return getString(R.string.sdcard1);
            return name;
        }
        public String getData()
        {
            return data;
        }
        public String getPath()
        {
            return path;
        }
        @Override
        public int compareTo(Option o) {
            if(this.name != null)
                return this.name.toLowerCase().compareTo(o.getName().toLowerCase());
            else
                throw new IllegalArgumentException();
        }
        public int getType()
        {
            if (name.equalsIgnoreCase("uploading")) return TYPE_BUTTON;

            return TYPE_ROW;
        }

    }
}