package com.truex.googlereferenceapp;

import android.util.Log;

import com.truex.googlereferenceapp.dagger.DaggerAppComponent;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import dagger.android.AndroidInjector;
import dagger.android.DaggerApplication;

public class MainApplication extends DaggerApplication {
    private static final String CLASSTAG = MainApplication.class.getSimpleName();

    @Override
    protected AndroidInjector<? extends MainApplication> applicationInjector() {
        // HACK Alert! We are only doing this for testing with the older video contents.
        disableSSLCertificateChecking();

        return DaggerAppComponent.builder().create(this);
    }

    // Useful for working with older, expired https assets that are difficult for us to update.
    private static void disableSSLCertificateChecking() {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                // Not implemented
            }

            @Override
            public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                // Not implemented
            }
        } };

        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (KeyManagementException e) {
            Log.e(CLASSTAG, e.toString());
        } catch (NoSuchAlgorithmException e) {
            Log.e(CLASSTAG, e.toString());
        }
    }
}
