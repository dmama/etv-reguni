package ch.vd.unireg.interfaces.organisation.data;

import ch.vd.registre.base.date.RegDate;

/**
 * @author Raphaël Marmier, 2016-04-13, <raphael.marmier@vd.ch>
 */
public class ServiceOrganisationEvent {

	private long noEvenement;

	private Long siteCible;

	private Long numeroEntreeJournalRC;
	private RegDate dateEntreeJournalRC;

	private Long numeroDocumentFOSC;
	private RegDate datePublicationFOSC;

	private Organisation pseudoHistory;

	public ServiceOrganisationEvent(long noEvenement, Long siteCible, Organisation pseudoHistory) {
		this.noEvenement = noEvenement;
		this.pseudoHistory = pseudoHistory;
	}

	public long getNoEvenement() {
		return noEvenement;
	}

	public Long getSiteCible() {
		return siteCible;
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
