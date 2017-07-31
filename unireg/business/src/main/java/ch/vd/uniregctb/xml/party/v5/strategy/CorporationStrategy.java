package ch.vd.uniregctb.xml.party.v5.strategy;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.xml.party.corporation.v5.Capital;
import ch.vd.unireg.xml.party.corporation.v5.Corporation;
import ch.vd.unireg.xml.party.corporation.v5.CorporationFlag;
import ch.vd.unireg.xml.party.corporation.v5.CorporationStatus;
import ch.vd.unireg.xml.party.corporation.v5.LegalForm;
import ch.vd.unireg.xml.party.corporation.v5.LegalSeat;
import ch.vd.unireg.xml.party.corporation.v5.MonetaryAmount;
import ch.vd.unireg.xml.party.corporation.v5.TaxSystem;
import ch.vd.unireg.xml.party.landregistry.v1.LandRight;
import ch.vd.unireg.xml.party.landregistry.v1.VirtualLandOwnershipRight;
import ch.vd.unireg.xml.party.landregistry.v1.VirtualUsufructRight;
import ch.vd.unireg.xml.party.landtaxlightening.v1.IciAbatement;
import ch.vd.unireg.xml.party.landtaxlightening.v1.IciAbatementRequest;
import ch.vd.unireg.xml.party.landtaxlightening.v1.IfoncExemption;
import ch.vd.unireg.xml.party.v5.PartyPart;
import ch.vd.unireg.xml.party.v5.UidNumberList;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.documentfiscal.AutreDocumentFiscal;
import ch.vd.uniregctb.foncier.AllegementFoncier;
import ch.vd.uniregctb.foncier.DegrevementICI;
import ch.vd.uniregctb.foncier.DemandeDegrevementICI;
import ch.vd.uniregctb.foncier.ExonerationIFONC;
import ch.vd.uniregctb.metier.bouclement.ExerciceCommercial;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.DroitRFRangeMetierComparator;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.CapitalHisto;
import ch.vd.uniregctb.tiers.DomicileHisto;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.FlagEntreprise;
import ch.vd.uniregctb.tiers.FormeLegaleHisto;
import ch.vd.uniregctb.tiers.IdentificationEntreprise;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.xml.Context;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;
import ch.vd.uniregctb.xml.ServiceException;
import ch.vd.uniregctb.xml.party.v5.BusinessYearBuilder;
import ch.vd.uniregctb.xml.party.v5.CorporationFlagBuilder;
import ch.vd.uniregctb.xml.party.v5.EasementRightHolderComparator;
import ch.vd.uniregctb.xml.party.v5.LandRightBuilder;
import ch.vd.uniregctb.xml.party.v5.LandTaxLighteningBuilder;
import ch.vd.uniregctb.xml.party.v5.TaxLighteningBuilder;

