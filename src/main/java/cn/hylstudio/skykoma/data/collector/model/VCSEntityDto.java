package cn.hylstudio.skykoma.data.collector.model;

import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class VCSEntityDto {
    private String id;
    private String vcsType;
    private String name;
    private String path;
}
