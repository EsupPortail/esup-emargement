package org.esupportail.emargement.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
	@ConditionalOnProperty(prefix="emargement.datasource.apogee", name="jdbc-url")
	public DataSource apogeeDataSource() {
		return DataSourceBuilder.create().build();
	}

	@Bean(name="apogeeJdbcTemplate")
	@ConditionalOnProperty(prefix="emargement.datasource.apogee", name="jdbc-url")
	public JdbcTemplate jdbcTemplate(@Qualifier("apogeeDb") DataSource dsApogee) {
		return new JdbcTemplate(dsApogee);
	}
	
}
