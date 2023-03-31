package cn.hylstudio.skykoma.data.collector.repo.neo4j;

import cn.hylstudio.skykoma.data.collector.entity.neo4j.FileEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

public interface FileEntityRepo extends Neo4jRepository<FileEntity, String> {
    FileEntity findByRelativePath(String relativePath);

    @Query("""
            MATCH
            (scanRecord:ScanRecordEntity)-[:ROOT_AT]->(:FileEntity)-[:CONTAINS*1..]->(file:FileEntity)
            WHERE
            scanRecord.scanId = $scanId
            AND
            file.relativePath = $relativePath
            RETURN file
            """)
    FileEntity findByScanIdAndRelativePath(String scanId, String relativePath);
}
