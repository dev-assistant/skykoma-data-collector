package cn.hylstudio.skykoma.data.collector.repo.neo4j;

import cn.hylstudio.skykoma.data.collector.entity.neo4j.PsiElementEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;

public interface PsiElementEntityRepo extends Neo4jRepository<PsiElementEntity, String> {

    @Query("""
            MATCH (scanRecord:ScanRecordEntity)-[:ROOT_AT|CONTAINS*1..]->(file:FileEntity)
            MATCH (psiElement:PsiElementEntity)
            WHERE
            scanRecord.scanId = $scanId
            AND
            file.id = $fileEntityId
            AND
            psiElement.id in $psiElementIds
            MERGE (file)-[:HAS_PSI_ELEMENTS]->(psiElement)
            """)
    void attachToFileEntity(String scanId, String fileEntityId, List<String> psiElementIds);

    @Query("""
            MATCH (scanRecord:ScanRecordEntity)-[:MODULE_ROOT|SYMBOL_LINK|CONTAINS*1..]->(file:FileEntity)
            WHERE
            scanRecord.scanId = $scanId
            AND
            file.type = "file"
            MATCH (file)-[:HAS_PSI_ELEMENTS|CONTAINS*1..]->
            //match class info
            (class:PsiElementEntity{className:'com.intellij.psi.impl.source.PsiClassImpl'})
            MATCH (class)-[:CONTAINS]->
            (classModifierList:PsiElementEntity{className:'com.intellij.psi.impl.source.PsiModifierListImpl'})
            MATCH (classModifierList)-[:CONTAINS]->
            (classAnnotation:PsiElementEntity{className:'com.intellij.psi.impl.source.tree.java.PsiAnnotationImpl'})
            MATCH (classAnnotation)-[:HAS_ATTR]->
            (classAnnotationAttr:AnnotationAttrEntity)
            //match method info
            MATCH (class)-[:CONTAINS]->
            (method:PsiElementEntity{className:'com.intellij.psi.impl.source.PsiMethodImpl'})
            MATCH (method)-[:CONTAINS]->
            (methodModifierList:PsiElementEntity{className:'com.intellij.psi.impl.source.PsiModifierListImpl'})
            MATCH (methodModifierList)-[:CONTAINS]->
            (methodAnnotation:PsiElementEntity{className:'com.intellij.psi.impl.source.tree.java.PsiAnnotationImpl'})
            MATCH (methodAnnotation)-[:HAS_ATTR]->
            (methodAnnotationAttr:AnnotationAttrEntity)
            MERGE (class)-[:HAS_ANNOTATION]->(classAnnotation)
            MERGE (method)-[:HAS_ANNOTATION]->(methodAnnotation)
            """)
    //连接所有class和method上的注解
    void connectClassAndMethodAnnotations(String scanId);
}
