package ch.vd.unireg.interfaces.entreprise.data;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;

/**
 * @author Raphaël Marmier, 2016-04-13, <raphael.marmier@vd.ch>
 */
public class EntrepriseCivileEvent implements Serializable {

	private static final long serialVersionUID = 8598759066120296877L;

	private final long noEvenement;

	private final Long noEtablissementCivilCible;

	private Long numeroEntreeJournalRC;
	private RegDate dateEntreeJournalRC;

	private String numeroDocumentFOSC;
	private RegDate datePublicationFOSC;

	private final EntrepriseCivile pseudoHistory;

	public EntrepriseCivileEvent(long noEvenement, Long noEtablissementCivilCible, EntrepriseCivile pseudoHistory) {
		this.noEvenement = noEvenement;
		this.pseudoHistory = pseudoHistory;
		this.noEtablissementCivilCible = noEtablissementCivilCible;
	}

	public long getNoEvenement() {
		return noEvenement;
	}

	/**
	 * <p>
	 *     Renvoie le numéro cantonal de l'établissement civil (établissement) ciblé par l'événement.
	 * </p>
	 *
	 * <p>
	 *     <strong>ATTENTION</strong>: cet établissement civil peut ne pas faire partie de l'entreprise renvoyée par getPseudoHistory()! Il peut être membre d'une autre entreprise
	 *     ciblée par le même événement RCEnt.
	 * </p>
	 *
	 * @return le numéro cantonal de l'établissement civil ciblé par l'événement.
	 */
	public Long getNoEtablissementCivilCible() {
		return noEtablissementCivilCible;
	}

	public EntrepriseCivile getPseudoHistory() {
		return pseudoHistory;
	}

	public Long getNumeroEntreeJournalRC() {
		return numeroEntreeJournalRC;
	}

	public void setNumeroEntreeJournalRC(Long numeroEntreeJournalRC) {
		this.numeroEntreeJournalRC = numeroEntreeJournalRC;
	}

	public RegDate getDateEntreeJournalRC() {
		return dateEntreeJournalRC;
	}

	public void setDateEntreeJournalRC(RegDate dateEntreeJournalRC) {
		this.dateEntreeJournalRC = dateEntreeJournalRC;
	}

	public String getNumeroDocumentFOSC() {
		return numeroDocumentFOSC;
	}

	public void setNumeroDocumentFOSC(String numeroDocumentFOSC) {
		this.numeroDocumentFOSC = numeroDocumentFOSC;
	}

	public RegDate getDatePublicationFOSC() {
		return datePublicationFOSC;
	}

	public void setDatePublicationFOSC(RegDate datePublicationFOSC) {
		this.datePublicationFOSC = datePublicationFOSC;
	}
}
