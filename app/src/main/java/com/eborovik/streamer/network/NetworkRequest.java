package com.eborovik.streamer.network;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * Network Request class to abstract implementation details of requests
 */
public class NetworkRequest {
    private static final String BASE_URL = "http://10.0.2.2:5000/";
    private Callback mCallback;

    private OkHttpClient mClient;

    public NetworkRequest() {
        mClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(new NetInterceptor()) //Add interceptor
                .readTimeout(8, TimeUnit.SECONDS) //Set request timeout
                .writeTimeout(8, TimeUnit.SECONDS)
                .connectTimeout(5, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    /**
     * Sets the callback for the network request
     * @param callback
     */
    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    /**
     * Login
     *
     * @param username - username
     * @param password - password
     * @param callback - callback
     */
    public void doLogin(@NonNull String username,
                        @NonNull String password,
                        Callback callback) {
        setCallback(callback);

        Map<String, String> params = new HashMap<>();
        params.put("email", username);
        params.put("password", password);

        String loginUrl = BASE_URL + "api/user/login";
        doPostRequest(loginUrl, params, callback);
    }

    /**
     * Sign up
     *
     * @param username - username
     * @param password - password
     * @param profileColor - theme color for the user profile
     * @param callback - callback
     */
    public void doSignUp(@NonNull String username,
                         @NonNull String password,
                         @NonNull String profileColor,
                         @Nullable Callback callback) {
        setCallback(callback);

        Map<String, String> params = new HashMap<>();
        params.put("username", username);
        params.put("password", password);
        params.put("extra", profileColor);

        String signUpUrl = BASE_URL + "users";
        doPostRequest(signUpUrl, params, callback);
    }

    /**
     * Get protected quote
     *
     * @param token - token
     * @param callback - callback
     */
    public void doGetProtectedQuote(@NonNull String token, @Nullable Callback callback) {
        setCallback(callback);

        String protectedQuoteUrl = BASE_URL + "api/protected/random-quote";
        doGetRequestWithToken(protectedQuoteUrl, new HashMap<String, String>(), token, callback);
    }

    /**
     * Execute post request
     *
     * @param url
     * @param params
     * @param callback
     */
    private void doPostRequest(@NonNull String url, @NonNull Map<String, String> params,
                               @Nullable final Callback callback) {
        HttpUrl httpUrl = HttpUrl.parse(url);

        /*FormBody.Builder bodyBuilder = new FormBody.Builder();
        for (String string : params.keySet()) {
            bodyBuilder.addEncoded(string, params.get(string));
        }*/

        String json = "{\"email\":\"" + params.get("email") + "\",\"password\":\"" + params.get("password")+ "\"}";

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), json);

        Request request = new Request.Builder()
                .addHeader("content-type", "application/json")
                .url(httpUrl)
                .post(body)
                .build();


        /*Request request = new Request.Builder()
                .url(httpUrl)
                .post(bodyBuilder.build())
                .build();*/

        doRequest(request, callback);
    }

    private void doGetRequestWithToken(@NonNull String url, @NonNull Map<String, String> params,
                                       @Nullable String token, @Nullable Callback callback) {
        HttpUrl httpUrl = HttpUrl.parse(url);

        HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
        for (String key : params.keySet()) {
            urlBuilder.addQueryParameter(key, params.get(key));
        }

        Request.Builder requestBuilder = new Request.Builder()
                .url(urlBuilder.build())
                .get();

        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer " + token);
        }

        doRequest(requestBuilder.build(), callback);
    }

    private void doGetRequestNoToken(@NonNull String url, @NonNull Map<String, String> params,
                                     @NonNull String token, @Nullable Callback callback) {
        doGetRequestWithToken(url, params, token, callback);
    }

    /**
     * Makes request and fires callback as at when due
     *
     * @param request
     * @param callback
     */
    private void doRequest(@NonNull Request request, final Callback callback) {

        mClient.newCall(request)
                .enqueue(new okhttp3.Callback() {
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    @Override
                    public void onFailure(Call call, final IOException e) {
                        if (callback != null) {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    e.printStackTrace();
                                    callback.onError(e.toString());
                                }
                            });
                        }
                    }

                    @Override
                    public void onResponse(final Call call, final Response response) {
                        if(callback != null) {
                            try {
                                final String stringResponse = response.body().string();
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Object res = buildObjectFromResponse(stringResponse,
                                                callback.type());
                                        if (res != null) {
                                            callback.onResponse(res);
                                        } else {
                                            callback.onError(stringResponse);
                                        }
                                    }
                                });
                            } catch (final IOException ioe) {
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.onError(ioe.toString());
                                    }
                                });
                            }
                        }
                    }
                });
    }

    private Object buildObjectFromResponse(String response, Class cls) {
        if (cls == String.class) {
            return response;
        } else {
            try {
                return new Gson().fromJson(response, cls);
            } catch (JsonSyntaxException jse) {
                return null;
            }
        }
    }

    /**
     * Callback interface for network response and error
     * @param <T>
     */
    public interface Callback<T> {
        void onResponse(@NonNull T response);
        void onError(String error);
        Class<T> type();
    }

    /**
     * ApiResponse interface
     */
    public interface ApiResponse {
        String string();
    }
}
