package org.esupportail.emargement.domain;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.SequenceGenerator;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.Parameter;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity @Table(uniqueConstraints={@UniqueConstraint(columnNames={"key","context_id"})}) @FilterDef(name="filter",parameters={@ParamDef(type="short",name="context")}) @Filter(name="filter",condition="context_id = :context") public class SessionType implements ContextSupport {
	private static final String[] NATIVE_KEYS = { "CM", "COL", "CONC", "CONF", "EXAM", "EXPO", "FOR", "REU", "SEM", "TD", "TP" };
	@Id @GenericGenerator(name="gen",strategy="org.hibernate.id.enhanced.SequenceStyleGenerator",parameters={@Parameter(name="increment",value="1"),@Parameter(name="sequence_name",value="session_type_sequence")}) @GeneratedValue(generator="gen") @JsonIgnore private Short id;
	@NotBlank(message="Le code du type de session est vide !") @Size(max=5,message="Le code du type de session comporte plus de 5 caractères !") @Column(unique=true) @JsonIgnore private String key;
	@NotBlank(message="L'intitulé du type de session est vide !") @Size(max=64,message="L'intitulé du type de session comporte plus de 64 caractères !") private String title;
	@Size(max=256,message="La description du type de session comporte plus de 256 caractères !") @JsonIgnore private String description;
	@NotNull(message="Le contexte du type de session est inconnu !") @ManyToOne @JsonIgnore private Context context;
	public void setId(short id) { this.id = id; }
	public void setKey(String key) { this.key = key; }
	public void setTitle(String title) { this.title = title; }
	public void setDescription(String description) { this.description = description; }
	public void setContext(Context context) { this.context = context; }
	public boolean getNative() { return ArrayUtils.contains(NATIVE_KEYS, key); }
	public Short getId() { return id; }
	public String getKey() { return key; }
	public String getTitle() { return title; }
	public String getDescription() { return description; }
	public Context getContext() { return context; }
}
