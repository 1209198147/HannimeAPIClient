package com.shikou.model.entities.page;

import com.shikou.model.entities.PlaylistItem;
import com.shikou.model.entities.Profile;
import com.shikou.model.entities.VideoInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户页
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPage {
    /**
     * 用户信息
     */
    private Profile profile;

    /**
     * 影片
     */
    List<VideoInfo> videoList;

    /**
     * 播放清单
     */
    List<PlaylistItem> playlists;
}
