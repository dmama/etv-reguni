package ch.vd.uniregctb.individu;

import java.util.Date;
import java.util.List;

import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.type.Sexe;

/**
 * @author claudio
 *
 */
public class IndividuView {
	/**
	 *
	 */
	private static final long serialVersionUID = -4421065649314020793L;
	/**
	 * Numero individu
	 */
	private Long numeroIndividu;

	/**
	 * Nom.
	 */
	private String nom;

	/**
	 * Nom de naissance
	 */
	private String nomNaissance;

	/**
	 * Prenom
	 */
	private String prenom;

	/**
	 * Autres prenoms
	 */
	private String autresPrenoms;


	/**
	 * Date a laquelle la personne est nee. Il arrive que seule l annee, voire
	 * l annee et le mois soient connus.
	 */
	private Date dateNaissance;

	/**
	 * Le sexe
	 */
	private Sexe sexe;

	/**
	 * Etat civil
	 */
	private String etatCivil;

	/**
	 * Date dernier changement etat civil
	 */
	private Date dateDernierChgtEtatCivil;

	/**
	* Numero d'assure social
	 */
	private String numeroAssureSocial;

	/**
	 * L'ancien numero AVS
	 */
	private String ancienNumeroAVS;

	/**
	 * Numero de registre des etrangers
	 */
	private String numeroRCE;

	/**
	 * Origine
	 */
	private String origine;

	/**
	 * Nationalite
	 */
	private String nationalite;


	private List<PermisView> permisView;

	/**
	 * @return the numeroIndividu
	 */
	public Long getNumeroIndividu() {
		return numeroIndividu;
	}

	/**
	 * @param numeroIndividu the numeroIndividu to set
	 */
	public void setNumeroIndividu(Long numeroIndividu) {
		this.numeroIndividu = numeroIndividu;
	}

	/**
	 * @return the nom
	 */
	public String getNom() {
		return nom;
	}

	/**
	 * @param nom the nom to set
	 */
	public void setNom(String nom) {
		this.nom = nom;
	}

	/**
	 * @return the nomNaissance
	 */
	public String getNomNaissance() {
		return nomNaissance;
	}

	/**
	 * @param nomNaissance the nomNaissance to set
	 */
	public void setNomNaissance(String nomNaissance) {
		this.nomNaissance = nomNaissance;
	}

	/**
	 * @return the prenom
	 */
	public String getPrenom() {
		return prenom;
	}

	/**
	 * @param prenom the prenom to set
	 */
	public void setPrenom(String prenom) {
		this.prenom = prenom;
	}

	/**
	 * @return the autresPrenoms
	 */
	public String getAutresPrenoms() {
		return autresPrenoms;
	}

	/**
	 * @param autresPrenoms the autresPrenoms to set
	 */
	public void setAutresPrenoms(String autresPrenoms) {
		this.autresPrenoms = autresPrenoms;
	}

	/**
	 * @return the dateNaissance
	 */
	public Date getDateNaissance() {
		return dateNaissance;
	}

	/**
	 * @param dateNaissance the dateNaissance to set
	 */
	public void setDateNaissance(Date dateNaissance) {
		this.dateNaissance = dateNaissance;
	}

	/**
	 * @return the sexe
	 */
	public Sexe getSexe() {
		return sexe;
	}

	/**
	 * @param sexe the sexe to set
	 */
	public void setSexe(Sexe sexe) {
		this.sexe = sexe;
	}

	/**
	 * @return the etatCivil
	 */
	public String getEtatCivil() {
		return etatCivil;
	}

	/**
	 * @param etatCivil the etatCivil to set
	 */
	public void setEtatCivil(String etatCivil) {
		this.etatCivil = etatCivil;
	}

	/**
	 * @return the dateDernierChgtEtatCivil
	 */
	public Date getDateDernierChgtEtatCivil() {
		return dateDernierChgtEtatCivil;
	}

	/**
	 * @param dateDernierChgtEtatCivil the dateDernierChgtEtatCivil to set
	 */
	public void setDateDernierChgtEtatCivil(Date dateDernierChgtEtatCivil) {
		this.dateDernierChgtEtatCivil = dateDernierChgtEtatCivil;
	}

	/**
	 * @return the numeroAssureSocial
	 */
	public String getNumeroAssureSocial() {
		return numeroAssureSocial;
	}

	/**
	 * @param numeroAssureSocial the numeroAssureSocial to set
	 */
	public void setNumeroAssureSocial(String numeroAssureSocial) {
		this.numeroAssureSocial = numeroAssureSocial;
	}

	/**
	 * @return the ancienNumeroAVS
	 */
	public String getAncienNumeroAVS() {
		return ancienNumeroAVS;
	}

	/**
	 * @param ancienNumeroAVS the ancienNumeroAVS to set
	 */
	public void setAncienNumeroAVS(String ancienNumeroAVS) {
		this.ancienNumeroAVS = ancienNumeroAVS;
	}

	/**
	 * @return the numeroRCE
	 */
	public String getNumeroRCE() {
		return numeroRCE;
	}

	/**
	 * @param numeroRCE the numeroRCE to set
	 */
	public void setNumeroRCE(String numeroRCE) {
		this.numeroRCE = numeroRCE;
	}

	/**
	 * @return the origine
	 */
	public String getOrigine() {
		return origine;
	}

	/**
	 * @param origine the origine to set
	 */
	public void setOrigine(String origine) {
		this.origine = origine;
	}

	public String getNumeroIndividuFormatte() {
		return FormatNumeroHelper.numeroIndividuToDisplay(numeroIndividu);
	}

	public String getNationalite() {
		return nationalite;
	}

	public void setNationalite(String nationalite) {
		this.nationalite = nationalite;
	}

	public List<PermisView> getPermisView() {
		return permisView;
	}

	public void setPermisView(List<PermisView> permisView) {
		this.permisView = permisView;
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
	public int hashCode()
	{
		int hash = 7;
		hash = 31 * hash ;
		hash = 31 * hash + (null == numeroIndividu ? 0 : numeroIndividu.hashCode());
		return hash;
	}


}
