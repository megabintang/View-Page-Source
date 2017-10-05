package com.viewsource.bintang.weburl;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, LoaderManager.LoaderCallbacks<String>, View.OnClickListener {
    private TextView TextViewUrl;
    private EditText EditTextUrl;
    private TextView TextLinkWebUrl;

    private ProgressBar Bar;
    private Spinner SpinnerUrl;
    private Button ButtonUrl;

    private FrameLayout.LayoutParams ParamsUrl;
    private static final int HTTP = 0;
    private static final int HTTPS = 1;

    private static boolean mIndicator = false;
    private static final String _URL = "url";
    private static final String TEXT_HTML = "text_html";
    private static final String TEXT_URL = "text_url";


    private static final int ID = 0;
    private int mScheme = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        if (savedInstanceState != null) {
            TextLinkWebUrl.setText(savedInstanceState.getString(TEXT_URL));
            TextViewUrl.setText(savedInstanceState.getString(TEXT_HTML));
        }if (mIndicator) {
            hideShow(true);
            getSupportLoaderManager().initLoader(ID, null, this);
        }else {
            hideShow(false);
        }
    }

    private void initView() {
        Bar = findViewById(R.id.prog);

        EditTextUrl = findViewById(R.id.edit_myurl);
        TextViewUrl = findViewById(R.id.result);
        TextLinkWebUrl = findViewById(R.id.web);

        FrameLayout layout = findViewById(R.id.frame);
        ParamsUrl = (FrameLayout.LayoutParams) layout.getLayoutParams();
        SpinnerUrl = findViewById(R.id.spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.list, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        SpinnerUrl.setAdapter(adapter);
        SpinnerUrl.setOnItemSelectedListener(this);

        ButtonUrl = findViewById(R.id.get);
        ButtonUrl.setOnClickListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(TEXT_URL, TextLinkWebUrl.getText().toString());
        outState.putString(TEXT_HTML, TextViewUrl.getText().toString());
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position == HTTP) {
            mScheme = HTTP;
        } else {
            mScheme = HTTPS;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private void hideShow(boolean valueBar) {
        if (valueBar) {
            Bar.setVisibility(View.VISIBLE);
            TextViewUrl.setVisibility(View.GONE);
            ParamsUrl.gravity = Gravity.CENTER;

        } else {
            Bar.setVisibility(View.GONE);
            TextViewUrl.setVisibility(View.VISIBLE);
            ParamsUrl.gravity = Gravity.START;
        }
    }

    @Override
    public Loader<String> onCreateLoader(int id, Bundle args) {
        mIndicator = true;
        hideShow(true);
        String url = args.getString(_URL);
        return new WebTaskLoader(this, url);
    }

    @Override
    public void onLoadFinished(Loader<String> loader, String data) {
        mIndicator = false;
        hideShow(false);
        if (data != null && !data.isEmpty()) {
            TextViewUrl.setText(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<String> loader) {

    }

    @Override
    public void onClick(View view) {
        String url = EditTextUrl.getText().toString();
        if (mScheme == HTTP) {
            url = "http://" + url;
        } else {
            url = "https://" + url;
        }
        validateProcess(url);

    }

    private boolean checkConnection() { //Cek Koneksi
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void cancelLoadError(String error) {
        Loader loader = getSupportLoaderManager().getLoader(ID);
        if (loader != null) {
            loader.cancelLoad();
        }

        TextViewUrl.setText(error);
        mIndicator = false;

        hideShow(false);
    }

    private void validateProcess(String url) {
        TextLinkWebUrl.setText("URL : " + url);
        boolean valid = Patterns.WEB_URL.matcher(url).matches();//Cek URL jika Valid
        if (!valid) {
            cancelLoadError("Alamat URL Tidak Valid");
        } else { if (checkConnection()) {
                    Bundle bundle = new Bundle();
                    bundle.putString(_URL, url);
                    getSupportLoaderManager().restartLoader(ID, bundle, this);
                 } else { cancelLoadError("Tidak ada Koneksi Internet");
                        }
        }
    }
}

class WebTaskLoader extends AsyncTaskLoader<String> {
    private String myResult;
    private boolean myCancel = false;
    private String myURL;

    public WebTaskLoader(Context context, String url) {
        super(context);
        myURL = url;
    }

    @Override
    public String loadInBackground() {
        URL url;
        String result;
        try {
            url = createURL(myURL);
            result = openReadConnection(url);
        }
        catch (MalformedURLException ex) {
            return "Alamat URL Tidak Valid atau InValid";
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return "UNKNOWN ERROR, Ulangi!";
        }
        return result;
    }
    @Override
    protected void onStartLoading() {
        if (myResult == null && !myCancel) {
            forceLoad();
        }   else {
            deliverResult(myResult);
        }
    }
    @Override
    public void onCanceled(String data) {
        super.onCanceled(data);
        myCancel = true;
    }
    @Override
    public void deliverResult(String data) {
        myResult = data;
        super.deliverResult(data);
    }

    private URL createURL(String url) throws MalformedURLException {
        URL urlWeb = new URL(url);
        return urlWeb;
    }

    private String openReadConnection(URL url) throws IOException {
        InputStream inputStream = null;
        HttpURLConnection connection = null;
        String result = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            connection.setReadTimeout(10000);
            connection.setConnectTimeout(15000);

            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
                result = readByteToString(inputStream);
            }   else {
                return "Error terjadi!, Kode error: " + connection.getResponseCode();
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            connection.disconnect();
        }

        return result;
    }

    private String readByteToString(InputStream inputStream) throws IOException {
        if (inputStream != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder builder = new StringBuilder();
            String line = reader.readLine();
            while (line != null) {
                builder.append(line);
                line = reader.readLine();
            }
            return builder.toString();
        }

        return null;
    }
}


