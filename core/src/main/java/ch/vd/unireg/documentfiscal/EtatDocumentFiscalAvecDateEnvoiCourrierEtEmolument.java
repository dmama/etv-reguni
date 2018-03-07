package ch.vd.unireg.documentfiscal;

import org.jetbrains.annotations.Nullable;

/**
 * Interface implémentée par les état de document qui mémorisent la date d'envoi du courrier et qui ajoute un émolument.
 */
public interface EtatDocumentFiscalAvecDateEnvoiCourrierEtEmolument extends EtatDocumentFiscalAvecDateEnvoiCourrier {

	/**
	 * @return le montant de l'émolument
	 */
	@Nullable
	Integer getEmolument();

	void setEmolument(Integer emolument);
}