@SuppressWarnings("Duplicates")
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
		to.setName(context.tiersService.getDerniereRaisonSociale(entreprise));

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

		if (parts != null && (parts.contains(PartyPart.LAND_RIGHTS) || parts.contains(PartyPart.VIRTUAL_LAND_RIGHTS))) {
			initLandRights(to, entreprise, parts, context);
		}

		if (parts != null && parts.contains(PartyPart.LAND_TAX_LIGHTENINGS)) {
			initLandTaxLightenings(to, entreprise);
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

	private static class DatedCategory implements DateRange {

		private final RegDate dateDebut;
		private final RegDate dateFin;
		private final CategorieEntreprise categorie;

		public DatedCategory(CategorieEntreprise categorie, DateRange range) {
			this(categorie, range.getDateDebut(), range.getDateFin());
		}

		public DatedCategory(CategorieEntreprise categorie, RegDate dateDebut, RegDate dateFin) {
			this.categorie = categorie;
			this.dateDebut = dateDebut;
			this.dateFin = dateFin;
		}

		public CategorieEntreprise getCategorie() {
			return categorie;
		}

		@Override
		public RegDate getDateDebut() {
			return dateDebut;
		}

		@Override
		public RegDate getDateFin() {
			return dateFin;
		}
	}

	private static class DatedLegalFormWithCategory extends DatedCategory implements CollatableDateRange<DatedLegalFormWithCategory> {

		private final FormeLegale formeLegale;

		public DatedLegalFormWithCategory(DatedCategory categorie, FormeLegale formeLegale) {
			super(categorie.getCategorie(), categorie);
			this.formeLegale = formeLegale;
		}

		public DatedLegalFormWithCategory(CategorieEntreprise categorie, RegDate dateDebut, RegDate dateFin, FormeLegale formeLegale) {
			super(categorie, dateDebut, dateFin);
			this.formeLegale = formeLegale;
		}

		public FormeLegale getFormeLegale() {
			return formeLegale;
		}

		@Override
		public boolean isCollatable(DatedLegalFormWithCategory next) {
			return DateRangeHelper.isCollatable(this, next)
					&& getCategorie() == next.getCategorie()
					&& getFormeLegale() == next.getFormeLegale();
		}

		@Override
		public DatedLegalFormWithCategory collate(DatedLegalFormWithCategory next) {
			Assert.isTrue(isCollatable(next));
			return new DatedLegalFormWithCategory(getCategorie(), getDateDebut(), next.getDateFin(), getFormeLegale());
		}

		public LegalForm toLegalForm() {
			final LegalForm lf = new LegalForm();
			lf.setDateFrom(DataHelper.coreToXMLv2(getDateDebut()));
			lf.setDateTo(DataHelper.coreToXMLv2(getDateFin()));
			lf.setType(EnumHelper.coreToXMLv5(formeLegale));
			lf.setLabel(formeLegale.getLibelle());
			lf.setLegalFormCategory(EnumHelper.coreToXMLv5(getCategorie()));
			return lf;
		}
	}

	@NotNull
	private List<LegalForm> extractFormesJuridiques(Entreprise entreprise, Context context) {
		final List<FormeLegaleHisto> histo = context.tiersService.getFormesLegales(entreprise, false);
		final List<DatedCategory> regimesFiscaux = context.regimeFiscalService.getRegimesFiscauxVDNonAnnulesTrie(entreprise).stream()
				.map(rf -> new DatedCategory(rf.getCategorie(), rf))
				.collect(Collectors.toList());
		final DateRangeHelper.AdapterCallback<DatedCategory> adapter = (range, debut, fin) -> {
			final RegDate dateDebut = debut != null ? debut : range.getDateDebut();
			final RegDate dateFin = fin != null ? fin : range.getDateFin();
			return new DatedCategory(range.getCategorie(), dateDebut, dateFin);
		};

		final List<DatedLegalFormWithCategory> resultatBrut = new LinkedList<>();
		for (FormeLegaleHisto fl : histo) {
			final List<DatedCategory> regimesLocaux = DateRangeHelper.extract(regimesFiscaux, fl.getDateDebut(), fl.getDateFin(), adapter);
			final List<DatedCategory> decoupage = DateRangeHelper.override(Collections.singletonList(new DatedCategory(CategorieEntreprise.AUTRE, fl)),
			                                                               regimesLocaux,
			                                                               adapter);
			decoupage.stream()
					.map(cat -> new DatedLegalFormWithCategory(cat, fl.getFormeLegale()))
					.forEach(resultatBrut::add);
		}

		final List<DatedLegalFormWithCategory> collated = DateRangeHelper.collate(resultatBrut);
		return collated.stream()
				.map(DatedLegalFormWithCategory::toLegalForm)
				.collect(Collectors.toList());
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

		if (parts != null && (parts.contains(PartyPart.LAND_RIGHTS) || parts.contains(PartyPart.VIRTUAL_LAND_RIGHTS))) {
			copyLandRights(to, from, parts, mode);
		}

		if (parts != null && parts.contains(PartyPart.LAND_TAX_LIGHTENINGS)) {
			copyColl(to.getIfoncExemptions(), from.getIfoncExemptions());
			copyColl(to.getIciAbatements(), from.getIciAbatements());
			copyColl(to.getIciAbatementRequests(), from.getIciAbatementRequests());
		}
	}

	private void initLandRights(Corporation to, Entreprise entreprise, @NotNull Set<PartyPart> parts, Context context) {

		final boolean includeVirtual = parts.contains(PartyPart.VIRTUAL_LAND_RIGHTS);
		final List<DroitRF> droits = context.registreFoncierService.getDroitsForCtb(entreprise, includeVirtual);

		final List<LandRight> landRights = to.getLandRights();
		droits.stream()
				.sorted(new DroitRFRangeMetierComparator())
				.map((droitRF) -> LandRightBuilder.newLandRight(droitRF,
				                                                context.registreFoncierService::getContribuableIdFor,
				                                                new EasementRightHolderComparator(context.tiersService)))
				.forEach(landRights::add);
	}

	private static void copyLandRights(Corporation to, Corporation from, Set<PartyPart> parts, CopyMode mode) {

		// Les droits réels et les droits virtuels représentent deux ensembles qui se recoupent.
		// Plus précisemment, les droits réels sont entièrement contenus dans les droits virtuels. En fonction
		// du mode de copie, il est donc nécessaire de compléter ou de filtrer les droits.
		if (mode == CopyMode.ADDITIVE) {
			if (parts.contains(PartyPart.VIRTUAL_LAND_RIGHTS) || to.getLandRights() == null || to.getLandRights().isEmpty()) {
				copyColl(to.getLandRights(), from.getLandRights());
			}
		}
		else {
			Assert.isEqual(CopyMode.EXCLUSIVE, mode);
			if (parts.contains(PartyPart.VIRTUAL_LAND_RIGHTS)) {
				copyColl(to.getLandRights(), from.getLandRights());
			}
			else {
				// on supprime les éventuels droits virtuels s'ils ne sont pas demandés
				if (from.getLandRights() != null && !from.getLandRights().isEmpty()) {
					to.getLandRights().clear();
					to.getLandRights().addAll(from.getLandRights().stream()
							.filter(CorporationStrategy::isReel)
							.collect(Collectors.toList()));
				}
				else {
					to.getLandRights().clear();
				}
			}

		}
	}

	private static boolean isReel(@NotNull LandRight f) {
		return !(f instanceof VirtualLandOwnershipRight) && !(f instanceof VirtualUsufructRight);
	}

	void initLandTaxLightenings(Corporation to, Entreprise entreprise) {

		// les exonérations
		final List<IfoncExemption> exemptions = to.getIfoncExemptions();
		entreprise.getAllegementsFonciers().stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(a -> a instanceof ExonerationIFONC)
				.sorted(new DateRangeComparator<AllegementFoncier>().thenComparing(a -> a.getImmeuble().getId()))
				.map(a -> LandTaxLighteningBuilder.buildIfoncExemption((ExonerationIFONC) a))
				.forEach(exemptions::add);

		// les dégrèvements
		final List<IciAbatement> abatements = to.getIciAbatements();
		entreprise.getAllegementsFonciers().stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(a -> a instanceof DegrevementICI)
				.sorted(new DateRangeComparator<AllegementFoncier>().thenComparing(a -> a.getImmeuble().getId()))
				.map(a -> LandTaxLighteningBuilder.buildIciAbatement((DegrevementICI) a))
				.forEach(abatements::add);

		// les demandes de dégrèvements
		final List<IciAbatementRequest> requests = to.getIciAbatementRequests();
		entreprise.getAutresDocumentsFiscaux().stream()
				.filter(d -> d instanceof DemandeDegrevementICI)
				.filter(AnnulableHelper::nonAnnule)
				.map (a -> (DemandeDegrevementICI)a)
				.sorted(Comparator.<DemandeDegrevementICI, RegDate>comparing(AutreDocumentFiscal::getDateEnvoi).thenComparing(a -> a.getImmeuble().getId()))
				.map(LandTaxLighteningBuilder::buildIciAbatementRequest)
				.forEach(requests::add);
	}
}
