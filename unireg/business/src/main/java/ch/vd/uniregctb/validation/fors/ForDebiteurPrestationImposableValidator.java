package ch.vd.uniregctb.validation.fors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.validation.periodicite.PeriodiciteValidator;

public class ForDebiteurPrestationImposableValidator extends ForFiscalAvecMotifsValidator<ForDebiteurPrestationImposable> {

	private static final Set<MotifFor> ALLOWED_OPENING_CAUSES = EnumSet.of(MotifFor.INDETERMINE, MotifFor.DEBUT_PRESTATION_IS, MotifFor.FUSION_COMMUNES, MotifFor.REACTIVATION, MotifFor.DEMENAGEMENT_SIEGE);
	private static final Set<MotifFor> ALLOWED_CLOSING_CAUSES = EnumSet.of(MotifFor.INDETERMINE, MotifFor.FIN_PRESTATION_IS, MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE, MotifFor.FUSION_COMMUNES, MotifFor.ANNULATION, MotifFor.DEMENAGEMENT_SIEGE);

	private static final Set<MotifFor> OPENING_MONTH_BEGINNING = EnumSet.of(MotifFor.DEBUT_PRESTATION_IS, MotifFor.DEMENAGEMENT_SIEGE);

	@Override
	protected Class<ForDebiteurPrestationImposable> getValidatedClass() {
		return ForDebiteurPrestationImposable.class;
	}

	/**
	 * [SIFISC-10141] les fors DPI peuvent avoir des dates de fermeture dans le futur
	 * @return <code>true</code>
	 */
	@Override
	protected boolean isDateFinFutureAllowed() {
		return true;
	}

	@Override
	public ValidationResults validate(ForDebiteurPrestationImposable ff) {
		final ValidationResults vr = super.validate(ff);
		if (!ff.isAnnule()) {

			if (ff.getGenreImpot() != GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE) {
				vr.addError("Par définition, le genre d'impôt d'un for fiscal 'débiteur prestation imposable' doit être DEBITEUR_PRESTATION_IMPOSABLE.");
			}

			final TypeAutoriteFiscale typeAutoriteFiscale = ff.getTypeAutoriteFiscale();
			if (typeAutoriteFiscale != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && typeAutoriteFiscale != TypeAutoriteFiscale.COMMUNE_HC) {
				vr.addError("Par définition, le type d'autorité fiscale d'un for fiscal 'débiteur prestation imposable' est limité à COMMUNE_OU_FRACTION_VD ou COMMUNE_HC");
			}

			// [SIFISC-8712] ensemble des valeurs autorisées pour les motifs d'ouverture/de fermeture
			if (ff.getMotifOuverture() != null && !ALLOWED_OPENING_CAUSES.contains(ff.getMotifOuverture())) {
				vr.addError("Le motif d'ouverture '" + ff.getMotifOuverture().getDescription(true) + "' n'est pas autorisé sur les fors fiscaux 'débiteur prestation imposable'.");
			}
			if (ff.getMotifFermeture() != null && !ALLOWED_CLOSING_CAUSES.contains(ff.getMotifFermeture())) {
				vr.addError("Le motif de fermeture '" + ff.getMotifFermeture().getDescription(false) + "' n'est pas autorisé sur les fors fiscaux 'débiteur prestation imposable'.");
			}

			// [SIFISC-8712] le motif d'ouverture est obligatoire
			if (ff.getMotifOuverture() == null) {
				vr.addError("Le motif d'ouverture est une donnée obligatoire sur les fors fiscaux 'débiteur prestation imposable'.");
			}
			// [SIFISC-8712] conformité du motif d'ouverture avec la date correspondante
			else if (ff.getDateDebut() != null && OPENING_MONTH_BEGINNING.contains(ff.getMotifOuverture()) && ff.getDateDebut().day() != 1) {
				vr.addError("Les fors ouverts avec le motif '" + ff.getMotifOuverture().getDescription(true) + "' doivent commencer un premier jour du mois.");
			}

			// [SIFISC-12888] les dates de fermeture des fors sont assouplies
			if (ff.getDateFin() != null) {

				// [SIFISC-8712] le motif de fermeture est obligatoire si le for est fermé
				if (ff.getMotifFermeture() == null) {
					vr.addError("Le motif de fermeture est une donnée obligatoire sur les fors fiscaux 'débiteur prestation imposable' fermés.");
				}

				// on ne fait le test que si la date de début est renseignée
				if (ff.getDateDebut() != null) {
					final Set<RegDate> finsAutorisees = getDatesFermetureAutorisees((DebiteurPrestationImposable) ff.getTiers(), ff, ff.getDateFin(), true);
					if (!finsAutorisees.contains(ff.getDateFin())) {
						vr.addError(String.format("La date de fermeture du for débiteur %s est incohérente avec sa date de début ainsi que les LR et périodicités du débiteur.", getEntityDisplayString(ff)));
					}
				}
			}
		}
		return vr;
	}

