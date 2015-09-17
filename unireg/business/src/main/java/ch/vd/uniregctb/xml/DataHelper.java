package ch.vd.uniregctb.xml;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateConstants;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.unireg.interfaces.infra.data.TypeAffranchissement;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.indexer.tiers.AutreCommunauteIndexable;
import ch.vd.uniregctb.indexer.tiers.CollectiviteAdministrativeIndexable;
import ch.vd.uniregctb.indexer.tiers.DebiteurPrestationImposableIndexable;
import ch.vd.uniregctb.indexer.tiers.EntrepriseIndexable;
import ch.vd.uniregctb.indexer.tiers.HabitantIndexable;
import ch.vd.uniregctb.indexer.tiers.MenageCommunIndexable;
import ch.vd.uniregctb.indexer.tiers.NonHabitantIndexable;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.FormulePolitesse;
import ch.vd.uniregctb.xml.address.AddressBuilder;

/**
 * Cette helper effectue la traduction des classes venant de 'core' en classes 'XML'.
 */
public abstract class DataHelper {

	//private static final Logger LOGGER = LoggerFactory.getLogger(DataHelper.class);

	public static boolean coreToXML(Boolean value) {
		return value != null && value;
	}

	public static ch.vd.unireg.xml.common.v1.Date coreToXMLv1(java.util.Date date) {
		if (date == null) {
			return null;
		}
		else {
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTime(date);

			final int year = cal.get(Calendar.YEAR);
			final int month = cal.get(Calendar.MONTH) + 1;
			final int day = cal.get(Calendar.DAY_OF_MONTH);
			return new ch.vd.unireg.xml.common.v1.Date(year, month, day);
		}
	}

	public static ch.vd.unireg.xml.common.v2.Date coreToXMLv2(java.util.Date date) {
		if (date == null) {
			return null;
		}
		else {
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTime(date);

			final int year = cal.get(Calendar.YEAR);
			final int month = cal.get(Calendar.MONTH) + 1;
			final int day = cal.get(Calendar.DAY_OF_MONTH);
			return new ch.vd.unireg.xml.common.v2.Date(year, month, day);
		}
	}

	public static ch.vd.unireg.xml.common.v1.Date coreToXMLv1(RegDate date) {
		if (date == null) {
			return null;
		}
		else {
			return new ch.vd.unireg.xml.common.v1.Date(date.year(), date.month(), date.day());
		}
	}

	public static ch.vd.unireg.xml.common.v2.Date coreToXMLv2(RegDate date) {
		if (date == null) {
			return null;
		}
		else {
			return new ch.vd.unireg.xml.common.v2.Date(date.year(), date.month(), date.day());
		}
	}

	public static RegDate xmlToCore(ch.vd.unireg.xml.common.v1.Date date) {
		if (date == null) {
			return null;
		}
		return RegDateHelper.get(date.getYear(), date.getMonth(), date.getDay(), DateConstants.EXTENDED_VALIDITY_RANGE);
	}

	public static RegDate xmlToCore(ch.vd.unireg.xml.common.v2.Date date) {
		if (date == null) {
			return null;
		}
		return RegDateHelper.get(date.getYear(), date.getMonth(), date.getDay(), DateConstants.EXTENDED_VALIDITY_RANGE);
	}

	public static ch.vd.unireg.xml.common.v1.PartialDate coreToPartialDateXmlv1(RegDate date) {
		if (date == null) {
			return null;
		}
		else {
			return new ch.vd.unireg.xml.common.v1.PartialDate(date.year(), date.month() == RegDate.UNDEFINED ? null : date.month(), date.day() == RegDate.UNDEFINED ? null : date.day());
		}
	}

	public static ch.vd.unireg.xml.common.v2.PartialDate coreToPartialDateXmlv2(RegDate date) {
		if (date == null) {
			return null;
		}
		else {
			return new ch.vd.unireg.xml.common.v2.PartialDate(date.year(), date.month() == RegDate.UNDEFINED ? null : date.month(), date.day() == RegDate.UNDEFINED ? null : date.day());
		}
	}

	/**
	 * @param date une date partielle à convertir en {@link RegDate}
	 * @return la date convertie
	 * @throws IllegalArgumentException en cas de souci à la conversion (date résultante invalide, mauvais type de "date partielle" dont le mois  est inconnu mais pas le jour, par exemple...)
	 */
	public static RegDate xmlToCore(ch.vd.unireg.xml.common.v1.PartialDate date) throws IllegalArgumentException {
		if (date == null) {
			return null;
		}
		final int year = date.getYear();
		final Integer month = date.getMonth();
		final Integer day = date.getDay();
		if (day == null && month == null) {
			return RegDateHelper.get(year, DateConstants.EXTENDED_VALIDITY_RANGE);
		}
		else if (day == null) {
			return RegDateHelper.get(year, month, DateConstants.EXTENDED_VALIDITY_RANGE);
		}
		else if (month == null) {
			throw new IllegalArgumentException("Date partielle avec jour connu mais pas le mois : " + date);
		}
		else {
			return RegDateHelper.get(year, month, day, DateConstants.EXTENDED_VALIDITY_RANGE);
		}
	}

