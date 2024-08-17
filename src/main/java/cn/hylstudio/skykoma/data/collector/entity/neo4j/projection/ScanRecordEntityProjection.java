package cn.hylstudio.skykoma.data.collector.entity.neo4j.projection;

import lombok.Data;
import lombok.Value;

@Data
public class ScanRecordEntityProjection {

    private final String id;
    private final String scanId;
    private final String status;
    private final Long createdAt;
    private final Long updatedAt;

    /***
     * DTO based Projection
     * If the store optimizes the query execution by limiting the fields to be loaded,
     * the fields to be loaded are determined from the parameter names of the constructor that is exposed.
     * @param id
     * @param scanId
     * @param status
     * @param createdAt
     * @param updatedAt
     */
    public ScanRecordEntityProjection(String id, String scanId, String status, Long createdAt, Long updatedAt) {
        this.id = id;
        this.scanId = scanId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

}
