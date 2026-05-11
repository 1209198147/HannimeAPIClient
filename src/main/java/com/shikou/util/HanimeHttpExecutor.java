package com.shikou.util;

import com.shikou.exception.ExceptionUtils;
import com.shikou.exception.HanimeApiException;
import com.shikou.exception.HanimeNetworkException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

/**
 * HTTP 请求执行工具类
 * <p>统一处理 HTTP 请求的发送、错误检查和异常转换</p>
 */
public final class HanimeHttpExecutor {

    private HanimeHttpExecutor() {}

    /**
     * 执行 HTTP 请求并返回响应体字符串
     *
     * @param client   OkHttpClient
     * @param request  请求对象
     * @param errorMsg 失败时的错误消息前缀
     * @return 响应体字符串
     * @throws HanimeApiException     HTTP 非成功响应
     * @throws HanimeNetworkException 网络IO异常
     */
    public static String executeForString(OkHttpClient client, Request request, String errorMsg)
            throws HanimeApiException, HanimeNetworkException {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                ExceptionUtils.fromResponse(response, errorMsg);
            }
            return response.body().string();
        } catch (IOException e) {
            throw new HanimeNetworkException("请求" + request.url().toString() + " 时失败", e);
        }
    }

    /**
     * 执行 HTTP 请求并直接返回 Response（调用方负责关闭）
     *
     * @param client   OkHttpClient
     * @param request  请求对象
     * @param errorMsg 失败时的错误消息前缀
     * @return Response 对象
     * @throws HanimeApiException     HTTP 非成功响应
     * @throws HanimeNetworkException 网络IO异常
     */
    public static Response executeForResponse(OkHttpClient client, Request request, String errorMsg)
            throws HanimeApiException, HanimeNetworkException {
        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful() || response.body() == null) {
                response.close();
                ExceptionUtils.fromResponse(response, errorMsg);
            }
            return response;
        } catch (IOException e) {
            throw new HanimeNetworkException("请求" + request.url().toString() + " 时失败", e);
        }
    }
}