	/**
	 * @param date une date partielle à convertir en {@link RegDate}
	 * @return la date convertie
	 * @throws IllegalArgumentException en cas de souci à la conversion (date résultante invalide, mauvais type de "date partielle" dont le mois  est inconnu mais pas le jour, par exemple...)
	 */
	public static RegDate xmlToCore(ch.vd.unireg.xml.common.v2.PartialDate date) throws IllegalArgumentException {
		if (date == null) {
			return null;
		}
		final int year = date.getYear();
		final Integer month = date.getMonth();
		final Integer day = date.getDay();
		if (day == null && month == null) {
			return RegDateHelper.get(year, DateConstants.EXTENDED_VALIDITY_RANGE);
		}
		else if (day == null) {
			return RegDateHelper.get(year, month, DateConstants.EXTENDED_VALIDITY_RANGE);
		}
		else if (month == null) {
			throw new IllegalArgumentException("Date partielle avec jour connu mais pas le mois : " + date);
		}
		else {
			return RegDateHelper.get(year, month, day, DateConstants.EXTENDED_VALIDITY_RANGE);
		}
	}

	public static List<ch.vd.unireg.xml.party.address.v1.Address> coreToXMLv1(List<AdresseEnvoiDetaillee> adresses, @Nullable DateRangeHelper.Range range,
	                                                                          ch.vd.unireg.xml.party.address.v1.AddressType type) throws ServiceException {
		if (adresses == null || adresses.isEmpty()) {
			return null;
		}

		final List<ch.vd.unireg.xml.party.address.v1.Address> list = new ArrayList<>();
		for (AdresseEnvoiDetaillee a : adresses) {
			if (range == null || DateRangeHelper.intersect(a, range)) {
				list.add(AddressBuilder.newAddress(a, type));
			}
		}

		return list.isEmpty() ? null : list;
	}

	public static List<ch.vd.unireg.xml.party.address.v2.Address> coreToXMLv2(List<AdresseEnvoiDetaillee> adresses, @Nullable DateRangeHelper.Range range,
	                                                                          ch.vd.unireg.xml.party.address.v2.AddressType type) throws ServiceException {
		if (adresses == null || adresses.isEmpty()) {
			return null;
		}

		final List<ch.vd.unireg.xml.party.address.v2.Address> list = new ArrayList<>();
		for (AdresseEnvoiDetaillee a : adresses) {
			if (range == null || DateRangeHelper.intersect(a, range)) {
				list.add(AddressBuilder.newAddress(a, type));
			}
		}

		return list.isEmpty() ? null : list;
	}

	public static List<ch.vd.unireg.xml.party.address.v1.AddressOtherParty> coreToXMLATv1(List<AdresseEnvoiDetaillee> adresses, @Nullable DateRangeHelper.Range range,
	                                                                                      ch.vd.unireg.xml.party.address.v1.AddressType type) throws ServiceException {
		if (adresses == null || adresses.isEmpty()) {
			return null;
		}

		final List<ch.vd.unireg.xml.party.address.v1.AddressOtherParty> list = new ArrayList<>();
		for (AdresseEnvoiDetaillee a : adresses) {
			if (range == null || DateRangeHelper.intersect(a, range)) {
				list.add(AddressBuilder.newOtherPartyAddress(a, type));
			}
		}

		return list.isEmpty() ? null : list;
	}

	public static List<ch.vd.unireg.xml.party.address.v2.AddressOtherParty> coreToXMLATv2(List<AdresseEnvoiDetaillee> adresses, @Nullable DateRangeHelper.Range range,
	                                                                                      ch.vd.unireg.xml.party.address.v2.AddressType type) throws ServiceException {
		if (adresses == null || adresses.isEmpty()) {
			return null;
		}

		final List<ch.vd.unireg.xml.party.address.v2.AddressOtherParty> list = new ArrayList<>();
		for (AdresseEnvoiDetaillee a : adresses) {
			if (range == null || DateRangeHelper.intersect(a, range)) {
				list.add(AddressBuilder.newOtherPartyAddress(a, type));
			}
		}

		return list.isEmpty() ? null : list;
	}

	public static ch.vd.unireg.xml.party.address.v1.TariffZone coreToXMLv1(TypeAffranchissement t) {
		if (t == null) {
			return null;
		}

		switch (t) {
		case SUISSE:
			return ch.vd.unireg.xml.party.address.v1.TariffZone.SWITZERLAND;
		case EUROPE:
			return ch.vd.unireg.xml.party.address.v1.TariffZone.EUROPE;
		case MONDE:
			return ch.vd.unireg.xml.party.address.v1.TariffZone.OTHER_COUNTRIES;
		default:
			throw new IllegalArgumentException("Type d'affranchissement inconnu = [" + t + ']');
		}
	}

