package cn.hylstudio.skykoma.data.collector.repo.neo4j;

import cn.hylstudio.skykoma.data.collector.entity.neo4j.ProjectEntity;
import cn.hylstudio.skykoma.data.collector.entity.neo4j.projection.ProjectEntityNodeProjection;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

public interface ProjectEntityRepo extends Neo4jRepository<ProjectEntity, String> {
//    ProjectEntity findByName(String projectName);
//    ProjectEntity findByKey(String projectKey);

    ProjectEntityNodeProjection findProjectEntityNodeProjectionByKey(String projectKey);

    @Query("""
            MATCH (a:ProjectEntity {id: $projectEntityId})
            MATCH (b:VCSEntity {id: $vcsEntityId})
            MERGE (a)-[:VCS_BY]->(b)
            """)
    void updateVcsEntity(String projectEntityId, String vcsEntityId);

    @Query("""
            MATCH (a:ProjectEntity {id: $projectEntityId})
            MATCH (b:ScanRecordEntity {id: $scanRecordEntityId})
            MERGE (a)-[:HAS_SCAN_RECORD]->(b)
            """)
    void addScanRecordRel(String projectEntityId, String scanRecordEntityId);
    @Query("""
            MATCH (scanRecord:ScanRecordEntity)-[:CONTAINS]->(:ModuleEntity)-[:MODULE_ROOT]->(moduleRoot:FileEntity)
            MATCH (srcRoot:FileEntity)
            WHERE
            scanRecord.scanId = $scanId AND
            moduleRoot.relativePath = srcRoot.relativePath AND
            moduleRoot <> srcRoot
            MERGE (moduleRoot)-[:SYMBOL_LINK]->(srcRoot)
            """)
//            RETURN project, r, scanRecord, rr, module, rrr, moduleRoot, rrrr, srcRoot
    void symbolLinkModuleRootToFileTree(String scanId);
}
