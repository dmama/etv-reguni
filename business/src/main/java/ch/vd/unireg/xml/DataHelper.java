package ch.vd.unireg.xml;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.FlushMode;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateConstants;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.unireg.interfaces.infra.data.TypeAffranchissement;
import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.adresse.TypeAdresseFiscale;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.indexer.tiers.AutreCommunauteIndexable;
import ch.vd.unireg.indexer.tiers.CollectiviteAdministrativeIndexable;
import ch.vd.unireg.indexer.tiers.DebiteurPrestationImposableIndexable;
import ch.vd.unireg.indexer.tiers.EntrepriseIndexable;
import ch.vd.unireg.indexer.tiers.EtablissementIndexable;
import ch.vd.unireg.indexer.tiers.HabitantIndexable;
import ch.vd.unireg.indexer.tiers.MenageCommunIndexable;
import ch.vd.unireg.indexer.tiers.NonHabitantIndexable;
import ch.vd.unireg.metier.assujettissement.PeriodeImposition;
import ch.vd.unireg.tiers.AppartenanceMenage;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.FormulePolitesse;
import ch.vd.unireg.xml.address.AddressBuilder;

/**
 * Cette helper effectue la traduction des classes venant de 'core' en classes 'XML'.
 */
public abstract class DataHelper {

	//private static final Logger LOGGER = LoggerFactory.getLogger(DataHelper.class);

	private static final RegDate MIN_DATE_V1_V2 = RegDate.get(1800, 1, 1);

	private static ch.vd.unireg.xml.common.v1.Date dateV1(int year, int month, int day) {
		if (year < MIN_DATE_V1_V2.year()) {
			// la date sera donc transmise fausse, mais de toute façon elle est tellement vieille...
			return new ch.vd.unireg.xml.common.v1.Date(MIN_DATE_V1_V2.year(), MIN_DATE_V1_V2.month(), MIN_DATE_V1_V2.day());
		}
		else {
			return new ch.vd.unireg.xml.common.v1.Date(year, month, day);
		}
	}

	private static ch.vd.unireg.xml.common.v1.PartialDate partialDateV1(int year, Integer month, Integer day) {
		if (year < MIN_DATE_V1_V2.year()) {
			// la date sera donc transmise fausse, mais de toute façon elle est tellement vieille...
			return new ch.vd.unireg.xml.common.v1.PartialDate(MIN_DATE_V1_V2.year(), MIN_DATE_V1_V2.month(), MIN_DATE_V1_V2.day());
		}
		else {
			return new ch.vd.unireg.xml.common.v1.PartialDate(year, month, day);
		}
	}

	private static ch.vd.unireg.xml.common.v2.Date dateV2(int year, int month, int day) {
		if (year < MIN_DATE_V1_V2.year()) {
			// la date sera donc transmise fausse, mais de toute façon elle est tellement vieille...
			return new ch.vd.unireg.xml.common.v2.Date(MIN_DATE_V1_V2.year(), MIN_DATE_V1_V2.month(), MIN_DATE_V1_V2.day());
		}
		else {
			return new ch.vd.unireg.xml.common.v2.Date(year, month, day);
		}
	}

