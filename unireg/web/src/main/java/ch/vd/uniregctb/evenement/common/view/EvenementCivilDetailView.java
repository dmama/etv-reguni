package ch.vd.uniregctb.evenement.common.view;

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
import ch.vd.uniregctb.individu.IndividuView;
import ch.vd.uniregctb.type.EtatEvenementCivil;

abstract public class EvenementCivilDetailView implements Serializable {

	private static final long serialVersionUID = -8937634691213707549L;

	private Long evtId;
	private EtatEvenementCivil evtEtat;
	private RegDate evtDate;
	private Date evtDateTraitement;
	private String evtCommentaireTraitement;
	private List<ErreurEvenementCivilView> evtErreurs = new ArrayList<ErreurEvenementCivilView>();
	private Long noIndividu;
	private IndividuView individu;
	private String individuError;
	private AdresseEnvoi adresse;
	private List<TiersAssocieView> tiersAssocies = new ArrayList<TiersAssocieView>();
	private Set<String> erreursTiersAssocies = new LinkedHashSet<String>();     // pour éviter les doublons mais conserver l'ordre d'insertion

	@SuppressWarnings("unused")
	public Long getEvtId() {
		return evtId;
	}

	public void setEvtId(Long evtId) {
		this.evtId = evtId;
	}

	@SuppressWarnings("unused")
	public EtatEvenementCivil getEvtEtat() {
		return evtEtat;
	}

	public void setEvtEtat(EtatEvenementCivil evtEtat) {
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
	public List<ErreurEvenementCivilView> getEvtErreurs() {
		return Collections.unmodifiableList(evtErreurs);
	}

	public void addEvtErreur(ErreurEvenementCivilView evtErreur) {
		evtErreurs.add(evtErreur);
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

	public Long getNoIndividu() {
		return noIndividu;
	}

	public void setNoIndividu(Long noIndividu) {
		this.noIndividu = noIndividu;
	}

	public IndividuView getIndividu() {
		return individu;
	}

	public void setIndividu(IndividuView individu) {
		this.individu = individu;
	}

	public AdresseEnvoi getAdresse() {
		return adresse;
	}

	public void setAdresse(AdresseEnvoi adresse) {
		this.adresse = adresse;
	}

	public void setIndividuError(String individuError) {
		this.individuError = individuError;
	}

	public String getIndividuError() {
		return individuError;
	}
}
