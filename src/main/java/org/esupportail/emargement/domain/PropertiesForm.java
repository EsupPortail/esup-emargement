package org.esupportail.emargement.domain;

import java.util.List;

public class PropertiesForm {
	public List<SessionLocation> list;
	 
	    public void addSessionLocation(SessionLocation sl) {
	        this.list.add(sl);
	    }

		public List<SessionLocation> getList() {
			return list;
		}

		public void setList(List<SessionLocation> list) {
			this.list = list;
		}
}
