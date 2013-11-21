package ch.vd.uniregctb.metier.piis;

import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.common.ForFiscalPrincipalContext;
import ch.vd.uniregctb.metier.common.Fraction;
import ch.vd.uniregctb.metier.common.FractionContrariante;
import ch.vd.uniregctb.metier.common.FractionDecalee;
import ch.vd.uniregctb.metier.common.Fractionnements;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;

public class FractionnementsPeriodesImpositionIS implements Iterable<Fraction> {

	private final int pf;
	private final ServiceInfrastructureService infraService;
	private final Fractionnements fractionnements;

	public FractionnementsPeriodesImpositionIS(List<ForFiscalPrincipal> principaux, int pf, ServiceInfrastructureService infraService) {
		this.pf = pf;
		this.infraService = infraService;
		this.fractionnements = new Fractionnements(principaux) {
			@Override
			protected Fraction isFractionOuverture(ForFiscalPrincipalContext forPrincipal) {
				return FractionnementsPeriodesImpositionIS.this.isFractionOuverture(forPrincipal);
			}

			@Override
			protected Fraction isFractionFermeture(ForFiscalPrincipalContext forPrincipal) {
				return FractionnementsPeriodesImpositionIS.this.isFractionFermeture(forPrincipal);
			}
		};
	}

	@NotNull
	private Localisation getLocalisation(ForFiscalPrincipal forPrincipal) {
		if (forPrincipal == null) {
			return Localisation.get(null, infraService);
		}
		else {
			return Localisation.get(forPrincipal.getNumeroOfsAutoriteFiscale(), forPrincipal.getDateDebut(), forPrincipal.getTypeAutoriteFiscale(), infraService);
		}
	}

	@Nullable
	protected static MotifFor getMotifEffectif(Localisation localisationAvant, MotifFor motifFermeture, Localisation localisationApres, MotifFor motifOuverture) {
		// changement de localisation -> on se moque des motifs
		if (!localisationAvant.isInconnue() && !localisationApres.isInconnue() && !localisationAvant.equals(localisationApres)) {
			if (localisationApres.isHS()) {
				return MotifFor.DEPART_HS;
			}
			else if (localisationAvant.isHS()) {
				return MotifFor.ARRIVEE_HS;
			}
			else if (localisationApres.isHC()) {
				return MotifFor.DEPART_HC;
			}
			else if (localisationAvant.isHC()) {
				return MotifFor.ARRIVEE_HC;
			}
			else {
				Assert.fail("On ne devrait jamais arriver ici avec des localisations différentes avant et après...");
			}
		}

		// à un moment, il faut savoir faire confiance aux données...
		final MotifFor motif = Fraction.getMotifEffectif(motifFermeture, motifOuverture);
		if (localisationApres.isInconnue() || localisationAvant.isInconnue()) {
			// on ne sait pas mieux faire que ça, de toute façon...
			return motif;
		}
		else if (motif == MotifFor.ARRIVEE_HC || motif == MotifFor.DEPART_HC || motif == MotifFor.ARRIVEE_HS || motif == MotifFor.DEPART_HS) {
			// ici, on a des mouvements à l'intérieur d'un même canton ou pays qui sont qualifiés de départ/arrivée -> mettons déménagement, plutôt...
			return MotifFor.DEMENAGEMENT_VD;
		}
		else {
			return motif;
		}
	}