	public static ch.vd.unireg.xml.party.address.v2.TariffZone coreToXMLv2(TypeAffranchissement t) {
		if (t == null) {
			return null;
		}

		switch (t) {
		case SUISSE:
			return ch.vd.unireg.xml.party.address.v2.TariffZone.SWITZERLAND;
		case EUROPE:
			return ch.vd.unireg.xml.party.address.v2.TariffZone.EUROPE;
		case MONDE:
			return ch.vd.unireg.xml.party.address.v2.TariffZone.OTHER_COUNTRIES;
		default:
			throw new IllegalArgumentException("Type d'affranchissement inconnu = [" + t + ']');
		}
	}

	public static TypeAdresseFiscale xmlToCore(ch.vd.unireg.xml.party.address.v1.AddressType type) {
		if (type == null) {
			return null;
		}
		switch (type) {
		case MAIL:
			return TypeAdresseFiscale.COURRIER;
		case REPRESENTATION:
			return TypeAdresseFiscale.REPRESENTATION;
		case RESIDENCE:
			return TypeAdresseFiscale.DOMICILE;
		case DEBT_PROSECUTION:
			return TypeAdresseFiscale.POURSUITE;
		case DEBT_PROSECUTION_OF_OTHER_PARTY:
			return TypeAdresseFiscale.POURSUITE_AUTRE_TIERS;
		default:
			throw new IllegalArgumentException("Unknown AddressType = [" + type + ']');
		}
	}

	public static TypeAdresseFiscale xmlToCore(ch.vd.unireg.xml.party.address.v2.AddressType type) {
		if (type == null) {
			return null;
		}
		switch (type) {
		case MAIL:
			return TypeAdresseFiscale.COURRIER;
		case REPRESENTATION:
			return TypeAdresseFiscale.REPRESENTATION;
		case RESIDENCE:
			return TypeAdresseFiscale.DOMICILE;
		case DEBT_PROSECUTION:
			return TypeAdresseFiscale.POURSUITE;
		case DEBT_PROSECUTION_OF_OTHER_PARTY:
			return TypeAdresseFiscale.POURSUITE_AUTRE_TIERS;
		default:
			throw new IllegalArgumentException("Unknown AddressType = [" + type + ']');
		}
	}

	public static String truncate(String valeur, int nbChar) {

		if (valeur != null && valeur.length() > nbChar) {
			valeur = valeur.substring(0, nbChar);
		}

		return valeur;
	}

	public static ch.vd.unireg.xml.party.v1.PartyInfo coreToXMLv1(ch.vd.uniregctb.indexer.tiers.TiersIndexedData value) {
		if (value == null) {
			return null;
		}

		final ch.vd.unireg.xml.party.v1.PartyInfo i = new ch.vd.unireg.xml.party.v1.PartyInfo();
		i.setNumber(value.getNumero().intValue());
		i.setName1(value.getNom1());
		i.setName2(value.getNom2());
		i.setStreet(value.getRue());
		i.setZipCode(value.getNpa());
		i.setTown(value.getLocalite());
		i.setCountry(value.getPays());
		i.setDateOfBirth(DataHelper.coreToXMLv1(value.getRegDateNaissance()));
		i.setType(DataHelper.getPartyTypeV1(value));
		i.setDebtorCategory(EnumHelper.coreToXMLv1(value.getCategorieImpotSource()));
		i.setDebtorCommunicationMode(EnumHelper.coreToXMLv1(value.getModeCommunication()));
		return i;
	}

	public static ch.vd.unireg.xml.party.v2.PartyInfo coreToXMLv2(ch.vd.uniregctb.indexer.tiers.TiersIndexedData value) {
		if (value == null) {
			return null;
		}

		final ch.vd.unireg.xml.party.v2.PartyInfo i = new ch.vd.unireg.xml.party.v2.PartyInfo();
		i.setNumber(value.getNumero().intValue());
		i.setName1(value.getNom1());
		i.setName2(value.getNom2());
		i.setStreet(value.getRue());
		i.setZipCode(value.getNpa());
		i.setTown(value.getLocalite());
		i.setCountry(value.getPays());
		i.setDateOfBirth(DataHelper.coreToXMLv1(value.getRegDateNaissance()));
		i.setType(DataHelper.getPartyTypeV2(value));
		i.setDebtorCategory(EnumHelper.coreToXMLv2(value.getCategorieImpotSource()));
		i.setDebtorCommunicationMode(EnumHelper.coreToXMLv2(value.getModeCommunication()));
		i.setLastTaxResidenceBeginDate(DataHelper.coreToXMLv1(value.getDateOuvertureFor()));
		i.setLastTaxResidenceEndDate(DataHelper.coreToXMLv1(value.getDateFermetureFor()));
		if (StringUtils.isNotBlank(value.getNavs13_1())) {
			i.setVn1(Long.valueOf(value.getNavs13_1()));
		}
		if (StringUtils.isNotBlank(value.getNavs13_2())) {
			i.setVn2(Long.valueOf(value.getNavs13_2()));
		}
		return i;
	}

