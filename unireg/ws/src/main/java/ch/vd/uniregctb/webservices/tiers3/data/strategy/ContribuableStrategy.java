package ch.vd.uniregctb.webservices.tiers3.data.strategy;

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.situationfamille.VueSituationFamille;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.webservices.tiers3.BusinessExceptionCode;
import ch.vd.uniregctb.webservices.tiers3.Contribuable;
import ch.vd.uniregctb.webservices.tiers3.PeriodeImposition;
import ch.vd.uniregctb.webservices.tiers3.TiersPart;
import ch.vd.uniregctb.webservices.tiers3.WebServiceException;
import ch.vd.uniregctb.webservices.tiers3.data.AssujettissementBuilder;
import ch.vd.uniregctb.webservices.tiers3.data.PeriodeAssujettissementBuilder;
import ch.vd.uniregctb.webservices.tiers3.data.PeriodeImpositionBuilder;
import ch.vd.uniregctb.webservices.tiers3.data.SituationFamilleBuilder;
import ch.vd.uniregctb.webservices.tiers3.impl.Context;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.ExceptionHelper;

public abstract class ContribuableStrategy<T extends Contribuable> extends TiersStrategy<T> {

	private static final Logger LOGGER = Logger.getLogger(ContribuableStrategy.class);

	@Override
	protected void initParts(T to, Tiers from, @Nullable Set<TiersPart> parts, Context context) throws WebServiceException {
		super.initParts(to, from, parts, context);

		ch.vd.uniregctb.tiers.Contribuable ctb=(ch.vd.uniregctb.tiers.Contribuable) from;
		if (parts != null && parts.contains(TiersPart.SITUATIONS_FAMILLE)) {
			initSituationsFamille(to, ctb, context);
		}

		if (parts != null && (parts.contains(TiersPart.ASSUJETTISSEMENTS_ROLE) || parts.contains(TiersPart.PERIODES_ASSUJETTISSEMENT))) {
			initAssujettissements(to, ctb, parts);
		}

		if (parts != null && parts.contains(TiersPart.PERIODES_IMPOSITION)) {
			initPeriodesImposition(to, ctb, context);
		}
	}

	@Override
	protected void copyParts(T to, T from, @Nullable Set<TiersPart> parts, CopyMode mode) {
		super.copyParts(to, from, parts, mode);

		if (parts != null && parts.contains(TiersPart.SITUATIONS_FAMILLE)) {
			copyColl(to.getSituationsFamille(), from.getSituationsFamille());
		}

		if (parts != null && parts.contains(TiersPart.ASSUJETTISSEMENTS_ROLE)) {
			copyColl(to.getAssujettissementsRole(), from.getAssujettissementsRole());
		}

		if (parts != null && parts.contains(TiersPart.PERIODES_ASSUJETTISSEMENT)) {
			copyColl(to.getPeriodesAssujettissementLIC(), from.getPeriodesAssujettissementLIC());
			copyColl(to.getPeriodesAssujettissementLIFD(), from.getPeriodesAssujettissementLIFD());
		}

		if (parts != null && parts.contains(TiersPart.PERIODES_IMPOSITION)) {
			copyColl(to.getPeriodesImposition(), from.getPeriodesImposition());
		}
	}

	private static void initSituationsFamille(Contribuable left, ch.vd.uniregctb.tiers.Contribuable contribuable, Context context) {

		final List<VueSituationFamille> situations = context.situationService.getVueHisto(contribuable);

		for (ch.vd.uniregctb.situationfamille.VueSituationFamille situation : situations) {
			left.getSituationsFamille().add(SituationFamilleBuilder.newSituationFamille(situation));
		}
	}

	private static void initAssujettissements(Contribuable left, ch.vd.uniregctb.tiers.Contribuable right, Set<TiersPart> parts) throws WebServiceException {
		/*
		 * Note: il est nécessaire de calculer l'assujettissement sur TOUTE la période de validité du contribuable pour obtenir un résultat
		 * correct avec le collate.
		 */
		final List<ch.vd.uniregctb.metier.assujettissement.Assujettissement> list;
		try {
			list = ch.vd.uniregctb.metier.assujettissement.Assujettissement.determine(right, null, true /* collate */);
		}
		catch (AssujettissementException e) {
			LOGGER.error(e, e);
			throw ExceptionHelper.newBusinessException(e, BusinessExceptionCode.ASSUJETTISSEMENT);
		}

		if (list != null) {

			final boolean wantAssujettissements = parts.contains(TiersPart.ASSUJETTISSEMENTS_ROLE);
			final boolean wantPeriodes = parts.contains(TiersPart.PERIODES_ASSUJETTISSEMENT);

			for (ch.vd.uniregctb.metier.assujettissement.Assujettissement a : list) {
				if (wantAssujettissements) {
					left.getAssujettissementsRole().add(AssujettissementBuilder.newAssujettissement(a));
				}
				if (wantPeriodes) {
					left.getPeriodesAssujettissementLIC().add(PeriodeAssujettissementBuilder.toLIC(a));
					left.getPeriodesAssujettissementLIFD().add(PeriodeAssujettissementBuilder.toLIFD(a));
				}
			}
		}
	}

	private static void initPeriodesImposition(Contribuable left, ch.vd.uniregctb.tiers.Contribuable contribuable, Context context)
			throws WebServiceException {

		// [UNIREG-913] On n'expose pas les périodes fiscales avant la première période définie dans les paramètres
		final int premierePeriodeFiscale = context.parametreService.getPremierePeriodeFiscale();
		final DateRangeHelper.Range range = new DateRangeHelper.Range(RegDate.get(premierePeriodeFiscale, 1, 1), RegDate.get(RegDate.get().year(), 12, 31));

		final List<ch.vd.uniregctb.metier.assujettissement.PeriodeImposition> list;
		try {
			list = ch.vd.uniregctb.metier.assujettissement.PeriodeImposition.determine(contribuable, range);
		}
		catch (AssujettissementException e) {
			LOGGER.error(e, e);
			throw ExceptionHelper.newBusinessException(e, BusinessExceptionCode.ASSUJETTISSEMENT);
		}
		if (list != null) {
			PeriodeImposition derniere = null;
			for (ch.vd.uniregctb.metier.assujettissement.PeriodeImposition p : list) {
				final PeriodeImposition periode = PeriodeImpositionBuilder.newPeriodeImposition(p);
				left.getPeriodesImposition().add(periode);
				derniere = periode;
			}
			// [UNIREG-910] la période d'imposition courante est laissée ouverte
			if (derniere != null && derniere.getDateFin() != null) {
				final RegDate aujourdhui = RegDate.get();
				final RegDate dateFin = DataHelper.webToCore(derniere.getDateFin());
				if (dateFin.isAfter(aujourdhui)) {
					derniere.setDateFin(null);
				}
			}
		}
	}
}
