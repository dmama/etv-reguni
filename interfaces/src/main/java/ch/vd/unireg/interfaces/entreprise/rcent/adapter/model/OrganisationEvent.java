package ch.vd.unireg.interfaces.entreprise.rcent.adapter.model;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;

/**
 * @author Raphaël Marmier, 2016-04-13, <raphael.marmier@vd.ch>
 */
public class OrganisationEvent {
	private final long eventNumber;

	private final Long targetLocationId;

	private Long commercialRegisterEntryNumber;
	private RegDate commercialRegisterEntryDate;

	private String documentNumberFOSC;
	private RegDate publicationDateFOSC;

	private final Organisation pseudoHistory;

	public OrganisationEvent(long eventNumber, Long targetLocationId, @NotNull Organisation pseudoHistory) {
		this.eventNumber = eventNumber;
		this.targetLocationId = targetLocationId;
		this.pseudoHistory = pseudoHistory;
	}

	public long getEventNumber() {
		return eventNumber;
	}

	/**
	 * <p>
	 *     Renvoie le numéro cantonal de la location (établissement) ciblé par l'événement.
	 * </p>
	 *
	 * <p>
	 *     <strong>ATTENTION</strong>: cet établissement civil peut ne pas faire partie de l'entreprise renvoyée par getPseudoHistory()! Il peut être membre d'une autre entreprise
	 *     ciblée par le même événement RCEnt.
	 * </p>
	 *
	 * @return le numéro cantonal de la location (établissement) ciblé par l'événement.
	 */
	public Long getTargetLocationId() {
		return targetLocationId;
	}

	/**
	 * @return l'historique partiel de l'entreprise.
	 */
	public Organisation getPseudoHistory() {
		return pseudoHistory;
	}

	public RegDate getPublicationDateFOSC() {
		return publicationDateFOSC;
	}

	public void setPublicationDateFOSC(RegDate publicationDateFOSC) {
		this.publicationDateFOSC = publicationDateFOSC;
	}

	public String getDocumentNumberFOSC() {
		return documentNumberFOSC;
	}

	public void setDocumentNumberFOSC(String documentNumberFOSC) {
		this.documentNumberFOSC = documentNumberFOSC;
	}

	public RegDate getCommercialRegisterEntryDate() {
		return commercialRegisterEntryDate;
	}

	public void setCommercialRegisterEntryDate(RegDate commercialRegisterEntryDate) {
		this.commercialRegisterEntryDate = commercialRegisterEntryDate;
	}

	public Long getCommercialRegisterEntryNumber() {
		return commercialRegisterEntryNumber;
	}

	public void setCommercialRegisterEntryNumber(Long commercialRegisterEntryNumber) {
		this.commercialRegisterEntryNumber = commercialRegisterEntryNumber;
	}
}
