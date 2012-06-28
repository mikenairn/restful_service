package com.hybble.rest;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * User: Michael Nairn
 * Date: Jun 13, 2012
 */
public class RESTService extends IntentService {

    public static final String EXTRA_HTTP_VERB = "EXTRA_HTTP_VERB";
    public static final String EXTRA_PARAMS = "EXTRA_PARAMS";
    public static final String EXTRA_HEADERS = "EXTRA_HEADERS";
    public static final String EXTRA_PAYLOAD = "EXTRA_PAYLOAD";
    public static final String EXTRA_RESULT_RECEIVER = "EXTRA_RESULT_RECEIVER";

    public static final String REST_RESULT = "REST_RESULT";

    public static final int STATUS_FINISHED = 0;
    public static final int STATUS_RUNNING = 1;
    public static final int STATUS_ERROR = 2;
    public static final int STATUS_RESULT = 3;
    public static final int STATUS_LAST = STATUS_RESULT;

    public enum Verb {
        GET, POST, PUT, DELETE
    }

    public RESTService() {
        this(TAG);
    }

    public RESTService(String name) {
        super(name);
    }

    public static HttpClient getHttpClient() {
        if (null == httpClient) {
            httpClient = new DefaultHttpClient();
        }
        return httpClient;
    }

    public static void setHttpClient(HttpClient httpClient) {
        RESTService.httpClient = httpClient;
    }

    protected static HttpClient httpClient = null;

    protected void sendStatus(Intent intent, ResultReceiver receiver, int statusCode, Bundle resultData) {
        receiver.send(statusCode, resultData);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Uri action = intent.getData();
        Bundle extras = intent.getExtras();

        if (extras == null || action == null || !extras.containsKey(EXTRA_RESULT_RECEIVER)) {
            Log.e(TAG, "You did not pass extras or data with the Intent.");
            return;
        }

        Verb verb = null == extras.getString(EXTRA_HTTP_VERB) ? Verb.GET : Verb.valueOf(extras.getString(EXTRA_HTTP_VERB));
        Bundle params = extras.getParcelable(EXTRA_PARAMS);
        Bundle headers = extras.getParcelable(EXTRA_HEADERS);
        Bundle payload = extras.getParcelable(EXTRA_PAYLOAD);
        ResultReceiver receiver = extras.getParcelable(EXTRA_RESULT_RECEIVER);

        try {
            HttpClient httpClient = new DefaultHttpClient();
            sendStatus(intent, receiver, STATUS_RUNNING, null);
            HttpRequestBase request = createHttpRequest(verb, action, params, headers, payload);
            if (request != null) {
                Log.d(TAG, "Executing request: " + verb + ": " + action.toString());
                HttpContext localContext = new BasicHttpContext();
                HttpResponse response = httpClient.execute(request, localContext);
                HttpEntity responseEntity = response.getEntity();
                StatusLine responseStatus = response.getStatusLine();
                int statusCode = responseStatus != null ? responseStatus.getStatusCode() : 0;
                Log.d(TAG,"statusCode = " + statusCode);

                if (responseEntity != null) {
                    Bundle resultData = new Bundle();
                    resultData.putString(REST_RESULT, EntityUtils.toString(responseEntity));
                    sendStatus(intent, receiver, statusCode, resultData);
                } else {
                    sendStatus(intent, receiver, statusCode, null);
                }
            }
            sendStatus(intent, receiver, STATUS_FINISHED, null);
        }
        catch (URISyntaxException e) {
            Log.e(TAG, "URI syntax was incorrect. " + verb + ": " + action.toString(), e);
            sendStatus(intent, receiver, STATUS_ERROR, null);
        }
        catch (UnsupportedEncodingException e) {
            Log.e(TAG, "A UrlEncodedFormEntity was created with an unsupported encoding.", e);
            sendStatus(intent, receiver, STATUS_ERROR, null);
        }
        catch (ClientProtocolException e) {
            Log.e(TAG, "There was a problem when sending the request.", e);
            sendStatus(intent, receiver, STATUS_ERROR, null);
        }
        catch (IOException e) {
            Log.e(TAG, "There was a problem when sending the request.", e);
            sendStatus(intent, receiver, STATUS_ERROR, null);
        }
    }

    private HttpRequestBase createHttpRequest(Verb verb, Uri action, Bundle params, Bundle headers, Bundle payload)
            throws URISyntaxException {
        HttpRequestBase request;
        URI uri = new URI(attachQueryParams(action, params).toString());
        switch (verb) {
            case POST:
                request = new HttpPost(uri);
                break;
            case PUT:
                request = new HttpPut(uri);
                break;
            case DELETE:
                request = new HttpDelete(uri);
                break;
            case GET:
            default:
                request = new HttpGet(uri);
                break;
        }
        attachHeaders(request, headers);
        attachPayload(request, payload);
        return request;
    }


    private Uri attachQueryParams(Uri action, Bundle params) {
        Uri.Builder uriBuilder = action.buildUpon();
        if (null != params) {
            for (String p : params.keySet()) {
                uriBuilder.appendQueryParameter(p, params.getString(p));
            }
        }
        return uriBuilder.build();
    }

    private void attachHeaders(HttpRequestBase request, Bundle headers) {
        if (null != headers) {
            for (String h : headers.keySet()) {
                request.addHeader(h, headers.getString(h));
                Log.d(TAG, "Adding HEADER :: " + h + " = " + headers.getString(h));
            }
        }
    }

    private void attachPayload(HttpRequestBase request, Bundle payload) {
        if (null != payload && request instanceof HttpEntityEnclosingRequest) {
            try {
                HttpEntity entity = new StringEntity(payload.toString(), "UTF-8");
                ((HttpEntityEnclosingRequest) request).setEntity(entity);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    private static final String TAG = RESTService.class.getName();
}