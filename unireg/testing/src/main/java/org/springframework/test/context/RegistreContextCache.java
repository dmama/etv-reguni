package org.springframework.test.context;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

public class RegistreContextCache extends ContextCache {

	private final Logger LOGGER = Logger.getLogger(RegistreContextCache.class);

	private ConfigurableApplicationContext currentContext = null;
	private String currentKey = null;

	@Override
	boolean contains(String key) {
		return key.equals(currentKey);
	}

	@Override
	ApplicationContext get(String key) {
		if (key.equals(currentKey)) {
			return currentContext;
		}
		// Si on veut créer un autre contexte, on supprime le courant d'abord
		closeContext();
		return null;
	}

	@Override
	void put(String key, ApplicationContext context) {
		closeContext();

		currentKey = key;
		currentContext = (GenericApplicationContext)context;
	}

	@Override
	ApplicationContext remove(String key) {
		if (key.equals(currentKey)) {
			ApplicationContext a = currentContext;
			closeContext();
			return a;
		}
		return null;
	}

	@Override
	void clear() {
		closeContext();
	}

	private void closeContext() {
		if (currentContext != null) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Closing spring context: " + currentKey);
			}
			currentContext.close();
		}

		currentContext = null;
		currentKey = null;
	}

	@Override
	void clearStatistics() {
	}

	@Override
	int getHitCount() {
		return 0;
	}

	@Override
	int getMissCount() {
		return 0;
	}

	@Override
	void setDirty(String key) {
		if (key.equals(currentKey)) {
			closeContext();
		}
	}

	@Override
	int size() {
		if (currentContext != null) {
			return 1;
		}
		return 0;
	}

	@Override
	public String toString() {
		return super.toString();
	}

}
