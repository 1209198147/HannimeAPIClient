package com.shikou.model.entities.results;

import com.shikou.model.entities.Pagination;
import com.shikou.model.entities.PlaylistItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 播放列表结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistsResult extends Pagination {
    private List<PlaylistItem> playlists;
}
