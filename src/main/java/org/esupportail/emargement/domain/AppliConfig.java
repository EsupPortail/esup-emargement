package org.esupportail.emargement.domain;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@Entity
@FilterDef(name = "contextFilter", parameters = {@ParamDef(name = "context", type = "long")})
@Filter(name = "contextFilter", condition = "context_id= :context")
public class AppliConfig {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "key", length = 120)
    private String key;

    @Column(name = "value", columnDefinition = "TEXT")
    private String value;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    private String category;
    
    private Integer version;

    public static enum TypeConfig {
        HTML, TEXT, BOOLEAN
    }
    
    @Column
    @Enumerated(EnumType.STRING)
    private TypeConfig type = TypeConfig.HTML;
    
	@ManyToOne
	private Context context;
	
	@Transient
	private String commonValue;

	@Transient
	private boolean sameValue;

	@Transient
	private List<AppliConfig> contextConfigs;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public TypeConfig getType() {
		return type;
	}

	public void setType(TypeConfig type) {
		this.type = type;
	}
	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public String getCommonValue() {
		return commonValue;
	}

	public void setCommonValue(String commonValue) {
		this.commonValue = commonValue;
	}

	public boolean isSameValue() {
		return sameValue;
	}

	public void setSameValue(boolean sameValue) {
		this.sameValue = sameValue;
	}

	public List<AppliConfig> getContextConfigs() {
		return contextConfigs;
	}

	public void setContextConfigs(List<AppliConfig> contextConfigs) {
		this.contextConfigs = contextConfigs;
	}
}