	public static ch.vd.unireg.xml.party.v3.PartyInfo coreToXMLv3(ch.vd.uniregctb.indexer.tiers.TiersIndexedData value) {
		if (value == null) {
			return null;
		}

		final ch.vd.unireg.xml.party.v3.PartyInfo i = new ch.vd.unireg.xml.party.v3.PartyInfo();
		i.setNumber(value.getNumero().intValue());
		i.setName1(value.getNom1());
		i.setName2(value.getNom2());
		i.setStreet(value.getRue());
		i.setZipCode(value.getNpa());
		i.setTown(value.getLocalite());
		i.setCountry(value.getPays());
		i.setDateOfBirth(DataHelper.coreToPartialDateXmlv2(value.getRegDateNaissance()));
		i.setType(DataHelper.getPartyTypeV3(value));
		i.setDebtorCategory(EnumHelper.coreToXMLv3(value.getCategorieImpotSource()));
		i.setDebtorCommunicationMode(EnumHelper.coreToXMLv3(value.getModeCommunication()));
		i.setLastTaxResidenceBeginDate(DataHelper.coreToXMLv2(value.getDateOuvertureFor()));
		i.setLastTaxResidenceEndDate(DataHelper.coreToXMLv2(value.getDateFermetureFor()));
		if (StringUtils.isNotBlank(value.getNavs13_1())) {
			i.setVn1(Long.valueOf(value.getNavs13_1()));
		}
		if (StringUtils.isNotBlank(value.getNavs13_2())) {
			i.setVn2(Long.valueOf(value.getNavs13_2()));
		}
		i.setIndividualTaxLiability(EnumHelper.coreToXMLv2(value.getAssujettissementPP()));
		if (i.getType() == ch.vd.unireg.xml.party.v3.PartyType.NATURAL_PERSON) {
			i.setNaturalPersonSubtype(DataHelper.getNaturalPersonSubtypeV3(value));
		}

		final List<String> numerosIDE = value.getNumerosIDE();
		if (!numerosIDE.isEmpty()) {
			i.setUidNumbers(new ch.vd.unireg.xml.party.v3.UidNumberList(numerosIDE));
		}
		return i;
	}

	public static ch.vd.unireg.xml.party.v4.PartyInfo coreToXMLv4(ch.vd.uniregctb.indexer.tiers.TiersIndexedData value) {
		if (value == null) {
			return null;
		}

		final ch.vd.unireg.xml.party.v4.PartyInfo i = new ch.vd.unireg.xml.party.v4.PartyInfo();
		i.setNumber(value.getNumero().intValue());
		i.setName1(value.getNom1());
		i.setName2(value.getNom2());
		i.setStreet(value.getRue());
		i.setZipCode(value.getNpa());
		i.setTown(value.getLocalite());
		i.setCountry(value.getPays());
		i.setDateOfBirth(DataHelper.coreToPartialDateXmlv2(value.getRegDateNaissance()));
		i.setType(DataHelper.getPartyTypeV4(value));
		i.setDebtorCategory(EnumHelper.coreToXMLv3(value.getCategorieImpotSource()));
		i.setDebtorCommunicationMode(EnumHelper.coreToXMLv3(value.getModeCommunication()));
		i.setLastTaxResidenceBeginDate(DataHelper.coreToXMLv2(value.getDateOuvertureFor()));
		i.setLastTaxResidenceEndDate(DataHelper.coreToXMLv2(value.getDateFermetureFor()));
		if (StringUtils.isNotBlank(value.getNavs13_1())) {
			i.setVn1(Long.valueOf(value.getNavs13_1()));
		}
		if (StringUtils.isNotBlank(value.getNavs13_2())) {
			i.setVn2(Long.valueOf(value.getNavs13_2()));
		}
		i.setIndividualTaxLiability(EnumHelper.coreToXMLv2(value.getAssujettissementPP()));
		if (i.getType() == ch.vd.unireg.xml.party.v4.PartyType.NATURAL_PERSON) {
			i.setNaturalPersonSubtype(DataHelper.getNaturalPersonSubtypeV4(value));
		}

		final List<String> numerosIDE = value.getNumerosIDE();
		if (!numerosIDE.isEmpty()) {
			i.setUidNumbers(new ch.vd.unireg.xml.party.v4.UidNumberList(numerosIDE));
		}
		return i;
	}

	/**
	 * Retourne le numéro de la déclaration d'impôt associée avec une période d'imposition.
	 *
	 * @param periodeImposition la période d'imposition considérée
	 * @return l'id de déclaration associée; ou <b>null</b> si aucune déclaration n'est émise.
	 */
	public static Long getAssociatedDi(PeriodeImposition periodeImposition) {

		final Contribuable contribuable = periodeImposition.getContribuable();
		final List<ch.vd.uniregctb.declaration.Declaration> dis = contribuable.getDeclarationsForPeriode(periodeImposition.getDateDebut().year(), false);
		if (dis == null) {
			return null;
		}

		Long idDi = null;

		for (ch.vd.uniregctb.declaration.Declaration di : dis) {
			if (!di.isAnnule() && DateRangeHelper.intersect(periodeImposition, di)) {
				if (idDi != null) {
					final String erreur = String.format("Incohérence des données: trouvé deux déclarations (ids %d et %d) "
							+ "associées avec la période d'imposition du %s au %s sur le contribuable n°%d", idDi, di.getId(),
							periodeImposition.getDateDebut().toString(), periodeImposition.getDateFin().toString(), contribuable.getNumero());
					throw new ValidationException(contribuable, erreur);
				}
				idDi = di.getId();
			}
		}

		return idDi;
	}

