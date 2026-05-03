package com.shikou.exception;

import okhttp3.Response;

/**
 * 异常工具类 - 提供异常处理的辅助方法
 */
public class ExceptionUtils {
    
    /**
     * 根据HTTP响应状态码创建相应的异常
     *
     * @param response HTTP响应
     * @param message  错误消息
     * @return 对应的异常实例
     */
    public static HanimeException fromResponse(Response response, String message) {
        if (response == null) {
            return new HanimeNetworkException("网络响应为空", message);
        }
        
        int code = response.code();
        switch (code) {
            case 400:
                return new HanimeValidationException("请求参数错误", code);
            case 401:
                return new HanimeAuthenticationException("未授权访问", code);
            case 403:
                return new HanimeAuthenticationException("禁止访问", code);
            case 404:
                return new HanimeApiException("资源不存在", code);
            case 429:
                return new HanimeApiException("请求过于频繁", code);
            case 500:
                return new HanimeApiException("服务器内部错误", code);
            case 502:
                return new HanimeApiException("网关错误", code);
            case 503:
                return new HanimeApiException("服务不可用", code);
            case 504:
                return new HanimeApiException("网关超时", code);
            default:
                if (code >= 400 && code < 500) {
                    return new HanimeApiException("客户端错误", code);
                } else if (code >= 500 && code < 600) {
                    return new HanimeApiException("服务器错误", code);
                } else {
                    return new HanimeNetworkException("网络错误", code);
                }
        }
    }
    
    /**
     * 将IOException转换为Hanime异常
     *
     * @param ioException IOException
     * @param context     上下文信息
     * @return HanimeException
     */
    public static HanimeException fromIOException(java.io.IOException ioException, String context) {
        String message = ioException.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = "IO操作失败";
        }
        
        // 检查是否是网络连接相关的异常
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("connection refused") || 
            lowerMessage.contains("timeout") || 
            lowerMessage.contains("network") || 
            lowerMessage.contains("socket") || 
            lowerMessage.contains("connect")) {
            return new HanimeNetworkException(context + ": " + message, ioException);
        }
        
        return new HanimeApiException(context + ": " + message, ioException);
    }
}