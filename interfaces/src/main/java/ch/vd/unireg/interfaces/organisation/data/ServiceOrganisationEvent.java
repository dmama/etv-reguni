package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;

/**
 * @author Raphaël Marmier, 2016-04-13, <raphael.marmier@vd.ch>
 */
public class ServiceOrganisationEvent implements Serializable {

	private static final long serialVersionUID = 8598759066120296877L;

	private final long noEvenement;

	private final Long noEtablissementCivilCible;

	private Long numeroEntreeJournalRC;
	private RegDate dateEntreeJournalRC;

	private Long numeroDocumentFOSC;
	private RegDate datePublicationFOSC;

	private final Organisation pseudoHistory;

	public ServiceOrganisationEvent(long noEvenement, Long noEtablissementCivilCible, Organisation pseudoHistory) {
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
	 *     <strong>ATTENTION</strong>: cet établissement civil peut ne pas faire partie de l'organisation renvoyée par getPseudoHistory()! Il peut être membre d'une autre organisation
	 *     ciblée par le même événement RCEnt.
	 * </p>
	 *
	 * @return le numéro cantonal de l'établissement civil ciblé par l'événement.
	 */
	public Long getNoEtablissementCivilCible() {
		return noEtablissementCivilCible;
	}

	public Organisation getPseudoHistory() {
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

	public Long getNumeroDocumentFOSC() {
		return numeroDocumentFOSC;
	}

	public void setNumeroDocumentFOSC(Long numeroDocumentFOSC) {
		this.numeroDocumentFOSC = numeroDocumentFOSC;
	}

	public RegDate getDatePublicationFOSC() {
		return datePublicationFOSC;
	}

	public void setDatePublicationFOSC(RegDate datePublicationFOSC) {
		this.datePublicationFOSC = datePublicationFOSC;
	}
}
