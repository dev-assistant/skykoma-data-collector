package cn.hylstudio.skykoma.data.collector.model.payload;

import lombok.Data;

@Data
public class UpdateScanStatusPayload {
    private String projectKey;
    private String scanId;
    private String status;

    public UpdateScanStatusPayload(String scanId, String status) {
        this.scanId = scanId;
        this.status = status;
    }
}
