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
            //match annotation elements
            MATCH (file)-[:HAS_PSI_ELEMENTS|CONTAINS*1..]->(annotationElement:PsiElementEntity{psiType:'Annotation'})
            //add annotation alias label
            SET annotationElement:AnnotationEntity
            //attach annotation to scanRecord
            MERGE (scanRecord)-[:CONTAINS]->(annotationElement)
            """)
    void aliasElementsAnnotationEntity(String scanId);
    @Query("""
            MATCH (scanRecord:ScanRecordEntity)-[:MODULE_ROOT|SYMBOL_LINK|CONTAINS*1..]->(file:FileEntity)
            WHERE
            scanRecord.scanId = $scanId
            AND
            file.type = "file"
            //match field elements
            MATCH (file)-[:HAS_PSI_ELEMENTS|CONTAINS*1..]->(fieldElement:PsiElementEntity{psiType:'Field'})
            //add field alias label
            SET fieldElement:FieldEntity
            """)
    void aliasElementsFieldEntity(String scanId);

    @Query("""
            MATCH (scanRecord:ScanRecordEntity{scanId:$scanId})
            MATCH (scanRecord)-[:CONTAINS]->(annotation:AnnotationEntity)
            MATCH (annotation)<-[:CONTAINS]-(modifierList)<-[:CONTAINS]-(annotationElement)
            MERGE (annotationElement)-[:HAS_ANNOTATION]->(annotation)
            """)
        //连接所有元素上的注解
    void connectAllAnnotations(String scanId);
    @Query("""
            MATCH (scanRecord:ScanRecordEntity{scanId:$scanId})-[:CONTAINS]->
            (methodAnnotation:AnnotationEntity{qualifiedName: 'org.springframework.web.bind.annotation.RequestMapping'})
            //match method
            MATCH (method:PsiElementEntity{className:'com.intellij.psi.impl.source.PsiMethodImpl'})-[:HAS_ANNOTATION]->(methodAnnotation)
            MATCH (methodAnnotation)-[:HAS_ATTR]->(apiPathsOnMethod:AnnotationAttrEntity{name:"value"})
            MATCH (methodAnnotation)-[:HAS_ATTR]->(apiMethods:AnnotationAttrEntity{name:"method"})
            //match class
            MATCH (class:PsiElementEntity{psiType:'Class'})-[:CONTAINS]->(method)
            OPTIONAL MATCH (class)-[:HAS_ANNOTATION]->
            (classAnnotation:AnnotationEntity{qualifiedName: 'org.springframework.web.bind.annotation.RequestMapping'})
            OPTIONAL MATCH (classAnnotation)-[:HAS_ATTR]->(classAnnotationAttr:AnnotationAttrEntity{name:"value"})
            UNWIND CASE
              WHEN classAnnotationAttr is null THEN [""]
              ELSE classAnnotationAttr.values
            END AS prefix
            UNWIND apiMethods.values AS httpMethod
            UNWIND apiPathsOnMethod.values AS path
            WITH
            scanRecord,
            httpMethod,
            prefix + path AS pathWithPrefix,
            CASE WHEN httpMethod STARTS WITH "RequestMethod." THEN substring(httpMethod, size("RequestMethod."))
            ELSE httpMethod END + " " + prefix + path AS fullPath
            MERGE (apiEndpoint:ApiEndpointEntity {scanId: scanRecord.scanId, method: httpMethod, path: pathWithPrefix, fullPath: fullPath})
            MERGE (method)-[:HAS_ENDPOINT]->(apiEndpoint)
            MERGE (scanRecord)-[:HAS_ENDPOINT]->(apiEndpoint)
            """)
    //连接带注解的method和ApiEndpointEntity
    void connectMethodToApiEndpoint(String scanId);

}
