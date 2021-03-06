package ch.vd.unireg.xml.party.v5.strategy;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseMandataire;
import ch.vd.unireg.interfaces.efacture.data.DestinataireAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.EtatDestinataire;
import ch.vd.unireg.metier.assujettissement.AssujettissementException;
import ch.vd.unireg.metier.assujettissement.PeriodeImposition;
import ch.vd.unireg.situationfamille.VueSituationFamille;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.Mandat;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.xml.Context;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.ExceptionHelper;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.party.agent.v1.Agent;
import ch.vd.unireg.xml.party.taxpayer.v5.Taxpayer;
import ch.vd.unireg.xml.party.taxresidence.v4.SimplifiedTaxLiability;
import ch.vd.unireg.xml.party.taxresidence.v4.TaxationPeriod;
import ch.vd.unireg.xml.party.v5.AgentBuilder;
import ch.vd.unireg.xml.party.v5.EBillingStatusBuilder;
import ch.vd.unireg.xml.party.v5.FamilyStatusBuilder;
import ch.vd.unireg.xml.party.v5.InternalPartyPart;
import ch.vd.unireg.xml.party.v5.SimplifiedTaxLiabilityBuilder;
import ch.vd.unireg.xml.party.v5.TaxLiabilityBuilder;
import ch.vd.unireg.xml.party.v5.TaxationPeriodBuilder;

