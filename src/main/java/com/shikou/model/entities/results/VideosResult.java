package com.shikou.model.entities.results;

import com.shikou.model.entities.Pagination;
import com.shikou.model.entities.VideoInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 视频结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideosResult extends Pagination {
    List<VideoInfo> videos;
}
