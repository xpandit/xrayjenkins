/**
 * XP.RAVEN Project
 * <p>
 * Copyright (C) 2016 Xpand IT.
 * <p>
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.service;

import java.io.FileReader;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.xpandit.plugins.xrayjenkins.model.XrayInstance;

/**
 * REST client for JIRA/Xray communication.
 */
public class XrayRestClient {
    private CloseableHttpClient httpclient;

    private static String AUTHORIZATION_HEADER_PREFIX = "Basic ";


    private final XrayInstance xrayInstance;

    public static XrayRestClient createXrayRestClient(XrayInstance xrayInstance) {
        return new XrayRestClient(xrayInstance);
    }

    private static String getBasicAuthorizationHeaderValue(String userName, String password) {
        byte[] bytesEncoded = Base64.encodeBase64((userName + ":" + password).getBytes());
        String authorizationHeader = AUTHORIZATION_HEADER_PREFIX + new String(bytesEncoded);
        return authorizationHeader;
    }

    public XrayRestClient(XrayInstance xrayInstance) {
        this.xrayInstance = xrayInstance;

        createHttpClient();
    }

    public void destroy() {
        if (this.httpclient != null) {
            try {
                this.httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public Boolean testConnection(){
        if (xrayInstance == null) {
            throw new IllegalStateException("Null Xray instance");
        }
        List<Header> headers = createHeaders();
        
        HttpGet get = new HttpGet(this.xrayInstance.getServerAddress());
        HttpResponse response;
        try {
            response = this.httpclient.execute(get);
            int statusCode = response.getStatusLine().getStatusCode();
            
            if(statusCode != 200){
                return false;
            }
            else{
                return true;
            }
        } catch (IOException e) {
            return false;
        }
    }
    
    private List<Header> createHeaders(){
        String basicAuthorizationHeaderValue = getBasicAuthorizationHeaderValue(xrayInstance.getUsername(), xrayInstance.getPassword());
        Header header = new BasicHeader("Authorization", basicAuthorizationHeaderValue);

        List<Header> headers = new ArrayList<>();
        headers.add(header);
        
        return headers;
    }

    private void createHttpClient() {
        if (xrayInstance == null) {
            throw new IllegalStateException("Null Xray instance");
        }

        List<Header> headers = createHeaders();
        
        try {
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(), SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            this.httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).setDefaultHeaders(headers).build();
        } catch (KeyManagementException e1) {
            e1.printStackTrace();
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        } catch (KeyStoreException e1) {
            e1.printStackTrace();
        }
    }

    public HttpResponse httpGet(String sufix, Map<String, String> params) throws IOException {
        if (httpclient == null) {
            throw new IllegalStateException("Null HTTP client");
        }

        String baseUrl = this.xrayInstance.getServerAddress() + sufix;

        if (!params.isEmpty()) {
            boolean first = true;
            for (String s : params.keySet()) {
                if (first) {
                    baseUrl += "?";
                    first = false;
                } else {
                    baseUrl += "&";
                }
                baseUrl += s + "=" + params.get(s);
            }

        }

        HttpGet get = new HttpGet(baseUrl);

        return this.httpclient.execute(get);
    }

    public HttpResponse httpPost(String sufix, String filePath) throws IOException {
        if (httpclient == null) {
            throw new IllegalStateException("Null HTTP client");
        }

        String baseUrl = this.xrayInstance.getServerAddress() + sufix;
        HttpPost post = new HttpPost(baseUrl);
        JSONParser parser = new JSONParser();
        
        try {
            Object obj = parser.parse(new FileReader(filePath));
            
            JSONArray jsonObject = (JSONArray) obj;
            StringEntity input = new StringEntity(jsonObject.toJSONString());
            
            input.setContentType("application/json");
            post.setEntity(input);
            
        } catch (ParseException e) {
            e.printStackTrace();
        }
        
        return this.httpclient.execute(post);
    }


    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        destroy();
    }
}
