package org.esupportail.emargement.domain;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
public class Plan {

	private static final byte DEFAULT_SIZE = 10, MAX_SIZE = Byte.MAX_VALUE;
	private boolean hasAlphanumEnum = true;

	@Min(value=1,message="Le plan a un nombre de colonnes inférieur à 1 !") 
	@Max(value=MAX_SIZE,message="Le plan a dépassé le maximum de colonnes !") 
	private byte columns = DEFAULT_SIZE;

	@Min(value=1,message="Le plan a un nombre de lignes inférieur à 1 !") 
	@Max(value=MAX_SIZE,message="Le plan a dépassé le maximum de lignes !") 
	private byte rows = DEFAULT_SIZE;

	private Short[] specialPlaces = new Short[0];

	@NotEmpty(message="Le plan n'a pas de place !") 
	private Short[] standardPlaces = new Short[0];

	public void setHasAlphanumEnum(boolean alphanum) { 
		hasAlphanumEnum = alphanum; 
	}

	public void setColumns(byte cols) { 
		columns = cols; 
	}

	public void setRows(byte rows) { 
		this.rows = rows; 
	}

	public void setSpecialPlaces(Short[] places) { 
		this.specialPlaces = places; 
	}

	public void setStandardPlaces(Short[] places) { 
		this.standardPlaces = places; 
	}

	public boolean getHasAlphanumEnum() { 
		return hasAlphanumEnum; 
	}

	public byte getColumns() { 
		return columns; 
	}

	public byte getRows() { 
		return rows; 
	}

	@JsonIgnore public short getCapacity() { 
		return (short)standardPlaces.length; 
	}

	public Short[] getSpecialPlaces() { 
		return specialPlaces; 
	}

	public Short[] getStandardPlaces() { 
		return standardPlaces; 
	}
}
