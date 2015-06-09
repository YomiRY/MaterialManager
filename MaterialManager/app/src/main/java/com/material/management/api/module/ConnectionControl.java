package com.material.management.api.module;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.channels.ConnectionPendingException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class ConnectionControl {
    public static final String GEO_TRANSFORM_URL = "http://maps.google.com/maps/api/geocode/json?";
    public static final String PLACE_NEARBY_SEARCH_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?";
    public static final String PLACE_DETAIL_SEARCH_URL = "https://maps.googleapis.com/maps/api/place/details/json?";
    public static final String PLACE_PHOTO_SEARCH_URL = "https://maps.googleapis.com/maps/api/place/photo?";
    public static final int GEO_ADDRESS_TRANSFORM = 0;
    public static final int PLACE_NEARBY_SEARCH = 1;
    public static final int PLACE_DETAIL_SEARCH = 2;
    public static final int PLACE_PHOTO_SEARCH = 3;

    private static ConnectionControl singleton = null;

    protected AsyncHttpClient dataHttpClient = null;

    public static ConnectionControl getInstance() {
        if (null == singleton)
            singleton = new ConnectionControl();
        return singleton;
    }

    private static String getDataUrl(int type) {
        String url = null;
        if (type == GEO_ADDRESS_TRANSFORM)
            url = GEO_TRANSFORM_URL;
        if(type == PLACE_NEARBY_SEARCH)
            url = PLACE_NEARBY_SEARCH_URL;
        if(type == PLACE_DETAIL_SEARCH)
            url = PLACE_DETAIL_SEARCH_URL;
        if(type == PLACE_PHOTO_SEARCH)
            url = PLACE_PHOTO_SEARCH_URL;
        return url;
    }

    public void getData(int type, ViewCallbackListener callback, RequestParams params) {
        ControlThread a = new ControlThread(type, callback);
        a.setUrl(getDataUrl(type) + params.toString());
        a.get();

    }

    public void getData(int type, ViewCallbackListener callback, RequestParams params, String tag) {
        ControlThread a = new ControlThread(type, callback);
        a.setUrl(getDataUrl(type) + params.toString());
        a.setTag(tag);
        a.get();

    }

    protected AsyncHttpClient getDataHttpClient() {
        if (null == dataHttpClient) {
            dataHttpClient = new AsyncHttpClient();
            try {
                KeyStore trustStore;
                trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);
                SSLSocketFactory sf;
                sf = new SSLSocketFactoryEx(trustStore);
                // TODO: 允許所有主機的驗證
                sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                dataHttpClient.setSSLSocketFactory(sf);
            } catch (KeyStoreException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (CertificateException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (KeyManagementException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (UnrecoverableKeyException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return dataHttpClient;
    }

    class ControlThread {
        private int type;
        private String url = "";
        private String tag = "";
        private String content_type = "";
        private ViewCallbackListener callback = null;
        private RequestParams params = null;
        private JsonResponseHandler handler = new JsonResponseHandler();

        public ControlThread() {}

        public ControlThread(int type, ViewCallbackListener callback) {
            this.type = type;
            this.callback = callback;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public void setParams(RequestParams params) {
            this.params = params;
        }

        public void setContentType(String content_type) {
            this.content_type = content_type;
        }

        public void get() {
            AsyncHttpClient client = getDataHttpClient();

            client.get(url, handler);
        }



        class JsonResponseHandler extends JsonHttpResponseHandler {

            @Override
            protected void handleFailureMessage(Throwable arg0, String arg1) {
                // TODO Auto-generated method stub
                super.handleFailureMessage(arg0, arg1);

                JSONObject jsonObject = new JSONObject();
                try {
                    if ( arg0.getCause() instanceof ConnectTimeoutException) {
                        jsonObject.put("error_code", "001");
                        jsonObject.put("message", "Connection timeout");
                    }
                    if ( arg0.getCause() instanceof ConnectionPendingException) {
                        jsonObject.put("error_code", "002");
                        jsonObject.put("message", "Connection pending");
                    }
                    else {
                        jsonObject.put("error_code", "000");
                        jsonObject.put("message", "Connecting failed");
                    }

                    callback.callbackFromController(jsonObject, arg0, arg1);
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            @Override
            public void onSuccess(JSONObject jsonObject) {
                super.onSuccess(jsonObject);

                if (!tag.isEmpty())
                    callback.callbackFromController(jsonObject, tag);
                else
                    callback.callbackFromController(jsonObject);
            }


            @Override
            public void onFailure(Throwable arg0, String arg1) {
                // TODO Auto-generated method stub
                super.onFailure(arg0, arg1);
            }

            @Override
            public void onFailure(Throwable message, JSONObject jsonObject) {
                // TODO Auto-generated method stub
                super.onFailure(message, jsonObject);
            }

            @Override
            protected Object parseResponse(String arg0) throws JSONException {
                // TODO Auto-generated method stub
                return super.parseResponse(arg0);
            }
        }
    }
}


