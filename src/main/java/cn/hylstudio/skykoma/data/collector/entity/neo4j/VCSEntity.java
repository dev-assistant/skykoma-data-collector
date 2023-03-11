package cn.hylstudio.skykoma.data.collector.entity.neo4j;

import cn.hylstudio.skykoma.data.collector.model.VCSEntityDto;
import lombok.Data;
import org.neo4j.cypherdsl.core.StatementBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.neo4j.core.schema.*;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

@Data
@Node
public class VCSEntity {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;
    @Property
    private String vcsType;//git
    @Property
    private String name;
    private String path;
    @CreatedDate
    @Property
    private Long createdAt;

    @LastModifiedDate
    @Property
    private Long updatedAt;

    public VCSEntity() {
    }

    public VCSEntity(VCSEntityDto vcsEntityDto) {
        // -------------- generated by skykoma begin --------------
        this.id = vcsEntityDto.getId();
        this.vcsType = vcsEntityDto.getVcsType();
        this.name = vcsEntityDto.getName();
        this.path = vcsEntityDto.getPath();
        // -------------- generated by skykoma end --------------

    }
}
