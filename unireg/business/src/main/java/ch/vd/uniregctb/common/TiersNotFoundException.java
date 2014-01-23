package ch.vd.uniregctb.common;

public class TiersNotFoundException extends ObjectNotFoundException {

	public TiersNotFoundException() {
		super("Le tiers spécifié n'existe pas");
	}

	public TiersNotFoundException(long tiersId) {
		super(String.format("Le tiers n°%s n'existe pas", FormatNumeroHelper.numeroCTBToDisplay(tiersId)));
	}
}
