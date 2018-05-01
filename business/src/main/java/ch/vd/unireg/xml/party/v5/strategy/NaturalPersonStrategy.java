package ch.vd.unireg.xml.party.v5.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ch.ech.ech0007.v4.CantonAbbreviation;
import ch.ech.ech0044.v3.NamedPersonId;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.HibernateDateRangeEntity;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.data.Origine;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.civil.data.PermisList;
import ch.vd.unireg.interfaces.civil.rcpers.EchHelper;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.metier.piis.PeriodeImpositionImpotSource;
import ch.vd.unireg.metier.piis.PeriodeImpositionImpotSourceServiceException;
import ch.vd.unireg.registrefoncier.DroitRF;
import ch.vd.unireg.registrefoncier.DroitRFRangeMetierComparator;
import ch.vd.unireg.tiers.Heritage;
import ch.vd.unireg.tiers.IdentificationEntreprise;
import ch.vd.unireg.tiers.IdentificationPersonne;
import ch.vd.unireg.tiers.IndividuNotFoundException;
import ch.vd.unireg.tiers.OriginePersonnePhysique;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.xml.Context;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.EnumHelper;
import ch.vd.unireg.xml.ExceptionHelper;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.party.landregistry.v1.LandOwnershipRight;
import ch.vd.unireg.xml.party.landregistry.v1.LandRight;
import ch.vd.unireg.xml.party.landregistry.v1.RealLandRight;
import ch.vd.unireg.xml.party.landregistry.v1.VirtualInheritedLandRight;
import ch.vd.unireg.xml.party.landregistry.v1.VirtualLandOwnershipRight;
import ch.vd.unireg.xml.party.landregistry.v1.VirtualTransitiveLandRight;
import ch.vd.unireg.xml.party.person.v5.Nationality;
import ch.vd.unireg.xml.party.person.v5.NaturalPerson;
import ch.vd.unireg.xml.party.person.v5.NaturalPersonCategory;
import ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType;
import ch.vd.unireg.xml.party.person.v5.Origin;
import ch.vd.unireg.xml.party.person.v5.ParentFullName;
import ch.vd.unireg.xml.party.person.v5.ResidencyPeriod;
import ch.vd.unireg.xml.party.taxresidence.v4.WithholdingTaxationPeriod;
import ch.vd.unireg.xml.party.v5.EasementRightHolderComparator;
import ch.vd.unireg.xml.party.v5.InternalPartyPart;
import ch.vd.unireg.xml.party.v5.LandRightBuilder;
import ch.vd.unireg.xml.party.v5.ResidencyPeriodBuilder;
import ch.vd.unireg.xml.party.v5.UidNumberList;
import ch.vd.unireg.xml.party.v5.WithholdingTaxationPeriodBuilder;

