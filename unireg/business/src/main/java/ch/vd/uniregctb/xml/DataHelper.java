package ch.vd.uniregctb.xml;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.xml.common.v1.Date;
import ch.vd.unireg.xml.party.address.v1.Address;
import ch.vd.unireg.xml.party.address.v1.AddressOtherParty;
import ch.vd.unireg.xml.party.address.v1.AddressType;
import ch.vd.unireg.xml.party.address.v1.TariffZone;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.type.FormulePolitesse;
import ch.vd.uniregctb.xml.address.AddressBuilder;

/**
 * Cette helper effectue la traduction des classes venant de 'core' en classes 'XML'.
 */
public abstract class DataHelper {

	//private static final Logger LOGGER = Logger.getLogger(DataHelper.class);

	public static boolean coreToXML(Boolean value) {
		return value != null && value;
	}

	public static Date coreToXML(java.util.Date date) {
		if (date == null) {
			return null;
		}
		else {
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTime(date);

			final Date d = new Date();
			d.setYear(cal.get(Calendar.YEAR));
			d.setMonth(cal.get(Calendar.MONTH) + 1);
			d.setDay(cal.get(Calendar.DAY_OF_MONTH));
			return d;
		}
	}

	public static Date coreToXML(RegDate date) {
		if (date == null) {
			return null;
		}
		else {
			final Date d = new Date();
			d.setYear(date.year());
			d.setMonth(date.month());
			d.setDay(date.day());
			return d;
		}
	}

	public static RegDate xmlToCore(Date date) {
		if (date == null) {
			return null;
		}
		return RegDate.get(date.getYear(), date.getMonth(), date.getDay());
	}

	public static List<Address> coreToXML(List<AdresseEnvoiDetaillee> adresses, @Nullable DateRangeHelper.Range range, AddressType type) throws ServiceException {
		if (adresses == null || adresses.isEmpty()) {
			return null;
		}

		List<Address> list = new ArrayList<Address>();
		for (AdresseEnvoiDetaillee a : adresses) {
			if (range == null || DateRangeHelper.intersect(a, range)) {
				list.add(AddressBuilder.newAddress(a, type));
			}
		}

		return list.isEmpty() ? null : list;
	}

	public static List<AddressOtherParty> coreToXMLAT(List<AdresseEnvoiDetaillee> adresses, @Nullable DateRangeHelper.Range range, AddressType type) throws ServiceException {
		if (adresses == null || adresses.isEmpty()) {
			return null;
		}

		List<AddressOtherParty> list = new ArrayList<AddressOtherParty>();
		for (AdresseEnvoiDetaillee a : adresses) {
			if (range == null || DateRangeHelper.intersect(a, range)) {
				list.add(AddressBuilder.newOtherPartyAddress(a, type));
			}
		}

		return list.isEmpty() ? null : list;
	}

	public static Date coreToXML(String s) {
		return coreToXML(RegDateHelper.dashStringToDate(s));
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

	public static TariffZone coreToXML(ch.vd.uniregctb.interfaces.model.TypeAffranchissement t) {
		if (t == null) {
			return null;
		}

		switch (t) {
		case SUISSE:
			return TariffZone.SWITZERLAND;
		case EUROPE:
			return TariffZone.EUROPE;
		case MONDE:
			return TariffZone.OTHER_COUNTRIES;
		default:
			throw new IllegalArgumentException("Type d'affranchissement inconnu = [" + t + "]");
		}
	}

	public static TypeAdresseFiscale xmlToCore(AddressType type) {
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
			throw new IllegalArgumentException("Unknown AddressType = [" + type + "]");
		}
	}
}
