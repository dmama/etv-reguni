package ch.vd.unireg.xml.party.v5.strategy;


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
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.HibernateDateRangeEntity;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscal;
import ch.vd.unireg.foncier.AllegementFoncier;
import ch.vd.unireg.foncier.DegrevementICI;
import ch.vd.unireg.foncier.DemandeDegrevementICI;
import ch.vd.unireg.foncier.ExonerationIFONC;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.metier.bouclement.ExerciceCommercial;
import ch.vd.unireg.metier.periodeexploitation.PeriodeExploitationService.PeriodeContext;
import ch.vd.unireg.registrefoncier.DroitRF;
import ch.vd.unireg.registrefoncier.DroitRFRangeMetierComparator;
import ch.vd.unireg.tiers.AllegementFiscal;
import ch.vd.unireg.tiers.CapitalHisto;
import ch.vd.unireg.tiers.DomicileHisto;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.EtatEntreprise;
import ch.vd.unireg.tiers.FlagEntreprise;
import ch.vd.unireg.tiers.FormeLegaleHisto;
import ch.vd.unireg.tiers.FusionEntreprises;
import ch.vd.unireg.tiers.IdentificationEntreprise;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.CategorieEntreprise;
import ch.vd.unireg.xml.Context;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.EnumHelper;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.party.corporation.v5.Capital;
import ch.vd.unireg.xml.party.corporation.v5.Corporation;
import ch.vd.unireg.xml.party.corporation.v5.CorporationFlag;
import ch.vd.unireg.xml.party.corporation.v5.CorporationStatus;
import ch.vd.unireg.xml.party.corporation.v5.LegalForm;
import ch.vd.unireg.xml.party.corporation.v5.LegalSeat;
import ch.vd.unireg.xml.party.corporation.v5.MonetaryAmount;
import ch.vd.unireg.xml.party.corporation.v5.TaxSystem;
import ch.vd.unireg.xml.party.landregistry.v1.HousingRight;
import ch.vd.unireg.xml.party.landregistry.v1.LandOwnershipRight;
import ch.vd.unireg.xml.party.landregistry.v1.LandRight;
import ch.vd.unireg.xml.party.landregistry.v1.UsufructRight;
import ch.vd.unireg.xml.party.landregistry.v1.VirtualLandOwnershipRight;
import ch.vd.unireg.xml.party.landregistry.v1.VirtualUsufructRight;
import ch.vd.unireg.xml.party.landtaxlightening.v1.IciAbatement;
import ch.vd.unireg.xml.party.landtaxlightening.v1.IciAbatementRequest;
import ch.vd.unireg.xml.party.landtaxlightening.v1.IfoncExemption;
import ch.vd.unireg.xml.party.landtaxlightening.v1.VirtualLandTaxLightening;
import ch.vd.unireg.xml.party.taxresidence.v4.OperatingPeriod;
import ch.vd.unireg.xml.party.v5.BusinessYearBuilder;
import ch.vd.unireg.xml.party.v5.CorporationFlagBuilder;
import ch.vd.unireg.xml.party.v5.EasementRightHolderComparator;
import ch.vd.unireg.xml.party.v5.InternalPartyPart;
import ch.vd.unireg.xml.party.v5.LandRightBuilder;
import ch.vd.unireg.xml.party.v5.LandTaxLighteningBuilder;
import ch.vd.unireg.xml.party.v5.OperatingPeriodBuilder;
import ch.vd.unireg.xml.party.v5.TaxLighteningBuilder;
import ch.vd.unireg.xml.party.v5.UidNumberList;

@SuppressWarnings("Duplicates")
public class CorporationStrategy extends TaxPayerStrategy<Corporation> {

