package cn.hylstudio.skykoma.data.collector.repo.neo4j;

import cn.hylstudio.skykoma.data.collector.entity.neo4j.ClassEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;

public interface ClassEntityRepo extends Neo4jRepository<ClassEntity, String> {
    List<ClassEntity> findByScanId(String scanId);

    @Query("""
            MATCH (class:ClassEntity{scanId:$scanId})
            MATCH (scanRecord:ScanRecordEntity{scanId:$scanId})
            MERGE (scanRecord)-[:HAS_CLASS]->(class)
            """)
    void attachClassEntityToScanRecord(String scanId);
}
