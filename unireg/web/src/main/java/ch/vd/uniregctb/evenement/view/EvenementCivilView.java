package ch.vd.uniregctb.evenement.view;

import java.util.Date;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class EvenementCivilView {

	private static final long serialVersionUID = 1822889718034929426L;

	private Long id;
	private TypeEvenementCivil type;
	private EtatEvenementCivil etat = EtatEvenementCivil.A_TRAITER;
	private Date dateTraitement;
	private RegDate dateEvenement;
	private Long numeroIndividuPrincipal;
	private Long numeroIndividuConjoint;
	private PersonnePhysique habitantPrincipal;
	private PersonnePhysique habitantConjoint;
	private Integer numeroOfsCommuneAnnonce;
	private Set<EvenementCivilExterneErreur> erreurs;
	private Long numeroCTB;
	private String nom1;
	private String nom2;

	public EvenementCivilView(EvenementCivilExterne evt, TiersDAO tiersDAO) {
		this.id = evt.getId();
		this.etat = evt.getEtat();
		if (evt.getHabitantPrincipalId() != null) {
			this.habitantPrincipal = (PersonnePhysique) tiersDAO.get(evt.getHabitantPrincipalId());
		}
		if (evt.getHabitantConjointId() != null) {
			this.habitantConjoint = (PersonnePhysique) tiersDAO.get(evt.getHabitantConjointId());
		}
		this.numeroIndividuPrincipal = evt.getNumeroIndividuPrincipal();
		this.numeroIndividuConjoint = evt.getNumeroIndividuConjoint();
		this.type = evt.getType();
		this.numeroOfsCommuneAnnonce = evt.getNumeroOfsCommuneAnnonce();
		this.dateEvenement = evt.getDateEvenement();
		this.dateTraitement = evt.getDateTraitement();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public TypeEvenementCivil getType() {
		return type;
	}

	public void setType(TypeEvenementCivil type) {
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

	public Long getNumeroIndividuPrincipal() {
		return numeroIndividuPrincipal;
	}

	public void setNumeroIndividuPrincipal(Long numeroIndividuPrincipal) {
		this.numeroIndividuPrincipal = numeroIndividuPrincipal;
	}

	public Long getNumeroIndividuConjoint() {
		return numeroIndividuConjoint;
	}

	public void setNumeroIndividuConjoint(Long numeroIndividuConjoint) {
		this.numeroIndividuConjoint = numeroIndividuConjoint;
	}

	public PersonnePhysique getHabitantPrincipal() {
		return habitantPrincipal;
	}

	public void setHabitantPrincipal(PersonnePhysique habitantPrincipal) {
		this.habitantPrincipal = habitantPrincipal;
	}

	public PersonnePhysique getHabitantConjoint() {
		return habitantConjoint;
	}

	public void setHabitantConjoint(PersonnePhysique habitantConjoint) {
		this.habitantConjoint = habitantConjoint;
	}

	public Integer getNumeroOfsCommuneAnnonce() {
		return numeroOfsCommuneAnnonce;
	}

	public void setNumeroOfsCommuneAnnonce(Integer numeroOfsCommuneAnnonce) {
		this.numeroOfsCommuneAnnonce = numeroOfsCommuneAnnonce;
	}

	public Set<EvenementCivilExterneErreur> getErreurs() {
		return erreurs;
	}

	public void setErreurs(Set<EvenementCivilExterneErreur> erreurs) {
		this.erreurs = erreurs;
	}

	public Long getNumeroCTB() {
		return numeroCTB;
	}

	public void setNumeroCTB(Long numeroCTB) {
		this.numeroCTB = numeroCTB;
	}

	public String getNom1() {
		return nom1;
	}

	public void setNom1(String nom1) {
		this.nom1 = nom1;
	}

	public String getNom2() {
		return nom2;
	}

	public void setNom2(String nom2) {
		this.nom2 = nom2;
	}

}