	@Override
	public Corporation newFrom(Tiers right, @Nullable Set<InternalPartyPart> parts, Context context) throws ServiceException {
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
			final EntrepriseCivile entrepriseCivile = context.serviceEntreprise.getEntrepriseHistory(entreprise.getNumeroEntreprise());
			final List<DateRanged<String>> numeros = entrepriseCivile.getNumeroIDE();
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
	protected void initParts(Corporation to, Tiers from, @Nullable Set<InternalPartyPart> parts, Context context) throws ServiceException {
		super.initParts(to, from, parts, context);

		final Entreprise entreprise = (Entreprise) from;

		if (parts != null && parts.contains(InternalPartyPart.CAPITALS)) {
			to.getCapitals().addAll(extractCapitaux(entreprise, context));
		}

		if (parts != null && parts.contains(InternalPartyPart.CORPORATION_STATUSES)) {
			to.getCorporationStatuses().addAll(extractEtats(entreprise.getEtatsNonAnnulesTries()));
		}

		if (parts != null && parts.contains(InternalPartyPart.LEGAL_FORMS)) {
			to.getLegalForms().addAll(extractFormesJuridiques(entreprise, context));
		}

		if (parts != null && parts.contains(InternalPartyPart.TAX_SYSTEMS)) {
			final List<RegimeFiscal> regimesFiscaux = entreprise.getRegimesFiscauxNonAnnulesTries();
			to.getTaxSystemsVD().addAll(extractRegimesFiscaux(regimesFiscaux, RegimeFiscal.Portee.VD));
			to.getTaxSystemsCH().addAll(extractRegimesFiscaux(regimesFiscaux, RegimeFiscal.Portee.CH));
		}

		if (parts != null && parts.contains(InternalPartyPart.LEGAL_SEATS)) {
			to.getLegalSeats().addAll(extractSieges(entreprise, context));
		}

		if (parts != null && parts.contains(InternalPartyPart.TAX_LIGHTENINGS)) {
			initAllegementsFiscaux(to, entreprise);
		}

		if (parts != null && parts.contains(InternalPartyPart.BUSINESS_YEARS)) {
			initExercicesCommerciaux(to, entreprise, context);
		}

		if (parts != null && parts.contains(InternalPartyPart.CORPORATION_FLAGS)) {
			initFlags(to, entreprise);
		}

		if (parts != null && (parts.contains(InternalPartyPart.REAL_LAND_RIGHTS) ||
				parts.contains(InternalPartyPart.VIRTUAL_TRANSITIVE_LAND_RIGHTS) ||
				parts.contains(InternalPartyPart.VIRTUAL_INHERITED_REAL_LAND_RIGHTS) ||
				parts.contains(InternalPartyPart.VIRTUAL_INHERITED_VIRTUAL_LAND_RIGHTS))) {
			initLandRights(to, entreprise, parts, context);
		}

		if (parts != null && (parts.contains(InternalPartyPart.LAND_TAX_LIGHTENINGS) ||
				parts.contains(InternalPartyPart.VIRTUAL_LAND_TAX_LIGHTENINGS))) {
			initLandTaxLightenings(to, entreprise, parts, context);
		}

		if (parts != null && parts.contains(InternalPartyPart.OPERATING_PERIODS)) {
			initOperatingPeriods(to, entreprise, context);
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
			if (!isCollatable(next)) {
				throw new IllegalArgumentException();
			}
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
			// [SIFISC-26050] on ignore les exercices commerciaux d'avant 1800 car les dates correspondantes ne sont pas exposables dans la XSD
			if (ex.getDateFin().year() >= 1800) {
				corporation.getBusinessYears().add(BusinessYearBuilder.newBusinessYear(ex));
			}
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
	public Corporation clone(Corporation right, @Nullable Set<InternalPartyPart> parts) {
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
	protected void copyParts(Corporation to, Corporation from, @Nullable Set<InternalPartyPart> parts, CopyMode mode) {
		super.copyParts(to, from, parts, mode);

		if (parts != null && parts.contains(InternalPartyPart.LEGAL_SEATS)) {
			copyColl(to.getLegalSeats(), from.getLegalSeats());
		}

		if (parts != null && parts.contains(InternalPartyPart.LEGAL_FORMS)) {
			copyColl(to.getLegalForms(), from.getLegalForms());
		}

		if (parts != null && parts.contains(InternalPartyPart.CAPITALS)) {
			copyColl(to.getCapitals(), from.getCapitals());
		}

		if (parts != null && parts.contains(InternalPartyPart.CORPORATION_STATUSES)) {
			copyColl(to.getCorporationStatuses(), from.getCorporationStatuses());
		}

		if (parts != null && parts.contains(InternalPartyPart.TAX_SYSTEMS)) {
			copyColl(to.getTaxSystemsVD(), from.getTaxSystemsVD());
			copyColl(to.getTaxSystemsCH(), from.getTaxSystemsCH());
		}

		if (parts != null && parts.contains(InternalPartyPart.TAX_LIGHTENINGS)) {
			copyColl(to.getTaxLightenings(), from.getTaxLightenings());
		}

		if (parts != null && parts.contains(InternalPartyPart.BUSINESS_YEARS)) {
			copyColl(to.getBusinessYears(), from.getBusinessYears());
		}

		if (parts != null && parts.contains(InternalPartyPart.CORPORATION_FLAGS)) {
			copyColl(to.getCorporationFlags(), from.getCorporationFlags());
		}

		if (parts != null && (parts.contains(InternalPartyPart.REAL_LAND_RIGHTS) ||
				parts.contains(InternalPartyPart.VIRTUAL_TRANSITIVE_LAND_RIGHTS) ||
				parts.contains(InternalPartyPart.VIRTUAL_INHERITED_REAL_LAND_RIGHTS) ||
				parts.contains(InternalPartyPart.VIRTUAL_INHERITED_VIRTUAL_LAND_RIGHTS))) {
			copyLandRights(to, from, parts, mode);
		}

		if (parts != null && (parts.contains(InternalPartyPart.LAND_TAX_LIGHTENINGS) ||
				parts.contains(InternalPartyPart.VIRTUAL_LAND_TAX_LIGHTENINGS))) {
			copyLandTaxLightenings(to, from, parts);
		}

		if (parts != null && parts.contains(InternalPartyPart.OPERATING_PERIODS)) {
			copyColl(to.getOperatingPeriods(), from.getOperatingPeriods());
		}
	}

	private void initLandRights(Corporation to, Entreprise entreprise, @NotNull Set<InternalPartyPart> parts, Context context) {

		final boolean includeVirtualTransitive = parts.contains(InternalPartyPart.VIRTUAL_TRANSITIVE_LAND_RIGHTS) || parts.contains(InternalPartyPart.VIRTUAL_INHERITED_VIRTUAL_LAND_RIGHTS);
		final boolean includeVirtualInheritance = parts.contains(InternalPartyPart.VIRTUAL_INHERITED_REAL_LAND_RIGHTS) || parts.contains(InternalPartyPart.VIRTUAL_INHERITED_VIRTUAL_LAND_RIGHTS);
		final List<DroitRF> droits = context.registreFoncierService.getDroitsForCtb(entreprise, includeVirtualTransitive, includeVirtualInheritance);

		// [SIFISC-24999] si l'entreprise est absorbée dans une autre entreprise, on l'indique sur les droits de propriété
		final RegDate dateDebutFusion = entreprise.getRapportsSujet().stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(FusionEntreprises.class::isInstance)
				.map(HibernateDateRangeEntity::getDateDebut)
				.min(RegDate::compareTo)
				.orElse(null);

		final List<LandRight> landRights = to.getLandRights();
		droits.stream()
				.sorted(new DroitRFRangeMetierComparator())
				.map((droitRF) -> LandRightBuilder.newLandRight(droitRF,
				                                                context.registreFoncierService::getContribuableIdFor,
				                                                new EasementRightHolderComparator(context.tiersService)))
				.filter(r -> NaturalPersonStrategy.rightMatchesPart(r, parts))
				.map(d -> updateDateDebutFusion(d, dateDebutFusion))
				.forEach(landRights::add);
	}

	@NotNull
	public static LandRight updateDateDebutFusion(@NotNull LandRight landRight, @Nullable RegDate dateDebutHeritage) {
		if (dateDebutHeritage != null) {
			if (landRight instanceof LandOwnershipRight) {
				((LandOwnershipRight) landRight).setDateInheritedTo(DataHelper.coreToXMLv2(dateDebutHeritage));
			}
			// [IMM-1105] les servitudes sont bien transferées d'une entreprise à l'autre en cas de fusion : il faut bien renseigner cette date
			else if (landRight instanceof UsufructRight) {
				((UsufructRight) landRight).setDateInheritedTo(DataHelper.coreToXMLv2(dateDebutHeritage));
			}
			else if (landRight instanceof HousingRight) {
				((HousingRight) landRight).setDateInheritedTo(DataHelper.coreToXMLv2(dateDebutHeritage));
			}
			else if (landRight instanceof VirtualLandOwnershipRight) {
				((VirtualLandOwnershipRight) landRight).setDateInheritedTo(DataHelper.coreToXMLv2(dateDebutHeritage));
			}
			else if (landRight instanceof VirtualUsufructRight) {
				((VirtualUsufructRight) landRight).setDateInheritedTo(DataHelper.coreToXMLv2(dateDebutHeritage));
			}
		}
		return landRight;
	}

	private static void copyLandRights(Corporation to, Corporation from, Set<InternalPartyPart> parts, CopyMode mode) {

		if (!parts.contains(InternalPartyPart.REAL_LAND_RIGHTS) &&
				!parts.contains(InternalPartyPart.VIRTUAL_TRANSITIVE_LAND_RIGHTS) &&
				!parts.contains(InternalPartyPart.VIRTUAL_INHERITED_REAL_LAND_RIGHTS) &&
				!parts.contains(InternalPartyPart.VIRTUAL_INHERITED_VIRTUAL_LAND_RIGHTS)) {
			throw new IllegalArgumentException("Au moins une des parts REAL_LAND_RIGHTS, VIRTUAL_TRANSITIVE_LAND_RIGHTS, VIRTUAL_INHERITED_REAL_LAND_RIGHTS ou VIRTUAL_INHERITED_VIRTUAL_LAND_RIGHTS doit être spécifiée.");
		}

		// Les parts internes sur les droits ne se recoupent pas : on doit retourner exactement ce que l'on nous demande (pas d'interprétation)
		if (mode == CopyMode.EXCLUSIVE) {
			to.getLandRights().clear();
		}
		to.getLandRights().addAll(from.getLandRights().stream()
				                          .filter(r -> NaturalPersonStrategy.rightMatchesPart(r, parts))
				                          .collect(Collectors.toList()));
	}

	void initLandTaxLightenings(Corporation to, Entreprise entreprise, Set<InternalPartyPart> parts, Context context) {

		if (parts != null && parts.contains(InternalPartyPart.LAND_TAX_LIGHTENINGS)) {
			// les exonérations
			final List<IfoncExemption> exemptions = to.getIfoncExemptions();
			entreprise.getAllegementsFonciers().stream()
					.filter(AnnulableHelper::nonAnnule)
					.filter(ExonerationIFONC.class::isInstance)
					.map(ExonerationIFONC.class::cast)
					.sorted(new DateRangeComparator<AllegementFoncier>().thenComparing(a -> a.getImmeuble().getId()))
					.map(LandTaxLighteningBuilder::buildIfoncExemption)
					.forEach(exemptions::add);

			// les dégrèvements
			final List<IciAbatement> abatements = to.getIciAbatements();
			entreprise.getAllegementsFonciers().stream()
					.filter(AnnulableHelper::nonAnnule)
					.filter(DegrevementICI.class::isInstance)
					.map(DegrevementICI.class::cast)
					.sorted(new DateRangeComparator<AllegementFoncier>().thenComparing(a -> a.getImmeuble().getId()))
					.map(LandTaxLighteningBuilder::buildIciAbatement)
					.forEach(abatements::add);

			// les demandes de dégrèvements
			final List<IciAbatementRequest> requests = to.getIciAbatementRequests();
			entreprise.getAutresDocumentsFiscaux().stream()
					.filter(AnnulableHelper::nonAnnule)
					.filter(DemandeDegrevementICI.class::isInstance)
					.map(DemandeDegrevementICI.class::cast)
					.sorted(Comparator.<DemandeDegrevementICI, RegDate>comparing(AutreDocumentFiscal::getDateEnvoi).thenComparing(a -> a.getImmeuble().getId()))
					.map(LandTaxLighteningBuilder::buildIciAbatementRequest)
					.forEach(requests::add);
		}

		// les allégements virtuels
		if (parts != null && parts.contains(InternalPartyPart.VIRTUAL_LAND_TAX_LIGHTENINGS)) {
			final List<VirtualLandTaxLightening> virtuals = to.getVirtualLandTaxLightenings();
			context.registreFoncierService.determineAllegementsFonciersVirtuels(entreprise).stream()
					.map(LandTaxLighteningBuilder::buildVirtualLandTaxLightening)
					.forEach(virtuals::add);
		}
	}

	private static void copyLandTaxLightenings(Corporation to, Corporation from, @Nullable Set<InternalPartyPart> parts) {
		if (parts != null && parts.contains(InternalPartyPart.LAND_TAX_LIGHTENINGS)) {
			copyColl(to.getIfoncExemptions(), from.getIfoncExemptions());
			copyColl(to.getIciAbatements(), from.getIciAbatements());
			copyColl(to.getIciAbatementRequests(), from.getIciAbatementRequests());
		}
		if (parts != null && parts.contains(InternalPartyPart.VIRTUAL_LAND_TAX_LIGHTENINGS)) {
			copyColl(to.getVirtualLandTaxLightenings(), from.getVirtualLandTaxLightenings());
		}
	}

	private void initOperatingPeriods(Corporation to, Entreprise from, Context context) {
		final List<OperatingPeriod> toPeriods = to.getOperatingPeriods();
		final List<DateRange> periodes = context.periodeExploitationService.determinePeriodesExploitation(from, PeriodeContext.THEORIQUE);
		periodes.stream()
				.map(OperatingPeriodBuilder::newPeriod)
				.forEach(toPeriods::add);
	}

}
