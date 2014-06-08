package ru.shutoff.test.test;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RadioButton;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.util.Vector;

public class StartActivity extends Activity {

    static final String KOP = "kop";
    static final String TITLE = "title";

    Vector<KOP> kops;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        kops = new Vector<StartActivity.KOP>();

        scanPath(Environment.getExternalStorageDirectory(), 0);

        if (kops.size() == 0) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.error)
                    .setMessage(R.string.no_kops)
                    .setNegativeButton(R.string.cancel, null)
                    .create();
            dialog.show();
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                }
            });
            return;
        }

        if (kops.size() == 1) {
            Intent i = new Intent(StartActivity.this, MainActivity.class);
            i.putExtra("data", kops.get(0).path);
            startActivity(i);
            finish();
            return;
        }

        final LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.select_kop)
                .setView(inflater.inflate(R.layout.list, null))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, null)
                .create();
        dialog.show();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
        final ListView list = (ListView) dialog.findViewById(R.id.list);
        list.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return kops.size();
            }

            @Override
            public Object getItem(int position) {
                return kops.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null)
                    v = inflater.inflate(R.layout.item, null);
                RadioButton button = (RadioButton) v.findViewById(R.id.item);
                button.setText(kops.get(position).name);
                button.setChecked(position == list.getSelectedItemPosition());
                return v;
            }
        });
        list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                BaseAdapter adapter = (BaseAdapter) list.getAdapter();
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        list.setSelection(0);
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(StartActivity.this, MainActivity.class);
                i.putExtra("data", kops.get(list.getSelectedItemPosition()).path);
                startActivity(i);
                dialog.dismiss();
            }
        });
    }

    void scanPath(File path, final int level) {
        if (!path.isDirectory())
            return;
        if (path.toString().substring(0, 1).equals("."))
            return;
        File index_xml = new File(path, "index.xml");
        if (index_xml.exists()) {
            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(new FileInputStream(index_xml), "utf-8");
                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        if (xpp.getName().equals(KOP)) {
                            String title = xpp.getAttributeValue("", TITLE);
                            if (!title.equals("")) {
                                kops.add(new KOP(title, path.toString()));
                                break;
                            }
                        }
                    }
                    eventType = xpp.next();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (level > 2)
            return;
        path.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                scanPath(pathname, level + 1);
                return false;
            }
        });
    }

    static class KOP {

        String name;
        String path;

        KOP(String n, String p) {
            name = n;
            path = p;
        }
    }
}
