package org.esupportail.emargement.config;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:/emargement.properties", encoding = "UTF-8")
public class DatasourceLocalConfig {
	
	@Primary
	@Bean(name="localDb")
	@ConfigurationProperties(prefix="emargement.datasource")
	public DataSource localDataSource() {
		return DataSourceBuilder.create().build();
	}
}
