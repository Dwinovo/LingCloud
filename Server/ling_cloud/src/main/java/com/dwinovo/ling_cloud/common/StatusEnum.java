package com.dwinovo.ling_cloud.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StatusEnum {

       // ========= 通用成功 =========
    SUCCESS(0, "成功"),

    // ========= 1xxx 通用/参数/请求错误 =========
    BAD_REQUEST(1000, "请求参数错误"),
    VALIDATION_FAILED(1001, "参数校验失败"),
    JSON_PARSE_ERROR(1002, "请求体解析失败"),
    METHOD_NOT_ALLOWED(1003, "方法不支持"),
    UNSUPPORTED_MEDIA_TYPE(1004, "不支持的 Content-Type"),
    RESOURCE_CONFLICT(1005, "资源冲突"),
    ILLEGAL_OPERATION(1006, "非法操作"),

    // ========= 2xxx 用户/认证/权限 =========
    UNAUTHORIZED(2000, "未登录或登录状态已失效"),
    TOKEN_EXPIRED(2001, "登录凭证已过期"),
    TOKEN_INVALID(2002, "登录凭证无效"),
    ACCESS_DENIED(2003, "无权限访问该资源"),
    LOGIN_FAILED(2004, "账号或密码错误"),
    ACCOUNT_LOCKED(2005, "账号已被锁定"),
    ACCOUNT_DISABLED(2006, "账号已被禁用"),

    USER_NOT_FOUND(2100, "用户不存在"),
    USER_ALREADY_EXISTS(2101, "用户已存在"),
    USERNAME_ALREADY_EXISTS(2102, "账号已被占用"),
    NICKNAME_ALREADY_EXISTS(2103, "昵称已被占用"),
    PASSWORD_INCORRECT(2104, "密码错误"),
    PASSWORD_TOO_WEAK(2105, "密码强度不足"),
    OLD_PASSWORD_INCORRECT(2106, "原密码错误"),

    // ========= 3xxx 文件/目录/存储相关 =========
    FILE_NOT_FOUND(3000, "文件不存在"),
    FILE_ALREADY_EXISTS(3001, "同名文件已存在"),
    FILE_UPLOAD_FAILED(3002, "文件上传失败"),
    FILE_DOWNLOAD_FAILED(3003, "文件下载失败"),
    FILE_DELETE_FAILED(3004, "文件删除失败"),
    FILE_NAME_INVALID(3005, "文件名不合法"),
    FILE_TOO_LARGE(3006, "文件大小超出限制"),
    FILE_TYPE_NOT_SUPPORTED(3007, "不支持的文件类型"),
    FILE_HASH_MISMATCH(3008, "文件哈希不一致"),
    FILE_ENCRYPTION_ERROR(3009, "文件加密失败"),
    FILE_DECRYPTION_ERROR(3010, "文件解密失败"),
    FILE_IN_USE(3011, "文件正在被占用"),

    FOLDER_NOT_FOUND(3100, "目录不存在"),
    FOLDER_ALREADY_EXISTS(3101, "同名目录已存在"),
    CANNOT_DELETE_ROOT_FOLDER(3102, "不能删除根目录"),
    MOVE_TARGET_INVALID(3103, "非法的移动目标目录"),

    STORAGE_QUOTA_EXCEEDED(3200, "存储空间已用完"),
    STORAGE_BACKEND_ERROR(3201, "存储服务异常"),
    OSS_UPLOAD_ERROR(3202, "OSS 上传失败"),
    OSS_DOWNLOAD_ERROR(3203, "OSS 下载失败"),
    OSS_DELETE_ERROR(3204, "OSS 删除失败"),

    SHARE_LINK_NOT_FOUND(3300, "分享链接不存在"),
    SHARE_LINK_EXPIRED(3301, "分享链接已过期"),
    SHARE_ACCESS_DENIED(3302, "无权访问该分享"),
    SHARE_CANCELLED(3303, "分享已被取消"),

    // ========= 4xxx 限流/风控/频率控制 =========
    RATE_LIMITED(4000, "请求过于频繁，请稍后再试"),
    TOO_MANY_REQUESTS(4001, "短时间内请求次数过多"),
    OPERATION_TOO_FREQUENT(4002, "操作过于频繁"),
    RISK_CONTROL_BLOCKED(4003, "操作被风控拦截"),

    // ========= 5xxx 系统/第三方/未知错误 =========
    INTERNAL_SERVER_ERROR(5000, "服务器内部错误"),
    SERVICE_UNAVAILABLE(5001, "服务暂不可用"),
    DB_ERROR(5002, "数据库操作异常"),
    CONFIG_ERROR(5003, "系统配置错误"),
    THIRD_PARTY_ERROR(5004, "第三方服务调用失败"),
    UNKNOWN_ERROR(5999, "未知错误");

    private final int code;
    private final String message;
}

