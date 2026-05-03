package com.shikou.exception;

/**
 * Hanime 网络异常类
 * <p>
 * 用于处理网络连接、超时、DNS解析等网络相关问题
 * </p>
 */
public class HanimeNetworkException extends HanimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 构造网络异常
     *
     * @param message 错误消息
     */
    public HanimeNetworkException(String message) {
        super("NETWORK_ERROR", message);
    }
    
    /**
     * 构造带错误码的网络异常
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public HanimeNetworkException(String code, String message) {
        super(code, message);
    }
    
    /**
     * 构造带原始异常的网络异常
     *
     * @param message 错误消息
     * @param cause   原始异常
     */
    public HanimeNetworkException(String message, Throwable cause) {
        super("NETWORK_ERROR", message, cause);
    }
    
    /**
     * 构造带错误码和原始异常的网络异常
     *
     * @param code    错误码
     * @param message 错误消息
     * @param cause   原始异常
     */
    public HanimeNetworkException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
    
    /**
     * 构造带HTTP状态码的网络异常
     *
     * @param message    错误消息
     * @param httpStatus HTTP状态码
     */
    public HanimeNetworkException(String message, int httpStatus) {
        super("NETWORK_ERROR", message, httpStatus);
    }
    
    /**
     * 构造带错误码、HTTP状态码和原始异常的网络异常
     *
     * @param code       错误码
     * @param message    错误消息
     * @param cause      原始异常
     * @param httpStatus HTTP状态码
     */
    public HanimeNetworkException(String code, String message, Throwable cause, int httpStatus) {
        super(code, message, cause, httpStatus);
    }
}