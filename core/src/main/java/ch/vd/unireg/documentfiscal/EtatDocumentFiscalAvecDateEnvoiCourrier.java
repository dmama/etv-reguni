package ch.vd.unireg.documentfiscal;

import ch.vd.registre.base.date.RegDate;

/**
 * Interface implémentée par les état de document qui mémorisent la date d'envoi du courrier.
 */
public interface EtatDocumentFiscalAvecDateEnvoiCourrier {

	/**
	 * @return la date d'envoir du courrier (généralement quelques jours plus tardive que la date d'obtention)
	 */
	RegDate getDateEnvoiCourrier();

	void setDateEnvoiCourrier(RegDate dateEnvoiCourrier);
}
