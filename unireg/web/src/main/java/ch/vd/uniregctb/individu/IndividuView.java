package ch.vd.uniregctb.individu;

import java.io.Serializable;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.type.Sexe;

/**
 *
 */
public class IndividuView implements Serializable {

	private static final long serialVersionUID = 2125635768219037077L;

	private Long numeroIndividu;
	private String nom;
	private String nomNaissance;
	private String prenomUsuel;
	private String tousPrenoms;
	private RegDate dateNaissance;
	private Sexe sexe;
	private String etatCivil;
	private RegDate dateDernierChgtEtatCivil;
	private String numeroAssureSocial;
	private String ancienNumeroAVS;
	private String numeroRCE;
	private List<OrigineView> origines;
	private List<NationaliteView> nationalites;
	private String prenomsPere;
	private String nomPere;
	private String prenomsMere;
	private String nomMere;

	private boolean canceled;
	private Long numeroIndividuRemplacant;

	private List<PermisView> permisView;

	public Long getNumeroIndividu() {
		return numeroIndividu;
	}

	public void setNumeroIndividu(Long numeroIndividu) {
		this.numeroIndividu = numeroIndividu;
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	@SuppressWarnings("unused")
	public String getNomNaissance() {
		return nomNaissance;
	}

	public void setNomNaissance(String nomNaissance) {
		this.nomNaissance = nomNaissance;
	}

	@SuppressWarnings("unused")
	public String getPrenomUsuel() {
		return prenomUsuel;
	}

	public void setPrenomUsuel(String prenomUsuel) {
		this.prenomUsuel = prenomUsuel;
	}

	@SuppressWarnings("unused")
	public String getTousPrenoms() {
		return tousPrenoms;
	}

	public void setTousPrenoms(String tousPrenoms) {
		this.tousPrenoms = tousPrenoms;
	}

	public RegDate getDateNaissance() {
		return dateNaissance;
	}

	public void setDateNaissance(RegDate dateNaissance) {
		this.dateNaissance = dateNaissance;
	}

	@SuppressWarnings("unused")
	public Sexe getSexe() {
		return sexe;
	}

	public void setSexe(Sexe sexe) {
		this.sexe = sexe;
	}

	public String getEtatCivil() {
		return etatCivil;
	}

	public void setEtatCivil(String etatCivil) {
		this.etatCivil = etatCivil;
	}

	@SuppressWarnings("unused")
	public RegDate getDateDernierChgtEtatCivil() {
		return dateDernierChgtEtatCivil;
	}

	public void setDateDernierChgtEtatCivil(RegDate dateDernierChgtEtatCivil) {
		this.dateDernierChgtEtatCivil = dateDernierChgtEtatCivil;
	}

	@SuppressWarnings("unused")
	public String getNumeroAssureSocial() {
		return numeroAssureSocial;
	}

	public void setNumeroAssureSocial(String numeroAssureSocial) {
		this.numeroAssureSocial = numeroAssureSocial;
	}

	@SuppressWarnings("unused")
	public String getAncienNumeroAVS() {
		return ancienNumeroAVS;
	}

	public void setAncienNumeroAVS(String ancienNumeroAVS) {
		this.ancienNumeroAVS = ancienNumeroAVS;
	}

	@SuppressWarnings("unused")
	public String getNumeroRCE() {
		return numeroRCE;
	}

	public void setNumeroRCE(String numeroRCE) {
		this.numeroRCE = numeroRCE;
	}

	@SuppressWarnings("unused")
	public List<OrigineView> getOrigines() {
		return origines;
	}

	public void setOrigines(List<OrigineView> origines) {
		this.origines = origines;
	}

	@SuppressWarnings("unused")
	public String getNumeroIndividuFormatte() {
		return FormatNumeroHelper.numeroIndividuToDisplay(numeroIndividu);
	}

	@SuppressWarnings("unused")
	public List<NationaliteView> getNationalites() {
		return nationalites;
	}

	public void setNationalites(List<NationaliteView> nationalites) {
		this.nationalites = nationalites;
	}

	@SuppressWarnings("unused")
	public List<PermisView> getPermisView() {
		return permisView;
	}

	public void setPermisView(List<PermisView> permisView) {
		this.permisView = permisView;
	}

	@SuppressWarnings("UnusedDeclaration")
	public boolean isCanceled() {
		return canceled;
	}

	public void setCanceled(boolean canceled) {
		this.canceled = canceled;
	}

	@SuppressWarnings("UnusedDeclaration")
	public Long getNumeroIndividuRemplacant() {
		return numeroIndividuRemplacant;
	}

	@SuppressWarnings("UnusedDeclaration")
	public String getNumeroIndividuRemplacantFormatte() {
		return FormatNumeroHelper.numeroIndividuToDisplay(numeroIndividuRemplacant);
	}

	public void setNumeroIndividuRemplacant(Long numeroIndividuRemplacant) {
		this.numeroIndividuRemplacant = numeroIndividuRemplacant;
	}

	@SuppressWarnings("UnusedDeclaration")
	public String getPrenomsPere() {
		return prenomsPere;
	}

	public void setPrenomsPere(String prenomsPere) {
		this.prenomsPere = prenomsPere;
	}

	@SuppressWarnings("UnusedDeclaration")
	public String getNomPere() {
		return nomPere;
	}

	public void setNomPere(String nomPere) {
		this.nomPere = nomPere;
	}

	@SuppressWarnings("UnusedDeclaration")
	public String getPrenomsMere() {
		return prenomsMere;
	}

	public void setPrenomsMere(String prenomsMere) {
		this.prenomsMere = prenomsMere;
	}

	@SuppressWarnings("UnusedDeclaration")
	public String getNomMere() {
		return nomMere;
	}

	public void setNomMere(String nomMere) {
		this.nomMere = nomMere;
	}

	/**
	 * Redefinition de la methode equals pour gerer les ajout/suppression d'objets dans une collection
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if ((obj == null) || (obj.getClass() != this.getClass()))
			return false;
		// object must be Test at this point
		IndividuView individuView = (IndividuView) obj;
		return (numeroIndividu.equals(individuView.numeroIndividu) || (numeroIndividu != null && numeroIndividu.equals(individuView.numeroIndividu)));
	}

	/**
	 * Redefinition de la methode hashCode pour gerer les ajout/suppression d'objets
	 * dans une collection
	 */
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 31 * hash ;
		hash = 31 * hash + (null == numeroIndividu ? 0 : numeroIndividu.hashCode());
		return hash;
	}
}
