package ch.vd.unireg.jms;

import org.springframework.core.io.Resource;
import org.w3c.dom.ls.LSResourceResolver;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.util.exception.ESBValidationException;
import ch.vd.technical.esb.validation.EsbXmlValidation;

public class EsbMessageValidatorImpl implements EsbMessageValidator {

	private final EsbXmlValidation validator = new EsbXmlValidation();

	public void setSources(Resource[] sources) throws Exception {
		validator.setSources(sources);
	}

	public void setResourceResolver(LSResourceResolver resourceResolver) {
		validator.setResourceResolver(resourceResolver);
	}

	@Override
	public void validate(EsbMessage msg) throws ESBValidationException {
		validator.validate(msg);
	}
}