	private static final Map<String, ch.vd.unireg.xml.party.v1.PartyType> indexedData2TypeV1 = new HashMap<String, ch.vd.unireg.xml.party.v1.PartyType>() {
		{
			put(HabitantIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v1.PartyType.NATURAL_PERSON);
			put(NonHabitantIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v1.PartyType.NATURAL_PERSON);
			put(EntrepriseIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v1.PartyType.CORPORATION);
			put(MenageCommunIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v1.PartyType.HOUSEHOLD);
			put(AutreCommunauteIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v1.PartyType.CORPORATION);
			put(EntrepriseIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v1.PartyType.CORPORATION);
			put(DebiteurPrestationImposableIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v1.PartyType.DEBTOR);
		}
	};

	private static final Map<String, ch.vd.unireg.xml.party.v2.PartyType> indexedData2TypeV2 = new HashMap<String, ch.vd.unireg.xml.party.v2.PartyType>() {
		{
			put(HabitantIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v2.PartyType.NATURAL_PERSON);
			put(NonHabitantIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v2.PartyType.NATURAL_PERSON);
			put(EntrepriseIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v2.PartyType.CORPORATION);
			put(MenageCommunIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v2.PartyType.HOUSEHOLD);
			put(AutreCommunauteIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v2.PartyType.CORPORATION);
			put(EntrepriseIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v2.PartyType.CORPORATION);
			put(DebiteurPrestationImposableIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v2.PartyType.DEBTOR);
		}
	};

	private static final Map<String, ch.vd.unireg.xml.party.v3.PartyType> indexedData2TypeV3 = new HashMap<String, ch.vd.unireg.xml.party.v3.PartyType>() {
		{
			put(HabitantIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v3.PartyType.NATURAL_PERSON);
			put(NonHabitantIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v3.PartyType.NATURAL_PERSON);
			put(EntrepriseIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v3.PartyType.CORPORATION);
			put(MenageCommunIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v3.PartyType.HOUSEHOLD);
			put(AutreCommunauteIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v3.PartyType.OTHER_COMMUNITY);
			put(EntrepriseIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v3.PartyType.CORPORATION);
			put(DebiteurPrestationImposableIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v3.PartyType.DEBTOR);
			put(CollectiviteAdministrativeIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v3.PartyType.ADMINISTRATIVE_AUTHORITY);
		}
	};

	private static final Map<String, ch.vd.unireg.xml.party.v4.PartyType> indexedData2TypeV4 = new HashMap<String, ch.vd.unireg.xml.party.v4.PartyType>() {
		{
			put(HabitantIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v4.PartyType.NATURAL_PERSON);
			put(NonHabitantIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v4.PartyType.NATURAL_PERSON);
			put(EntrepriseIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v4.PartyType.CORPORATION);
			put(MenageCommunIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v4.PartyType.HOUSEHOLD);
			put(AutreCommunauteIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v4.PartyType.OTHER_COMMUNITY);
			put(EntrepriseIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v4.PartyType.CORPORATION);
			put(DebiteurPrestationImposableIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v4.PartyType.DEBTOR);
			put(CollectiviteAdministrativeIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v4.PartyType.ADMINISTRATIVE_AUTHORITY);
		}
	};

	private static final Map<String, ch.vd.unireg.xml.party.v3.NaturalPersonSubtype> indexedData2NaturalPersonSubtypeV3 = new HashMap<String, ch.vd.unireg.xml.party.v3.NaturalPersonSubtype>() {
		{
			put(HabitantIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v3.NaturalPersonSubtype.RESIDENT);
			put(NonHabitantIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v3.NaturalPersonSubtype.NON_RESIDENT);
		}
	};

	private static final Map<String, ch.vd.unireg.xml.party.v4.NaturalPersonSubtype> indexedData2NaturalPersonSubtypeV4 = new HashMap<String, ch.vd.unireg.xml.party.v4.NaturalPersonSubtype>() {
		{
			put(HabitantIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v4.NaturalPersonSubtype.RESIDENT);
			put(NonHabitantIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v4.NaturalPersonSubtype.NON_RESIDENT);
		}
	};

	/**
	 * Détermine le type d'un tiers à partir de ses données indexées.
	 *
	 * @param tiers les données indexés du tiers
	 * @return le type du tiers; ou <b>null</b> si le type de tiers n'est pas connu.
	 */
	public static ch.vd.unireg.xml.party.v1.PartyType getPartyTypeV1(ch.vd.uniregctb.indexer.tiers.TiersIndexedData tiers) {

		final String typeAsString = tiers.getTiersType();

		if (StringUtils.isEmpty(typeAsString)) {
			return null;
		}

		return indexedData2TypeV1.get(typeAsString);
	}

