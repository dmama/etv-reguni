package ch.vd.unireg.evenement.entreprise.view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseEnvoi;
import ch.vd.unireg.evenement.common.view.TiersAssocieView;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseBasicInfo;
import ch.vd.unireg.organisation.EntrepriseView;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.TypeEvenementEntreprise;

/**
 * Structure permettant l'affichage de la page de detail de l'evenement
 */
public class EvenementEntrepriseDetailView implements Serializable {

	private static final long serialVersionUID = 4622877003735146179L;

	private Long evtId;
	private Long noEvenement;
	private RegDate evtDate;
	private TypeEvenementEntreprise evtType;
	private Long refEvtId;
	private EtatEvenementEntreprise evtEtat;
	private boolean recyclable;
	private boolean forcable;
	private Date evtDateTraitement;
	private String evtCommentaireTraitement;
	private long annonceIDEId;
	private boolean correctionDansLePasse;
	private final List<ErreurEvenementEntrepriseView> evtErreurs = new ArrayList<>();

	private String foscNumero;
	private RegDate foscDate;
	private String foscLienDirect;

	private Long noOrganisation;
	private EntrepriseView organisation;
	private String organisationError;
	private AdresseEnvoi adresse;
	private TiersAssocieView tiersAssocie;
	private final Set<String> erreursTiersAssocies = new LinkedHashSet<>();     // pour éviter les doublons mais conserver l'ordre d'insertion

	private List<EvenementEntrepriseBasicInfo> nonTraitesSurMemeOrganisation;

	private boolean embedded;

	private Long nextId;

	@SuppressWarnings("unused")
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

	@SuppressWarnings("unused")
	public EtatEvenementEntreprise getEvtEtat() {
		return evtEtat;
	}

	public void setEvtEtat(EtatEvenementEntreprise evtEtat) {
		this.evtEtat = evtEtat;
	}

	@SuppressWarnings("unused")
	public RegDate getEvtDate() {
		return evtDate;
	}

	public void setEvtDate(RegDate evtDate) {
		this.evtDate = evtDate;
	}

	@SuppressWarnings("unused")
	public Date getEvtDateTraitement() {
		return evtDateTraitement;
	}

	public void setEvtDateTraitement(Date evtDateTraitement) {
		this.evtDateTraitement = evtDateTraitement;
	}

	public void setEvtCommentaireTraitement(String evtCommentaireTraitement) {
		this.evtCommentaireTraitement = evtCommentaireTraitement;
	}

	@SuppressWarnings("unused")
	public String getEvtCommentaireTraitement() {
		return evtCommentaireTraitement;
	}

	@SuppressWarnings("unused")
	public List<ErreurEvenementEntrepriseView> getEvtErreurs() {
		return evtErreurs;
	}

	public void addEvtErreur(ErreurEvenementEntrepriseView evtErreur) {
		evtErreurs.add(evtErreur);
	}

	@SuppressWarnings("unused")
	public TypeEvenementEntreprise getEvtType() {
		return evtType;
	}

	public void setEvtType(TypeEvenementEntreprise evtType) {
		this.evtType = evtType;
	}

	@SuppressWarnings("unused")
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

	@SuppressWarnings("UnusedDeclaration")
	public Long getRefEvtId() {
		return refEvtId;
	}

	public void setRefEvtId(Long refEvtId) {
		this.refEvtId = refEvtId;
	}

	@SuppressWarnings("UnusedDeclaration")
	public TiersAssocieView getTiersAssocie() {
		return tiersAssocie;
	}

	@SuppressWarnings("unused")
	public List<TiersAssocieView> getTiersAssocies() {
		return tiersAssocie == null ? Collections.emptyList() : Collections.singletonList(tiersAssocie);
	}

	public void setTiersAssocie(TiersAssocieView tiersAssocie) {
		this.tiersAssocie = tiersAssocie;
	}

	@SuppressWarnings("unused")
	public Collection<String> getErreursTiersAssocies() {
		return Collections.unmodifiableCollection(erreursTiersAssocies);
	}

	public void addErreursTiersAssocies(String message) {
		erreursTiersAssocies.add(message);
	}

	public Long getNoOrganisation() {
		return noOrganisation;
	}

	public void setNoOrganisation(Long noOrganisation) {
		this.noOrganisation = noOrganisation;
	}

	public String getFoscNumero() {
		return foscNumero;
	}

	public void setFoscNumero(String foscNumero) {
		this.foscNumero = foscNumero;
	}

	public RegDate getFoscDate() {
		return foscDate;
	}

	public void setFoscDate(RegDate foscDate) {
		this.foscDate = foscDate;
	}

	public String getFoscLienDirect() {
		return foscLienDirect;
	}

	public void setFoscLienDirect(String foscLienDirect) {
		this.foscLienDirect = foscLienDirect;
	}

	public EntrepriseView getOrganisation() {
		return organisation;
	}

	public void setOrganisation(EntrepriseView organisation) {
		this.organisation = organisation;
	}

	public AdresseEnvoi getAdresse() {
		return adresse;
	}

	public void setAdresse(AdresseEnvoi adresse) {
		this.adresse = adresse;
	}

	public void setOrganisationError(String organisationError) {
		this.organisationError = organisationError;
	}

	public String getOrganisationError() {
		return organisationError;
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

	@SuppressWarnings("UnusedDeclaration")
	public List<EvenementEntrepriseBasicInfo> getNonTraitesSurMemeOrganisation() {
		return nonTraitesSurMemeOrganisation;
	}

	public void setNonTraitesSurMemeOrganisation(List<EvenementEntrepriseBasicInfo> nonTraitesSurMemeOrganisation) {
		this.nonTraitesSurMemeOrganisation = nonTraitesSurMemeOrganisation;
	}

	public boolean isEmbedded() {
		return embedded;
	}

	public void setEmbedded(boolean embedded) {
		this.embedded = embedded;
	}

	public Long getNextId() {
		return nextId;
	}

	public void setNextId(Long nextId) {
		this.nextId = nextId;
	}
}
