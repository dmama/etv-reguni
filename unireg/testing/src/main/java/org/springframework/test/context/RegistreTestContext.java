package org.springframework.test.context;

import org.springframework.test.context.TestContext;

public class RegistreTestContext extends TestContext {

	private static final long serialVersionUID = -3014112456226404523L;

	public RegistreTestContext(Class<?> testClass, ContextCache contextCache) {
		super(testClass, contextCache);
	}

}