	/**
	 * Détermine le type d'un tiers à partir de ses données indexées.
	 *
	 * @param tiers les données indexés du tiers
	 * @return le type du tiers; ou <b>null</b> si le type de tiers n'est pas connu.
	 */
	public static ch.vd.unireg.xml.party.v2.PartyType getPartyTypeV2(ch.vd.uniregctb.indexer.tiers.TiersIndexedData tiers) {

		final String typeAsString = tiers.getTiersType();

		if (StringUtils.isEmpty(typeAsString)) {
			return null;
		}

		return indexedData2TypeV2.get(typeAsString);
	}

	public static ch.vd.unireg.xml.party.v3.PartyType getPartyTypeV3(ch.vd.uniregctb.indexer.tiers.TiersIndexedData tiers) {

		final String typeAsString = tiers.getTiersType();

		if (StringUtils.isEmpty(typeAsString)) {
			return null;
		}

		return indexedData2TypeV3.get(typeAsString);
	}

	public static ch.vd.unireg.xml.party.v4.PartyType getPartyTypeV4(ch.vd.uniregctb.indexer.tiers.TiersIndexedData tiers) {

		final String typeAsString = tiers.getTiersType();

		if (StringUtils.isEmpty(typeAsString)) {
			return null;
		}

		return indexedData2TypeV4.get(typeAsString);
	}

	public static ch.vd.unireg.xml.party.v3.NaturalPersonSubtype getNaturalPersonSubtypeV3(ch.vd.uniregctb.indexer.tiers.TiersIndexedData tiers) {
		final String typeAsString = tiers.getTiersType();

		if (StringUtils.isEmpty(typeAsString)) {
			return null;
		}

		return indexedData2NaturalPersonSubtypeV3.get(typeAsString);
	}

	public static ch.vd.unireg.xml.party.v4.NaturalPersonSubtype getNaturalPersonSubtypeV4(ch.vd.uniregctb.indexer.tiers.TiersIndexedData tiers) {
		final String typeAsString = tiers.getTiersType();

		if (StringUtils.isEmpty(typeAsString)) {
			return null;
		}

		return indexedData2NaturalPersonSubtypeV4.get(typeAsString);
	}

	public static Set<TiersDAO.Parts> xmlToCoreV1(Set<ch.vd.unireg.xml.party.v1.PartyPart> parts) {

		if (parts == null) {
			return null;
		}

		final Set<TiersDAO.Parts> results = EnumSet.noneOf(TiersDAO.Parts.class);
		for (ch.vd.unireg.xml.party.v1.PartyPart p : parts) {
			switch (p) {
			case ADDRESSES:
				results.add(TiersDAO.Parts.ADRESSES);
				results.add(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS);
				break;
			case TAX_DECLARATIONS:
			case TAX_DECLARATIONS_STATUSES:
			case TAX_DECLARATIONS_DEADLINES:
				results.add(TiersDAO.Parts.DECLARATIONS);
				break;
			case TAX_RESIDENCES:
			case VIRTUAL_TAX_RESIDENCES:
			case MANAGING_TAX_RESIDENCES:
			case TAX_LIABILITIES:
			case SIMPLIFIED_TAX_LIABILITIES:
			case TAXATION_PERIODS:
				results.add(TiersDAO.Parts.FORS_FISCAUX);
				break;
			case RELATIONS_BETWEEN_PARTIES:
			case CHILDREN:
			case PARENTS:
			case HOUSEHOLD_MEMBERS:
				results.add(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS);
				break;
			case FAMILY_STATUSES:
				results.add(TiersDAO.Parts.SITUATIONS_FAMILLE);
				break;
			case DEBTOR_PERIODICITIES:
				results.add(TiersDAO.Parts.PERIODICITES);
				break;
			case IMMOVABLE_PROPERTIES:
				results.add(TiersDAO.Parts.IMMEUBLES);
				break;
			case BANK_ACCOUNTS:
			case CAPITALS:
			case CORPORATION_STATUSES:
			case LEGAL_FORMS:
			case TAX_SYSTEMS:
			case LEGAL_SEATS:
				// rien à faire
				break;
			default:
				throw new IllegalArgumentException("Type de parts inconnue = [" + p + ']');
			}
		}

		return results;
	}

