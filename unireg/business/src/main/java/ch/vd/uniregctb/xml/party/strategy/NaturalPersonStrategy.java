package ch.vd.uniregctb.xml.party.strategy;

import java.util.Set;

import ch.ech.ech0044.v2.NamedPersonId;
import ch.ech.ech0044.v2.PersonIdentification;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.civil.data.PermisList;
import ch.vd.unireg.interfaces.civil.rcpers.EchHelper;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.party.person.v1.NaturalPerson;
import ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory;
import ch.vd.unireg.xml.party.person.v1.NaturalPersonCategoryPeriod;
import ch.vd.unireg.xml.party.v1.PartyPart;
import ch.vd.uniregctb.tiers.IdentificationPersonne;
import ch.vd.uniregctb.xml.Context;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;
import ch.vd.uniregctb.xml.ExceptionHelper;
import ch.vd.uniregctb.xml.ServiceException;

public class NaturalPersonStrategy extends TaxPayerStrategy<NaturalPerson> {

	private static final Logger LOGGER = Logger.getLogger(NaturalPersonStrategy.class);
	private static final String CH_AHV = "CH.AHV"; // voir spécification eCH-0044
	private static final String CH_ZAR = "CH.ZAR";
	private static final String VD_UNIREG = "VD.UNIREG";

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
			to.setIdentification(newPersonIdentification(personne));
			to.setDateOfBirth(DataHelper.coreToXML(personne.getDateNaissance()));
			to.setDateOfDeath(DataHelper.coreToXML(personne.getDateDeces()));

			final NaturalPersonCategory category = EnumHelper.coreToXML(personne.getCategorieEtranger());
			to.setCategory(category);
			to.getCategories().add(new NaturalPersonCategoryPeriod(null, null, category, null));
		}
		else {
			final Individu individu = context.serviceCivilService.getIndividu(personne.getNumeroIndividu(), null, AttributeIndividu.PERMIS);

			if (individu == null) {
				final String message = String.format("Impossible de trouver l'individu n°%d pour l'habitant n°%d", personne
						.getNumeroIndividu(), personne.getNumero());
				LOGGER.error(message);
				throw ExceptionHelper.newBusinessException(message, BusinessExceptionCode.UNKNOWN_INDIVIDUAL);
			}

			to.setIdentification(newPersonIdentification(individu, personne.getNumero()));
			to.setDateOfBirth(DataHelper.coreToXML(individu.getDateNaissance()));
			to.setDateOfDeath(DataHelper.coreToXML(personne.getDateDeces() == null ? individu.getDateDeces() : personne.getDateDeces()));

			final PermisList list = individu.getPermis();
			if (list == null || list.isEmpty()) {
				to.setCategory(NaturalPersonCategory.SWISS);
				to.getCategories().add(new NaturalPersonCategoryPeriod(null, null, NaturalPersonCategory.SWISS, null));
			}
			else {
				// le permis actif courant
				to.setCategory(EnumHelper.coreToXML(list.getPermisActif(null).getTypePermis()));

				// l'historique des permis (SIFISC-8072)
				for (Permis permis : list) {
					to.getCategories().add(new NaturalPersonCategoryPeriod(DataHelper.coreToXML(permis.getDateDebut()),
					                                                          DataHelper.coreToXML(permis.getDateFin()),
					                                                          EnumHelper.coreToXML(permis.getTypePermis()), null));
				}
			}
		}
	}

	@Override
	protected void copyBase(NaturalPerson to, NaturalPerson from) {
		super.copyBase(to, from);
		to.setIdentification(cloneIdentification(from.getIdentification()));
		to.setDateOfBirth(from.getDateOfBirth());
		to.setDateOfDeath(from.getDateOfDeath());
		to.setCategory(from.getCategory());
	}

	@Override
	protected void copyParts(NaturalPerson to, NaturalPerson from, @Nullable Set<PartyPart> parts, CopyMode mode) {
		super.copyParts(to, from, parts, mode);

		// les permis sont toujours renseignés (par de PART spécifique)
		copyColl(to.getCategories(), from.getCategories());
	}

	private static PersonIdentification newPersonIdentification(ch.vd.uniregctb.tiers.PersonnePhysique personne) {
		final PersonIdentification identification = new PersonIdentification();
		identification.setLocalPersonId(new NamedPersonId(VD_UNIREG, String.valueOf(personne.getNumero())));
		identification.setOfficialName(personne.getNom());
		identification.setFirstName(personne.getPrenom());
		identification.setSex(EchHelper.sexeToEch44(personne.getSexe()));
		identification.setVn(EchHelper.avs13ToEch(personne.getNumeroAssureSocial()));
		for (ch.vd.uniregctb.tiers.IdentificationPersonne ident : personne.getIdentificationsPersonnes()) {
			identification.getOtherPersonId().add(newNamedPersonId(ident));
		}
		identification.setDateOfBirth(EchHelper.partialDateToEch44(personne.getDateNaissance()));
		return identification;
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

	private static PersonIdentification newPersonIdentification(Individu individu, long numero) {
		final PersonIdentification identification = new PersonIdentification();
		identification.setLocalPersonId(new NamedPersonId(VD_UNIREG, String.valueOf(numero)));
		identification.setOfficialName(individu.getNom());
		identification.setFirstName(individu.getPrenom());
		identification.setSex(EchHelper.sexeToEch44(individu.getSexe()));
		identification.setVn(EchHelper.avs13ToEch(individu.getNouveauNoAVS()));
		if (StringUtils.isNotBlank(individu.getNoAVS11())) {
			identification.getOtherPersonId().add(new NamedPersonId(CH_AHV, individu.getNoAVS11())); // selon le document STAN_d_DEF_2010-06-11_eCH-0044_Personenidentifikation.pdf
		}
		if (StringUtils.isNotBlank(individu.getNumeroRCE())) {
			identification.getOtherPersonId().add(new NamedPersonId(CH_ZAR, individu.getNumeroRCE()));  // [SIFISC-4352]
		}
		identification.setDateOfBirth(EchHelper.partialDateToEch44(individu.getDateNaissance()));
		return identification;
	}

	private static PersonIdentification cloneIdentification(PersonIdentification identification) {
		return new PersonIdentification(identification.getVn(), identification.getLocalPersonId(), DataHelper.deepClone(identification.getOtherPersonId()), DataHelper
				.deepClone(identification.getEuPersonId()), identification.getOfficialName(), identification.getFirstName(), identification.getSex(),
				DataHelper.clone(identification.getDateOfBirth()));
	}
}
