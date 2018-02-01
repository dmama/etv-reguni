package ch.vd.unireg.common;

public class EntrepriseNotFoundException extends TiersNotFoundException {

	public EntrepriseNotFoundException(long tiersId) {
		super(String.format("L'entreprise n°%s n'existe pas", FormatNumeroHelper.numeroCTBToDisplay(tiersId)));
	}
}
