package com.shikou.exception;

import java.io.Serial;

/**
 * Hanime 客户端基础异常类
 * <p>
 * 提供统一的异常处理机制，包含错误码、错误消息、HTTP状态码等信息
 * </p>
 */
public class HanimeException extends Exception {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /** 错误码 */
    private final String code;
    
    /** HTTP状态码 */
    private final int httpStatus;
    
    /** 原始异常（用于链式异常） */
    private final Throwable cause;
    
    /**
     * 构造基础异常
     *
     * @param message 错误消息
     */
    public HanimeException(String message) {
        this(message, null, 0);
    }
    
    /**
     * 构造带错误码的异常
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public HanimeException(String code, String message) {
        this(code, message, null, 0);
    }
    
    /**
     * 构造带HTTP状态码的异常
     *
     * @param message    错误消息
     * @param httpStatus HTTP状态码
     */
    public HanimeException(String message, int httpStatus) {
        this(null, message, null, httpStatus);
    }
    
    /**
     * 构造带错误码和HTTP状态码的异常
     *
     * @param code       错误码
     * @param message    错误消息
     * @param httpStatus HTTP状态码
     */
    public HanimeException(String code, String message, int httpStatus) {
        this(code, message, null, httpStatus);
    }
    
    /**
     * 构造带原始异常的异常
     *
     * @param message 错误消息
     * @param cause   原始异常
     */
    public HanimeException(String message, Throwable cause) {
        this(null, message, cause, 0);
    }
    
    /**
     * 构造带错误码和原始异常的异常
     *
     * @param code    错误码
     * @param message 错误消息
     * @param cause   原始异常
     */
    public HanimeException(String code, String message, Throwable cause) {
        this(code, message, cause, 0);
    }
    
    /**
     * 完整构造函数
     *
     * @param code       错误码
     * @param message    错误消息
     * @param cause      原始异常
     * @param httpStatus HTTP状态码
     */
    public HanimeException(String code, String message, Throwable cause, int httpStatus) {
        super(message, cause);
        this.code = code != null ? code : "UNKNOWN_ERROR";
        this.httpStatus = httpStatus;
        this.cause = cause;
    }
    
    // Getters
    
    /**
     * 获取错误码
     *
     * @return 错误码
     */
    public String getCode() {
        return code;
    }
    
    /**
     * 获取HTTP状态码
     *
     * @return HTTP状态码
     */
    public int getHttpStatus() {
        return httpStatus;
    }
    
    /**
     * 获取原始异常
     *
     * @return 原始异常
     */
    public Throwable getCause() {
        return cause;
    }
    
    /**
     * 获取详细错误信息
     *
     * @return 详细的错误信息字符串
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HanimeException{");
        sb.append("code='").append(code).append("'");
        sb.append(", httpStatus=").append(httpStatus);
        sb.append(", message='").append(getMessage()).append("'");
        if (cause != null) {
            sb.append(", cause=").append(cause.getClass().getSimpleName());
        }
        sb.append('}');
        return sb.toString();
    }
}