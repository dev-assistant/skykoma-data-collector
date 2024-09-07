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
    private Integer elemDepth;
    @Property
    private Integer propDepth;
    @Property
    private String className;
    @Property
    private String containingFileName;
    @Property
    private String originText;
    @Property
    private Integer lineNumber;
    @Property
    private Integer startOffset;
    @Property
    private Integer endOffset;
    @Property
    private Boolean hasErr;
    @Property
    private String error;
    @Property
    private Boolean inProject;
    @Property
    private String relativePath;
    @Property
    private String absolutePath;
    @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
    private List<PsiElementEntity> childElements;
    @Relationship(type = "HAS_PROPS", direction = Relationship.Direction.OUTGOING)
    private List<PsiElementPropsEntity> props;

//    @Property
//    private String psiType; //Class Annotation unknown
//    @Property
//    private String qualifiedName;//Class Annotation
//    @Property
//    private String canonicalText;//Field type canonicalText
//    @Property
//    private String variableName;//Field
//    @Property
//    private Boolean hasInitializer;//Field
    @CreatedDate
    @Property
    private Long createdAt;
    @LastModifiedDate
    @Property
    private Long updatedAt;

    public PsiElementEntity() {

    }

}