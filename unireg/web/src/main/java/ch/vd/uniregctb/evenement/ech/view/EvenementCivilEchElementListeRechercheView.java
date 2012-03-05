package ch.vd.uniregctb.evenement.ech.view;

import java.util.Date;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

/**
 * @inheritDoc
 *
 * TODO voir à supprimer les references a PersonnePhysique et rendre la classe serialisable
 */
public class EvenementCivilEchElementListeRechercheView {

	private Long id;
	private TypeEvenementCivilEch type;
	private EtatEvenementCivil etat = EtatEvenementCivil.A_TRAITER;
	private Date dateTraitement;
	private RegDate dateEvenement;
	private Long numeroIndividu;
	private PersonnePhysique personnePhysique;
	private Set<EvenementCivilEchErreur> erreurs;
	private Long numeroCTB;
	private String nom;
	private String commentaireTraitement;

	public EvenementCivilEchElementListeRechercheView(EvenementCivilEch evt, TiersDAO tiersDAO) {
		this.id = evt.getId();
		this.etat = evt.getEtat();
		this.numeroIndividu = evt.getNumeroIndividu();
		if (this.numeroIndividu != null) {
			this.personnePhysique = tiersDAO.getPPByNumeroIndividu(this.numeroIndividu, true);
		}
		this.type = evt.getType();
		this.dateEvenement = evt.getDateEvenement();
		this.dateTraitement = evt.getDateTraitement();
		this.commentaireTraitement = evt.getCommentaireTraitement();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public TypeEvenementCivilEch getType() {
		return type;
	}

	public void setType(TypeEvenementCivilEch type) {
		this.type = type;
	}

	public EtatEvenementCivil getEtat() {
		return etat;
	}

	public void setEtat(EtatEvenementCivil etat) {
		this.etat = etat;
	}

	public Date getDateTraitement() {
		return dateTraitement;
	}

	public void setDateTraitement(Date dateTraitement) {
		this.dateTraitement = dateTraitement;
	}

	public RegDate getDateEvenement() {
		return dateEvenement;
	}

	public void setDateEvenement(RegDate dateEvenement) {
		this.dateEvenement = dateEvenement;
	}

	public Long getNumeroIndividu() {
		return numeroIndividu;
	}

	public void setNumeroIndividu(Long numeroIndividu) {
		this.numeroIndividu = numeroIndividu;
	}

	public PersonnePhysique getPersonnePhysique() {
		return personnePhysique;
	}

	public void setPersonnePhysique(PersonnePhysique personnePhysique) {
		this.personnePhysique = personnePhysique;
	}

	public Set<EvenementCivilEchErreur> getErreurs() {
		return erreurs;
	}

	public void setErreurs(Set<EvenementCivilEchErreur> erreurs) {
		this.erreurs = erreurs;
	}

	public Long getNumeroCTB() {
		return numeroCTB;
	}

	public void setNumeroCTB(Long numeroCTB) {
		this.numeroCTB = numeroCTB;
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom1) {
		this.nom = nom;
	}


	public String getCommentaireTraitement() {
		return commentaireTraitement;
	}

	public void setCommentaireTraitement(String commentaireTraitement) {
		this.commentaireTraitement = commentaireTraitement;
	}
}
