package ch.vd.uniregctb.webservices.tiers3.exception;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.webservices.tiers3.CodeQuittancement;

public class QuittancementErreur extends Exception {

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
