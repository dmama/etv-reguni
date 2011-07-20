package ch.vd.uniregctb.webservices.tiers3.data.strategy;

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.webservices.tiers3.PartyPart;
import ch.vd.unireg.webservices.tiers3.TaxationPeriod;
import ch.vd.unireg.webservices.tiers3.Taxpayer;
import ch.vd.unireg.webservices.tiers3.WebServiceException;
import ch.vd.unireg.xml.exception.BusinessExceptionCode;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.situationfamille.VueSituationFamille;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.webservices.tiers3.data.FamilyStatusBuilder;
import ch.vd.uniregctb.webservices.tiers3.data.SimplifiedTaxLiabilityBuilder;
import ch.vd.uniregctb.webservices.tiers3.data.TaxLiabilityBuilder;
import ch.vd.uniregctb.webservices.tiers3.data.TaxationPeriodBuilder;
import ch.vd.uniregctb.webservices.tiers3.impl.Context;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.ExceptionHelper;

public abstract class TaxPayerStrategy<T extends Taxpayer> extends PartyStrategy<T> {

	private static final Logger LOGGER = Logger.getLogger(TaxPayerStrategy.class);

	@Override
	protected void initParts(T to, Tiers from, @Nullable Set<PartyPart> parts, Context context) throws WebServiceException {
		super.initParts(to, from, parts, context);

		ch.vd.uniregctb.tiers.Contribuable ctb=(ch.vd.uniregctb.tiers.Contribuable) from;
		if (parts != null && parts.contains(PartyPart.FAMILY_STATUSES)) {
			initFamilyStatuses(to, ctb, context);
		}

		if (parts != null && (parts.contains(PartyPart.ORDINARY_TAX_LIABILITIES) || parts.contains(PartyPart.SIMPLIFIED_TAX_LIABILITIES))) {
			initTaxLiabilities(to, ctb, parts);
		}

		if (parts != null && parts.contains(PartyPart.TAXATION_PERIODS)) {
			initTaxationPeriods(to, ctb, context);
		}
	}

	@Override
	protected void copyParts(T to, T from, @Nullable Set<PartyPart> parts, CopyMode mode) {
		super.copyParts(to, from, parts, mode);

		if (parts != null && parts.contains(PartyPart.FAMILY_STATUSES)) {
			copyColl(to.getFamilyStatuses(), from.getFamilyStatuses());
		}

		if (parts != null && parts.contains(PartyPart.ORDINARY_TAX_LIABILITIES)) {
			copyColl(to.getOrdinaryTaxLiabilities(), from.getOrdinaryTaxLiabilities());
		}

		if (parts != null && parts.contains(PartyPart.SIMPLIFIED_TAX_LIABILITIES)) {
			copyColl(to.getSimplifiedTaxLiabilityVD(), from.getSimplifiedTaxLiabilityVD());
			copyColl(to.getSimplifiedTaxLiabilityCH(), from.getSimplifiedTaxLiabilityCH());
		}

		if (parts != null && parts.contains(PartyPart.TAXATION_PERIODS)) {
			copyColl(to.getTaxationPeriods(), from.getTaxationPeriods());
		}
	}

	private static void initFamilyStatuses(Taxpayer left, ch.vd.uniregctb.tiers.Contribuable contribuable, Context context) {

		final List<VueSituationFamille> situations = context.situationService.getVueHisto(contribuable);

		for (ch.vd.uniregctb.situationfamille.VueSituationFamille situation : situations) {
			left.getFamilyStatuses().add(FamilyStatusBuilder.newFamilyStatus(situation));
		}
	}

	private static void initTaxLiabilities(Taxpayer left, ch.vd.uniregctb.tiers.Contribuable right, Set<PartyPart> parts) throws WebServiceException {
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
			throw ExceptionHelper.newBusinessException(e, BusinessExceptionCode.TAX_LIABILITY);
		}

		if (list != null) {

			final boolean wantAssujettissements = parts.contains(PartyPart.ORDINARY_TAX_LIABILITIES);
			final boolean wantPeriodes = parts.contains(PartyPart.SIMPLIFIED_TAX_LIABILITIES);

			for (ch.vd.uniregctb.metier.assujettissement.Assujettissement a : list) {
				if (wantAssujettissements) {
					left.getOrdinaryTaxLiabilities().add(TaxLiabilityBuilder.newTaxLiability(a));
				}
				if (wantPeriodes) {
					left.getSimplifiedTaxLiabilityVD().add(SimplifiedTaxLiabilityBuilder.toVD(a));
					left.getSimplifiedTaxLiabilityCH().add(SimplifiedTaxLiabilityBuilder.toCH(a));
				}
			}
		}
	}

	private static void initTaxationPeriods(Taxpayer left, ch.vd.uniregctb.tiers.Contribuable contribuable, Context context)
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
			throw ExceptionHelper.newBusinessException(e, BusinessExceptionCode.TAX_LIABILITY);
		}
		if (list != null) {
			TaxationPeriod derniere = null;
			for (ch.vd.uniregctb.metier.assujettissement.PeriodeImposition p : list) {
				final TaxationPeriod periode = TaxationPeriodBuilder.newTaxationPeriod(p);
				left.getTaxationPeriods().add(periode);
				derniere = periode;
			}
			// [UNIREG-910] la période d'imposition courante est laissée ouverte
			if (derniere != null && derniere.getDateTo() != null) {
				final RegDate aujourdhui = RegDate.get();
				final RegDate dateFin = DataHelper.webToCore(derniere.getDateTo());
				if (dateFin.isAfter(aujourdhui)) {
					derniere.setDateTo(null);
				}
			}
		}
	}
}
