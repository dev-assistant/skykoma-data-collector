package cn.hylstudio.skykoma.data.collector.entity.neo4j;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.neo4j.core.schema.*;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Node
@Data
public class ClassEntity {

    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;
    @Property
    private String qualifiedName;
    @Property
    private String scanId;
    @Property
    private String canonicalText;
    @Property
    private Boolean isInterface;
    @Property
    private List<String> superTypeCanonicalTextsList;
    @Property
    private List<String> superTypeSuperTypeCanonicalTexts;
    @Relationship(type = "IS_CLASS", direction = Relationship.Direction.INCOMING)
    private List<PsiElementEntity> psiElements;
    @Relationship(type = "HAS_PARENT", direction = Relationship.Direction.OUTGOING)
    private Set<ClassEntityParentRel> parents;
    @Property
    private List<String> extendsCanonicalTextsList;
    @Property
    private List<String> extendsSuperTypeCanonicalTextsList;
    @Property
    private List<String> implementsCanonicalTextsList;
    @Property
    private List<String> implementsSuperTypeCanonicalTextsList;
    @CreatedDate
    @Property
    private Long createdAt;
    @LastModifiedDate
    @Property
    private Long updatedAt;

    public ClassEntity() {

    }

    private static Map<String, Map<String, ClassEntity>> classEntityCache = new ConcurrentHashMap<>();

    public static synchronized ClassEntity generateClassEntity(String scanId, String qualifiedName) {
        Map<String, ClassEntity> classEntityMap = classEntityCache.get(scanId);
        if (classEntityMap == null) {
            classEntityMap = new ConcurrentHashMap<>();
            classEntityCache.put(scanId, classEntityMap);
        }
        ClassEntity classEntity = classEntityMap.get(qualifiedName);
        if (classEntity == null) {
            classEntity = new ClassEntity();
            classEntity.setScanId(scanId);
            classEntityMap.put(qualifiedName, classEntity);
        }
        return classEntity;
    }

    public static Collection<ClassEntity> getAllClassEntity(String scanId) {
        Map<String, ClassEntity> classEntityMap = classEntityCache.get(scanId);
        if (classEntityMap == null) {
            classEntityMap = new ConcurrentHashMap<>();
            classEntityCache.put(scanId, classEntityMap);
        }
        return classEntityMap.values();
    }

    public synchronized void mergeParent(List<ClassEntityParentRel> v) {
        Set<ClassEntityParentRel> parents = this.getParents();
        if (parents == null) {
            parents = new HashSet<>();
            this.setParents(parents);
        }
        parents.addAll(v);
    }

    public synchronized void mergePsiElements(PsiElementEntity psiElementEntity) {
        List<PsiElementEntity> psiElements = this.getPsiElements();
        if (psiElements == null) {
            psiElements = new ArrayList<>();
            this.setPsiElements(psiElements);
        }
        psiElements.add(psiElementEntity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassEntity that = (ClassEntity) o;
        return qualifiedName.equals(that.qualifiedName) && scanId.equals(that.scanId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(qualifiedName, scanId);
    }
}