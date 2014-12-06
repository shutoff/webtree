package ru.shutoff.test.test;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xwalk.core.XWalkNavigationHistory;
import org.xwalk.core.XWalkUIClient;
import org.xwalk.core.XWalkView;

import java.io.File;
import java.io.FileInputStream;
import java.util.Vector;

public class MainActivity extends Activity {

    static final String ITEM = "item";
    static final String TITLE = "title";
    static final String FILE = "file";

    File data_dir;
    String prefix;

    Vector<Index> index;

    XWalkView webView;
    ListView listView;

    boolean force_exit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        data_dir = new File(getIntent().getStringExtra("data"));
        prefix = "file://" + data_dir.toString() + "/";

        setContentView(R.layout.activity_main);
        webView = (XWalkView) findViewById(R.id.content);

        webView.setUIClient(new XWalkUIClient(webView) {
            @Override
            public void onReceivedTitle(XWalkView view, String title) {
                setTitle(title);
            }
        });

        webView.load(prefix + "index.html", null);

        final File index_xml = new File(data_dir, "index.xml");
        index = new Vector<Index>();
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new FileInputStream(index_xml), "utf-8");
            int eventType = xpp.getEventType();
            int level = 0;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (xpp.getName().equals(ITEM)) {
                        String title = xpp.getAttributeValue("", TITLE);
                        if (!title.equals(""))
                            index.add(new Index(xpp.getAttributeValue("", FILE), title, level));
                        level++;
                    }
                }
                if (eventType == XmlPullParser.END_TAG) {
                    if (xpp.getName().equals(ITEM))
                        level--;
                }
                eventType = xpp.next();
            }

            for (int i = 0; i < index.size() - 1; i++) {
                if (index.get(i + 1).level > index.get(i).level)
                    index.get(i).parent = true;
            }

        } catch (Exception ex) {
            Toast toast = Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT);
            toast.show();
        }


        listView = (ListView) findViewById(R.id.list);
        listView.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return getTreeSize();
            }

            @Override
            public Object getItem(int position) {
                return index.get(getTreePos(position));
            }

            @Override
            public long getItemId(int position) {
                return getTreePos(position);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                    v = inflater.inflate(R.layout.list_item, null);
                }
                TextView text = (TextView) v.findViewById(R.id.text);
                Index i = index.get(getTreePos(position));
                text.setText(i.name);
                int padding = text.getPaddingRight();
                text.setPadding(padding * (i.level + 1), text.getPaddingTop(), padding, text.getPaddingBottom());
                return v;
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Index i = index.get(getTreePos(position));
                String file = i.file;
                if (file != null)
                    webView.load(prefix + file, null);
                if (i.parent) {
                    i.open = !i.open;
                    BaseAdapter adapter = (BaseAdapter) listView.getAdapter();
                    adapter.notifyDataSetChanged();
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.exit) {
            force_exit = true;
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // super.onConfigurationChanged(newConfig);
    }

    @Override
    public void finish() {
        XWalkNavigationHistory history = webView.getNavigationHistory();
        if (!force_exit && history.canGoBack()) {
            history.navigate(XWalkNavigationHistory.Direction.BACKWARD, 1);
            return;
        }
        super.finish();
    }

    int getTreePos(int p) {
        int pos = 0;
        int skip_level = 1;
        int i;
        for (i = 0; i < index.size(); i++) {
            Index item = index.get(i);
            if (item.level < skip_level) {
                skip_level = item.level + 1;
                if (item.open)
                    skip_level++;
                if (++pos > p)
                    return i;
            }
        }
        return i;
    }

    int getTreeSize() {
        int pos = 0;
        int skip_level = 1;
        for (int i = 0; i < index.size(); i++) {
            Index item = index.get(i);
            if (item.level < skip_level) {
                skip_level = item.level + 1;
                if (item.open)
                    skip_level++;
                pos++;
            }
        }
        return pos;
    }

    static class Index {
        String file;
        String name;
        int level;
        boolean open;
        boolean parent;

        Index(String f, String n, int l) {
            file = f;
            name = n;
            level = l;
        }
    }
}
