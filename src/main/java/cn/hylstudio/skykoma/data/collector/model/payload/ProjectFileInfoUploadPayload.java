package cn.hylstudio.skykoma.data.collector.model.payload;

import cn.hylstudio.skykoma.data.collector.model.FileDto;
import cn.hylstudio.skykoma.data.collector.model.ProjectInfoDto;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProjectFileInfoUploadPayload {
    private String scanId;
    private FileDto fileDto;
}
