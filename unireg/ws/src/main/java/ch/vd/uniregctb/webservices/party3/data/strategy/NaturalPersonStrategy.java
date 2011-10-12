package ch.vd.uniregctb.webservices.party3.data.strategy;

import java.util.Set;

import ch.ech.ech0044.v2.NamedPersonId;
import ch.ech.ech0044.v2.PersonIdentification;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.webservices.party3.PartyPart;
import ch.vd.unireg.webservices.party3.WebServiceException;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.party.person.v1.NaturalPerson;
import ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory;
import ch.vd.uniregctb.ech.EchHelper;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.HistoriqueIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.webservices.party3.impl.Context;
import ch.vd.uniregctb.webservices.party3.impl.DataHelper;
import ch.vd.uniregctb.webservices.party3.impl.EnumHelper;
import ch.vd.uniregctb.webservices.party3.impl.ExceptionHelper;

public class NaturalPersonStrategy extends TaxPayerStrategy<NaturalPerson> {

	private static final Logger LOGGER = Logger.getLogger(NaturalPersonStrategy.class);

	@Override
	public NaturalPerson newFrom(ch.vd.uniregctb.tiers.Tiers right, @Nullable Set<PartyPart> parts, Context context) throws WebServiceException {
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
	protected void initBase(NaturalPerson to, ch.vd.uniregctb.tiers.Tiers from, Context context) throws WebServiceException {
		super.initBase(to, from, context);

		final ch.vd.uniregctb.tiers.PersonnePhysique personne = (ch.vd.uniregctb.tiers.PersonnePhysique) from;
		if (!personne.isHabitantVD()) {
			to.setIdentification(newPersonIdentification(personne));
			to.setDateOfBirth(DataHelper.coreToWeb(personne.getDateNaissance()));
			to.setDateOfDeath(DataHelper.coreToWeb(personne.getDateDeces()));
			to.setCategory(EnumHelper.coreToWeb(personne.getCategorieEtranger()));
		}
		else {
			final ch.vd.uniregctb.interfaces.model.Individu individu = context.serviceCivilService.getIndividu(personne.getNumeroIndividu(), null, AttributeIndividu.PERMIS);

			if (individu == null) {
				final String message = String.format("Impossible de trouver l'individu n°%d pour l'habitant n°%d", personne
						.getNumeroIndividu(), personne.getNumero());
				LOGGER.error(message);
				throw ExceptionHelper.newBusinessException(message, BusinessExceptionCode.UNKNOWN_INDIVIDUAL);
			}

			final ch.vd.uniregctb.interfaces.model.HistoriqueIndividu data = individu.getDernierHistoriqueIndividu();
			to.setIdentification(newPersonIdentification(individu, data));
			to.setDateOfBirth(DataHelper.coreToWeb(individu.getDateNaissance()));
			to.setDateOfDeath(DataHelper.coreToWeb(personne.getDateDeces() == null ? individu.getDateDeces() : personne.getDateDeces()));

			final ch.vd.uniregctb.interfaces.model.Permis permis = individu.getPermisActif(null);
			if (permis == null) {
				to.setCategory(NaturalPersonCategory.SWISS);
			}
			else {
				to.setCategory(EnumHelper.coreToWeb(permis.getTypePermis()));
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

	private static PersonIdentification newPersonIdentification(ch.vd.uniregctb.tiers.PersonnePhysique personne) {
		final PersonIdentification identification = new PersonIdentification();
		identification.setOfficialName(personne.getNom());
		identification.setFirstName(personne.getPrenom());
		identification.setSex(EchHelper.sexeToEch44(personne.getSexe()));
		identification.setVn(EchHelper.avs13ToEch(personne.getNumeroAssureSocial()));
		for (ch.vd.uniregctb.tiers.IdentificationPersonne ident : personne.getIdentificationsPersonnes()) {
			identification.getOtherPersonId().add(new NamedPersonId(ident.getCategorieIdentifiant().name(), ident.getIdentifiant()));
		}
		identification.setDateOfBirth(EchHelper.partialDateToEch44(personne.getDateNaissance()));
		return identification;
	}

	private static PersonIdentification newPersonIdentification(Individu individu, HistoriqueIndividu data) {
		final PersonIdentification identification = new PersonIdentification();
		identification.setOfficialName(data.getNom());
		identification.setFirstName(data.getPrenom());
		identification.setSex(EchHelper.sexeToEch44(individu.isSexeMasculin() ? ch.vd.uniregctb.type.Sexe.MASCULIN : ch.vd.uniregctb.type.Sexe.FEMININ));
		identification.setVn(EchHelper.avs13ToEch(individu.getNouveauNoAVS()));
		if (StringUtils.isNotBlank(data.getNoAVS())) {
			identification.getOtherPersonId().add(new NamedPersonId("CH.AHV", data.getNoAVS())); // selon le document STAN_d_DEF_2010-06-11_eCH-0044_Personenidentifikation.pdf
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
