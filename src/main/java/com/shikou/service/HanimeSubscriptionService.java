package com.shikou.service;

import com.shikou.config.HanimeConfig;
import com.shikou.exception.HanimeException;
import com.shikou.exception.HanimeNetworkException;
import okhttp3.*;

import java.io.IOException;

/**
 * 订阅服务 - 订阅/取消订阅作者
 */
public class HanimeSubscriptionService {

    private final OkHttpClient client;
    private final HanimeConfig config;

    public HanimeSubscriptionService(OkHttpClient client, HanimeConfig config) {
        this.client = client;
        this.config = config;
    }

    /**
     * 订阅作者
     */
    public boolean subscribe(String csrfToken, String userId, String artistId) throws HanimeException {
        return toggleSubscription(csrfToken, userId, artistId, true);
    }

    /**
     * 取消订阅作者
     */
    public boolean unsubscribe(String csrfToken, String userId, String artistId) throws HanimeException {
        return toggleSubscription(csrfToken, userId, artistId, false);
    }

    private boolean toggleSubscription(String csrfToken, String userId, String artistId, boolean subscribe) throws HanimeException {
        FormBody formBody = new FormBody.Builder()
                .add("_token", csrfToken)
                .add("subscribe-user-id", userId)
                .add("subscribe-artist-id", artistId)
                .add("subscribe-status", subscribe ? "" : "1")
                .build();

        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "subscribe")
                .post(formBody)
                .addHeader("X-CSRF-TOKEN", csrfToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        }catch (IOException e){
            throw new HanimeNetworkException(e.getMessage());
        }
    }
}
