package ch.vd.unireg.evenement.ech.view;

import java.io.Serializable;
import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseEnvoi;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.individu.IndividuView;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.TypeEvenementCivilEch;

public class EvenementCivilEchElementListeRechercheView implements Serializable {

	private static final long serialVersionUID = 3596946976206609859L;

	private final Long id;
	private final TypeEvenementCivilEch type;
	private final EtatEvenementCivil etat;
	private final ActionEvenementCivilEch action;
	private final Date dateTraitement;
	private final RegDate dateEvenement;
	private final Long numeroIndividu;
	private Long numeroCTB;
	private String nom;
	private final String commentaireTraitement;

	private IndividuView individu;
	private AdresseEnvoi adresse;


	public EvenementCivilEchElementListeRechercheView(EvenementCivilEch evt) {
		this.id = evt.getId();
		this.etat = evt.getEtat();
		this.action = evt.getAction();
		this.numeroIndividu = evt.getNumeroIndividu();
		this.type = evt.getType();
		this.dateEvenement = evt.getDateEvenement();
		this.dateTraitement = evt.getDateTraitement();
		this.commentaireTraitement = evt.getCommentaireTraitement();
	}

	public Long getId() {
		return id;
	}

	public TypeEvenementCivilEch getType() {
		return type;
	}

	public EtatEvenementCivil getEtat() {
		return etat;
	}

	@SuppressWarnings("unused")
	public Date getDateTraitement() {
		return dateTraitement;
	}

	@SuppressWarnings("unused")
	public RegDate getDateEvenement() {
		return dateEvenement;
	}

	@SuppressWarnings("unused")
	public Long getNumeroIndividu() {
		return numeroIndividu;
	}

	@SuppressWarnings("unused")
	public Long getNumeroCTB() {
		return numeroCTB;
	}

	public void setNumeroCTB(Long numeroCTB) {
		this.numeroCTB = numeroCTB;
	}

	@SuppressWarnings("unused")
	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	@SuppressWarnings("unused")
	public String getCommentaireTraitement() {
		return commentaireTraitement;
	}

	@SuppressWarnings("unused")
	public ActionEvenementCivilEch getAction() {
		return action;
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
}
