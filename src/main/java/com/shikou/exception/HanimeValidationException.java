package com.shikou.exception;

/**
 * Hanime 验证异常类
 * <p>
 * 用于处理参数验证失败的情况
 * </p>
 */
public class HanimeValidationException extends HanimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 构造验证异常
     *
     * @param message 错误消息
     */
    public HanimeValidationException(String message) {
        super("VALIDATION_ERROR", message);
    }
    
    /**
     * 构造带错误码的验证异常
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public HanimeValidationException(String code, String message) {
        super(code, message);
    }
    
    /**
     * 构造带原始异常的验证异常
     *
     * @param message 错误消息
     * @param cause   原始异常
     */
    public HanimeValidationException(String message, Throwable cause) {
        super("VALIDATION_ERROR", message, cause);
    }
    
    /**
     * 构造带错误码和原始异常的验证异常
     *
     * @param code    错误码
     * @param message 错误消息
     * @param cause   原始异常
     */
    public HanimeValidationException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
    
    /**
     * 构造带HTTP状态码的验证异常
     *
     * @param message    错误消息
     * @param httpStatus HTTP状态码
     */
    public HanimeValidationException(String message, int httpStatus) {
        super("VALIDATION_ERROR", message, httpStatus);
    }
    
    /**
     * 构造带错误码、HTTP状态码和原始异常的验证异常
     *
     * @param code       错误码
     * @param message    错误消息
     * @param cause      原始异常
     * @param httpStatus HTTP状态码
     */
    public HanimeValidationException(String code, String message, Throwable cause, int httpStatus) {
        super(code, message, cause, httpStatus);
    }
}