@SuppressWarnings("Duplicates")
public class NaturalPersonStrategy extends TaxPayerStrategy<NaturalPerson> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ch.vd.unireg.xml.party.v3.strategy.NaturalPersonStrategy.class);
	private static final String CH_AHV = "CH.AHV"; // voir spécification eCH-0044
	private static final String CH_ZAR = "CH.ZAR";

	@Override
	public NaturalPerson newFrom(Tiers right, @Nullable Set<InternalPartyPart> parts, Context context) throws ServiceException {
		final NaturalPerson pp = new NaturalPerson();
		initBase(pp, right, context);
		initParts(pp, right, parts, context);
		return pp;
	}

	@Override
	public NaturalPerson clone(NaturalPerson right, @Nullable Set<InternalPartyPart> parts) {
		final NaturalPerson pp = new NaturalPerson();
		copyBase(pp, right);
		copyParts(pp, right, parts, CopyMode.EXCLUSIVE);
		return pp;
	}

	@Override
	protected void initBase(NaturalPerson to, Tiers from, Context context) throws ServiceException {
		super.initBase(to, from, context);

		final PersonnePhysique personne = (PersonnePhysique) from;
		if (!personne.isHabitantVD()) {
			to.setOfficialName(personne.getNom());
			to.setFirstName(personne.getPrenomUsuel());
			to.setFirstNames(personne.getTousPrenoms());
			to.setSex(EnumHelper.coreToXMLv5(personne.getSexe()));
			to.setVn(EchHelper.avs13ToEch(personne.getNumeroAssureSocial()));
			for (IdentificationPersonne ident : personne.getIdentificationsPersonnes()) {
				to.getOtherPersonId().add(newNamedPersonId(ident));
			}
			to.setDateOfBirth(DataHelper.coreToPartialDateXmlv2(personne.getDateNaissance()));
			to.setDateOfDeath(DataHelper.coreToXMLv2(personne.getDateDeces()));

			final NaturalPersonCategoryType categoryType = EnumHelper.coreToXMLv5(personne.getCategorieEtranger());
			if (categoryType != null) {
				to.getCategories().add(new NaturalPersonCategory(DataHelper.coreToXMLv2(personne.getDateDebutValiditeAutorisation()), null, categoryType, null));
			}

			// les noms et prénoms des parents (SIFISC-12136)
			if (StringUtils.isNotBlank(personne.getPrenomsMere()) || StringUtils.isNotBlank(personne.getNomMere())) {
				to.setMotherName(new ParentFullName(personne.getPrenomsMere(), personne.getNomMere()));
			}
			if (StringUtils.isNotBlank(personne.getPrenomsPere()) || StringUtils.isNotBlank(personne.getNomPere())) {
				to.setFatherName(new ParentFullName(personne.getPrenomsPere(), personne.getNomPere()));
			}

			// [SIFISC-13351] la nationalité, le nom de naissance et la commune d'origine
			to.setBirthName(personne.getNomNaissance());
			if (personne.getNumeroOfsNationalite() != null) {
				final int ofs = personne.getNumeroOfsNationalite();
				final Nationality.Swiss swiss = ofs == ServiceInfrastructureRaw.noOfsSuisse ? new Nationality.Swiss() : null;
				final Nationality.Stateless stateless = ofs == ServiceInfrastructureRaw.noPaysApatride ? new Nationality.Stateless() : null;
				final Integer foreignCountry = swiss == null && stateless == null ? ofs : null;
				to.getNationalities().add(new Nationality(null, null, swiss, stateless, foreignCountry, null));
			}
			final OriginePersonnePhysique origine = personne.getOrigine();
			if (origine != null) {
				to.getOrigins().add(new Origin(StringUtils.abbreviate(origine.getLibelle(), 50), CantonAbbreviation.valueOf(origine.getSigleCanton())));
			}
		}
		else {
			final Individu individu = context.serviceCivilService.getIndividu(personne.getNumeroIndividu(), null, AttributeIndividu.PERMIS, AttributeIndividu.NATIONALITES, AttributeIndividu.ORIGINE);
			if (individu == null) {
				throw new IndividuNotFoundException(personne);
			}

			to.setOfficialName(individu.getNom());
			to.setFirstName(individu.getPrenomUsuel());
			to.setFirstNames(individu.getTousPrenoms());
			to.setSex(EnumHelper.coreToXMLv5(individu.getSexe()));
			to.setVn(EchHelper.avs13ToEch(individu.getNouveauNoAVS()));
			if (StringUtils.isNotBlank(individu.getNoAVS11())) {
				to.getOtherPersonId().add(new NamedPersonId(CH_AHV, individu.getNoAVS11())); // selon le document STAN_d_DEF_2010-06-11_eCH-0044_Personenidentifikation.pdf
			}
			if (StringUtils.isNotBlank(individu.getNumeroRCE())) {
				to.getOtherPersonId().add(new NamedPersonId(CH_ZAR, individu.getNumeroRCE()));  // [SIFISC-4352]
			}
			to.setDateOfBirth(DataHelper.coreToPartialDateXmlv2(individu.getDateNaissance()));
			to.setDateOfDeath(DataHelper.coreToXMLv2(personne.getDateDeces() == null ? individu.getDateDeces() : personne.getDateDeces()));

			final PermisList list = individu.getPermis();
			if (list == null || list.isEmpty()) {
				to.getCategories().add(new NaturalPersonCategory(null, null, NaturalPersonCategoryType.SWISS, null));
			}
			else {
				// l'historique des permis (SIFISC-8072)
				for (Permis permis : list) {
					to.getCategories().add(new NaturalPersonCategory(DataHelper.coreToXMLv2(permis.getDateDebut()),
					                                                 DataHelper.coreToXMLv2(permis.getDateFin()),
					                                                 EnumHelper.coreToXMLv5(permis.getTypePermis()), null));
				}
			}

			// les noms et prénoms des parents (SIFISC-12136)
			final NomPrenom npMere = individu.getNomOfficielMere();
			if (npMere != null) {
				to.setMotherName(new ParentFullName(npMere.getPrenom(), npMere.getNom()));
			}
			final NomPrenom npPere = individu.getNomOfficielPere();
			if (npPere != null) {
				to.setFatherName(new ParentFullName(npPere.getPrenom(), npPere.getNom()));
			}

			// [SIFISC-13351] la nationalité, le nom de naissance et la commune d'origine
			to.setBirthName(individu.getNomNaissance());
			if (individu.getNationalites() != null && !individu.getNationalites().isEmpty()) {
				for (Nationalite nat : individu.getNationalites()) {
					final int ofs = nat.getPays().getNoOFS();
					final Nationality.Swiss swiss = ofs == ServiceInfrastructureRaw.noOfsSuisse ? new Nationality.Swiss() : null;
					final Nationality.Stateless stateless = ofs == ServiceInfrastructureRaw.noPaysApatride ? new Nationality.Stateless() : null;
					final Integer foreignCountry = swiss == null && stateless == null ? ofs : null;
					to.getNationalities().add(new Nationality(DataHelper.coreToXMLv2(nat.getDateDebut()),
					                                          DataHelper.coreToXMLv2(nat.getDateFin()),
				                                              swiss,
				                                              stateless,
				                                              foreignCountry,
				                                              null));
				}
			}
			if (individu.getOrigines() != null && !individu.getOrigines().isEmpty()) {
				for (Origine org : individu.getOrigines()) {
					to.getOrigins().add(new Origin(org.getNomLieu(), CantonAbbreviation.valueOf(org.getSigleCanton())));
				}
			}
		}

		//L'exposition du numéro IDE
		final Set<IdentificationEntreprise> ides = personne.getIdentificationsEntreprise();
		if (ides != null && !ides.isEmpty()) {
			final List<String> ideList = new ArrayList<>(ides.size());
			for (IdentificationEntreprise ide : ides) {
				ideList.add(ide.getNumeroIde());
			}
			to.setUidNumbers(new UidNumberList(ideList));
		}
	}

	@Override
	protected void copyBase(NaturalPerson to, NaturalPerson from) {
		super.copyBase(to, from);
		to.setOfficialName(from.getOfficialName());
		to.setFirstName(from.getFirstName());
		to.setFirstNames(from.getFirstNames());
		to.setSex(from.getSex());
		to.setVn(from.getVn());
		to.getOtherPersonId().clear();
		to.getOtherPersonId().addAll(DataHelper.deepCloneV3(from.getOtherPersonId()));
		to.setDateOfBirth(from.getDateOfBirth());
		to.setDateOfDeath(from.getDateOfDeath());

		// les noms et prénoms des parents (SIFISC-12136)
		to.setMotherName(from.getMotherName());
		to.setFatherName(from.getFatherName());

		// les nom de naissance, origines et nationalités (SIFISC-13351 & SIFISC-13558)
		to.setBirthName(from.getBirthName());
		copyColl(to.getNationalities(), from.getNationalities());
		copyColl(to.getOrigins(), from.getOrigins());

		// les permis sont toujours renseignés (pas de PART spécifique)
		copyColl(to.getCategories(), from.getCategories());

		// la liste des IDE
		to.setUidNumbers(from.getUidNumbers());
	}

	@Override
	protected void initParts(NaturalPerson to, Tiers from, @Nullable Set<InternalPartyPart> parts, Context context) throws ServiceException {
		super.initParts(to, from, parts, context);

		final PersonnePhysique pp = (PersonnePhysique) from;
		if (parts != null && parts.contains(InternalPartyPart.WITHHOLDING_TAXATION_PERIODS)) {
			initWithholdingTaxationPeriods(to, pp, context);
		}
		if (parts != null && parts.contains(InternalPartyPart.RESIDENCY_PERIODS)) {
			initResidencyPeriods(to, pp, context);
		}
		if (parts != null && (parts.contains(InternalPartyPart.LAND_RIGHTS) || parts.contains(InternalPartyPart.VIRTUAL_LAND_RIGHTS) || parts.contains(InternalPartyPart.VIRTUAL_INHERITANCE_LAND_RIGHTS))) {
			initLandRights(to, pp, parts, context);
		}
	}

	@Override
	protected void copyParts(NaturalPerson to, NaturalPerson from, @Nullable Set<InternalPartyPart> parts, CopyMode mode) {
		super.copyParts(to, from, parts, mode);

		if (parts != null && parts.contains(InternalPartyPart.WITHHOLDING_TAXATION_PERIODS)) {
			copyColl(to.getWithholdingTaxationPeriods(), from.getWithholdingTaxationPeriods());
		}
		if (parts != null && parts.contains(InternalPartyPart.RESIDENCY_PERIODS)) {
			copyColl(to.getResidencyPeriods(), from.getResidencyPeriods());
		}
		if (parts != null && (parts.contains(InternalPartyPart.LAND_RIGHTS) || parts.contains(InternalPartyPart.VIRTUAL_LAND_RIGHTS) || parts.contains(InternalPartyPart.VIRTUAL_INHERITANCE_LAND_RIGHTS))) {
			copyLandRights(to, from, parts, mode);
		}
	}

	private static NamedPersonId newNamedPersonId(IdentificationPersonne ident) {
		final NamedPersonId id;
		switch (ident.getCategorieIdentifiant()) {
		case CH_AHV_AVS:
			id = new NamedPersonId(CH_AHV, ident.getIdentifiant()); // [SIFISC-4352]
			break;
		case CH_ZAR_RCE:
			id = new NamedPersonId(CH_ZAR, ident.getIdentifiant());
			break;
		default:
			throw new IllegalArgumentException("Catégorie d'identification inconnue [" + ident.getCategorieIdentifiant() + "]");
		}
		return id;
	}

	private static void initWithholdingTaxationPeriods(NaturalPerson left, PersonnePhysique pp, Context context) throws ServiceException {

		final List<PeriodeImpositionImpotSource> list;
		try {
			list = context.periodeImpositionImpotSourceService.determine(pp);
		}
		catch (PeriodeImpositionImpotSourceServiceException e) {
			LOGGER.error(e.getMessage(), e);
			throw ExceptionHelper.newBusinessException(e, BusinessExceptionCode.TAX_LIABILITY);
		}

		if (list != null && !list.isEmpty()) {
			for (PeriodeImpositionImpotSource p : list) {
				final WithholdingTaxationPeriod period = WithholdingTaxationPeriodBuilder.newWithholdingTaxationPeriod(p);
				left.getWithholdingTaxationPeriods().add(period);
			}
		}
	}

	private static void initResidencyPeriods(NaturalPerson left, PersonnePhysique pp, Context context) throws ServiceException {
		final List<DateRange> list;
		try {
			list = context.tiersService.getPeriodesDeResidence(pp, false);
		}
		catch (IndividuNotFoundException e) {
			LOGGER.error(e.getMessage(), e);
			throw ExceptionHelper.newBusinessException(e, BusinessExceptionCode.UNKNOWN_INDIVIDUAL);
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw ExceptionHelper.newBusinessException(e, BusinessExceptionCode.INFRASTRUCTURE);
		}

		final List<ResidencyPeriod> residencyPeriods = left.getResidencyPeriods();
		list.stream()
				.map(ResidencyPeriodBuilder::newPeriod)
				.forEach(residencyPeriods::add);
	}

	private void initLandRights(NaturalPerson to, PersonnePhysique pp, @NotNull Set<InternalPartyPart> parts, Context context) {

		final boolean includeVirtualTransitive = parts.contains(InternalPartyPart.VIRTUAL_LAND_RIGHTS);
		final boolean includeVirtualInheritance = parts.contains(InternalPartyPart.VIRTUAL_INHERITANCE_LAND_RIGHTS);
		final List<DroitRF> droits = context.registreFoncierService.getDroitsForCtb(pp, includeVirtualTransitive, includeVirtualInheritance);

		// [SIFISC-24999] si la personne physique possède des héritiers, on l'indique sur les droits de propriété
		final RegDate dateDebutHeritage = pp.getRapportsObjet().stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(Heritage.class::isInstance)
				.map(HibernateDateRangeEntity::getDateDebut)
				.min(RegDate::compareTo)
				.orElse(null);

		final List<LandRight> landRights = to.getLandRights();
		droits.stream()
				.sorted(new DroitRFRangeMetierComparator())
				.map((droitRF) -> LandRightBuilder.newLandRight(droitRF,
				                                                context.registreFoncierService::getContribuableIdFor,
				                                                new EasementRightHolderComparator(context.tiersService)))
				.map(d -> updateDateDebutHeritage(d, dateDebutHeritage))
				.forEach(landRights::add);
	}

	@NotNull
	public static LandRight updateDateDebutHeritage(@NotNull LandRight landRight, @Nullable RegDate dateDebutHeritage) {
		if (dateDebutHeritage != null) {
			if (landRight instanceof LandOwnershipRight) {
				((LandOwnershipRight) landRight).setDateInheritedTo(DataHelper.coreToXMLv2(dateDebutHeritage));
			}
			else if (landRight instanceof VirtualLandOwnershipRight) {
				((VirtualLandOwnershipRight) landRight).setDateInheritedTo(DataHelper.coreToXMLv2(dateDebutHeritage));
			}
// [IMM-1105] les héritages entre personnes physiques n'influencent pas les servitudes : il ne faut jamais renseigner cette date
//			else if (landRight instanceof UsufructRight) {
//				((UsufructRight) landRight).setDateInheritedTo(DataHelper.coreToXMLv2(dateDebutHeritage));
//			}
//			else if (landRight instanceof HousingRight) {
//				((HousingRight) landRight).setDateInheritedTo(DataHelper.coreToXMLv2(dateDebutHeritage));
//			}
//			else if (landRight instanceof VirtualUsufructRight) {
//				((VirtualUsufructRight) landRight).setDateInheritedTo(DataHelper.coreToXMLv2(dateDebutHeritage));
//			}
		}
		return landRight;
	}

	@SuppressWarnings("StatementWithEmptyBody")
	private static void copyLandRights(NaturalPerson to, NaturalPerson from, Set<InternalPartyPart> parts, CopyMode mode) {

		if (!parts.contains(InternalPartyPart.LAND_RIGHTS) && !parts.contains(InternalPartyPart.VIRTUAL_LAND_RIGHTS) && !parts.contains(InternalPartyPart.VIRTUAL_INHERITANCE_LAND_RIGHTS)) {
			throw new IllegalArgumentException("Au moins un des parts LAND_RIGHTS, VIRTUAL_LAND_RIGHTS ou VIRTUAL_INHERITANCE_LAND_RIGHTS doit être spécifiée.");
		}

		// Les droits réels et les droits virtuels représentent deux ensembles qui se recoupent.
		// Plus précisemment, les droits réels sont entièrement contenus dans les droits virtuels. En fonction
		// du mode de copie, il est donc nécessaire de compléter ou de filtrer les droits.
		if (mode == CopyMode.ADDITIVE) {
			if (to.getLandRights() == null || to.getLandRights().isEmpty()
					|| (parts.contains(InternalPartyPart.VIRTUAL_LAND_RIGHTS) && parts.contains(InternalPartyPart.VIRTUAL_INHERITANCE_LAND_RIGHTS))) {
				// la collection de destination est vide (ou la source contient tous les droits), on copie tout
				copyColl(to.getLandRights(), from.getLandRights());
			}
			else if (parts.contains(InternalPartyPart.VIRTUAL_LAND_RIGHTS)) {
				// la collection de destination n'est pas vide, on ajoute uniquement les droits virtuels transitifs
				to.getLandRights().addAll(from.getLandRights().stream()
						                          .filter(VirtualTransitiveLandRight.class::isInstance)
						                          .collect(Collectors.toList()));
			}
			else if (parts.contains(InternalPartyPart.VIRTUAL_INHERITANCE_LAND_RIGHTS)) {
				// la collection de destination n'est pas vide, on ajoute uniquement les droits virtuels hérités
				to.getLandRights().addAll(from.getLandRights().stream()
						                          .filter(VirtualInheritedLandRight.class::isInstance)
						                          .collect(Collectors.toList()));
			}
			else {
				// rien à faire: soit la destination est vide et les droits réels sont copiés, soit les droits virtuels sont demandés et on n'arrive pas ici.
			}
		}
		else {
			Assert.isEqual(CopyMode.EXCLUSIVE, mode);
			if (parts.contains(InternalPartyPart.VIRTUAL_LAND_RIGHTS) && parts.contains(InternalPartyPart.VIRTUAL_INHERITANCE_LAND_RIGHTS)) {
				// on veut tous les droits, on copie tout
				copyColl(to.getLandRights(), from.getLandRights());
			}
			else {
				// on ajoute les droits réels et les droits virtuels transitifs/hérités s'ils sont demandés
				if (from.getLandRights() != null && !from.getLandRights().isEmpty()) {
					to.getLandRights().clear();
					to.getLandRights().addAll(from.getLandRights().stream()
							                          .filter(r -> rightMatchesPart(r, parts))
							                          .collect(Collectors.toList()));
				}
				else {
					to.getLandRights().clear();
				}
			}
		}
	}

	private static boolean rightMatchesPart(LandRight right, Set<InternalPartyPart> parts) {
		return right instanceof RealLandRight
				|| (right instanceof VirtualTransitiveLandRight && parts.contains(InternalPartyPart.VIRTUAL_LAND_RIGHTS))
				|| (right instanceof VirtualInheritedLandRight && parts.contains(InternalPartyPart.VIRTUAL_INHERITANCE_LAND_RIGHTS));
	}
}
