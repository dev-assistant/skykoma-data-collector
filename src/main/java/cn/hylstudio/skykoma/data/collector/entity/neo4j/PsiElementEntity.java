package cn.hylstudio.skykoma.data.collector.entity.neo4j;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.neo4j.core.schema.*;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.util.List;


@Node
@Data
public class PsiElementEntity {//AnnotationEntity, FieldEntity

    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;
    @Property
    private String psiType; //Class Annotation unknown
    @Property
    private String className;
    @Property
    private String originText;
    @Property
    private Integer startOffset;
    @Property
    private Integer endOffset;
    @Property
    private Integer lineNumber;
    @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
    private List<PsiElementEntity> childElements;
    @Property
    private String qualifiedName;//Class Annotation
    //    @Relationship(type = "REFER_CLASS", direction = Relationship.Direction.OUTGOING)
    //    private ClassEntity classInfo;
    @Property
    private String canonicalText;//Field type canonicalText
    @Property
    private String variableName;//Field
    @Property
    private Boolean hasInitializer;//Field
    @Relationship(type = "HAS_ATTR", direction = Relationship.Direction.OUTGOING)
    private List<AnnotationAttrEntity> attrs;//Annotation
    @CreatedDate
    @Property
    private Long createdAt;
    @LastModifiedDate
    @Property
    private Long updatedAt;

    public PsiElementEntity() {

    }

}