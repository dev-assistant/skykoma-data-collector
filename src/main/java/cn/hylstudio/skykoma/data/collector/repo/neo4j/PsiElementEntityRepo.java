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
}