	public static Set<TiersDAO.Parts> xmlToCoreV2(Set<ch.vd.unireg.xml.party.v2.PartyPart> parts) {

		if (parts == null) {
			return null;
		}

		final Set<TiersDAO.Parts> results = EnumSet.noneOf(TiersDAO.Parts.class);
		for (ch.vd.unireg.xml.party.v2.PartyPart p : parts) {
			switch (p) {
			case ADDRESSES:
				results.add(TiersDAO.Parts.ADRESSES);
				results.add(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS);
				break;
			case TAX_DECLARATIONS:
			case TAX_DECLARATIONS_STATUSES:
			case TAX_DECLARATIONS_DEADLINES:
				results.add(TiersDAO.Parts.DECLARATIONS);
				break;
			case TAX_RESIDENCES:
			case VIRTUAL_TAX_RESIDENCES:
			case MANAGING_TAX_RESIDENCES:
			case TAX_LIABILITIES:
			case SIMPLIFIED_TAX_LIABILITIES:
			case TAXATION_PERIODS:
				results.add(TiersDAO.Parts.FORS_FISCAUX);
				break;
			case RELATIONS_BETWEEN_PARTIES:
			case CHILDREN:
			case PARENTS:
			case HOUSEHOLD_MEMBERS:
				results.add(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS);
				break;
			case FAMILY_STATUSES:
				results.add(TiersDAO.Parts.SITUATIONS_FAMILLE);
				break;
			case DEBTOR_PERIODICITIES:
				results.add(TiersDAO.Parts.PERIODICITES);
				break;
			case IMMOVABLE_PROPERTIES:
				results.add(TiersDAO.Parts.IMMEUBLES);
				break;
			case BANK_ACCOUNTS:
			case CAPITALS:
			case CORPORATION_STATUSES:
			case LEGAL_FORMS:
			case TAX_SYSTEMS:
			case LEGAL_SEATS:
				// rien à faire
				break;
			default:
				throw new IllegalArgumentException("Type de parts inconnue = [" + p + ']');
			}
		}

		return results;
	}

	public static Set<TiersDAO.Parts> xmlToCoreV3(Set<ch.vd.unireg.xml.party.v3.PartyPart> parts) {

		if (parts == null) {
			return null;
		}

		final Set<TiersDAO.Parts> results = EnumSet.noneOf(TiersDAO.Parts.class);
		for (ch.vd.unireg.xml.party.v3.PartyPart p : parts) {
			switch (p) {
			case ADDRESSES:
				results.add(TiersDAO.Parts.ADRESSES);
				results.add(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS);
				break;
			case TAX_DECLARATIONS:
			case TAX_DECLARATIONS_STATUSES:
			case TAX_DECLARATIONS_DEADLINES:
				results.add(TiersDAO.Parts.DECLARATIONS);
				break;
			case TAX_RESIDENCES:
			case VIRTUAL_TAX_RESIDENCES:
			case MANAGING_TAX_RESIDENCES:
			case TAX_LIABILITIES:
			case SIMPLIFIED_TAX_LIABILITIES:
			case TAXATION_PERIODS:
				results.add(TiersDAO.Parts.FORS_FISCAUX);
				break;
			case WITHHOLDING_TAXATION_PERIODS:
				results.add(TiersDAO.Parts.FORS_FISCAUX);
				results.add(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS);
				break;
			case CHILDREN:
			case PARENTS:
			case RELATIONS_BETWEEN_PARTIES:
			case HOUSEHOLD_MEMBERS:
				results.add(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS);
				break;
			case FAMILY_STATUSES:
				results.add(TiersDAO.Parts.SITUATIONS_FAMILLE);
				break;
			case DEBTOR_PERIODICITIES:
				results.add(TiersDAO.Parts.PERIODICITES);
				break;
			case IMMOVABLE_PROPERTIES:
				results.add(TiersDAO.Parts.IMMEUBLES);
				break;
			case BANK_ACCOUNTS:
			case CAPITALS:
			case CORPORATION_STATUSES:
			case LEGAL_FORMS:
			case TAX_SYSTEMS:
			case LEGAL_SEATS:
			case EBILLING_STATUSES:
				// rien à faire
				break;
			default:
				throw new IllegalArgumentException("Type de parts inconnue = [" + p + ']');
			}
		}

		return results;
	}

	public static Set<TiersDAO.Parts> xmlToCoreV4(Set<ch.vd.unireg.xml.party.v4.PartyPart> parts) {

		if (parts == null) {
			return null;
		}

		final Set<TiersDAO.Parts> results = EnumSet.noneOf(TiersDAO.Parts.class);
		for (ch.vd.unireg.xml.party.v4.PartyPart p : parts) {
			switch (p) {
			case ADDRESSES:
				results.add(TiersDAO.Parts.ADRESSES);
				results.add(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS);
				break;
			case TAX_DECLARATIONS:
			case TAX_DECLARATIONS_STATUSES:
			case TAX_DECLARATIONS_DEADLINES:
				results.add(TiersDAO.Parts.DECLARATIONS);
				break;
			case TAX_RESIDENCES:
			case VIRTUAL_TAX_RESIDENCES:
			case MANAGING_TAX_RESIDENCES:
			case TAX_LIABILITIES:
			case SIMPLIFIED_TAX_LIABILITIES:
			case TAXATION_PERIODS:
				results.add(TiersDAO.Parts.FORS_FISCAUX);
				break;
			case WITHHOLDING_TAXATION_PERIODS:
				results.add(TiersDAO.Parts.FORS_FISCAUX);
				results.add(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS);
				break;
			case CHILDREN:
			case PARENTS:
			case RELATIONS_BETWEEN_PARTIES:
			case HOUSEHOLD_MEMBERS:
				results.add(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS);
				break;
			case FAMILY_STATUSES:
				results.add(TiersDAO.Parts.SITUATIONS_FAMILLE);
				break;
			case DEBTOR_PERIODICITIES:
				results.add(TiersDAO.Parts.PERIODICITES);
				break;
			case IMMOVABLE_PROPERTIES:
				results.add(TiersDAO.Parts.IMMEUBLES);
				break;
			case TAX_LIGHTENINGS:
				results.add(TiersDAO.Parts.ALLEGEMENTS_FISCAUX);
				break;
			case BANK_ACCOUNTS:
			case CAPITALS:
			case LEGAL_FORMS:
			case TAX_SYSTEMS:
			case LEGAL_SEATS:
			case EBILLING_STATUSES:
				// rien à faire
				break;
			default:
				throw new IllegalArgumentException("Type de parts inconnue = [" + p + ']');
			}
		}

		return results;
	}

