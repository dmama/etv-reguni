package ch.vd.uniregctb.evenement.organisation.view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationBasicInfo;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

/**
 * Structure permettant l'affichage de la page de detail de l'evenement
 */
public class EvenementOrganisationSummaryView implements Serializable {

	private static final long serialVersionUID = 4622877003735146179L;

	private Long evtId;
	private Long noEvenement;
	private TypeEvenementOrganisation evtType;
	private RegDate evtDate;
	private Long noOrganisation;
	private long annonceIDEId;
	private boolean correctionDansLePasse;

	private EtatEvenementOrganisation evtEtat;
	private Date evtDateTraitement;
	private String evtCommentaireTraitement;
	private List<ErreurEvenementOrganisationView> evtErreurs = new ArrayList<>();
	private String erreursEvt;

	private boolean recyclable;
	private boolean forcable;

	private List<EvenementOrganisationBasicInfo> nonTraitesSurMemeOrganisation;

	public Long getEvtId() {
		return evtId;
	}

	public void setEvtId(Long evtId) {
		this.evtId = evtId;
	}

	public Long getNoEvenement() {
		return noEvenement;
	}

	public void setNoEvenement(Long noEvenement) {
		this.noEvenement = noEvenement;
	}

	public EtatEvenementOrganisation getEvtEtat() {
		return evtEtat;
	}

	public void setEvtEtat(EtatEvenementOrganisation evtEtat) {
		this.evtEtat = evtEtat;
	}

	public RegDate getEvtDate() {
		return evtDate;
	}

	public void setEvtDate(RegDate evtDate) {
		this.evtDate = evtDate;
	}

	public Date getEvtDateTraitement() {
		return evtDateTraitement;
	}

	public void setEvtDateTraitement(Date evtDateTraitement) {
		this.evtDateTraitement = evtDateTraitement;
	}

	public void setEvtCommentaireTraitement(String evtCommentaireTraitement) {
		this.evtCommentaireTraitement = evtCommentaireTraitement;
	}

	public String getEvtCommentaireTraitement() {
		return evtCommentaireTraitement;
	}

	public List<ErreurEvenementOrganisationView> getEvtErreurs() {
		return evtErreurs;
	}

	public void addEvtErreur(ErreurEvenementOrganisationView evtErreur) {
		evtErreurs.add(evtErreur);
	}

	public TypeEvenementOrganisation getEvtType() {
		return evtType;
	}

	public void setEvtType(TypeEvenementOrganisation evtType) {
		this.evtType = evtType;
	}

	public boolean isRecyclable() {
		return recyclable;
	}

	public void setRecyclable(boolean recyclable) {
		this.recyclable = recyclable;
	}

	public boolean isForcable() {
		return forcable;
	}

	public void setForcable(boolean forcable) {
		this.forcable = forcable;
	}

	public long getAnnonceIDEId() {
		return annonceIDEId;
	}

	public void setAnnonceIDEId(long annonceIDEId) {
		this.annonceIDEId = annonceIDEId;
	}

	public boolean isCorrectionDansLePasse() {
		return correctionDansLePasse;
	}

	public void setCorrectionDansLePasse(boolean correctionDansLePasse) {
		this.correctionDansLePasse = correctionDansLePasse;
	}

	public Long getNoOrganisation() {
		return noOrganisation;
	}

	public void setNoOrganisation(Long noOrganisation) {
		this.noOrganisation = noOrganisation;
	}

	public List<EvenementOrganisationBasicInfo> getNonTraitesSurMemeOrganisation() {
		return nonTraitesSurMemeOrganisation;
	}

	public void setNonTraitesSurMemeOrganisation(List<EvenementOrganisationBasicInfo> nonTraitesSurMemeOrganisation) {
		this.nonTraitesSurMemeOrganisation = nonTraitesSurMemeOrganisation;
	}

	public String getErreursEvt() {
		return erreursEvt;
	}

	public void setErreursEvt(String erreursEvt) {
		this.erreursEvt = erreursEvt;
	}
}
