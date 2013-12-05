package ch.vd.uniregctb.metier.assujettissement;

import java.util.List;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.metier.common.DecalageDateHelper;
import ch.vd.uniregctb.metier.common.ForFiscalPrincipalContext;
import ch.vd.uniregctb.metier.common.Fraction;
import ch.vd.uniregctb.metier.common.FractionDecalee;
import ch.vd.uniregctb.metier.common.FractionSimple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;

public class FractionnementsRole extends FractionnementsAssujettissement {

	private static final int PREMIERE_ANNEE_DECALAGE_FIN_MOIS_POUR_MIXTE2_PARTI_HC = 2014;

	public FractionnementsRole(List<ForFiscalPrincipal> principaux) {
		super(principaux);
	}

	@Override
	protected Fraction isFractionOuverture(ForFiscalPrincipalContext forPrincipal) {
		final ForFiscalPrincipal previous = forPrincipal.previous;
		final ForFiscalPrincipal current = forPrincipal.current;
		final ForFiscalPrincipal next = forPrincipal.next;

		final MotifFor motifOuverture = current.getMotifOuverture();

		Fraction fraction = null;

		if (motifOuverture == MotifFor.VEUVAGE_DECES) {
			// fractionnement systématique à la date d'ouverture pour ce motif
			fraction = new FractionSimple(current.getDateDebut(), motifOuverture, null);
		}
		else if (AssujettissementServiceImpl.isDepartOuArriveeHorsSuisse(previous, current) &&
				AssujettissementServiceImpl.isDepartDepuisOuArriveeVersVaud(current, previous) &&
				!AssujettissementServiceImpl.isDepartHCApresArriveHSMemeAnnee(current, next)) {
			// De manière générale, les transitions Suisse <-> Hors-Suisse provoquent des fractionnements
			// [UNIREG-1742] le départ hors-Suisse depuis hors-canton ne doit pas fractionner la période d'assujettissement (car le rattachement économique n'est pas interrompu)
			// [UNIREG-2759] l'arrivée de hors-Suisse ne doit pas fractionner si le for se ferme dans la même année avec un départ hors-canton
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
	protected Fraction isFractionFermeture(ForFiscalPrincipalContext forPrincipal) {
		final ForFiscalPrincipal current = forPrincipal.current;
		final ForFiscalPrincipal next = forPrincipal.next;

		if (current.getDateFin() == null) {
			return null;
		}

		final MotifFor motifFermeture = current.getMotifFermeture();

		Fraction fraction = null;

		if (motifFermeture == MotifFor.VEUVAGE_DECES) {
			// fractionnement systématique à la date de fermeture pour ce motif
			fraction = new FractionSimple(current.getDateFin().getOneDayAfter(), null, motifFermeture);
		}
		else if (AssujettissementServiceImpl.isDepartOuArriveeHorsSuisse(current, next) &&
				AssujettissementServiceImpl.isDepartDepuisOuArriveeVersVaud(current, next) &&
				!AssujettissementServiceImpl.isDepartHCApresArriveHSMemeAnnee(next, forPrincipal.nextnext)) {
			// De manière générale, les transitions Suisse <-> Hors-Suisse provoquent des fractionnements
			// [UNIREG-1742] le départ hors-Suisse depuis hors-canton ne doit pas fractionner la période d'assujettissement (car le rattachement économique n'est pas interrompu)
			// [UNIREG-2759] l'arrivée de hors-Suisse ne doit pas fractionner si le for se ferme dans la même année avec un départ hors-canton
			fraction = new FractionSimple(current.getDateFin().getOneDayAfter(), null, motifFermeture);
		}
		else if (AssujettissementServiceImpl.isDepartDansHorsCanton(current, next) && current.getModeImposition() == ModeImposition.MIXTE_137_2) {
			// [SIFISC-7281] le départ hors-canton d'un sourcier mixte 137 al2 doit fractionner la période d'assujettissement
			// [SIFISC-10365] dès 2014, ce fractionnement est décalé à la fin du mois
			if (current.getDateFin().year() < PREMIERE_ANNEE_DECALAGE_FIN_MOIS_POUR_MIXTE2_PARTI_HC) {
				fraction = new FractionSimple(current.getDateFin().getOneDayAfter(), null, motifFermeture);
			}
			else {
				final RegDate dateFraction = current.getDateFin().getLastDayOfTheMonth().getOneDayAfter();
				fraction = new FractionDecalee(dateFraction, new DateRangeHelper.Range(current.getDateFin(), dateFraction), motifFermeture, null);
			}
		}

		return fraction;
	}
}
