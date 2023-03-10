package cn.hylstudio.skykoma.data.collector.entity.neo4j;

import cn.hylstudio.skykoma.data.collector.model.payload.ProjectInfoUploadPayload;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.neo4j.core.schema.*;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.util.List;

@Node
@Data
public class ProjectEntity {

    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;
    @Property
    private String name;
    @Relationship(type = "VCS_BY", direction = Relationship.Direction.OUTGOING)
    private VCSEntity vcsEntity;
    @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
    private List<ModuleEntity> modules;
    @Relationship(type = "ROOT_AT", direction = Relationship.Direction.OUTGOING)
    private FileEntity rootFolder;
    @CreatedDate
    @Property
    private Long createdAt;

    @LastModifiedDate
    @Property
    private Long updatedAt;

    public ProjectEntity() {

    }
    public ProjectEntity(ProjectInfoUploadPayload payload) {
        // -------------- generated by skykoma begin --------------
        this.id = payload.getId();
        this.name = payload.getName();
        // -------------- generated by skykoma end --------------

    }
}

