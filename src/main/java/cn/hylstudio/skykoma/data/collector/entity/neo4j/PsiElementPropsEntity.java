package cn.hylstudio.skykoma.data.collector.entity.neo4j;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.neo4j.core.schema.*;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.util.List;


@Node
@Data
public class PsiElementPropsEntity {

    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;
    @Property
    private Integer elemDepth;
    @Property
    private Integer propDepth;
    @CreatedDate
    @Property
    private Long createdAt;
    @LastModifiedDate
    @Property
    private Long updatedAt;

    public PsiElementPropsEntity() {

    }

}