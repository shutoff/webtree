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
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RadioButton;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.Vector;

public class StartActivity extends Activity {

    static final String KOP = "kop";
    static final String TITLE = "title";

    Vector<KOP> kops;

    int current;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        kops = new Vector<StartActivity.KOP>();

        scanExternalMounts();

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
        final Button okButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        okButton.setEnabled(false);
        current = -1;

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
            public View getView(final int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null)
                    v = inflater.inflate(R.layout.item, null);
                RadioButton button = (RadioButton) v.findViewById(R.id.item);
                button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked && (position != list.getSelectedItemPosition())) {
                            current = position;
                            BaseAdapter adapter = (BaseAdapter) list.getAdapter();
                            adapter.notifyDataSetChanged();
                            okButton.setEnabled(true);
                        }
                    }
                });
                button.setText(kops.get(position).name);
                button.setChecked(position == current);
                return v;
            }
        });
        list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                current = position;
                BaseAdapter adapter = (BaseAdapter) list.getAdapter();
                adapter.notifyDataSetChanged();
                okButton.setEnabled(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        list.setSelection(0);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(StartActivity.this, MainActivity.class);
                i.putExtra("data", kops.get(current).path);
                startActivity(i);
                dialog.dismiss();
            }
        });
    }

    void scanExternalMounts() {
        String reg = "(?i).*vold.*(vfat|ntfs|exfat|fat32|ext3|ext4).*rw.*";
        String s = "";
        try {
            final Process process = new ProcessBuilder().command("mount")
                    .redirectErrorStream(true).start();
            process.waitFor();
            final InputStream is = process.getInputStream();
            final byte[] buffer = new byte[1024];
            while (is.read(buffer) != -1) {
                s = s + new String(buffer);
            }
            is.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }

        // parse output
        final String[] lines = s.split("\n");
        for (String line : lines) {
            if (!line.toLowerCase(Locale.US).contains("asec")) {
                if (line.matches(reg)) {
                    String[] parts = line.split(" ");
                    for (String part : parts) {
                        if (part.startsWith("/"))
                            if (!part.toLowerCase(Locale.US).contains("vold")) {
                                scanPath(new File(part), 0);
                            }
                    }
                }
            }
        }
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
