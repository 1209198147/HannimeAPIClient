package com.shikou.exception;

import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * 异常工具类 - 提供异常处理的辅助方法
 */
public class ExceptionUtils {
    
    /**
     * 将HTTP状态码映射为对应的错误信息
     *
     * @param code HTTP状态码
     * @return 对应的错误信息
     */
    public static String mapHttpCodeToMessage(int code) {
        switch (code) {
            case 400: return "请求参数错误";
            case 401: return "未授权访问";
            case 403: return "禁止访问";
            case 404: return "资源不存在";
            case 429: return "请求过于频繁";
            case 500: return "服务器内部错误";
            case 502: return "网关错误";
            case 503: return "服务不可用";
            case 504: return "网关超时";
            default:
                if (code >= 400 && code < 500) {
                    return "客户端错误";
                } else if (code >= 500 && code < 600) {
                    return "服务器错误";
                }
                return "网络错误";
        }
    }

    /**
     * 根据HTTP响应状态码创建相应的异常
     * <p>
     * 如果 {@code message} 为空，则使用HTTP状态码映射的默认错误信息；
     * 否则使用传入的 {@code message}
     * </p>
     *
     * @param response HTTP响应
     * @param message  自定义错误消息，可为空
     */
    public static void fromResponse(Response response, String message) throws HanimeApiException {
        int code = response.code();
        if(StringUtils.isBlank(message)){
            message = mapHttpCodeToMessage(code);
        }
        throw new HanimeApiException(message, code);
    }
    
    /**
     * 将IOException转换为Hanime异常
     *
     * @param ioException IOException
     * @param context     上下文信息
     * @return HanimeException
     */
    public static void fromIOException(IOException ioException, String context) throws HanimeNetworkException, IOException {
        // 检查是否是网络连接相关的异常
        String message = ioException.getMessage();
        if (StringUtils.isBlank(message)) {
            message = "未知错误";
        }

        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("connection refused") ||
                lowerMessage.contains("timeout") ||
                lowerMessage.contains("network") ||
                lowerMessage.contains("socket") ||
                lowerMessage.contains("connect")) {
            throw new HanimeNetworkException(context + ": " + message, ioException);
        }
        throw ioException;
    }
}