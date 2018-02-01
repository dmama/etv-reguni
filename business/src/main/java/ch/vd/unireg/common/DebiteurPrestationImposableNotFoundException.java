package ch.vd.unireg.common;

public class DebiteurPrestationImposableNotFoundException extends TiersNotFoundException {

	public DebiteurPrestationImposableNotFoundException(long tiersId) {
		super(String.format("Le débiteur de prestation imposable n°%s n'existe pas", FormatNumeroHelper.numeroCTBToDisplay(tiersId)));
	}
}
