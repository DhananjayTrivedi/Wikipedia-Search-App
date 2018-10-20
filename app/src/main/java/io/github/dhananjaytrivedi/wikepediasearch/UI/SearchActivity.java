package io.github.dhananjaytrivedi.wikepediasearch.UI;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.dhananjaytrivedi.wikepediasearch.Adapter.ResultAdapter;
import io.github.dhananjaytrivedi.wikepediasearch.DAO.WikiResultsStorage;
import io.github.dhananjaytrivedi.wikepediasearch.Model.Result;
import io.github.dhananjaytrivedi.wikepediasearch.R;

public class SearchActivity extends AppCompatActivity {

    final String TAG = "DJ";
    ArrayList<Result> resultsList = new ArrayList<>();
    ArrayList<Result> visitedPages = new ArrayList<>();
    @BindView(R.id.searchResultSubmitButton)
    ImageView searchSubmitButton;
    @BindView(R.id.inputQueryET)
    EditText inputQueryEditText;
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.errorMessageLayout)
    RelativeLayout errorMessage;

    private ResultAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);

        ButterKnife.bind(this);
        errorMessage.setVisibility(View.INVISIBLE);

        showPastVisitedPages();

        searchSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                errorMessage.setVisibility(View.INVISIBLE);
                if (isNetworkAvailable()) {
                    if (inputQueryEditText.getText().toString().equals("")) {
                        showSnackbarMessage("We don't know what to look for");
                    } else {
                        getResultsFromAPI();
                    }
                } else {
                    showSnackbarMessage("We need Internet Connectivity To Complete This Task");
                }
            }
        });
    }

    private void showPastVisitedPages() {
        visitedPages = WikiResultsStorage.getStoredPagesArrayList(SearchActivity.this);
        Collections.reverse(visitedPages);
        if (visitedPages != null) {
            for (Result result : visitedPages) {
                Log.d(TAG, result.getTitle());
            }
            updateDisplayWithResultsData(visitedPages);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        WikiResultsStorage.saveVisitedPagesInSharedPreferences(SearchActivity.this);

    }

    private void showSnackbarMessage(String inputMessageString) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), inputMessageString, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    private void getResultsFromAPI() {

        String inputQueryString = inputQueryEditText.getText().toString();
        inputQueryString = inputQueryString.replaceAll("\\s", "%");
        String API_URL = "https://en.wikipedia.org/w/api.php?action=query&format=json&generator=prefixsearch&prop=pageprops|pageimages|description&redirects=&ppprop=displaytitle&piprop=thumbnail&pithumbsize=300&pilimit=40&gpssearch=" + inputQueryString + "&gpsnamespace=0&gpslimit=6";
        Log.d(TAG, API_URL);

        StringRequest request = new StringRequest(API_URL, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Response Received From Server.");
                Log.d(TAG, response);

                try {
                    resultsList = parseJSONData(response);
                    if (resultsList == null) {
                        updateDisplayForNoData();
                    } else {
                        updateDisplayWithResultsData(resultsList);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "ErrorListener : Error");
            }
        });

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);

    }

    private void updateDisplayWithResultsData(ArrayList<Result> results) {

        Log.d(TAG, "*************** UPDATING LAYOUT ***************");

        adapter = new ResultAdapter(results, SearchActivity.this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(SearchActivity.this));

    }

    private void updateDisplayForNoData() {
        errorMessage.setVisibility(View.VISIBLE);
    }

    private ArrayList<Result> parseJSONData(String response) throws JSONException {

        ArrayList<Result> results = new ArrayList<>();
        JSONObject parentObject = new JSONObject(response);
        if (parentObject.has("query")) {
            JSONObject queryObject = parentObject.getJSONObject("query");
            JSONObject pagesObject = queryObject.getJSONObject("pages");

            Iterator<String> iterator = pagesObject.keys();
            while (iterator.hasNext()) {
                String pageKey = iterator.next();
                JSONObject page = pagesObject.getJSONObject(pageKey);

                String pageID = page.getString("pageid");
                String title = page.getString("title");
                String description = page.getString("description");
                String imageURL = "default";

                if (page.has("thumbnail")) {
                    JSONObject thumbnailObject = page.getJSONObject("thumbnail");
                    imageURL = thumbnailObject.getString("source");
                }

                Result result = new Result();
                result.setPageID(pageID);
                result.setTitle(title);
                result.setDescription(description);
                result.setImageURL(imageURL);

                results.add(result);

            }

            // Added all the results objects to the Result Array

            return results;
        } else {
            return null;
        }
    }

    /*
     * This method check whether the device is connected to Internet or not
     **/

    public boolean isNetworkAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
}
