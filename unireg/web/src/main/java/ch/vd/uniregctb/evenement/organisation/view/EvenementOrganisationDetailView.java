package ch.vd.uniregctb.evenement.organisation.view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseEnvoi;
import ch.vd.uniregctb.evenement.common.view.TiersAssocieView;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationBasicInfo;
import ch.vd.uniregctb.organisation.OrganisationView;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

/**
 * Structure permettant l'affichage de la page de detail de l'evenement
 */
public class EvenementOrganisationDetailView implements Serializable {

	private static final long serialVersionUID = 4622877003735146179L;

	private Long evtId;
	private RegDate evtDate;
	private TypeEvenementOrganisation evtType;
	private Long refEvtId;
	private EtatEvenementOrganisation evtEtat;
	private boolean recyclable;
	private Date evtDateTraitement;
	private String evtCommentaireTraitement;
	private List<ErreurEvenementOrganisationView> evtErreurs = new ArrayList<>();

	private Long noOrganisation;
	private OrganisationView organisation;
	private String organisationError;
	private AdresseEnvoi adresse;
	private List<TiersAssocieView> tiersAssocies = new ArrayList<>();
	private Set<String> erreursTiersAssocies = new LinkedHashSet<>();     // pour éviter les doublons mais conserver l'ordre d'insertion

	private List<EvenementOrganisationBasicInfo> nonTraitesSurMemeOrganisation;

	@SuppressWarnings("unused")
	public Long getEvtId() {
		return evtId;
	}

	public void setEvtId(Long evtId) {
		this.evtId = evtId;
	}

	@SuppressWarnings("unused")
	public EtatEvenementOrganisation getEvtEtat() {
		return evtEtat;
	}

	public void setEvtEtat(EtatEvenementOrganisation evtEtat) {
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
	public List<ErreurEvenementOrganisationView> getEvtErreurs() {
		return Collections.unmodifiableList(evtErreurs);
	}

	public void addEvtErreur(ErreurEvenementOrganisationView evtErreur) {
		evtErreurs.add(evtErreur);
	}

	@SuppressWarnings("unused")
	public TypeEvenementOrganisation getEvtType() {
		return evtType;
	}

	public void setEvtType(TypeEvenementOrganisation evtType) {
		this.evtType = evtType;
	}

	@SuppressWarnings("unused")
	public boolean isRecyclable() {
		return recyclable;
	}

	public void setRecyclable(boolean recyclable) {
		this.recyclable = recyclable;
	}

	@SuppressWarnings("UnusedDeclaration")
	public Long getRefEvtId() {
		return refEvtId;
	}

	public void setRefEvtId(Long refEvtId) {
		this.refEvtId = refEvtId;
	}

	@SuppressWarnings("unused")
	public List<TiersAssocieView> getTiersAssocies() {
		return Collections.unmodifiableList(tiersAssocies);
	}

	public void addTiersAssocies(TiersAssocieView tiersAssocie) {
		tiersAssocies.add(tiersAssocie);
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

	public OrganisationView getOrganisation() {
		return organisation;
	}

	public void setOrganisation(OrganisationView organisation) {
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

	@SuppressWarnings("UnusedDeclaration")
	public List<EvenementOrganisationBasicInfo> getNonTraitesSurMemeOrganisation() {
		return nonTraitesSurMemeOrganisation;
	}

	public void setNonTraitesSurMemeOrganisation(List<EvenementOrganisationBasicInfo> nonTraitesSurMemeOrganisation) {
		this.nonTraitesSurMemeOrganisation = nonTraitesSurMemeOrganisation;
	}
}
