package com.xpandit.plugins.xrayjenkins.Utils;

import com.xpandit.xray.service.impl.delegates.HttpRequestProvider;
import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;

public class ProxyUtil {
    private ProxyUtil() {}

    /**
     * Gets the Proxy Bean based on the jenkins configuration.
     * 
     * @return If there is an proxy configured, it will return the bean with this information, otherwise, it will return null.
     */
    public static HttpRequestProvider.ProxyBean createProxyBean() {
        ProxyConfiguration proxyConfiguration = Jenkins.getInstance().proxy;
        if (proxyConfiguration != null) {
            final HttpHost proxy = getProxy(proxyConfiguration);
            final CredentialsProvider credentialsProvider = getCredentialsProvider(proxyConfiguration);
            
            return new HttpRequestProvider.ProxyBean(proxy, credentialsProvider);
        }
        
        return null;
    }

    private static HttpHost getProxy(ProxyConfiguration proxyConfiguration) {
        return new HttpHost(proxyConfiguration.name, proxyConfiguration.port);
    }

    private static CredentialsProvider getCredentialsProvider(ProxyConfiguration proxyConfiguration) {
        if (StringUtils.isBlank(proxyConfiguration.getUserName())) {
            return null;
        }

        final CredentialsProvider credsProvider = new BasicCredentialsProvider();
        final AuthScope authScope = new AuthScope(proxyConfiguration.name, proxyConfiguration.port);
        final Credentials credentials = new UsernamePasswordCredentials(proxyConfiguration.getUserName(), proxyConfiguration.getPassword());
        
        credsProvider.setCredentials(authScope, credentials);
        return credsProvider;
    }
}
