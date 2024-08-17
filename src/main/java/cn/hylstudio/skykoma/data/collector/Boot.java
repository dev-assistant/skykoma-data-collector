package cn.hylstudio.skykoma.data.collector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.neo4j.config.EnableNeo4jAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableNeo4jAuditing
@EnableAsync
public class Boot {

    public static void main(String[] args) {
        SpringApplication.run(Boot.class, args);
    }

}
