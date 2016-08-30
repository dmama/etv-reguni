package ch.vd.uniregctb.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoCloseableContainer<T extends AutoCloseable> implements AutoCloseable {

	private static final Logger LOGGER = LoggerFactory.getLogger(AutoCloseableContainer.class);

	private final T[] elements;

	public AutoCloseableContainer(T[] elements) {
		this.elements = elements;
	}

	public T[] getElements() {
		return elements;
	}

	@Override
	public void close() {
		if (elements != null && elements.length > 0) {
			for (T elt : elements) {
				try {
					elt.close();
				}
				catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
				}
			}
		}
	}
}
