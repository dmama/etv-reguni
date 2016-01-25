package ch.vd.uniregctb.evenement;

import org.jetbrains.annotations.Nullable;
import org.springframework.core.io.Resource;
import org.w3c.dom.ls.LSResourceResolver;

import ch.vd.uniregctb.jms.EsbMessageValidator;
import ch.vd.uniregctb.jms.EsbMessageValidatorImpl;
import ch.vd.uniregctb.jms.EsbMessageValidatorTracing;
import ch.vd.uniregctb.stats.ServiceTracing;

public abstract class EsbMessageValidationHelper {

	public static EsbMessageValidator buildValidator(@Nullable ServiceTracing tracing, LSResourceResolver resourceResolver, Resource[] sources) throws Exception {
		final EsbMessageValidatorImpl impl = new EsbMessageValidatorImpl();
		impl.setResourceResolver(resourceResolver);
		impl.setSources(sources);

		if (tracing != null) {
			return new EsbMessageValidatorTracing(tracing, impl);
		}
		else {
			return impl;
		}
	}
}
