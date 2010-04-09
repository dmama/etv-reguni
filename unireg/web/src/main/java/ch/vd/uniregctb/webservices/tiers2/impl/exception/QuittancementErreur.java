package ch.vd.uniregctb.webservices.tiers2.impl.exception;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.webservices.tiers2.data.CodeQuittancement;
import ch.vd.uniregctb.webservices.tiers2.exception.BusinessException;

public class QuittancementErreur extends BusinessException {

	private static final long serialVersionUID = 1L;

	private final CodeQuittancement code;

	public QuittancementErreur(CodeQuittancement code, String message) {
		super(message);
		Assert.isTrue(code.name().startsWith("ERREUR"));
		this.code = code;
	}

	public CodeQuittancement getCode() {
		return code;
	}
}
