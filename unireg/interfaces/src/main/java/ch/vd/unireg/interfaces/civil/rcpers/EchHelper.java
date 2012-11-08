package ch.vd.unireg.interfaces.civil.rcpers;

import javax.xml.datatype.XMLGregorianCalendar;

import ch.ech.ech0007.v4.CantonAbbreviation;
import ch.ech.ech0044.v2.DatePartiallyKnown;
import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.ServiceCivilException;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.type.Sexe;

public abstract class EchHelper {

	public static String sexeToEch44(ch.vd.uniregctb.type.Sexe sexe) {
		if (sexe == null) {
			return null;
		}
		switch (sexe) {
		case MASCULIN:
			return "1";
		case FEMININ:
			return "2";
		default:
			throw new IllegalArgumentException("Type de sexe inconnu = [" + sexe + ']');
		}
	}

	public static Sexe sexeFromEch44(String sexe) {
		if (sexe == null) {
			return null;
		}
		if ("1".equals(sexe)) {
			return Sexe.MASCULIN;
		}
		else if ("2".equals(sexe)) {
			return Sexe.FEMININ;
		}
		else {
			throw new IllegalArgumentException("Type de sexe inconnu = [" + sexe + ']');
		}
	}

	public static Long avs13ToEch(String nAVS13) {
		if (StringUtils.isBlank(nAVS13)) {
			return null;
		}
		return Long.valueOf(nAVS13);
	}

	public static String avs13FromEch(long vn) {
		return String.valueOf(vn);
	}

	public static DatePartiallyKnown partialDateToEch44(RegDate from) {
		if (from == null) {
			return null;
		}
		final DatePartiallyKnown to = new DatePartiallyKnown();
		final XMLGregorianCalendar cal = XmlUtils.regdate2xmlcal(from);

		if (from.day() == RegDate.UNDEFINED) {
			if (from.month() == RegDate.UNDEFINED) {
				to.setYear(cal);
			}
			else {
				to.setYearMonth(cal);
			}
		}
		else {
			to.setYearMonthDay(cal);
		}

		return to;
	}

	public static RegDate partialDateFromEch44(DatePartiallyKnown from) {
		if (from == null) {
			return null;
		}

		final RegDate date;
		if (from.getYearMonthDay() == null) {
			if (from.getYearMonth() == null) {
				date = RegDate.get(from.getYear().getYear());
			}
			else {
				XMLGregorianCalendar cal = from.getYearMonth();
				date = RegDate.get(cal.getYear(), cal.getMonth());
			}
		}
		else {
			XMLGregorianCalendar cal = from.getYearMonthDay();
			date = RegDate.get(cal.getYear(), cal.getMonth(), cal.getDay());
		}

		return date;
	}

	public static RegDate partialDateFromEch44(ch.ech.ech0044.v1.DatePartiallyKnown from) {
		if (from == null) {
			return null;
		}

		final RegDate date;
		if (from.getYearMonthDay() == null) {
			if (from.getYearMonth() == null) {
				date = RegDate.get(from.getYear().getYear());
			}
			else {
				XMLGregorianCalendar cal = from.getYearMonth();
				date = RegDate.get(cal.getYear(), cal.getMonth());
			}
		}
		else {
			XMLGregorianCalendar cal = from.getYearMonthDay();
			date = RegDate.get(cal.getYear(), cal.getMonth(), cal.getDay());
		}

		return date;
	}

	public static TypeEtatCivil etatCivilFromEch11(String maritalStatus, String cancelationReason) {
		// voir la spécification http://subversion.etat-de-vaud.ch/SVN_ACI/registre/rcpers/trunk/06-Deploiement/ManuelsTechniques/TEC-CatalogueOfficielCaracteres.doc
		// au chapitre "Etat civil".
		if (maritalStatus == null) {
			return null;
		}
		if ("1".equals(maritalStatus)) {
			return TypeEtatCivil.CELIBATAIRE;
		}
		else if ("2".equals(maritalStatus)) {
			return TypeEtatCivil.MARIE;
		}
		else if ("3".equals(maritalStatus)) {
			return TypeEtatCivil.VEUF;
		}
		else if ("4".equals(maritalStatus)) {
			return TypeEtatCivil.DIVORCE;
		}
		else if ("5".equals(maritalStatus)) {
			return TypeEtatCivil.NON_MARIE;
		}
		else if ("6".equals(maritalStatus)) {
			return TypeEtatCivil.PACS;
		}
		else if ("7".equals(maritalStatus)) { // Partenariat dissous...
			if (cancelationReason == null) {
				throw new ServiceCivilException("Dans la cas d'une dissolution de partenariat, la raison précise doit être renseignée."); // SIREF-1834
			}
			if ("1".equals(cancelationReason)) {
				// Partenariat dissous judiciairement
				return TypeEtatCivil.PACS_TERMINE;
			}
			else if ("2".equals(cancelationReason)) {
				// Partenariat dissous en suite déclaration d'annulation
				return TypeEtatCivil.NON_MARIE;
			}
			else if ("3".equals(cancelationReason)) {
				// Partenariat dissous en suite déclaration d'abscence
				return TypeEtatCivil.PACS_VEUF;
			}
			else if ("4".equals(cancelationReason)) {
				// Partenariat dissous par décès
				return TypeEtatCivil.PACS_VEUF;
			}
			else if ("9".equals(cancelationReason)) {
				// Inconnu / autres motifs
				return TypeEtatCivil.PACS_TERMINE;
			}
			else {
				throw new IllegalArgumentException("Type de cancelation reason inconnu = [" + cancelationReason + ']');
			}
		}
		else {
			throw new IllegalArgumentException("Type de marital status inconnu = [" + maritalStatus + ']');
		}
	}

	public static String etatCivilToEch11(TypeEtatCivil etatCivil) {

		if (etatCivil == null) {
			return null;
		}

		switch (etatCivil) {
		case CELIBATAIRE:
			return "1";
		case MARIE:
			return "2";
		case VEUF:
			return "3";
		case DIVORCE:
			return "4";
		case NON_MARIE:
			return "5";
		case PACS:
			return "6";
		case PACS_TERMINE:
			return "7/1";
		case PACS_VEUF:
			return "7/4";
		case SEPARE:
		case PACS_SEPARE:
		default:
			throw new IllegalArgumentException("L'état-civil [" + etatCivil + "] n'existe pas dans la norme eCH");
		}
	}

	public static CantonAbbreviation sigleCantonToAbbreviation(String sigleCanton) {
		for (CantonAbbreviation abbreviation : CantonAbbreviation.values()) {
			if (abbreviation.name().equalsIgnoreCase(sigleCanton)) {
				return abbreviation;
			}
		}
		return null;
	}
}
