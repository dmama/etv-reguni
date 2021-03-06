package ch.vd.unireg.metier.assujettissement;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.metier.common.DecalageDateHelper;
import ch.vd.unireg.metier.common.ForFiscalPrincipalContext;
import ch.vd.unireg.metier.common.Fraction;
import ch.vd.unireg.metier.common.FractionDecalee;
import ch.vd.unireg.metier.common.FractionSimple;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;

import static ch.vd.unireg.metier.assujettissement.AssujettissementPersonnesPhysiquesCalculator.isDepartDansHorsCanton;
import static ch.vd.unireg.metier.assujettissement.AssujettissementPersonnesPhysiquesCalculator.isDepartDepuisOuArriveeVersVaud;
import static ch.vd.unireg.metier.assujettissement.AssujettissementPersonnesPhysiquesCalculator.isDepartHCApresArriveHSMemeAnnee;
import static ch.vd.unireg.metier.assujettissement.AssujettissementPersonnesPhysiquesCalculator.isDepartOuArriveeHorsSuisse;
import static ch.vd.unireg.metier.assujettissement.AssujettissementPersonnesPhysiquesCalculator.isMixte2Vaudois;
import static ch.vd.unireg.metier.assujettissement.AssujettissementPersonnesPhysiquesCalculator.isRattachementEconomiqueContinu;

public class FractionnementsRole extends FractionnementsAssujettissementPP {

	private static final int PREMIERE_ANNEE_DECALAGE_FIN_MOIS_POUR_MIXTE2_PARTI_HC = 2014;

	public FractionnementsRole(@NotNull List<ForFiscalPrincipalPP> principaux, @NotNull List<ForFiscalSecondaire> secondaires) {
		super(principaux, secondaires);
	}

	@Override
	protected Fraction isFractionOuverture(@NotNull ForFiscalPrincipalContext<ForFiscalPrincipalPP> forPrincipal, @NotNull List<ForFiscalSecondaire> secondaires) {
		final ForFiscalPrincipalPP previous = forPrincipal.getPrevious();
		final ForFiscalPrincipalPP current = forPrincipal.getCurrent();

		final MotifFor motifOuverture = current.getMotifOuverture();

		Fraction fraction = null;

		if (motifOuverture == MotifFor.VEUVAGE_DECES) {
			// fractionnement systématique à la date d'ouverture pour ce motif
			fraction = new FractionSimple(current.getDateDebut(), motifOuverture, null);
		}
		else if (isDepartOuArriveeHorsSuisse(previous, current) && isFractionDepartOuArriveeHorsSuisse(forPrincipal.slideToPrevious(), forPrincipal, secondaires)) {
			// fractionnement en cas de départ HS significatif
			fraction = new FractionSimple(current.getDateDebut(), motifOuverture, null);
		}
		else if ((previous == null || previous.getModeImposition() == ModeImposition.SOURCE) && current.getModeImposition().isRole() && motifOuverture == MotifFor.PERMIS_C_SUISSE) {
			// [SIFISC-8095] l'obtention d'un permis C ou nationalité suisse doit fractionner la période d'assujettissement *mais* avec un décalage au 1er du mois suivant
			final RegDate dateFraction = DecalageDateHelper.getDateDebutAssujettissementOrdinaireApresPermisCNationaliteSuisse(current.getDateDebut());
			fraction = new FractionDecalee(dateFraction, new DateRangeHelper.Range(current.getDateDebut(), dateFraction), motifOuverture, null);
		}

		checkMotifSurFractionOuverture(forPrincipal, fraction);
		return fraction;
	}

	@Override
	protected Fraction isFractionFermeture(@NotNull ForFiscalPrincipalContext<ForFiscalPrincipalPP> forPrincipal, @NotNull List<ForFiscalSecondaire> secondaires) {
		final ForFiscalPrincipalPP current = forPrincipal.getCurrent();
		final ForFiscalPrincipalPP next = forPrincipal.getNext();

		if (current.getDateFin() == null) {
			return null;
		}

		final MotifFor motifFermeture = current.getMotifFermeture();

		Fraction fraction = null;

		if (motifFermeture == MotifFor.VEUVAGE_DECES) {
			// fractionnement systématique à la date de fermeture pour ce motif
			fraction = new FractionSimple(current.getDateFin().getOneDayAfter(), null, motifFermeture);
		}
		else if (isDepartOuArriveeHorsSuisse(current, next) && isFractionDepartOuArriveeHorsSuisse(forPrincipal, forPrincipal.slideToNext(), secondaires)) {
			// fractionnement en cas de départ HS significatif
			fraction = new FractionSimple(current.getDateFin().getOneDayAfter(), null, motifFermeture);
		}
		else if (isDepartDansHorsCanton(current, next) && current.getModeImposition() == ModeImposition.MIXTE_137_2) {
			// [SIFISC-7281] le départ hors-canton d'un sourcier mixte 137 al2 doit fractionner la période d'assujettissement
			// [SIFISC-10365] dès 2014, ce fractionnement est décalé à la fin du mois
			if (current.getDateFin().year() < PREMIERE_ANNEE_DECALAGE_FIN_MOIS_POUR_MIXTE2_PARTI_HC) {
				fraction = new FractionSimple(current.getDateFin().getOneDayAfter(), null, motifFermeture);
			}
			else {
				// [SIFISC-21684] la fraction décalée n'est active qu'à partir du lendemain du départ (= à partir du jour d'arrivée dans l'autre canton)
				final RegDate dateFraction = current.getDateFin().getLastDayOfTheMonth().getOneDayAfter();
				fraction = new FractionDecalee(dateFraction, new DateRangeHelper.Range(current.getDateFin().getOneDayAfter(), dateFraction), motifFermeture, null);
			}
		}

		return fraction;
	}

	/**
	 * Détermine si le départ ou l'arrivée hors-Suisse détectée doit provoquer un fractionnement.
	 *
	 * @param before      le for principal avant le départ HS et son context.
	 * @param after       le for principal après le départ HS et son context.
	 * @param secondaires les fors secondaires du tiers
	 * @return <i>vrai</i> s'il faut fractionner l'assujettissement; <i>false</i> autrement.
	 */
	protected static boolean isFractionDepartOuArriveeHorsSuisse(@NotNull ForFiscalPrincipalContext<ForFiscalPrincipalPP> before,
	                                                             @NotNull ForFiscalPrincipalContext<ForFiscalPrincipalPP> after,
	                                                             @NotNull List<ForFiscalSecondaire> secondaires) {
		// De manière générale, les transitions Suisse <-> Hors-Suisse provoquent des fractionnements
		// [UNIREG-1742] le départ hors-Suisse depuis hors-canton ne doit pas fractionner la période d'assujettissement (car le rattachement économique n'est pas interrompu)
		// [SIFISC-17170] le départ hors-Suisse depuis hors-canton doit quand même fractionner la période d'assujettissement si le rattachement économique n'est pas continu avant et après le départ
		// [UNIREG-2759] l'arrivée de hors-Suisse ne doit pas fractionner si le for se ferme dans la même année avec un départ hors-canton
		// [SIFISC-14388] le cas d'une arrivée HS de mixte 2 doit fractionner à l'arrivée, même en cas de départ HC ensuite dans la même année
		final RegDate dateFin = (before.getCurrent() == null ? after.getCurrent().getDateDebut() : before.getCurrent().getDateFin());
		return (isDepartDepuisOuArriveeVersVaud(before) || !isRattachementEconomiqueContinu(secondaires, dateFin))
				&& (!isDepartHCApresArriveHSMemeAnnee(after) || isMixte2Vaudois(after));
	}
}
