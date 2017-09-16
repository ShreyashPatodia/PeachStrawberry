package com.comp30022.helium.strawberry.services;

import android.util.Log;

import com.android.volley.Response;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by shreyashpatodia on 16/09/17.
 */

public class RequestWrapper {

    private static final String urlPrefix = "http://gnomie.me";
    private static final String version = "/v1";
    private RestInterface restInterface;

    public RequestWrapper() {
        restInterface = RestInterface.getInstance();

    }
    public void makeGetRequest() {
        restInterface.get(new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.e("GET Success", response.toString());
                //Success Callback
                // Do what you need to here.
                // Maybe to make it flexible we pass of references to classes.
            }
        }, urlPrefix);
    }


}
