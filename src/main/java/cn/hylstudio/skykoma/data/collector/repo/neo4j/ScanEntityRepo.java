package cn.hylstudio.skykoma.data.collector.repo.neo4j;

import cn.hylstudio.skykoma.data.collector.entity.neo4j.ScanRecordEntity;
import cn.hylstudio.skykoma.data.collector.entity.neo4j.projection.ScanRecordEntityProjection;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

public interface ScanEntityRepo extends Neo4jRepository<ScanRecordEntity, String> {
    ScanRecordEntity findByScanId(String scanId);

    ScanRecordEntityProjection findScanRecordEntityProjectionByScanId(String scanId);

    @Query("""
            MATCH (scanRecord:ScanRecordEntity)
            WHERE scanRecord.scanId = $scanId
            SET scanRecord.status = $status
            """)
	void updateStatus(String scanId, String status);
}
