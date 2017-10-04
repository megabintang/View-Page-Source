package com.pagesource.dicky.webhtml;

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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, LoaderManager.LoaderCallbacks<String>, View.OnClickListener {
    private TextView myTextView;
    private EditText myEditText;
    private TextView myTextLinkWeb;
    private ProgressBar myBar;
    private Spinner mySpinner;
    private Button myButton;

    private FrameLayout.LayoutParams mParams;
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
            myTextLinkWeb.setText(savedInstanceState.getString(TEXT_URL));
            myTextView.setText(savedInstanceState.getString(TEXT_HTML));
        }if (mIndicator) {
            hideShow(true);
            getSupportLoaderManager().initLoader(ID, null, this);
        }else {
            hideShow(false);
        }
    }

    private void initView() {
        myBar = findViewById(R.id.prog);
        myEditText = findViewById(R.id.edit_url);
        myTextView = findViewById(R.id.result);
        myTextLinkWeb = findViewById(R.id.web);

        FrameLayout layout = findViewById(R.id.frame);
        mParams = (FrameLayout.LayoutParams) layout.getLayoutParams();
        mySpinner = findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.list, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mySpinner.setAdapter(adapter);
        mySpinner.setOnItemSelectedListener(this);

        myButton = findViewById(R.id.get);
        myButton.setOnClickListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(TEXT_URL, myTextLinkWeb.getText().toString());
        outState.putString(TEXT_HTML, myTextView.getText().toString());
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
            myBar.setVisibility(View.VISIBLE);
            myTextView.setVisibility(View.GONE);
            mParams.gravity = Gravity.CENTER;

        } else {
            myBar.setVisibility(View.GONE);
            myTextView.setVisibility(View.VISIBLE);
            mParams.gravity = Gravity.START;
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
            myTextView.setText(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<String> loader) {

    }

    @Override
    public void onClick(View view) {
        String url = myEditText.getText().toString();
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
        myTextView.setText(error);
        mIndicator = false;
        hideShow(false);
    }

    private void validateProcess(String url) {
        myTextLinkWeb.setText("URL : " + url);
        boolean valid = Patterns.WEB_URL.matcher(url).matches();//Cek URL jika Valid di sini
        if (!valid) {
            cancelLoadError("URL INVALID");
        } else {
            if (checkConnection()) {
                Bundle bundle = new Bundle();
                bundle.putString(_URL, url);
                getSupportLoaderManager().restartLoader(ID, bundle, this);
            } else {
                cancelLoadError("NO INTERNET CONNECTION");
              }
        }
    }
}

class WebTaskLoader extends AsyncTaskLoader<String> {
    private String myResult;
    private String myURL;
    private boolean myCancel = false;

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
            return "URL INI INVALID";
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return "UNKNOWN ERROR, TRY AGAIN PLEASE";
        }
        return result;
    }

    @Override
    protected void onStartLoading() {
        if (myResult == null && !myCancel) {
            forceLoad();
        }
        else {
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
            }
             else {
                return "ERROR, Code Error: (Baca arti kode error)" + connection.getResponseCode();
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


