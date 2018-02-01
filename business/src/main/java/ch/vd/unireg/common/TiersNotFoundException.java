package ch.vd.uniregctb.common;

public class TiersNotFoundException extends ObjectNotFoundException {

	public TiersNotFoundException() {
		this("Le tiers spécifié n'existe pas");
	}

	public TiersNotFoundException(long tiersId) {
		this(String.format("Le tiers n°%s n'existe pas", FormatNumeroHelper.numeroCTBToDisplay(tiersId)));
	}

	protected TiersNotFoundException(String message) {
		super(message);
	}
}
