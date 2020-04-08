package org.esupportail.emargement.security;

public class ContextHelper {

	private static ThreadLocal<String> currentContext = new ThreadLocal<>();
	
	private static ThreadLocal<Long> currentContextId = new ThreadLocal<>();
	
	public static Long getCurrenyIdContext() {
		return currentContextId.get();
	}

	public static void setCurrenyIdContext(Long id) {
		 currentContextId.set(id);
	}
	
	public static String getCurrentContext() {
		return currentContext.get();
	}

	public static void setCurrentContext(String context) {
		currentContext.set(context);
	}

	public static void clear() {
		currentContext.set(null);
	}

}
