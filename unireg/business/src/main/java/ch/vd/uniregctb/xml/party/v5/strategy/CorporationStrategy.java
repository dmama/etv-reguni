package ch.vd.uniregctb.xml.party.v5.strategy;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.xml.party.corporation.v5.Capital;
import ch.vd.unireg.xml.party.corporation.v5.Corporation;
import ch.vd.unireg.xml.party.corporation.v5.CorporationFlag;
import ch.vd.unireg.xml.party.corporation.v5.CorporationStatus;
import ch.vd.unireg.xml.party.corporation.v5.LegalForm;
import ch.vd.unireg.xml.party.corporation.v5.LegalSeat;
import ch.vd.unireg.xml.party.corporation.v5.MonetaryAmount;
import ch.vd.unireg.xml.party.corporation.v5.TaxSystem;
import ch.vd.unireg.xml.party.v5.PartyPart;
import ch.vd.unireg.xml.party.v5.UidNumberList;
import ch.vd.uniregctb.metier.bouclement.ExerciceCommercial;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.CapitalHisto;
import ch.vd.uniregctb.tiers.CategorieEntrepriseHelper;
import ch.vd.uniregctb.tiers.DomicileHisto;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.FlagEntreprise;
import ch.vd.uniregctb.tiers.FormeLegaleHisto;
import ch.vd.uniregctb.tiers.IdentificationEntreprise;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.xml.Context;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;
import ch.vd.uniregctb.xml.ServiceException;
import ch.vd.uniregctb.xml.party.v5.BusinessYearBuilder;
import ch.vd.uniregctb.xml.party.v5.CorporationFlagBuilder;
import ch.vd.uniregctb.xml.party.v5.TaxLighteningBuilder;

public class CorporationStrategy extends TaxPayerStrategy<Corporation> {

