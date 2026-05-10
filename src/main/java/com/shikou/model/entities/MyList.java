package com.shikou.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户播放列表信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyList {

    /** 列表项 */
    private List<PlaylistItem> items;
}
