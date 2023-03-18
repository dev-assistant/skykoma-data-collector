package cn.hylstudio.skykoma.data.collector.repo.neo4j;

import cn.hylstudio.skykoma.data.collector.entity.neo4j.ScanRecordEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface ScanEntityRepo extends Neo4jRepository<ScanRecordEntity, String> {
    ScanRecordEntity findByScanId(String scanId);
}
