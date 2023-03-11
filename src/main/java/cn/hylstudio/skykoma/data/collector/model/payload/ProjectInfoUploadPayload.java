package cn.hylstudio.skykoma.data.collector.model.payload;

import cn.hylstudio.skykoma.data.collector.model.ModuleDto;
import cn.hylstudio.skykoma.data.collector.model.VCSEntityDto;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ProjectInfoUploadPayload {
    private String id;
    private String name;
    private VCSEntityDto vcsEntityDto;
    private List<ModuleDto> modules;
}
