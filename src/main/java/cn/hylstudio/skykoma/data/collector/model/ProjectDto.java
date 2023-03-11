package cn.hylstudio.skykoma.data.collector.model;

import cn.hylstudio.skykoma.data.collector.entity.neo4j.ProjectEntity;
import lombok.Data;

@Data
public class ProjectDto {
    private String id;
    private String name;

    public ProjectDto() {
    }

    public ProjectDto(ProjectEntity entity) {
        this.id = entity.getId();
        this.name = entity.getName();

    }

}
