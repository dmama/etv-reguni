package ch.vd.uniregctb.webservices.tiers3.data;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import ch.vd.unireg.webservices.tiers3.Address;
import ch.vd.unireg.webservices.tiers3.AddressInformation;
import ch.vd.unireg.webservices.tiers3.AddressOtherParty;
import ch.vd.unireg.webservices.tiers3.CoupleMailAddressInfo;
import ch.vd.unireg.webservices.tiers3.FormattedAddress;
import ch.vd.unireg.webservices.tiers3.OrganisationMailAddressInfo;
import ch.vd.unireg.webservices.tiers3.OtherPartyAddressType;
import ch.vd.unireg.webservices.tiers3.PersonMailAddressInfo;
import ch.vd.unireg.webservices.tiers3.PersonName;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.common.CasePostale;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.common.NpaEtLocalite;
import ch.vd.uniregctb.common.RueEtNumero;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

public class AddressBuilder {

//	private static final Logger LOGGER = Logger.getLogger(AddressBuilder.class);

	public static Address newAddress(AdresseEnvoiDetaillee adresse) {
		final Address a = new Address();
		fillAddress(adresse, a);
		return a;
	}

	public static AddressOtherParty newOtherPartyAddress(AdresseEnvoiDetaillee adresse) {
		final AddressOtherParty a = new AddressOtherParty();
		fillAddress(adresse, a);
		a.setType(source2type(adresse.getSource()));
		return a;
	}

	private static void fillAddress(AdresseEnvoiDetaillee adresse, Address a) {
		a.setDateFrom(DataHelper.coreToWeb(adresse.getDateDebut()));
		a.setDateTo(DataHelper.coreToWeb(adresse.getDateFin()));

		fillRecipient(a, adresse);
		fillDestination(a, adresse);
		fillFormattedAddress(a, adresse);
	}

	private static OtherPartyAddressType source2type(AdresseGenerique.SourceType source) {

		if (source == null) {
			return null;
		}

		switch (source) {
		case FISCALE:
			return OtherPartyAddressType.SPECIFIC;
		case REPRESENTATION:
			return OtherPartyAddressType.REPRESENTATIVE;
		case CURATELLE:
			return OtherPartyAddressType.WELFARE_ADVOCATE;
		case CONSEIL_LEGAL:
			return OtherPartyAddressType.LEGAL_ADVISER;
		case TUTELLE:
			return OtherPartyAddressType.GUARDIAN;
		default:
			throw new IllegalArgumentException("Le type de source = [" + source + "] n'est pas représentable comme type d'adresse autre tiers");
		}

	}

	private static void fillRecipient(Address a, AdresseEnvoiDetaillee adresse) {

		if (adresse.getDestinataire() instanceof PersonnePhysique) {
			final PersonMailAddressInfo personInfo = new PersonMailAddressInfo();
			personInfo.setMrMrs(DataHelper.salutations2MrMrs(adresse.getSalutations()));
			final List<NomPrenom> nomsPrenoms = adresse.getNomsPrenoms();
			if (!nomsPrenoms.isEmpty()) {
				personInfo.setFirstName(nomsPrenoms.get(0).getPrenom());
				personInfo.setLastName(nomsPrenoms.get(0).getNom());
			}
			personInfo.setSalutation(adresse.getSalutations());
			personInfo.setFormalGreeting(adresse.getFormuleAppel());
			a.setPerson(personInfo);
		}
		else if (adresse.getDestinataire() instanceof MenageCommun) {
			final CoupleMailAddressInfo coupleInfo = new CoupleMailAddressInfo();
			coupleInfo.setSalutation(adresse.getSalutations());
			coupleInfo.setFormalGreeting(adresse.getFormuleAppel());
			for (NomPrenom nomPrenom : adresse.getNomsPrenoms()) {
				coupleInfo.getNames().add(new PersonName(nomPrenom.getPrenom(), nomPrenom.getNom()));
			}
			a.setCouple(coupleInfo);
		}
		else {
			final OrganisationMailAddressInfo organisationInfo = new OrganisationMailAddressInfo();
			final List<String> noms = adresse.getNomsPrenomsOuRaisonsSociales();
			if (noms.size() > 0) {
				organisationInfo.setOrganisationName(noms.get(0));
			}
			if (noms.size() > 1) {
				organisationInfo.setOrganisationNameAddOn1(noms.get(1));
			}
			if (noms.size() > 2) {
				organisationInfo.setOrganisationNameAddOn2(noms.get(2));
			}
			if (adresse.getPourAdresse() != null) {
				organisationInfo.setLastName(adresse.getPourAdresse());
			}
			organisationInfo.setFormalGreeting(adresse.getFormuleAppel());
			a.setOrganisation(organisationInfo);
		}
	}

	private static void fillDestination(Address to, AdresseEnvoiDetaillee from) {
		final AddressInformation info = new AddressInformation();

		info.setComplementaryInformation(from.getComplement());
		info.setCareOf(from.getPourAdresse());
		info.setDwellingNumber(from.getNumeroAppartement());

		final RueEtNumero rueEtNumero = from.getRueEtNumero();
		if (rueEtNumero != null) {
			info.setStreet(rueEtNumero.getRue());
			info.setHouseNumber(rueEtNumero.getNumero());
		}

		final CasePostale casePostale = from.getCasePostale();
		if (casePostale != null) {
			info.setPostOfficeBoxText(casePostale.getType().format());
			info.setPostOfficeBoxNumber(casePostale.getNumero() == null ? null : casePostale.getNumero().longValue());
		}

		final NpaEtLocalite npaEtLocalite = from.getNpaEtLocalite();
		if (npaEtLocalite != null) {
			if (from.isSuisse()) {
				if (StringUtils.isNotBlank(npaEtLocalite.getNpa())) {
					info.setSwissZipCode(Long.valueOf(npaEtLocalite.getNpa()));
				}
			}
			else {
				info.setForeignZipCode(npaEtLocalite.getNpa());
			}
			info.setTown(npaEtLocalite.getLocalite());
		}

		info.setCountry(from.getPays()); // FIXME (msi) il faut renseigner le code ISO sur 2 positions du pays, et pas son nom complet !
		info.setCountryName(from.getPays());
		info.setSwissZipCodeId(from.getNumeroOrdrePostal());
		info.setStreetId(from.getNumeroTechniqueRue());
		info.setCountryId(from.getNoOfsPays());
		info.setTariffZone(EnumHelper.coreToWeb(from.getTypeAffranchissement()));

		to.setAddressInformation(info);
	}

	private static void fillFormattedAddress(Address a, AdresseEnvoiDetaillee adresse) {
		final FormattedAddress formattedAdresse = new FormattedAddress();
		formattedAdresse.setLine1(adresse.getLigne1());
		formattedAdresse.setLine2(adresse.getLigne2());
		formattedAdresse.setLine3(adresse.getLigne3());
		formattedAdresse.setLine4(adresse.getLigne4());
		formattedAdresse.setLine5(adresse.getLigne5());
		formattedAdresse.setLine6(adresse.getLigne6());
		a.setFormattedAddress(formattedAdresse);
	}
}
