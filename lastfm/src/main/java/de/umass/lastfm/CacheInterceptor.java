package de.umass.lastfm;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import androidx.annotation.NonNull;
import de.umass.lastfm.cache.DefaultExpirationPolicy;
import de.umass.lastfm.cache.ExpirationPolicy;
import okhttp3.CacheControl;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

// https://stackoverflow.com/questions/49453564/how-to-cache-okhttp-response-from-web-server

public class CacheInterceptor implements Interceptor {
    private final ExpirationPolicy policy;

    public CacheInterceptor() {
        this.policy = new DefaultExpirationPolicy();
    }

    public CacheInterceptor(@NonNull ExpirationPolicy policy) {
        this.policy = policy;
    }

    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        HttpUrl url = request.url();
        Response response = chain.proceed(request);

        long cacheTimeMillis = policy.getExpirationTime(url);

        if (cacheTimeMillis > 0 && !request.method().equals("GET")) {
            Caller.getInstance().getLogger().log(Level.WARNING, "CacheInterceptor: Ignoring non-GET cacheable request");
            return response;
        } else if (cacheTimeMillis > 0) {
            CacheControl cacheControl = new CacheControl.Builder()
                    .maxAge((int) (cacheTimeMillis / 1000), TimeUnit.SECONDS)
                    .build();

            return response.newBuilder()
                    .removeHeader("Pragma")
                    .removeHeader("Cache-Control")
                    .header("Cache-Control", cacheControl.toString())
                    .build();
        } else {
            return response;
        }
    }

}