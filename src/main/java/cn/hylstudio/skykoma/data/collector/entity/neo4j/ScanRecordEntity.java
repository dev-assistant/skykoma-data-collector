package cn.hylstudio.skykoma.data.collector.entity.neo4j;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.neo4j.core.schema.*;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.util.List;

@Node
@Data
@NoArgsConstructor
public class ScanRecordEntity {

    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;
    @Property
    private String scanId;
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
}
