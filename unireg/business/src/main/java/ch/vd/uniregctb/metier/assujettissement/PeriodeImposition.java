package ch.vd.uniregctb.metier.assujettissement;

import java.util.List;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.Qualification;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

/**
 * Classe de base abstraite représentant une période d'imposition.
 * <p/>
 * <b>Note:</b> la différence entre une période d'imposition et un assujettissement (voir {@link Assujettissement}) est subtile, mais bien
 * réelle.
 * <ul>
 * <li>Un <b>assujettissement</b> représente le type de contribuable tel que calculé en appliquant les règles fiscales au plus près de leurs
 * définition, sans tenir compte du contexte d'utilisation.</li>
 * <li>Une <b>période d'imposition</b> est une notion qui représente la période et le type de contribuable, mais limité à une période
 * fiscale et en tenant compte d'un context orienté "déclaration d'impôt".</li>
 * </ul>
 * <p/>
 * Dans la plupart des cas, les deux notions sont confondues; mais pas dans tous.
 * <p/>
 * <b>Exemple où les deux notions sont différentes:</b> le cas du contribuable vaudois qui part hors-Suisse en gardant un immeuble dans le
 * canton. Dans ce cas, on a:
 * <ul>
 * <li><b>assujettissements:</b> un assujettissement VaudoisOrdinaire pour la période du 1er janvier à la date du départ; et un second
 * assujettissement HorsSuisse pour le reste de l'année.</li>
 * <li><b>période d'imposition:</b> une seule période couvrant toute l'année avec le type de document DECLARATION_IMPOT_COMPLETE car les
 * deux assujettissements VaudoisOrdinaire et HorsSuisse génèrent le même type de DI au final.</li>
 * </ul>
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class PeriodeImposition implements CollatableDateRange {

	private final RegDate debut;
	private final RegDate fin;
	private final CategorieEnvoiDI categorieEnvoiDI;
	private final Contribuable contribuable;
	private final Qualification qualification;
	private final Integer codeSegment;  // [SIFISC-2100]
	private final TypeAdresseRetour adresseRetour; // [UNIREG-1741]

	/**
	 * <code>vrai</code> si la période d'imposition est optionnelle (= une DI n'est émise que sur demande du contribuable).
	 */
	private final boolean optionnelle;

	/**
	 * <code>vrai</code> si la déclaration correspondante à la période d'imposition est remplacée par une note à l'administration fiscale d'une autre canton (= la DI n'est émise).
	 */
	private final boolean remplaceeParNote;

	protected static enum CauseFermeture {
		/**
		 * si la période d'imposition se termine à cause d'un décès (ou d'un veuvage).
		 */
		VEUVAGE_DECES,

		/**
		 * si la période d'imposition (HS) se termine pour cause de fin d'activité indépendante ou de vente d'immeuble
		 */
		FIN_ASSUJETTISSEMENT_HS,

		/**
		 * si la période d'imposition se termine pour une autre raison
		 */
		AUTRE
	}

	/**
	 * Cause pour laquelle la période d'imposition se termine
	 */
	private final CauseFermeture causeFermeture;


	private static Qualification getQualification(Contribuable contribuable, int anneePrecedente) {
		Qualification qualification = null;
		DeclarationImpotOrdinaire precedente = getDeclarationPrecedente(contribuable, anneePrecedente);
		if (precedente != null) {
			qualification = precedente.getQualification();
		}
		return qualification;
	}

	private static Integer getCodeSegment(Contribuable contribuable, int anneePrecedente) {
		Integer codeSegment = null;
		final DeclarationImpotOrdinaire precedente = getDeclarationPrecedente(contribuable, anneePrecedente);
		if (precedente != null) {
			codeSegment = precedente.getCodeSegment();
		}
		return codeSegment;
	}

	private static DeclarationImpotOrdinaire getDeclarationPrecedente(Contribuable contribuable, int anneePrecedente) {
		DeclarationImpotOrdinaire precedenteDI = null;
		final List<Declaration> declarations = contribuable.getDeclarationsForPeriode(anneePrecedente, false);
		if (declarations != null && !declarations.isEmpty()) {
			Declaration precedenteDeclaration = declarations.get(declarations.size() - 1);
			if (precedenteDeclaration instanceof DeclarationImpotOrdinaire) {
				precedenteDI = (DeclarationImpotOrdinaire) precedenteDeclaration;
			}
		}
		return precedenteDI;
	}

	public static Qualification determineQualification(Contribuable contribuable, int annee) {

		Qualification qualification = getQualification(contribuable, annee - 1);
		if (qualification == null) {
			qualification = getQualification(contribuable, annee - 2);
		}

		return qualification;
	}

	public static Integer determineCodeSegment(Contribuable contribuable, int annee) {
		Integer codeSegment = getCodeSegment(contribuable, annee - 1);
		if (codeSegment == null) {
			codeSegment = getCodeSegment(contribuable, annee - 2);
		}
		return codeSegment;
	}

	protected PeriodeImposition(RegDate dateDebut, RegDate dateFin, CategorieEnvoiDI categorieEnvoiDI, Contribuable contribuable, Qualification qualification, Integer codeSegment,
	                          TypeAdresseRetour adresseRetour, CauseFermeture causeFermeture) {
		this(dateDebut, dateFin, categorieEnvoiDI, contribuable, qualification, codeSegment, adresseRetour, false, false, causeFermeture);
	}

	protected PeriodeImposition(RegDate dateDebut, RegDate dateFin, CategorieEnvoiDI categorieEnvoiDI, Contribuable contribuable, Qualification qualification, Integer codeSegment,
	                          TypeAdresseRetour adresseRetour, boolean optionnelle, boolean remplaceeParNote, CauseFermeture causeFermeture) {
		DateRangeHelper.assertValidRange(dateDebut, dateFin);
		this.debut = dateDebut;
		this.fin = dateFin;
		this.categorieEnvoiDI = categorieEnvoiDI;
		this.contribuable = contribuable;
		this.qualification = qualification;
		this.codeSegment = codeSegment;
		this.adresseRetour = adresseRetour;
		this.optionnelle = optionnelle;
		this.remplaceeParNote = remplaceeParNote;
		this.causeFermeture = causeFermeture;
	}

	@Override
	public RegDate getDateDebut() {
		return debut;
	}

	@Override
	public RegDate getDateFin() {
		return fin;
	}

	public CategorieEnvoiDI getCategorieEnvoiDI() {
		return categorieEnvoiDI;
	}

	public TypeContribuable getTypeContribuable() {
		return categorieEnvoiDI.getTypeContribuable();
	}

	public TypeDocument getTypeDocument() {
		return categorieEnvoiDI.getTypeDocument();
	}

	public boolean isOptionnelle() {
		return optionnelle;
	}

	public boolean isRemplaceeParNote() {
		return remplaceeParNote;
	}

	/**
	 * @return <b>vrai</b> si la période d'imposition est celle d'un diplomate suisse sans immeuble (et donc qui ne reçoit pas de déclaration d'impôt ordinaire).
	 */
	public boolean isDiplomateSuisseSansImmeuble() {
		return categorieEnvoiDI == CategorieEnvoiDI.DIPLOMATE_SUISSE;
	}

	public boolean isFermetureCauseDeces() {
		return causeFermeture == CauseFermeture.VEUVAGE_DECES;
	}

	public boolean isFermetureCauseFinAssujettissementHorsSuisse() {
		return causeFermeture == CauseFermeture.FIN_ASSUJETTISSEMENT_HS;
	}

	public Contribuable getContribuable() {
		return contribuable;
	}

	public Qualification getQualification() {
		return qualification;
	}

	public Integer getCodeSegment() {
		return codeSegment;
	}

	public TypeAdresseRetour getAdresseRetour() {
		return adresseRetour;
	}

	@Override
	public DateRange collate(DateRange n) {
		final PeriodeImposition next = (PeriodeImposition) n;
		final TypeAdresseRetour adresseRetour = next.adresseRetour; // [UNIREG-1741] en prenant le second type, on est aussi correct en cas de décès. 
		Assert.isTrue(isCollatable(next));
		return new PeriodeImposition(debut, next.getDateFin(), collateCategorieEnvoi(categorieEnvoiDI, next.categorieEnvoiDI), contribuable, qualification, codeSegment, adresseRetour,
				optionnelle && next.optionnelle, remplaceeParNote && next.remplaceeParNote, next.causeFermeture);
	}

	@Override
	public boolean isCollatable(DateRange n) {
		final PeriodeImposition next = (PeriodeImposition) n;
		return isEquivalent(categorieEnvoiDI, next.categorieEnvoiDI) && isRangeCollatable(next);
	}

	private boolean isRangeCollatable(final PeriodeImposition next) {
		// on accepte les ranges qui se touchent *et* ceux qui se chevauchent, ceci parce que les périodes d'impositions peuvent être plus
		// larges que les assujettissement sous-jacents (cas des HorsCanton et HorsSuisse) et qu'il s'agit de pouvoir les
		// collater malgré tout.
		return this.getDateFin() != null && this.getDateFin().getOneDayAfter().isAfterOrEqual(next.getDateDebut());
	}

	/**
	 * @param left  le type de gauche
	 * @param right le type de droite
	 * @return <b>vrai</b> si les deux types de documents sont égaux, en <b>ne faisant pas</b> de différence entre DECLARATION_IMPOT_COMPLETE et DECLARATION_IMPOT_VAUDTAX.
	 */
	private boolean isEquivalent(CategorieEnvoiDI left, CategorieEnvoiDI right) {
		return (left == right) || (isCompleteOuVaudTax(left.getTypeDocument()) && isCompleteOuVaudTax(right.getTypeDocument()));
	}

	private boolean isCompleteOuVaudTax(TypeDocument doc) {
		return doc == TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH || doc == TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL || doc == TypeDocument.DECLARATION_IMPOT_VAUDTAX;
	}

	private CategorieEnvoiDI collateCategorieEnvoi(CategorieEnvoiDI left, CategorieEnvoiDI right) {
		// Dans la plupart des cas, on prend le type de contribuable le plus à jour, c'est-à-dire la valeur 'right'.
		// Sauf lorsque on a le choix entre en déclaration d'impôt vaudtax et une complète; dans ce cas on préfère la vaudtax.
		if (left == CategorieEnvoiDI.VAUDOIS_VAUDTAX && right == CategorieEnvoiDI.VAUDOIS_COMPLETE) {
			return CategorieEnvoiDI.VAUDOIS_VAUDTAX;
		}
		else {
			return right;
		}
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, debut, fin, NullDateBehavior.LATEST);
	}

	@Override
	public String toString() {
		return String.format("PeriodeImposition{debut=%s, fin=%s, categorieEnvoiDI=%s, optionnelle=%s, remplaceeParNote=%s}", debut, fin, categorieEnvoiDI, optionnelle, remplaceeParNote);
	}
}
