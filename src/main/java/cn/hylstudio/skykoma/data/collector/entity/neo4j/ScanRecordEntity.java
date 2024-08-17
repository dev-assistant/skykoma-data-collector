package cn.hylstudio.skykoma.data.collector.entity.neo4j;

import com.google.common.collect.Sets;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.neo4j.core.schema.*;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.util.List;
import java.util.Set;

@Node
@Data
@NoArgsConstructor
public class ScanRecordEntity {

    public static final String STATUS_INIT = "INIT";
    public static final String STATUS_UPLOADING = "UPLOADING";
    public static final String STATUS_UPLOADED = "UPLOADED";
    public static final String STATUS_SCANNING = "SCANNING";
    public static final String STATUS_SCANNED = "SCANNED";
    public static final String STATUS_FINISHED = "FINISHED";
    public static final Set<String> ALLOW_STATUS = Sets.newHashSet(
            STATUS_INIT,
            STATUS_UPLOADING,
            STATUS_UPLOADED,
            STATUS_SCANNING,
            STATUS_SCANNED,
            STATUS_FINISHED
    );

    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;
    @Property
    private String scanId;
    @Property
    private String status;
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