public abstract class TaxPayerStrategy<T extends Taxpayer> extends PartyStrategy<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(TaxPayerStrategy.class);

	@Override
	protected void initParts(T to, Tiers from, @Nullable Set<InternalPartyPart> parts, Context context) throws ServiceException {
		super.initParts(to, from, parts, context);

		Contribuable ctb = (Contribuable) from;
		if (parts != null && parts.contains(InternalPartyPart.FAMILY_STATUSES)) {
			initFamilyStatuses(to, ctb, context);
		}

		if (parts != null && (parts.contains(InternalPartyPart.TAX_LIABILITIES) || parts.contains(InternalPartyPart.SIMPLIFIED_TAX_LIABILITIES))) {
			initTaxLiabilities(to, ctb, parts, context);
		}

		if (parts != null && parts.contains(InternalPartyPart.TAXATION_PERIODS)) {
			initTaxationPeriods(to, ctb, context);
		}

		// [SIFISC-26536] la part IMMOVABLE_PROPERTIES est dépréciée

		if (parts != null && parts.contains(InternalPartyPart.EBILLING_STATUSES)) {
			initEBillingStatuses(to, ctb, context);
		}

		if (parts != null && parts.contains(InternalPartyPart.AGENTS)) {
			initAgents(to, ctb, context);
		}
	}

	@Override
	protected void copyParts(T to, T from, @Nullable Set<InternalPartyPart> parts, CopyMode mode) {
		super.copyParts(to, from, parts, mode);

		if (parts != null && parts.contains(InternalPartyPart.FAMILY_STATUSES)) {
			copyColl(to.getFamilyStatuses(), from.getFamilyStatuses());
		}

		if (parts != null && parts.contains(InternalPartyPart.TAX_LIABILITIES)) {
			copyColl(to.getTaxLiabilities(), from.getTaxLiabilities());
		}

		if (parts != null && parts.contains(InternalPartyPart.SIMPLIFIED_TAX_LIABILITIES)) {
			copyColl(to.getSimplifiedTaxLiabilityVD(), from.getSimplifiedTaxLiabilityVD());
			copyColl(to.getSimplifiedTaxLiabilityCH(), from.getSimplifiedTaxLiabilityCH());
		}

		if (parts != null && parts.contains(InternalPartyPart.TAXATION_PERIODS)) {
			copyColl(to.getTaxationPeriods(), from.getTaxationPeriods());
		}

		// [SIFISC-26536] la part IMMOVABLE_PROPERTIES est dépréciée et n'a aucun effet

		if (parts != null && parts.contains(InternalPartyPart.EBILLING_STATUSES)) {
			copyColl(to.getEbillingStatuses(), from.getEbillingStatuses());
		}

		if (parts != null && parts.contains(InternalPartyPart.AGENTS)) {
			copyColl(to.getAgents(), from.getAgents());
		}
	}

	private static void initFamilyStatuses(Taxpayer left, Contribuable contribuable, Context context) {

		final List<VueSituationFamille> situations = context.situationService.getVueHisto(contribuable);

		for (VueSituationFamille situation : situations) {
			left.getFamilyStatuses().add(FamilyStatusBuilder.newFamilyStatus(situation));
		}
	}

	private static void initTaxLiabilities(Taxpayer left, Contribuable right, Set<InternalPartyPart> parts, Context context) throws ServiceException {
		/*
		 * Note: il est nécessaire de calculer l'assujettissement sur TOUTE la période de validité du contribuable pour obtenir un résultat
		 * correct avec le collate.
		 */
		final List<ch.vd.unireg.metier.assujettissement.Assujettissement> list;
		try {
			list = context.assujettissementService.determine(right, (DateRange) null);
		}
		catch (AssujettissementException e) {
			LOGGER.error(e.getMessage(), e);
			throw ExceptionHelper.newBusinessException(e, BusinessExceptionCode.TAX_LIABILITY);
		}

		if (list != null) {

			final boolean wantAssujettissements = parts.contains(InternalPartyPart.TAX_LIABILITIES);
			final boolean wantAssujettissementsSimplifies = parts.contains(InternalPartyPart.SIMPLIFIED_TAX_LIABILITIES);

			for (ch.vd.unireg.metier.assujettissement.Assujettissement a : list) {
				if (wantAssujettissements) {
					left.getTaxLiabilities().add(TaxLiabilityBuilder.newTaxLiability(a));
				}
				if (wantAssujettissementsSimplifies) {
					final SimplifiedTaxLiability vd = SimplifiedTaxLiabilityBuilder.toVD(a);
					if (vd != null) {
						left.getSimplifiedTaxLiabilityVD().add(vd);
					}
					final SimplifiedTaxLiability ch = SimplifiedTaxLiabilityBuilder.toCH(a);
					if (ch != null) {
						left.getSimplifiedTaxLiabilityCH().add(ch);
					}
				}
			}
		}
	}

	private static void initTaxationPeriods(Taxpayer left, Contribuable contribuable, Context context)  throws ServiceException {

		final List<PeriodeImposition> list;
		try {
			list = context.periodeImpositionService.determine(contribuable);
		}
		catch (AssujettissementException e) {
			LOGGER.error(e.getMessage(), e);
			throw ExceptionHelper.newBusinessException(e, BusinessExceptionCode.TAX_LIABILITY);
		}
		if (list != null) {
			TaxationPeriod derniere = null;
			for (PeriodeImposition p : list) {
				final TaxationPeriod periode = TaxationPeriodBuilder.newTaxationPeriod(p);
				left.getTaxationPeriods().add(periode);
				derniere = periode;
			}
			// [UNIREG-910] la période d'imposition courante est laissée ouverte
			if (derniere != null && derniere.getDateTo() != null) {
				final RegDate aujourdhui = RegDate.get();
				final RegDate dateFin = DataHelper.xmlToCore(derniere.getDateTo());
				if (dateFin.isAfter(aujourdhui)) {
					derniere.setDateTo(null);
				}
			}
		}
	}

	// [SIFISC-11134] Ajout de la part "e-facture"
	private static void initEBillingStatuses(Taxpayer left, Contribuable contribuable, Context context) {
		final DestinataireAvecHisto histo = context.eFactureService.getDestinataireAvecSonHistorique(contribuable.getNumero());
		if (histo != null && histo.getHistoriquesEtats() != null) {
			for (EtatDestinataire etat : histo.getHistoriquesEtats()) {
				left.getEbillingStatuses().add(EBillingStatusBuilder.newEBillingStatus(etat));
			}
		}
	}

	// Ajout des mandataires
	private static void initAgents(Taxpayer left, Contribuable contribuable, Context context) throws ServiceException {

		try {
			// les adresses pures
			final Set<AdresseMandataire> adresses = contribuable.getAdressesMandataires();
			for (AdresseMandataire adresse : adresses) {
				if (!adresse.isAnnule()) {
					final Agent agent = AgentBuilder.newAgent(adresse, context);
					if (agent != null) {
						left.getAgents().add(agent);
					}
				}
			}

			// les liens
			final Set<RapportEntreTiers> rets = contribuable.getRapportsSujet();
			for (RapportEntreTiers ret : rets) {
				if (!ret.isAnnule() && ret instanceof Mandat) {
					left.getAgents().addAll(AgentBuilder.newAgents((Mandat) ret, context));
				}
			}

			// il faudrait peut-être trier ces données, non ?
			left.getAgents().sort(Comparator.comparing(Agent::getDateFrom));
		}
		catch (AdresseException e) {
			LOGGER.error(e.getMessage(), e);
			throw ExceptionHelper.newBusinessException(e, BusinessExceptionCode.ADDRESSES);
		}
	}
}
