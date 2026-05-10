package com.shikou.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 作者/艺术家信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Artist {

    /** 作者名称 */
    private String name;

    /** 作者头像URL */
    private String avatarUrl;

    /** 类型 */
    private String genre;

    /** 是否已订阅 */
    @Builder.Default
    private boolean subscribed = false;
}
