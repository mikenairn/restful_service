package com.hybble.rest;

import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;
import net.smartam.leeloo.client.OAuthClient;
import net.smartam.leeloo.client.URLConnectionClient;
import net.smartam.leeloo.client.request.OAuthClientRequest;
import net.smartam.leeloo.client.response.OAuthJSONAccessTokenResponse;
import net.smartam.leeloo.common.exception.OAuthProblemException;
import net.smartam.leeloo.common.exception.OAuthSystemException;
import net.smartam.leeloo.common.message.types.GrantType;

/**
 * User: Michael Nairn
 * Date: Jun 13, 2012
 */
public class OAuth2RESTService extends RESTService {

    public static final String OAUTH_TOKEN_TYPE = "OAUTH_TOKEN_TYPE";
    public static final String OAUTH_TOKEN = "OAUTH_TOKEN";
    public static final String OAUTH_REFRESH_TOKEN = "OAUTH_REFRESH_TOKEN";
    public static final String OAUTH_EXPIRES = "OAUTH_EXPIRES";
    public static final String OAUTH_SCOPE = "OAUTH_SCOPE";

    public static final String EXTRA_OAUTH2_CREDENTIALS = "EXTRA_OAUTH2_CREDENTIALS";

    public static final int STATUS_OAUTH_RESULT = STATUS_LAST + 1;

    public OAuth2RESTService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        refresh = true;
        addAuthHeaders(intent, null);
        super.onHandleIntent(intent);
    }

    private void addAuthHeaders(Intent intent, String token) {
        Log.d(TAG, "OAuth2RESTService.addAuthHeaders :: token = " + token);
        Bundle extras = intent.getExtras();
        Bundle oauth2Credentials = extras.getParcelable(EXTRA_OAUTH2_CREDENTIALS);

        if (null == oauth2Credentials) {
            Log.e(TAG, "No OAuth credential supplied!");
        } else {
            Bundle headers = extras.getParcelable(EXTRA_HEADERS);
            if (null == headers) {
                headers = new Bundle();
            }
            String tokenType = null == oauth2Credentials.getString(OAUTH_TOKEN_TYPE) ? "Bearer" : oauth2Credentials.getString(OAUTH_TOKEN_TYPE);
            String accessToken = null == token ? oauth2Credentials.getString(OAUTH_TOKEN) : token;
            headers.putString("AUTHORIZATION", tokenType + " " + accessToken);
            intent.replaceExtras(extras);
        }
    }

    @Override
    protected void sendStatus(Intent intent, ResultReceiver receiver, int statusCode, Bundle resultData) {
        Log.d(TAG, "OAuth2RESTService.sendStatus :: " + refresh);
        if ((401 == statusCode) && refresh) {
            Log.d(TAG, "UNAUTHORISED :: STATUS 401 OAuth2RESTService.sendStatus");
            refresh = false;
            Bundle extras = intent.getExtras();
            Bundle oauth2Credentials = extras.getParcelable(EXTRA_OAUTH2_CREDENTIALS);

            if (null != oauth2Credentials && null != oauth2Credentials.get(OAUTH_REFRESH_TOKEN)) {
                try {
                    Bundle authBundle = doAccessFromRefresh(oauth2Credentials.getString(OAUTH_REFRESH_TOKEN));
                    addAuthHeaders(intent, authBundle.getString(OAUTH_TOKEN));
                    super.sendStatus(intent, receiver, STATUS_OAUTH_RESULT, authBundle);
                    super.onHandleIntent(intent);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                    super.sendStatus(intent, receiver, statusCode, resultData);
                }
            }
        } else {
            super.sendStatus(intent, receiver, statusCode, resultData);
        }
    }

    private Bundle doAccessFromRefresh(String refreshToken) throws OAuthSystemException, OAuthProblemException {
        Log.d(TAG, "MainActivity.doAccessFromRefresh :: " + refreshToken);
        return doAccess(refreshToken, GrantType.REFRESH_TOKEN);
    }

    private Bundle doAccess(String token, GrantType grantType) throws OAuthSystemException, OAuthProblemException {
        Log.d(TAG, "MainActivity.doAccess :: " + token + " :: " + grantType);
        OAuthClientRequest.TokenRequestBuilder requestBuilder = OAuthClientRequest
                .tokenLocation(OAUTH2_PROVIDER + "/oauth2/token")
                .setGrantType(grantType)
                .setClientId(OAUTH2_CLIENT_ID)
                .setClientSecret(OAUTH2_CLIENT_SECRET)
                .setRedirectURI("http://www.example.com/redirect");

        if (grantType.equals(GrantType.REFRESH_TOKEN)) {
            requestBuilder.setRefreshToken(token);
        } else {
            requestBuilder.setCode(token);
        }
        OAuthClientRequest request = requestBuilder.buildBodyMessage();
        OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());

        OAuthJSONAccessTokenResponse oAuthResponse = oAuthClient.accessToken(request);
        String accessToken = oAuthResponse.getAccessToken();
        String expiresIn = oAuthResponse.getExpiresIn();
        String scope = oAuthResponse.getScope();
        String refreshToken = oAuthResponse.getRefreshToken();

        Log.d(TAG, "expiresIn = " + expiresIn);
        Log.d(TAG, "accessToken = " + accessToken);
        Log.d(TAG, "scope = " + scope);
        Log.d(TAG, "refreshToken = " + refreshToken);
        Bundle authBundle = new Bundle();
        authBundle.putString(OAUTH_TOKEN, accessToken);
        authBundle.putString(OAUTH_REFRESH_TOKEN, refreshToken);
        authBundle.putString(OAUTH_EXPIRES, expiresIn);
        authBundle.putString(OAUTH_SCOPE, scope);
        return authBundle;
    }

    final String OAUTH2_PROVIDER = "http://pure-leaf-6787.herokuapp.com";
    final String OAUTH2_CLIENT_ID = "5496d8355b5179a7dbcfda639886197b";
    final String OAUTH2_CLIENT_SECRET = "6da4d17be5700afe42520e63af316fea";

    private boolean refresh = true;
    private static final String TAG = OAuth2RESTService.class.getName();
}