	private Fraction isFractionOuverture(ForFiscalPrincipalContext forPrincipal) {
		final ForFiscalPrincipal current = forPrincipal.current;
		final ForFiscalPrincipal previous = forPrincipal.previous;

		Fraction fraction = null;

		// pas de fractionnement au début d'un for qui a déjà commencé sur une autre PF
		if (current.getDateDebut().year() == pf) {

			// décès -> fractionnement à la date pile
			if (current.getMotifOuverture() == MotifFor.VEUVAGE_DECES) {
				fraction = new FractionContrariante(current.getDateDebut(), MotifFor.VEUVAGE_DECES, null);
			}
			else {
				final Localisation currentLocalisation = getLocalisation(current);
				final Localisation previousLocalisation = getLocalisation(previous);
				final MotifFor motifEffectif = getMotifEffectif(previousLocalisation, previous != null ? previous.getMotifFermeture() : null, currentLocalisation, current.getMotifOuverture());

				if (motifEffectif == MotifFor.DEPART_HS || motifEffectif == MotifFor.ARRIVEE_HS) {
					fraction = new FractionContrariante(current.getDateDebut(), motifEffectif, null);
				}
				else if (motifEffectif == MotifFor.DEPART_HC || motifEffectif == MotifFor.ARRIVEE_HC) {
					final RegDate dateFraction = current.getDateDebut().getLastDayOfTheMonth().getOneDayAfter();
					fraction = new FractionDecalee(dateFraction, new DateRangeHelper.Range(current.getDateDebut(), dateFraction), motifEffectif, null);
				}
				else if (previous != null && previous.getModeImposition() == ModeImposition.SOURCE && !current.getModeImposition().isSource() && motifEffectif == MotifFor.PERMIS_C_SUISSE) {
					// attention, pas tout à fait pareil que le mariage, puisque pour le moment, l'obtention du permis C le 1er jour du mois
					// provoque un assujettissement ordinaire immédiat (à terme, il faudra changer ça...)
	                final RegDate dateFraction = current.getDateDebut().getOneDayBefore().getLastDayOfTheMonth().getOneDayAfter();
					fraction = new FractionDecalee(dateFraction, new DateRangeHelper.Range(current.getDateDebut(), dateFraction), motifEffectif, null);
				}
				else if (previous != null && previous.getModeImposition() == ModeImposition.SOURCE && !current.getModeImposition().isSource() && motifEffectif == MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION) {
	                final RegDate dateFraction = current.getDateDebut().getLastDayOfTheMonth().getOneDayAfter();
					fraction = new FractionDecalee(dateFraction, new DateRangeHelper.Range(current.getDateDebut(), dateFraction), motifEffectif, null);
				}
			}
		}

		return fraction;
	}

	private Fraction isFractionFermeture(ForFiscalPrincipalContext forPrincipal) {

		final ForFiscalPrincipal current = forPrincipal.current;
		final ForFiscalPrincipal next = forPrincipal.next;

		Fraction fraction = null;

		// pas de fractionnement sur un for ouvert ou fermé après la fin de la PF
		if (current.getDateFin() != null && current.getDateFin().year() == pf) {

			// décès -> fractionnement à la date pile
			if (current.getMotifFermeture() == MotifFor.VEUVAGE_DECES) {
				fraction = new FractionContrariante(current.getDateFin().getOneDayAfter(), null, MotifFor.VEUVAGE_DECES);
			}
			else {
				final Localisation currentLocalisation = getLocalisation(current);
				final Localisation nextLocalisation = getLocalisation(next);
				final MotifFor motifEffectif = getMotifEffectif(currentLocalisation, current.getMotifFermeture(), nextLocalisation, next != null ? next.getMotifOuverture() : null);

				if (motifEffectif == MotifFor.DEPART_HS || motifEffectif == MotifFor.ARRIVEE_HS) {
					fraction = new FractionContrariante(current.getDateFin().getOneDayAfter(), null, motifEffectif);
				}
				else if (motifEffectif == MotifFor.DEPART_HC || motifEffectif == MotifFor.ARRIVEE_HC) {
					final RegDate dateFraction = current.getDateFin().getOneDayAfter().getLastDayOfTheMonth().getOneDayAfter();
					fraction = new FractionDecalee(dateFraction, new DateRangeHelper.Range(current.getDateFin(), dateFraction), null, motifEffectif);
				}
				else if (current.getModeImposition() == ModeImposition.SOURCE && next != null && !next.getModeImposition().isSource() && motifEffectif == MotifFor.PERMIS_C_SUISSE) {
					// attention, pas tout à fait pareil que le mariage, puisque pour le moment, l'obtention du permis C le 1er jour du mois
					// provoque un assujettissement ordinaire immédiat (à terme, il faudra changer ça...)
					final RegDate dateFraction = current.getDateFin().getLastDayOfTheMonth().getOneDayAfter();
					fraction = new FractionDecalee(dateFraction, new DateRangeHelper.Range(current.getDateFin(), dateFraction), null, motifEffectif);
				}
				else if (current.getModeImposition() == ModeImposition.SOURCE && next != null && !next.getModeImposition().isSource() && motifEffectif == MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION) {
					final RegDate dateFraction = current.getDateFin().getOneDayAfter().getLastDayOfTheMonth().getOneDayAfter();
					fraction = new FractionDecalee(dateFraction, new DateRangeHelper.Range(current.getDateFin(), dateFraction), null, motifEffectif);
				}
			}
		}

		return fraction;
	}

	@Override
	public Iterator<Fraction> iterator() {
		return fractionnements.iterator();
	}

	public boolean isEmpty() {
		return fractionnements.isEmpty();
	}

	@Nullable
	public Fraction getAt(RegDate date) {
		return fractionnements.getAt(date);
	}
}
