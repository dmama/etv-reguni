package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;

@Entity
@DiscriminatorValue("ENVOI_QSNC")
public class TacheEnvoiQuestionnaireSNC extends TacheEnvoiDocument implements DateRange {

	/**
	 * Date de début d'imposition (précalculé) pour la déclaration à envoyer.
	 */
	private RegDate dateDebut;

	/**
	 * Date de fin d'imposition (précalculé) pour la déclaration à envoyer.
	 */
	private RegDate dateFin;

	/**
	 * Catégorie d'entreprise
	 */
	private CategorieEntreprise categorieEntreprise;

	// Ce constructeur est requis par Hibernate
	protected TacheEnvoiQuestionnaireSNC() {
	}

	public TacheEnvoiQuestionnaireSNC(TypeEtatTache etat, RegDate dateEcheance, Entreprise contribuable,
	                                  RegDate dateDebut, RegDate dateFin, CategorieEntreprise categorieEntreprise,
	                                  CollectiviteAdministrative collectiviteAdministrativeAssignee) {
		super(etat, dateEcheance, contribuable, collectiviteAdministrativeAssignee, TypeDocument.QUESTIONNAIRE_SNC);
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.categorieEntreprise = categorieEntreprise;
	}

	@Override
	@Column(name = "DECL_DATE_DEBUT")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate theDateDebut) {
		dateDebut = theDateDebut;
	}

	@Override
	@Column(name = "DECL_DATE_FIN")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate theDateFin) {
		dateFin = theDateFin;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return !isAnnule() && DateRange.super.isValidAt(date);
	}

	@Transient
	@Override
	public TypeTache getTypeTache() {
		return TypeTache.TacheEnvoiQuestionnaireSNC;
	}

	@Column(name = "CATEGORIE_ENTREPRISE", length= LengthConstants.TACHE_CATEGORIE_ENTREPRISE)
	@Enumerated(value = EnumType.STRING)
	public CategorieEntreprise getCategorieEntreprise() {
		return categorieEntreprise;
	}

	public void setCategorieEntreprise(CategorieEntreprise categorieEntreprise) {
		this.categorieEntreprise = categorieEntreprise;
	}
}