	@Override
	public Corporation newFrom(Tiers right, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {
		final Corporation corp = new Corporation();
		initBase(corp, right, context);
		initParts(corp, right, parts, context);
		return corp;
	}

	@Override
	protected void initBase(Corporation to, Tiers from, Context context) throws ServiceException {
		super.initBase(to, from, context);

		final Entreprise entreprise = (Entreprise) from;
		to.setName(context.tiersService.getRaisonSociale(entreprise));

		// L'exposition du numéro IDE
		if (entreprise.isConnueAuCivil()) {
			final Organisation organisation = context.serviceOrganisationService.getOrganisationHistory(entreprise.getNumeroEntreprise());
			final List<DateRanged<String>> numeros = organisation.getNumeroIDE();
			if (numeros != null && !numeros.isEmpty()) {
				to.setUidNumbers(new UidNumberList(Collections.singletonList(numeros.get(numeros.size() - 1).getPayload())));
			}
		}
		else {
			final Set<IdentificationEntreprise> ides = entreprise.getIdentificationsEntreprise();
			if (ides != null && !ides.isEmpty()) {
				final List<String> ideList = new ArrayList<>(ides.size());
				for (IdentificationEntreprise ide : ides) {
					ideList.add(ide.getNumeroIde());
				}
				to.setUidNumbers(new UidNumberList(ideList));
			}
		}
	}

	@Override
	protected void initParts(Corporation to, Tiers from, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {
		super.initParts(to, from, parts, context);

		final Entreprise entreprise = (Entreprise) from;

		if (parts != null && parts.contains(PartyPart.CAPITALS)) {
			to.getCapitals().addAll(extractCapitaux(entreprise, context));
		}

		if (parts != null && parts.contains(PartyPart.CORPORATION_STATUSES)) {
			to.getCorporationStatuses().addAll(extractEtats(entreprise.getEtatsNonAnnulesTries()));
		}

		if (parts != null && parts.contains(PartyPart.LEGAL_FORMS)) {
			to.getLegalForms().addAll(extractFormesJuridiques(entreprise, context));
		}

		if (parts != null && parts.contains(PartyPart.TAX_SYSTEMS)) {
			final List<RegimeFiscal> regimesFiscaux = entreprise.getRegimesFiscauxNonAnnulesTries();
			to.getTaxSystemsVD().addAll(extractRegimesFiscaux(regimesFiscaux, RegimeFiscal.Portee.VD));
			to.getTaxSystemsCH().addAll(extractRegimesFiscaux(regimesFiscaux, RegimeFiscal.Portee.CH));
		}

		if (parts != null && parts.contains(PartyPart.LEGAL_SEATS)) {
			to.getLegalSeats().addAll(extractSieges(entreprise, context));
		}

		if (parts != null && parts.contains(PartyPart.TAX_LIGHTENINGS)) {
			initAllegementsFiscaux(to, entreprise);
		}

		if (parts != null && parts.contains(PartyPart.BUSINESS_YEARS)) {
			initExercicesCommerciaux(to, entreprise, context);
		}

		if (parts != null && parts.contains(PartyPart.CORPORATION_FLAGS)) {
			initFlags(to, entreprise);
		}
	}

	@NotNull
	private List<Capital> extractCapitaux(Entreprise entreprise, Context context) {
		final List<CapitalHisto> data = context.tiersService.getCapitaux(entreprise, false);
		if (data != null && !data.isEmpty()) {
			final List<Capital> capitaux = new ArrayList<>(data.size());
			for (CapitalHisto mmh : data) {
				final Capital capital = new Capital();
				capital.setDateFrom(DataHelper.coreToXMLv2(mmh.getDateDebut()));
				capital.setDateTo(DataHelper.coreToXMLv2(mmh.getDateFin()));
				capital.setPaidInCapital(new MonetaryAmount(mmh.getMontant().getMontant(), mmh.getMontant().getMonnaie()));
				capitaux.add(capital);
			}
			return capitaux;
		}
		return Collections.emptyList();
	}

	@NotNull
	private List<LegalForm> extractFormesJuridiques(Entreprise entreprise, Context context) {
		final List<FormeLegaleHisto> histo = context.tiersService.getFormesLegales(entreprise, false);
		final List<LegalForm> liste = new ArrayList<>(histo.size());
		for (FormeLegaleHisto fl : histo) {
			final LegalForm lf = new LegalForm();
			lf.setDateFrom(DataHelper.coreToXMLv2(fl.getDateDebut()));
			lf.setDateTo(DataHelper.coreToXMLv2(fl.getDateFin()));
			lf.setType(EnumHelper.coreToXMLv5(fl.getFormeLegale()));
			lf.setLabel(fl.getFormeLegale().getLibelle());
			lf.setLegalFormCategory(EnumHelper.coreToXMLv5(CategorieEntrepriseHelper.map(fl.getFormeLegale())));
			liste.add(lf);
		}
		return liste;
	}

	@NotNull
	private List<TaxSystem> extractRegimesFiscaux(List<RegimeFiscal> regimes, RegimeFiscal.Portee portee) {
		final List<TaxSystem> liste = new ArrayList<>(regimes.size());
		for (RegimeFiscal regime : regimes) {
			if (regime.getPortee() == portee) {
				final TaxSystem ts = new TaxSystem();
				ts.setDateFrom(DataHelper.coreToXMLv2(regime.getDateDebut()));
				ts.setDateTo(DataHelper.coreToXMLv2(regime.getDateFin()));
				ts.setType(regime.getCode());
				ts.setScope(EnumHelper.coreToXMLv5(regime.getPortee()));
				liste.add(ts);
			}
		}
		return liste;
	}

	@NotNull
	private List<LegalSeat> extractSieges(Entreprise entreprise, Context context) {
		final List<LegalSeat> liste = new ArrayList<>();
		final List<DomicileHisto> sieges = context.tiersService.getSieges(entreprise, false);
		for (DomicileHisto siege : sieges) {
			if (!siege.isAnnule()) {
				final LegalSeat seat = new LegalSeat();
				seat.setDateFrom(DataHelper.coreToXMLv2(siege.getDateDebut()));
				seat.setDateTo(DataHelper.coreToXMLv2(siege.getDateFin()));
				seat.setFsoId(siege.getNumeroOfsAutoriteFiscale());
				seat.setType(EnumHelper.coreToXMLLegalSeatv5(siege.getTypeAutoriteFiscale()));
				liste.add(seat);
			}
		}
		return liste;
	}

	private static void initAllegementsFiscaux(Corporation corporation, Entreprise entreprise) {
		final List<AllegementFiscal> allegements = entreprise.getAllegementsFiscauxNonAnnulesTries();
		for (AllegementFiscal allegement : allegements) {
			corporation.getTaxLightenings().add(TaxLighteningBuilder.newTaxLightening(allegement));
		}
	}

	private void initExercicesCommerciaux(Corporation corporation, Entreprise entreprise, Context context) {
		final List<ExerciceCommercial> exercices = context.exerciceCommercialHelper.getExercicesCommerciauxExposables(entreprise);
		for (ExerciceCommercial ex : exercices) {
			corporation.getBusinessYears().add(BusinessYearBuilder.newBusinessYear(ex));
		}
	}

	private static void initFlags(Corporation corporation, Entreprise entreprise) {
		final List<FlagEntreprise> flags = entreprise.getFlagsNonAnnulesTries();
		for (FlagEntreprise flag : flags) {
			final CorporationFlag cf = CorporationFlagBuilder.newFlag(flag);
			if (cf != null) {
				corporation.getCorporationFlags().add(cf);
			}
		}
	}

	@NotNull
	private List<CorporationStatus> extractEtats(List<EtatEntreprise> etats) {
		if (etats == null || etats.isEmpty()) {
			return Collections.emptyList();
		}

		final List<CorporationStatus> statuses = new ArrayList<>(etats.size());
		for (EtatEntreprise etat : etats) {
			final CorporationStatus status = new CorporationStatus();
			status.setDateFrom(DataHelper.coreToXMLv2(etat.getDateObtention()));
			status.setStatusType(EnumHelper.coreToXMLv5(etat.getType()));
			status.setLabel(etat.getType().getLibelle());
			statuses.add(status);
		}
		return statuses;
	}

	@Override
	public Corporation clone(Corporation right, @Nullable Set<PartyPart> parts) {
		final Corporation pm = new Corporation();
		copyBase(pm, right);
		copyParts(pm, right, parts, CopyMode.EXCLUSIVE);
		return pm;
	}

	@Override
	protected void copyBase(Corporation to, Corporation from) {
		super.copyBase(to, from);
		to.setName(from.getName());
		to.setUidNumbers(from.getUidNumbers());
	}

	@Override
	protected void copyParts(Corporation to, Corporation from, @Nullable Set<PartyPart> parts, CopyMode mode) {
		super.copyParts(to, from, parts, mode);

		if (parts != null && parts.contains(PartyPart.LEGAL_SEATS)) {
			copyColl(to.getLegalSeats(), from.getLegalSeats());
		}

		if (parts != null && parts.contains(PartyPart.LEGAL_FORMS)) {
			copyColl(to.getLegalForms(), from.getLegalForms());
		}

		if (parts != null && parts.contains(PartyPart.CAPITALS)) {
			copyColl(to.getCapitals(), from.getCapitals());
		}

		if (parts != null && parts.contains(PartyPart.CORPORATION_STATUSES)) {
			copyColl(to.getCorporationStatuses(), from.getCorporationStatuses());
		}

		if (parts != null && parts.contains(PartyPart.TAX_SYSTEMS)) {
			copyColl(to.getTaxSystemsVD(), from.getTaxSystemsVD());
			copyColl(to.getTaxSystemsCH(), from.getTaxSystemsCH());
		}

		if (parts != null && parts.contains(PartyPart.TAX_LIGHTENINGS)) {
			copyColl(to.getTaxLightenings(), from.getTaxLightenings());
		}

		if (parts != null && parts.contains(PartyPart.BUSINESS_YEARS)) {
			copyColl(to.getBusinessYears(), from.getBusinessYears());
		}

		if (parts != null && parts.contains(PartyPart.CORPORATION_FLAGS)) {
			copyColl(to.getCorporationFlags(), from.getCorporationFlags());
		}
	}
}