	private static ch.vd.unireg.xml.common.v2.PartialDate partialDateV2(int year, Integer month, Integer day) {
		if (year < MIN_DATE_V1_V2.year()) {
			// la date sera donc transmise fausse, mais de toute façon elle est tellement vieille...
			return new ch.vd.unireg.xml.common.v2.PartialDate(MIN_DATE_V1_V2.year(), MIN_DATE_V1_V2.month(), MIN_DATE_V1_V2.day());
		}
		else {
			return new ch.vd.unireg.xml.common.v2.PartialDate(year, month, day);
		}
	}

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
			return dateV1(year, month, day);
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
			return dateV2(year, month, day);
		}
	}

	public static ch.vd.unireg.xml.common.v1.Date coreToXMLv1(RegDate date) {
		if (date == null) {
			return null;
		}
		else {
			return dateV1(date.year(), date.month(), date.day());
		}
	}

	public static ch.vd.unireg.xml.common.v2.Date coreToXMLv2(RegDate date) {
		if (date == null) {
			return null;
		}
		else {
			return dateV2(date.year(), date.month(), date.day());
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
			return partialDateV1(date.year(), date.month() == RegDate.UNDEFINED ? null : date.month(), date.day() == RegDate.UNDEFINED ? null : date.day());
		}
	}

	public static ch.vd.unireg.xml.common.v2.PartialDate coreToPartialDateXmlv2(RegDate date) {
		if (date == null) {
			return null;
		}
		else {
			return partialDateV2(date.year(), date.month() == RegDate.UNDEFINED ? null : date.month(), date.day() == RegDate.UNDEFINED ? null : date.day());
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

	public static List<ch.vd.unireg.xml.party.address.v3.Address> coreToXMLv3(List<AdresseEnvoiDetaillee> adresses, @Nullable DateRangeHelper.Range range,
	                                                                          ch.vd.unireg.xml.party.address.v3.AddressType type) throws ServiceException {
		if (adresses == null || adresses.isEmpty()) {
			return null;
		}

		final List<ch.vd.unireg.xml.party.address.v3.Address> list = new ArrayList<>();
		for (AdresseEnvoiDetaillee a : adresses) {
			if (range == null || DateRangeHelper.intersect(a, range)) {
				list.add(AddressBuilder.newAddress(a, type));
			}
		}
		//SIFISC-25503 on supprime toutes les adresses Fake des réponses des services ws et asynch
		final List<ch.vd.unireg.xml.party.address.v3.Address> listeResultat=list.stream().filter(a->!a.isFake()).collect(Collectors.toList());

		return listeResultat.isEmpty() ? null : list;
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

	public static List<ch.vd.unireg.xml.party.address.v3.AddressOtherParty> coreToXMLATv3(List<AdresseEnvoiDetaillee> adresses, @Nullable DateRangeHelper.Range range,
	                                                                                      ch.vd.unireg.xml.party.address.v3.AddressType type) throws ServiceException {
		if (adresses == null || adresses.isEmpty()) {
			return null;
		}

		final List<ch.vd.unireg.xml.party.address.v3.AddressOtherParty> list = new ArrayList<>();
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

	public static ch.vd.unireg.xml.party.address.v3.TariffZone coreToXMLv3(TypeAffranchissement t) {
		if (t == null) {
			return null;
		}

		switch (t) {
		case SUISSE:
			return ch.vd.unireg.xml.party.address.v3.TariffZone.SWITZERLAND;
		case EUROPE:
			return ch.vd.unireg.xml.party.address.v3.TariffZone.EUROPE;
		case MONDE:
			return ch.vd.unireg.xml.party.address.v3.TariffZone.OTHER_COUNTRIES;
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

	public static ch.vd.unireg.xml.party.v1.PartyInfo coreToXMLv1(ch.vd.unireg.indexer.tiers.TiersIndexedData value) {
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
		i.setDateOfBirth(DataHelper.coreToXMLv1(value.getRegDateNaissanceInscriptionRC()));
		i.setType(DataHelper.getPartyTypeV1(value));
		i.setDebtorCategory(EnumHelper.coreToXMLv1(value.getCategorieImpotSource()));
		i.setDebtorCommunicationMode(EnumHelper.coreToXMLv1(value.getModeCommunication()));
		return i;
	}

	public static ch.vd.unireg.xml.party.v2.PartyInfo coreToXMLv2(ch.vd.unireg.indexer.tiers.TiersIndexedData value) {
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
		i.setDateOfBirth(DataHelper.coreToXMLv1(value.getRegDateNaissanceInscriptionRC()));
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

	public static ch.vd.unireg.xml.party.v3.PartyInfo coreToXMLv3(ch.vd.unireg.indexer.tiers.TiersIndexedData value) {
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
		i.setDateOfBirth(DataHelper.coreToPartialDateXmlv2(value.getRegDateNaissanceInscriptionRC()));
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
		i.setIndividualTaxLiability(EnumHelper.coreToXMLIndividualv2(value.getAssujettissementPP()));
		i.setCorporationTaxLiability(EnumHelper.coreToXMLCorporationv2(value.getAssujettissementPM()));
		if (i.getType() == ch.vd.unireg.xml.party.v3.PartyType.NATURAL_PERSON) {
			i.setNaturalPersonSubtype(DataHelper.getNaturalPersonSubtypeV3(value));
		}

		final List<String> numerosIDE = value.getNumerosIDE();
		if (!numerosIDE.isEmpty()) {
			i.setUidNumbers(new ch.vd.unireg.xml.party.v3.UidNumberList(numerosIDE));
		}
		return i;
	}

	public static ch.vd.unireg.xml.party.v4.PartyInfo coreToXMLv4(ch.vd.unireg.indexer.tiers.TiersIndexedData value) {
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
		i.setDateOfBirth(DataHelper.coreToPartialDateXmlv2(value.getRegDateNaissanceInscriptionRC()));
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
		i.setIndividualTaxLiability(EnumHelper.coreToXMLIndividualv3(value.getAssujettissementPP()));
		i.setCorporationTaxLiability(EnumHelper.coreToXMLCorporationv3(value.getAssujettissementPM()));
		if (i.getType() == ch.vd.unireg.xml.party.v4.PartyType.NATURAL_PERSON) {
			i.setNaturalPersonSubtype(DataHelper.getNaturalPersonSubtypeV4(value));
		}

		final List<String> numerosIDE = value.getNumerosIDE();
		if (!numerosIDE.isEmpty()) {
			i.setUidNumbers(new ch.vd.unireg.xml.party.v4.UidNumberList(numerosIDE));
		}
		return i;
	}

	public static ch.vd.unireg.xml.party.v5.PartyInfo coreToXMLv5(ch.vd.unireg.indexer.tiers.TiersIndexedData value) {
		if (value == null) {
			return null;
		}

		final ch.vd.unireg.xml.party.v5.PartyInfo i = new ch.vd.unireg.xml.party.v5.PartyInfo();
		i.setNumber(value.getNumero().intValue());
		i.setName1(value.getNom1());
		i.setName2(value.getNom2());
		i.setStreet(value.getRue());
		i.setZipCode(value.getNpa());
		i.setTown(value.getLocalite());
		i.setCountry(value.getPays());
		i.setDateOfBirth(DataHelper.coreToPartialDateXmlv2(value.getRegDateNaissanceInscriptionRC()));
		i.setType(DataHelper.getPartyTypeV5(value));
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
		i.setIndividualTaxLiability(EnumHelper.coreToXMLIndividualv4(value.getAssujettissementPP()));
		i.setCorporationTaxLiability(EnumHelper.coreToXMLCorporationv4(value.getAssujettissementPM()));
		if (i.getType() == ch.vd.unireg.xml.party.v5.PartyType.NATURAL_PERSON) {
			i.setNaturalPersonSubtype(DataHelper.getNaturalPersonSubtypeV5(value));
		}

		final List<String> numerosIDE = value.getNumerosIDE();
		if (!numerosIDE.isEmpty()) {
			i.setUidNumbers(new ch.vd.unireg.xml.party.v5.UidNumberList(numerosIDE));
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
		final List<DeclarationImpotOrdinaire> dis = contribuable.getDeclarationsDansPeriode(DeclarationImpotOrdinaire.class, periodeImposition.getDateFin().year(), false);
		if (dis.isEmpty()) {
			return null;
		}

		Long idDi = null;

		for (DeclarationImpotOrdinaire di : dis) {
			if (DateRangeHelper.intersect(periodeImposition, di)) {
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
			put(EtablissementIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v1.PartyType.CORPORATION);
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
			put(EtablissementIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v2.PartyType.CORPORATION);
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
			put(EtablissementIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v3.PartyType.CORPORATION);
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
			put(EtablissementIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v4.PartyType.CORPORATION);
			put(DebiteurPrestationImposableIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v4.PartyType.DEBTOR);
			put(CollectiviteAdministrativeIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v4.PartyType.ADMINISTRATIVE_AUTHORITY);
		}
	};

	private static final Map<String, ch.vd.unireg.xml.party.v5.PartyType> indexedData2TypeV5 = new HashMap<String, ch.vd.unireg.xml.party.v5.PartyType>() {
		{
			put(HabitantIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v5.PartyType.NATURAL_PERSON);
			put(NonHabitantIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v5.PartyType.NATURAL_PERSON);
			put(EntrepriseIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v5.PartyType.CORPORATION);
			put(MenageCommunIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v5.PartyType.HOUSEHOLD);
			put(AutreCommunauteIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v5.PartyType.OTHER_COMMUNITY);
			put(EtablissementIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v5.PartyType.ESTABLISHMENT);
			put(DebiteurPrestationImposableIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v5.PartyType.DEBTOR);
			put(CollectiviteAdministrativeIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v5.PartyType.ADMINISTRATIVE_AUTHORITY);
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

	private static final Map<String, ch.vd.unireg.xml.party.v5.NaturalPersonSubtype> indexedData2NaturalPersonSubtypeV5 = new HashMap<String, ch.vd.unireg.xml.party.v5.NaturalPersonSubtype>() {
		{
			put(HabitantIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v5.NaturalPersonSubtype.RESIDENT);
			put(NonHabitantIndexable.SUB_TYPE, ch.vd.unireg.xml.party.v5.NaturalPersonSubtype.NON_RESIDENT);
		}
	};

	/**
	 * Détermine le type d'un tiers à partir de ses données indexées.
	 *
	 * @param tiers les données indexés du tiers
	 * @return le type du tiers; ou <b>null</b> si le type de tiers n'est pas connu.
	 */
	public static ch.vd.unireg.xml.party.v1.PartyType getPartyTypeV1(ch.vd.unireg.indexer.tiers.TiersIndexedData tiers) {

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
	public static ch.vd.unireg.xml.party.v2.PartyType getPartyTypeV2(ch.vd.unireg.indexer.tiers.TiersIndexedData tiers) {

		final String typeAsString = tiers.getTiersType();

		if (StringUtils.isEmpty(typeAsString)) {
			return null;
		}

		return indexedData2TypeV2.get(typeAsString);
	}

	public static ch.vd.unireg.xml.party.v3.PartyType getPartyTypeV3(ch.vd.unireg.indexer.tiers.TiersIndexedData tiers) {

		final String typeAsString = tiers.getTiersType();

		if (StringUtils.isEmpty(typeAsString)) {
			return null;
		}

		return indexedData2TypeV3.get(typeAsString);
	}

	public static ch.vd.unireg.xml.party.v4.PartyType getPartyTypeV4(ch.vd.unireg.indexer.tiers.TiersIndexedData tiers) {

		final String typeAsString = tiers.getTiersType();

		if (StringUtils.isEmpty(typeAsString)) {
			return null;
		}

		return indexedData2TypeV4.get(typeAsString);
	}

	public static ch.vd.unireg.xml.party.v5.PartyType getPartyTypeV5(ch.vd.unireg.indexer.tiers.TiersIndexedData tiers) {

		final String typeAsString = tiers.getTiersType();

		if (StringUtils.isEmpty(typeAsString)) {
			return null;
		}

		return indexedData2TypeV5.get(typeAsString);
	}

	public static ch.vd.unireg.xml.party.v3.NaturalPersonSubtype getNaturalPersonSubtypeV3(ch.vd.unireg.indexer.tiers.TiersIndexedData tiers) {
		final String typeAsString = tiers.getTiersType();

		if (StringUtils.isEmpty(typeAsString)) {
			return null;
		}

		return indexedData2NaturalPersonSubtypeV3.get(typeAsString);
	}

	public static ch.vd.unireg.xml.party.v4.NaturalPersonSubtype getNaturalPersonSubtypeV4(ch.vd.unireg.indexer.tiers.TiersIndexedData tiers) {
		final String typeAsString = tiers.getTiersType();

		if (StringUtils.isEmpty(typeAsString)) {
			return null;
		}

		return indexedData2NaturalPersonSubtypeV4.get(typeAsString);
	}

	public static ch.vd.unireg.xml.party.v5.NaturalPersonSubtype getNaturalPersonSubtypeV5(ch.vd.unireg.indexer.tiers.TiersIndexedData tiers) {
		final String typeAsString = tiers.getTiersType();

		if (StringUtils.isEmpty(typeAsString)) {
			return null;
		}

		return indexedData2NaturalPersonSubtypeV5.get(typeAsString);
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
				results.add(TiersDAO.Parts.FORS_FISCAUX);
				break;
			case TAX_LIABILITIES:
			case SIMPLIFIED_TAX_LIABILITIES:
			case TAXATION_PERIODS:
				results.add(TiersDAO.Parts.FORS_FISCAUX);
				results.add(TiersDAO.Parts.BOUCLEMENTS);
				break;
			case RELATIONS_BETWEEN_PARTIES:
			case CHILDREN:
			case PARENTS:
			case HOUSEHOLD_MEMBERS:
			case BANK_ACCOUNTS:
				results.add(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS);
				break;
			case FAMILY_STATUSES:
				results.add(TiersDAO.Parts.SITUATIONS_FAMILLE);
				break;
			case DEBTOR_PERIODICITIES:
				results.add(TiersDAO.Parts.PERIODICITES);
				break;
			case IMMOVABLE_PROPERTIES:
				// [SIFISC-26536] la part IMMOVABLE_PROPERTIES est dépréciée et n'a aucun effet
				break;
			case CORPORATION_STATUSES:
				results.add(TiersDAO.Parts.ETATS_FISCAUX);
				break;
			case CAPITALS:
			case LEGAL_FORMS:
				results.add(TiersDAO.Parts.DONNEES_CIVILES);
				break;
			case TAX_SYSTEMS:
				results.add(TiersDAO.Parts.REGIMES_FISCAUX);
				break;
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
				results.add(TiersDAO.Parts.FORS_FISCAUX);
				break;
			case TAX_LIABILITIES:
			case SIMPLIFIED_TAX_LIABILITIES:
			case TAXATION_PERIODS:
				results.add(TiersDAO.Parts.FORS_FISCAUX);
				results.add(TiersDAO.Parts.BOUCLEMENTS);
				break;
			case RELATIONS_BETWEEN_PARTIES:
			case CHILDREN:
			case PARENTS:
			case HOUSEHOLD_MEMBERS:
			case BANK_ACCOUNTS:
				results.add(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS);
				break;
			case FAMILY_STATUSES:
				results.add(TiersDAO.Parts.SITUATIONS_FAMILLE);
				break;
			case DEBTOR_PERIODICITIES:
				results.add(TiersDAO.Parts.PERIODICITES);
				break;
			case IMMOVABLE_PROPERTIES:
				// [SIFISC-26536] la part IMMOVABLE_PROPERTIES est dépréciée et n'a aucun effet
				break;
			case CORPORATION_STATUSES:
				results.add(TiersDAO.Parts.ETATS_FISCAUX);
				break;
			case CAPITALS:
			case LEGAL_FORMS:
				results.add(TiersDAO.Parts.DONNEES_CIVILES);
				break;
			case TAX_SYSTEMS:
				results.add(TiersDAO.Parts.REGIMES_FISCAUX);
				break;
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
				results.add(TiersDAO.Parts.FORS_FISCAUX);
				break;
			case TAX_LIABILITIES:
			case SIMPLIFIED_TAX_LIABILITIES:
			case TAXATION_PERIODS:
				results.add(TiersDAO.Parts.FORS_FISCAUX);
				results.add(TiersDAO.Parts.BOUCLEMENTS);
				break;
			case WITHHOLDING_TAXATION_PERIODS:
				results.add(TiersDAO.Parts.FORS_FISCAUX);
				results.add(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS);
				break;
			case CHILDREN:
			case PARENTS:
			case RELATIONS_BETWEEN_PARTIES:
			case HOUSEHOLD_MEMBERS:
			case BANK_ACCOUNTS:
				results.add(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS);
				break;
			case FAMILY_STATUSES:
				results.add(TiersDAO.Parts.SITUATIONS_FAMILLE);
				break;
			case DEBTOR_PERIODICITIES:
				results.add(TiersDAO.Parts.PERIODICITES);
				break;
			case IMMOVABLE_PROPERTIES:
				// [SIFISC-26536] la part IMMOVABLE_PROPERTIES est dépréciée et n'a aucun effet
				break;
			case CORPORATION_STATUSES:
				results.add(TiersDAO.Parts.ETATS_FISCAUX);
				break;
			case CAPITALS:
			case LEGAL_FORMS:
				results.add(TiersDAO.Parts.DONNEES_CIVILES);
				break;
			case TAX_SYSTEMS:
				results.add(TiersDAO.Parts.REGIMES_FISCAUX);
				break;
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
				results.add(TiersDAO.Parts.FORS_FISCAUX);
				break;
			case TAX_LIABILITIES:
			case SIMPLIFIED_TAX_LIABILITIES:
			case TAXATION_PERIODS:
				results.add(TiersDAO.Parts.FORS_FISCAUX);
				results.add(TiersDAO.Parts.BOUCLEMENTS);
				break;
			case WITHHOLDING_TAXATION_PERIODS:
				results.add(TiersDAO.Parts.FORS_FISCAUX);
				results.add(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS);
				break;
			case CHILDREN:
			case PARENTS:
			case RELATIONS_BETWEEN_PARTIES:
			case HOUSEHOLD_MEMBERS:
			case BANK_ACCOUNTS:
				results.add(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS);
				break;
			case FAMILY_STATUSES:
				results.add(TiersDAO.Parts.SITUATIONS_FAMILLE);
				break;
			case DEBTOR_PERIODICITIES:
				results.add(TiersDAO.Parts.PERIODICITES);
				break;
			case IMMOVABLE_PROPERTIES:
				// [SIFISC-26536] la part IMMOVABLE_PROPERTIES est dépréciée et n'a aucun effet
				break;
			case TAX_LIGHTENINGS:
				results.add(TiersDAO.Parts.ALLEGEMENTS_FISCAUX);
				break;
			case CORPORATION_STATUSES:
				results.add(TiersDAO.Parts.ETATS_FISCAUX);
				break;
			case CAPITALS:
			case LEGAL_FORMS:
				results.add(TiersDAO.Parts.DONNEES_CIVILES);
				break;
			case TAX_SYSTEMS:
				results.add(TiersDAO.Parts.REGIMES_FISCAUX);
				break;
			case BUSINESS_YEARS:
				results.add(TiersDAO.Parts.BOUCLEMENTS);
				break;
			case CORPORATION_FLAGS:
			    results.add(TiersDAO.Parts.FLAGS);
				break;
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

	public static Set<TiersDAO.Parts> xmlToCoreV5(Set<ch.vd.unireg.xml.party.v5.PartyPart> parts) {

		if (parts == null) {
			return null;
		}

		final Set<TiersDAO.Parts> results = EnumSet.noneOf(TiersDAO.Parts.class);
		for (ch.vd.unireg.xml.party.v5.PartyPart p : parts) {
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
				results.add(TiersDAO.Parts.FORS_FISCAUX);
				break;
			case TAX_LIABILITIES:
			case SIMPLIFIED_TAX_LIABILITIES:
			case TAXATION_PERIODS:
				results.add(TiersDAO.Parts.FORS_FISCAUX);
				results.add(TiersDAO.Parts.BOUCLEMENTS);
				break;
			case WITHHOLDING_TAXATION_PERIODS:
				results.add(TiersDAO.Parts.FORS_FISCAUX);
				results.add(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS);
				break;
			case CHILDREN:
			case PARENTS:
			case RELATIONS_BETWEEN_PARTIES:
			case HOUSEHOLD_MEMBERS:
			case BANK_ACCOUNTS:
			case INHERITANCE_RELATIONSHIPS:
				results.add(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS);
				break;
			case FAMILY_STATUSES:
				results.add(TiersDAO.Parts.SITUATIONS_FAMILLE);
				break;
			case DEBTOR_PERIODICITIES:
				results.add(TiersDAO.Parts.PERIODICITES);
				break;
			case IMMOVABLE_PROPERTIES:
				// [SIFISC-26536] la part IMMOVABLE_PROPERTIES est dépréciée et n'a aucun effet
				break;
			case TAX_LIGHTENINGS:
				results.add(TiersDAO.Parts.ALLEGEMENTS_FISCAUX);
				break;
			case CORPORATION_STATUSES:
				results.add(TiersDAO.Parts.ETATS_FISCAUX);
				break;
			case CAPITALS:
			case LEGAL_FORMS:
				results.add(TiersDAO.Parts.DONNEES_CIVILES);
				break;
			case TAX_SYSTEMS:
				results.add(TiersDAO.Parts.REGIMES_FISCAUX);
				break;
			case BUSINESS_YEARS:
				results.add(TiersDAO.Parts.BOUCLEMENTS);
				break;
			case CORPORATION_FLAGS:
			    results.add(TiersDAO.Parts.FLAGS);
				break;
			case AGENTS:
				results.add(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS);
				results.add(TiersDAO.Parts.ADRESSES_MANDATAIRES);
				break;
			case LABELS:
				results.add(TiersDAO.Parts.ETIQUETTES);
				results.add(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS);
				break;
			case LEGAL_SEATS:
			case EBILLING_STATUSES:
			case RESIDENCY_PERIODS:
				// rien à faire
				break;
			case LAND_RIGHTS:
			case VIRTUAL_LAND_RIGHTS:
			case LAND_TAX_LIGHTENINGS:
			case VIRTUAL_INHERITANCE_LAND_RIGHTS:
				// rien à faire
				break;
			default:
				throw new IllegalArgumentException("Type de parts inconnue = [" + p + ']');
			}
		}

		return results;
	}

	public static List<ForFiscalPrincipal> getForsFiscauxVirtuels(Tiers tiers, boolean doNotAutoflush, HibernateTemplate hibernateTemplate) {

		// Récupère les appartenances ménages du tiers
		final Set<RapportEntreTiers> rapports = tiers.getRapportsSujet();
		final List<RapportEntreTiers> rapportsMenage = rapports.stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(AppartenanceMenage.class::isInstance)
				.collect(Collectors.toList());

		if (rapportsMenage.isEmpty()) {
			return Collections.emptyList();
		}

		final List<ForFiscalPrincipal> forsVirtuels = new ArrayList<>();

		// Extrait les fors principaux du ménage, en les adaptant à la période de validité des appartenances ménages
		for (RapportEntreTiers a : rapportsMenage) {
			final Map<String, Long> params = new HashMap<>(1);
			params.put("menageId", a.getObjetId());

			final FlushMode flushMode = doNotAutoflush ? FlushMode.MANUAL : null;
			final List<ForFiscalPrincipal> forsMenage = hibernateTemplate.find("from ForFiscalPrincipalPP f where f.annulationDate is null and f.tiers.id = :menageId order by f.dateDebut asc", params, flushMode);
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

	public static <T extends Enum<T>> Set<T> toSet(Class<T> clazz, List<T> parts) {
		return parts.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(() -> EnumSet.noneOf(clazz)));
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
