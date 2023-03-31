package cn.hylstudio.skykoma.data.collector.entity.neo4j.projection;

import lombok.Data;

@Data
public class ScanRecordEntityProjection {

    private String id;
    private String scanId;
    private Long createdAt;
    private Long updatedAt;

    public ScanRecordEntityProjection() {
    }

    public ScanRecordEntityProjection(String scanId) {
        this.scanId = scanId;
    }
}