	/**
	 * Méthode utilitaire qui permet de savoir quelles sont les dates de fermeture possibles d'un for de débiteur IS
	 * @param dpi le débiteur lui-même
	 * @param fdpi le for que l'on cherche à fermer
	 * @param maxAllowed la date maximale de fermeture autorisée
	 * @param ignoreDateFermetureActuelle <code>true</code> s'il faut calculer une liste même en présence d'un for déjà fermé (si <code>false</code>, dans le cas d'un for déjà fermé, seule cette date sera dans la liste)
	 * @return une liste (potentiellement vide) des dates possibles de fermeture
	 */
	public static SortedSet<RegDate> getDatesFermetureAutorisees(DebiteurPrestationImposable dpi, ForDebiteurPrestationImposable fdpi, RegDate maxAllowed, boolean ignoreDateFermetureActuelle) {
		if (dpi != fdpi.getTiers()) {
			throw new IllegalArgumentException("Le for donné n'appartient pas au débiteur donné...");
		}

		// un for déjà fermé n'a qu'une date de fermeture possible...
		if (!ignoreDateFermetureActuelle && fdpi.getDateFin() != null) {
			return new TreeSet<>(Collections.singleton(fdpi.getDateFin()));
		}

		return getDatesFermetureAutorisees(dpi, fdpi.getDateDebut(), maxAllowed, dpi.getPeriodicitesNonAnnulees(true));
	}

	private static final SortedSet<RegDate> EMPTY_DATE_SET = Collections.unmodifiableSortedSet(new TreeSet<>());

	/**
	 * Méthode utilitaire qui permet de savoir quelles sont les dates de fermeture possibles d'un for de débiteur IS
	 * @param dpi le débiteur lui-même
	 * @param dateDebutFor le for que l'on cherche à fermer
	 * @param maxAllowed la date maximale de fermeture autorisée
	 * @param periodicites la liste des périodicités à prendre en compte pour le calcul des dates de fermetures valides
	 * @return un ensemble (trié, mais potentiellement vide) des dates possibles de fermeture
	 */
	@NotNull
	public static SortedSet<RegDate> getDatesFermetureAutorisees(DebiteurPrestationImposable dpi, RegDate dateDebutFor, RegDate maxAllowed, List<Periodicite> periodicites) {
		if (maxAllowed == null) {
			throw new IllegalArgumentException("maxAllowed must not be null!");
		}

		if (dateDebutFor == null || dateDebutFor.isAfter(maxAllowed)) {
			// je ne sais pas répondre...
			return EMPTY_DATE_SET;
		}

		// le for est donc ouvert... et sa date de fermeture...
		// - ne peut être plus tard que la date maximale donnée
		// - doit suivre les LR et périodicités existantes

		// on regarde la zone continue depuis "dateDebutFor" jusqu'à un éventuel for suivant ou la date max
		final List<ForFiscal> forsTries = dpi.getForsFiscauxNonAnnules(true);
		final List<DateRange> fors = new ArrayList<>(forsTries.size());
		for (ForFiscal ff : forsTries) {
			if (RegDateHelper.isAfter(ff.getDateDebut(), dateDebutFor, NullDateBehavior.EARLIEST)) {
				fors.add(ff);
			}
		}
		final DateRangeHelper.Range futur = new DateRangeHelper.Range(dateDebutFor, null);
		final List<DateRange> trousFuturs = DateRangeHelper.subtract(futur, fors);
		if (trousFuturs.isEmpty()) {
			// aucune date de fermeture possible
			return EMPTY_DATE_SET;
		}
		final DateRange premierTrou = trousFuturs.get(0);
		if (!premierTrou.isValidAt(dateDebutFor)) {
			throw new RuntimeException("Le premier trou devrait contenir la date de début du for à fermer...");
		}

		// construction de la liste des périodes postérieures à dateDebutFor et couvertes par des LR
		// -> parce que le for ne peut être fermé avant la fin des LR qui sont dans le trou
		final List<DeclarationImpotSource> lrsTriees = dpi.getDeclarationsTriees(DeclarationImpotSource.class, false);
		RegDate candidate = null;
		for (DeclarationImpotSource lr : CollectionsUtils.revertedOrder(lrsTriees)) {
			if (DateRangeHelper.intersect(lr, premierTrou)) {
				candidate = lr.getDateDebut().getOneDayBefore();
				break;
			}
		}
		if (candidate == null) {
			candidate = dateDebutFor.getOneDayBefore();
		}

		final SortedSet<RegDate> datesCandidates = new TreeSet<>();
		while (true) {
			final Periodicite p = DateRangeHelper.rangeAt(periodicites, candidate.getOneDayAfter());
			if (p == null || PeriodiciteValidator.validatePeriodicite(p).hasErrors()) {
				break;
			}
			candidate = p.getFinPeriode(candidate.getOneDayAfter());
			if (candidate.isAfter(maxAllowed) || !premierTrou.isValidAt(candidate)) {
				break;
			}
			datesCandidates.add(candidate);
		}

		// pour finir un trou entre deux fors, il est parfois nécessaire d'ajouter une date de fin qui ne colle pas forcément aux périodicités
		final RegDate dateFinTrou = premierTrou.getDateFin();
		if (dateFinTrou != null && dateFinTrou.isBeforeOrEqual(maxAllowed)) {
			datesCandidates.add(dateFinTrou);
		}

		return datesCandidates;
	}
}
