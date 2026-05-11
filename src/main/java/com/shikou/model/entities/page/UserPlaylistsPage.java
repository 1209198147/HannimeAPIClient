package com.shikou.model.entities.page;

import com.shikou.model.entities.PlaylistItem;
import com.shikou.model.entities.Profile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPlaylistsPage {
    /**
     * 用户信息
     */
    private Profile profile;

    /**
     * 排序方式
     */
    private Map<String, String> sort;

    /**
     * 播放列表
     */
    private List<PlaylistItem> playlists;
}
