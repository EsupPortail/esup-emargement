package org.esupportail.emargement.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@PropertySource(value = "classpath:/emargement.properties", encoding = "UTF-8")
public class DatasourceApogeeConfig {
	
	@Bean(name="apogeeDb")
	@ConfigurationProperties(prefix="emargement.datasource.apogee")
	public DataSource apogeeDataSource() {
		return DataSourceBuilder.create().build();
	}

	@Bean(name="apogeeJdbcTemplate")
	public JdbcTemplate jdbcTemplate(@Qualifier("apogeeDb") DataSource dsApogee) {
		return new JdbcTemplate(dsApogee);
	}
	
}
