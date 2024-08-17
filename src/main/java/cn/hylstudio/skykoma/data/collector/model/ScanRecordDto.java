package cn.hylstudio.skykoma.data.collector.model;

import cn.hylstudio.skykoma.data.collector.entity.neo4j.ScanRecordEntity;
import cn.hylstudio.skykoma.data.collector.entity.neo4j.projection.ScanRecordEntityProjection;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.Property;

@Data

public class ScanRecordDto {
    @Property
    private String scanId;
    @Property
    private String status;
    public ScanRecordDto(ScanRecordEntity v) {
        this.scanId = v.getScanId();
        this.status = v.getStatus();
    }

    public ScanRecordDto(ScanRecordEntityProjection v) {
        this.scanId = v.getScanId();
        this.status = v.getStatus();
    }
}
