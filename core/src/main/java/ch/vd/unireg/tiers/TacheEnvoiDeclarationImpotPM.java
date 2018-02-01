package ch.vd.unireg.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Transient;
import java.util.EnumSet;
import java.util.Set;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.type.CategorieEntreprise;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.type.TypeTache;

@Entity
@DiscriminatorValue("ENVOI_DI_PM")
public class TacheEnvoiDeclarationImpotPM extends TacheEnvoiDeclarationImpot {

	/**
	 * Tous les types de documents acceptés pour une tâche d'envoi de DI PM
	 */
	private static final Set<TypeDocument> TYPES_DOCUMENTS_AUTORISES = EnumSet.of(TypeDocument.DECLARATION_IMPOT_APM_BATCH,
	                                                                              TypeDocument.DECLARATION_IMPOT_APM_LOCAL,
	                                                                              TypeDocument.DECLARATION_IMPOT_PM_BATCH,
	                                                                              TypeDocument.DECLARATION_IMPOT_PM_LOCAL);

	/**
	 * Catégorie d'entreprise
	 */
	private CategorieEntreprise categorieEntreprise;

	/**
	 * Début de l'exercice commercial associé à la tâche d'envoi
	 */
	private RegDate dateDebutExercice;

	/**
	 * Fin de l'exercice commercial associé à la tâche d'envoi
	 */
	private RegDate dateFinExercice;

	// requis par Hibernate
	protected TacheEnvoiDeclarationImpotPM() {
	}

	public TacheEnvoiDeclarationImpotPM(TypeEtatTache etat, RegDate dateEcheance, ContribuableImpositionPersonnesMorales contribuable, RegDate dateDebut, RegDate dateFin,
	                                    RegDate dateDebutExercice, RegDate dateFinExercice, TypeContribuable typeContribuable, TypeDocument typeDocument,
	                                    CategorieEntreprise categorieEntreprise, CollectiviteAdministrative collectivite) {
		super(etat, dateEcheance, contribuable, dateDebut, dateFin, typeContribuable, typeDocument, collectivite);
		this.dateDebutExercice = dateDebutExercice;
		this.dateFinExercice = dateFinExercice;
		this.categorieEntreprise = categorieEntreprise;

		if (!TYPES_DOCUMENTS_AUTORISES.contains(typeDocument)) {
			throw new IllegalArgumentException("Le type de document " + typeDocument + " n'est pas accepté pour une tâche d'envoi de déclaration PM.");
		}
		if (!typeContribuable.isUsedForPM()) {
			throw new IllegalArgumentException("Le type de contribuable " + typeContribuable + " n'est pas accepté pour une tâche d'envoi de déclaration PM.");
		}
	}

	@Transient
	@Override
	public ContribuableImpositionPersonnesMorales getContribuable() {
		return (ContribuableImpositionPersonnesMorales) super.getContribuable();
	}

	@Transient
	@Override
	public TypeTache getTypeTache() {
		return TypeTache.TacheEnvoiDeclarationImpotPM;
	}

	@Column(name = "CATEGORIE_ENTREPRISE", length= LengthConstants.TACHE_CATEGORIE_ENTREPRISE)
	@Enumerated(value = EnumType.STRING)
	public CategorieEntreprise getCategorieEntreprise() {
		return categorieEntreprise;
	}

	public void setCategorieEntreprise(CategorieEntreprise categorieEntreprise) {
		this.categorieEntreprise = categorieEntreprise;
	}

	@Column(name = "DECL_DATE_DEBUT_EXERCICE")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateDebutExercice() {
		return dateDebutExercice;
	}

	public void setDateDebutExercice(RegDate dateDebutExercice) {
		this.dateDebutExercice = dateDebutExercice;
	}

	@Column(name = "DECL_DATE_FIN_EXERCICE")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateFinExercice() {
		return dateFinExercice;
	}

	public void setDateFinExercice(RegDate dateFinExercice) {
		this.dateFinExercice = dateFinExercice;
	}
}
