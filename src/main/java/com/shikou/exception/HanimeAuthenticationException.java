package com.shikou.exception;

public class HanimeAuthenticationException extends HanimeException {
    private static final long serialVersionUID = 1L;

    /**
     * 构造权限异常
     *
     * @param message 错误消息
     */
    public HanimeAuthenticationException(String message) {
        super("AUTH_ERROR", message);
    }

    /**
     * 构造带错误码的API异常
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public HanimeAuthenticationException(String code, String message) {
        super(code, message);
    }

    /**
     * 构造带原始异常的API异常
     *
     * @param message 错误消息
     * @param cause   原始异常
     */
    public HanimeAuthenticationException(String message, Throwable cause) {
        super("AUTH_ERROR", message, cause);
    }

    /**
     * 构造带错误码和原始异常的API异常
     *
     * @param code    错误码
     * @param message 错误消息
     * @param cause   原始异常
     */
    public HanimeAuthenticationException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }

    /**
     * 构造带HTTP状态码的API异常
     *
     * @param message    错误消息
     * @param httpStatus HTTP状态码
     */
    public HanimeAuthenticationException(String message, int httpStatus) {
        super("AUTH_ERROR", message, httpStatus);
    }

    /**
     * 构造带错误码、HTTP状态码和原始异常的API异常
     *
     * @param code       错误码
     * @param message    错误消息
     * @param cause      原始异常
     * @param httpStatus HTTP状态码
     */
    public HanimeAuthenticationException(String code, String message, Throwable cause, int httpStatus) {
        super(code, message, cause, httpStatus);
    }
}
