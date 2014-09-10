package ch.vd.uniregctb.xml.party.v3.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ch.ech.ech0007.v4.CantonAbbreviation;
import ch.ech.ech0044.v3.NamedPersonId;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.data.Origine;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.civil.data.PermisList;
import ch.vd.unireg.interfaces.civil.rcpers.EchHelper;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.party.person.v3.Nationality;
import ch.vd.unireg.xml.party.person.v3.NaturalPerson;
import ch.vd.unireg.xml.party.person.v3.NaturalPersonCategory;
import ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType;
import ch.vd.unireg.xml.party.person.v3.Origin;
import ch.vd.unireg.xml.party.person.v3.ParentFullName;
import ch.vd.unireg.xml.party.taxresidence.v2.WithholdingTaxationPeriod;
import ch.vd.unireg.xml.party.v3.PartyPart;
import ch.vd.unireg.xml.party.v3.UidNumberList;
import ch.vd.uniregctb.metier.piis.PeriodeImpositionImpotSource;
import ch.vd.uniregctb.metier.piis.PeriodeImpositionImpotSourceServiceException;
import ch.vd.uniregctb.tiers.IdentificationEntreprise;
import ch.vd.uniregctb.tiers.IdentificationPersonne;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.xml.Context;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;
import ch.vd.uniregctb.xml.ExceptionHelper;
import ch.vd.uniregctb.xml.ServiceException;
import ch.vd.uniregctb.xml.party.v3.WithholdingTaxationPeriodBuilder;

public class NaturalPersonStrategy extends TaxPayerStrategy<NaturalPerson> {

	private static final Logger LOGGER = LoggerFactory.getLogger(NaturalPersonStrategy.class);
	private static final String CH_AHV = "CH.AHV"; // voir spécification eCH-0044
	private static final String CH_ZAR = "CH.ZAR";

	@Override
	public NaturalPerson newFrom(ch.vd.uniregctb.tiers.Tiers right, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {
		final NaturalPerson pp = new NaturalPerson();
		initBase(pp, right, context);
		initParts(pp, right, parts, context);
		return pp;
	}

	@Override
	public NaturalPerson clone(NaturalPerson right, @Nullable Set<PartyPart> parts) {
		final NaturalPerson pp = new NaturalPerson();
		copyBase(pp, right);
		copyParts(pp, right, parts, CopyMode.EXCLUSIVE);
		return pp;
	}

	@Override
	protected void initBase(NaturalPerson to, ch.vd.uniregctb.tiers.Tiers from, Context context) throws ServiceException {
		super.initBase(to, from, context);

		final ch.vd.uniregctb.tiers.PersonnePhysique personne = (ch.vd.uniregctb.tiers.PersonnePhysique) from;
		if (!personne.isHabitantVD()) {
			to.setOfficialName(personne.getNom());
			to.setFirstName(personne.getPrenomUsuel());
			to.setFirstNames(personne.getTousPrenoms());
			to.setSex(EnumHelper.coreToXMLv3(personne.getSexe()));
			to.setVn(EchHelper.avs13ToEch(personne.getNumeroAssureSocial()));
			for (IdentificationPersonne ident : personne.getIdentificationsPersonnes()) {
				to.getOtherPersonId().add(newNamedPersonId(ident));
			}
			to.setDateOfBirth(DataHelper.coreToPartialDateXmlv2(personne.getDateNaissance()));
			to.setDateOfDeath(DataHelper.coreToXMLv2(personne.getDateDeces()));

			final NaturalPersonCategoryType categoryType = EnumHelper.coreToXMLv3(personne.getCategorieEtranger());
			to.getCategories().add(new NaturalPersonCategory(DataHelper.coreToXMLv2(personne.getDateDebutValiditeAutorisation()), null, categoryType, null));

			// les noms et prénoms des parents (SIFISC-12136)
			if (StringUtils.isNotBlank(personne.getPrenomsMere()) || StringUtils.isNotBlank(personne.getNomMere())) {
				to.setMotherName(new ParentFullName(personne.getPrenomsMere(), personne.getNomMere()));
			}
			if (StringUtils.isNotBlank(personne.getPrenomsPere()) || StringUtils.isNotBlank(personne.getNomPere())) {
				to.setFatherName(new ParentFullName(personne.getPrenomsPere(), personne.getNomPere()));
			}

			// [SIFISC-13351] la nationalité, le nom de naissance et la commune d'origine
			// TODO JDE à implémenter
			to.setBirthName(null);
			if (personne.getNumeroOfsNationalite() != null) {
				final int ofs = personne.getNumeroOfsNationalite();
				final Nationality.Swiss swiss = ofs == ServiceInfrastructureRaw.noOfsSuisse ? new Nationality.Swiss() : null;
				final Nationality.Stateless stateless = ofs == ServiceInfrastructureRaw.noPaysApatride ? new Nationality.Stateless() : null;
				final Integer foreignCountry = swiss == null && stateless == null ? ofs : null;
				to.getNationalities().add(new Nationality(null, null, swiss, stateless, foreignCountry, null));
			}
			if (StringUtils.isNotBlank(personne.getLibelleCommuneOrigine())) {
				// TODO JDE à implémenter...
				to.getOrigins();
			}
		}
		else {
			final Individu individu = context.serviceCivilService.getIndividu(personne.getNumeroIndividu(), null, AttributeIndividu.PERMIS, AttributeIndividu.NATIONALITES, AttributeIndividu.ORIGINE);

			if (individu == null) {
				final String message = String.format("Impossible de trouver l'individu n°%d pour l'habitant n°%d", personne
						.getNumeroIndividu(), personne.getNumero());
				LOGGER.error(message);
				throw ExceptionHelper.newBusinessException(message, BusinessExceptionCode.UNKNOWN_INDIVIDUAL);
			}

			to.setOfficialName(individu.getNom());
			to.setFirstName(individu.getPrenomUsuel());
			to.setFirstNames(individu.getTousPrenoms());
			to.setSex(EnumHelper.coreToXMLv3(individu.getSexe()));
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
					                                                 EnumHelper.coreToXMLv3(permis.getTypePermis()), null));
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

		// les permis sont toujours renseignés (pas de PART spécifique)
		copyColl(to.getCategories(), from.getCategories());

		//la liste des IDE
		to.setUidNumbers(from.getUidNumbers());
	}

	@Override
	protected void initParts(NaturalPerson to, Tiers from, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {
		super.initParts(to, from, parts, context);

		final PersonnePhysique pp = (PersonnePhysique) from;
		if (parts != null && parts.contains(PartyPart.WITHHOLDING_TAXATION_PERIODS)) {
			initWithholdingTaxationPeriods(to, pp, context);
		}
	}

	@Override
	protected void copyParts(NaturalPerson to, NaturalPerson from, @Nullable Set<PartyPart> parts, CopyMode mode) {
		super.copyParts(to, from, parts, mode);

		if (parts != null && parts.contains(PartyPart.WITHHOLDING_TAXATION_PERIODS)) {
			copyColl(to.getWithholdingTaxationPeriods(), from.getWithholdingTaxationPeriods());
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
}
