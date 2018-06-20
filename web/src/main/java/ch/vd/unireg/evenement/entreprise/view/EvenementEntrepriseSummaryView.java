package ch.vd.unireg.evenement.entreprise.view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseBasicInfo;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.TypeEvenementEntreprise;

/**
 * Structure permettant l'affichage de la page de detail de l'evenement
 */
public class EvenementEntrepriseSummaryView implements Serializable {

	private static final long serialVersionUID = 4622877003735146179L;

	private Long evtId;
	private Long noEvenement;
	private TypeEvenementEntreprise evtType;
	private RegDate evtDate;
	private Long noOrganisation;
	private long annonceIDEId;
	private boolean correctionDansLePasse;

	private EtatEvenementEntreprise evtEtat;
	private Date evtDateTraitement;
	private String evtCommentaireTraitement;
	private final List<ErreurEvenementEntrepriseView> evtErreurs = new ArrayList<>();
	private String erreursEvt;

	private boolean recyclable;
	private boolean forcable;

	private List<EvenementEntrepriseBasicInfo> nonTraitesSurMemeOrganisation;

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

	public EtatEvenementEntreprise getEvtEtat() {
		return evtEtat;
	}

	public void setEvtEtat(EtatEvenementEntreprise evtEtat) {
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

	public List<ErreurEvenementEntrepriseView> getEvtErreurs() {
		return evtErreurs;
	}

	public void addEvtErreur(ErreurEvenementEntrepriseView evtErreur) {
		evtErreurs.add(evtErreur);
	}

	public TypeEvenementEntreprise getEvtType() {
		return evtType;
	}

	public void setEvtType(TypeEvenementEntreprise evtType) {
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

	public List<EvenementEntrepriseBasicInfo> getNonTraitesSurMemeOrganisation() {
		return nonTraitesSurMemeOrganisation;
	}

	public void setNonTraitesSurMemeOrganisation(List<EvenementEntrepriseBasicInfo> nonTraitesSurMemeOrganisation) {
		this.nonTraitesSurMemeOrganisation = nonTraitesSurMemeOrganisation;
	}

	public String getErreursEvt() {
		return erreursEvt;
	}

	public void setErreursEvt(String erreursEvt) {
		this.erreursEvt = erreursEvt;
	}
}
