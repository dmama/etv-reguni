package ch.vd.uniregctb.common;

public class ContribuableNotFoundException extends TiersNotFoundException {

	public ContribuableNotFoundException(long tiersId) {
		super(String.format("Le contribuable n°%s n'existe pas", FormatNumeroHelper.numeroCTBToDisplay(tiersId)));
	}
}