	public static List<ForFiscalPrincipal> getForsFiscauxVirtuels(ch.vd.uniregctb.tiers.Tiers tiers, HibernateTemplate hibernateTemplate) {

		// Récupère les appartenances ménages du tiers
		final Set<RapportEntreTiers> rapports = tiers.getRapportsSujet();
		final Collection<RapportEntreTiers> rapportsMenage = CollectionUtils.select(rapports, new Predicate<RapportEntreTiers>() {
			@Override
			public boolean evaluate(RapportEntreTiers rapport) {
				return !rapport.isAnnule() && rapport instanceof AppartenanceMenage;
			}
		});

		if (rapportsMenage.isEmpty()) {
			return Collections.emptyList();
		}

		final List<ForFiscalPrincipal> forsVirtuels = new ArrayList<>();

		// Extrait les fors principaux du ménage, en les adaptant à la période de validité des appartenances ménages
		for (RapportEntreTiers a : rapportsMenage) {
			final Map<String, Long> params = new HashMap<>(1);
			params.put("menageId", a.getObjetId());

			final List<ForFiscalPrincipal> forsMenage = hibernateTemplate.find("from ForFiscalPrincipalPP f where f.annulationDate is null and f.tiers.id = :menageId order by f.dateDebut asc", params, null);
			final List<ForFiscalPrincipal> extraction = DateRangeHelper.extract(forsMenage, a.getDateDebut(), a.getDateFin(),
					new DateRangeHelper.AdapterCallback<ForFiscalPrincipal>() {
						@Override
						public ForFiscalPrincipal adapt(ForFiscalPrincipal f, RegDate debut, RegDate fin) {
							if (debut == null && fin == null) {
								return f;
							}
							else {
								ForFiscalPrincipal clone = (ForFiscalPrincipal) f.duplicate();
								clone.setDateDebut(debut);
								clone.setDateFin(fin);
								return clone;
							}
						}
					});

			forsVirtuels.addAll(extraction);
		}

		return forsVirtuels;
	}

	public static ch.vd.unireg.xml.common.v1.Date coreToXML(String s) {
		return coreToXMLv1(RegDateHelper.dashStringToDate(s));
	}

	public static <T extends Enum<T>> Set<T> toSet(List<T> parts) {
		return new HashSet<>(parts);
	}

	public static String salutations2MrMrs(String salutations) {
		if (FormulePolitesse.MADAME.salutations().equals(salutations)) {
			return "1";
		}
		if (FormulePolitesse.MONSIEUR.salutations().equals(salutations)) {
			return "2";
		}
		else {
			return null;
		}
	}

	public static List<ch.ech.ech0044.v2.NamedPersonId> deepCloneV2(List<ch.ech.ech0044.v2.NamedPersonId> list) {
		if (list == null) {
			return null;
		}
		final List<ch.ech.ech0044.v2.NamedPersonId> otherPersonId = new ArrayList<>(list.size());
		for (ch.ech.ech0044.v2.NamedPersonId namedPersonId : list) {
			otherPersonId.add(new ch.ech.ech0044.v2.NamedPersonId(namedPersonId.getPersonIdCategory(), namedPersonId.getPersonId()));
		}
		return otherPersonId;
	}

	public static ch.ech.ech0044.v2.DatePartiallyKnown clone(ch.ech.ech0044.v2.DatePartiallyKnown right) {
		if (right == null) {
			return null;
		}
		return new ch.ech.ech0044.v2.DatePartiallyKnown(right.getYearMonthDay(), right.getYearMonth(), right.getYear());
	}

	public static List<ch.ech.ech0044.v3.NamedPersonId> deepCloneV3(List<ch.ech.ech0044.v3.NamedPersonId> list) {
		if (list == null) {
			return null;
		}
		final List<ch.ech.ech0044.v3.NamedPersonId> otherPersonId = new ArrayList<>(list.size());
		for (ch.ech.ech0044.v3.NamedPersonId namedPersonId : list) {
			otherPersonId.add(new ch.ech.ech0044.v3.NamedPersonId(namedPersonId.getPersonIdCategory(), namedPersonId.getPersonId()));
		}
		return otherPersonId;
	}

	public static ch.ech.ech0044.v3.DatePartiallyKnown clone(ch.ech.ech0044.v3.DatePartiallyKnown right) {
		if (right == null) {
			return null;
		}
		return new ch.ech.ech0044.v3.DatePartiallyKnown(right.getYearMonthDay(), right.getYearMonth(), right.getYear());
	}
}
