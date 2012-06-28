package com.hybble.rest;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.hybble.rest.R;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Button btn = (Button) findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                runTest();
            }
        });
    }

    private void runTest() {
        Intent intent = new Intent(this, OAuth2RESTService.class);
        intent.setData(Uri.parse("http://pure-leaf-6787.herokuapp.com/api/loyalty/schemes.json"));
        Bundle params = new Bundle();
        Bundle headers = new Bundle();
        Bundle oauth2Credentials = new Bundle();
        oauth2Credentials.putString(OAuth2RESTService.OAUTH_TOKEN, ACCESS_TOKEN);
        oauth2Credentials.putString(OAuth2RESTService.OAUTH_REFRESH_TOKEN, REFRESH_TOKEN);
        intent.putExtra(RESTService.EXTRA_PARAMS, params);
        intent.putExtra(RESTService.EXTRA_HEADERS, headers);
        intent.putExtra(RESTService.EXTRA_RESULT_RECEIVER, getResultReceiver());
        intent.putExtra(OAuth2RESTService.EXTRA_OAUTH2_CREDENTIALS, oauth2Credentials);
        startService(intent);
    }

    private ResultReceiver getResultReceiver() {
        if (null == mReceiver) {
            mReceiver = new ResultReceiver(new Handler()) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    if (OAuth2RESTService.STATUS_OAUTH_RESULT == resultCode) {
                        String tkn = resultData.getString(OAuth2RESTService.OAUTH_TOKEN);
                        Log.d(TAG, "I should really save this token somewhere :: " + tkn);
                    } else if (resultData != null && resultData.containsKey(RESTService.REST_RESULT)) {
                        onRESTResult(resultCode, resultData.getString(RESTService.REST_RESULT));
                    } else {
                        onRESTResult(resultCode, null);
                    }
                }
            };
        }
        return mReceiver;
    }

    private void onRESTResult(int resultCode, String data) {
        Log.d(TAG, "TestRESTServiceActivity.onRESTResult :: " + resultCode);
        if (null != data) {
            TextView tv = (TextView) findViewById(R.id.text);
            tv.setText(data);
        }
    }

    private ResultReceiver mReceiver;
    private String ACCESS_TOKEN = "35015cceb35e0c1a118a7818b3a40a5c";
    private String REFRESH_TOKEN = "f6e4ea3462ceb81d5dff590d168951c2";
    private static final String TAG = MainActivity.class.getName();
}