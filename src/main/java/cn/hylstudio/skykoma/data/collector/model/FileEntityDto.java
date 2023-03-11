package cn.hylstudio.skykoma.data.collector.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class FileEntityDto {
    private String name;
    private String type;//file folder
    private String relativePath;
    private List<FileEntityDto> subFiles;
}
