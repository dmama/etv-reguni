package ch.vd.uniregctb.xml.address;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.common.NpaEtLocalite;
import ch.vd.uniregctb.common.RueEtNumero;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.xml.DataHelper;

public class AddressBuilder {

//	private static final Logger LOGGER = LoggerFactory.getLogger(AddressBuilder.class);

	public static ch.vd.unireg.xml.party.address.v1.Address newAddress(AdresseEnvoiDetaillee adresse, ch.vd.unireg.xml.party.address.v1.AddressType type) {
		final ch.vd.unireg.xml.party.address.v1.Address a = new ch.vd.unireg.xml.party.address.v1.Address();
		fillAddress(adresse, a, type);
		return a;
	}

	public static ch.vd.unireg.xml.party.address.v2.Address newAddress(AdresseEnvoiDetaillee adresse, ch.vd.unireg.xml.party.address.v2.AddressType type) {
		final ch.vd.unireg.xml.party.address.v2.Address a = new ch.vd.unireg.xml.party.address.v2.Address();
		fillAddress(adresse, a, type);
		return a;
	}

	public static ch.vd.unireg.xml.party.address.v1.AddressOtherParty newOtherPartyAddress(AdresseEnvoiDetaillee adresse, ch.vd.unireg.xml.party.address.v1.AddressType type) {
		final ch.vd.unireg.xml.party.address.v1.AddressOtherParty a = new ch.vd.unireg.xml.party.address.v1.AddressOtherParty();
		final ch.vd.unireg.xml.party.address.v1.Address base = new ch.vd.unireg.xml.party.address.v1.Address();
		fillAddress(adresse, base, type);
		a.setBase(base);
		a.setOtherPartyType(source2typeV1(adresse.getSource()));
		return a;
	}

	public static ch.vd.unireg.xml.party.address.v2.AddressOtherParty newOtherPartyAddress(AdresseEnvoiDetaillee adresse, ch.vd.unireg.xml.party.address.v2.AddressType type) {
		final ch.vd.unireg.xml.party.address.v2.AddressOtherParty a = new ch.vd.unireg.xml.party.address.v2.AddressOtherParty();
		final ch.vd.unireg.xml.party.address.v2.Address base = new ch.vd.unireg.xml.party.address.v2.Address();
		fillAddress(adresse, base, type);
		a.setBase(base);
		a.setOtherPartyType(source2typeV2(adresse.getSource()));
		return a;
	}

	private static void fillAddress(AdresseEnvoiDetaillee adresse, ch.vd.unireg.xml.party.address.v1.Address a, ch.vd.unireg.xml.party.address.v1.AddressType type) {
		a.setDateFrom(DataHelper.coreToXMLv1(adresse.getDateDebut()));
		a.setDateTo(DataHelper.coreToXMLv1(adresse.getDateFin()));
		a.setType(type);
		a.setFake(adresse.isArtificelle());
		a.setIncomplete(adresse.isIncomplete());

		fillRecipient(a, adresse);
		//SIFISC-8148 ne pas remplir pour des adresses de courrier pour lesquelle il manque le pays,
		// la ville ou le type d'affranchissement car ne respecte plus la xsd eCH-0010
		if (isCompleteForAddressInformation(adresse)) {
			fillDestination(a, adresse);
		}

		fillFormattedAddress(a, adresse);
	}

	private static void fillAddress(AdresseEnvoiDetaillee adresse, ch.vd.unireg.xml.party.address.v2.Address a, ch.vd.unireg.xml.party.address.v2.AddressType type) {
		a.setDateFrom(DataHelper.coreToXMLv2(adresse.getDateDebut()));
		a.setDateTo(DataHelper.coreToXMLv2(adresse.getDateFin()));
		a.setType(type);
		a.setFake(adresse.isArtificelle());
		a.setIncomplete(adresse.isIncomplete());

		fillRecipient(a, adresse);
		//SIFISC-8148 ne pas remplir pour des adresses de courrier pour lesquelle il manque le pays,
		// la ville ou le type d'affranchissement car ne respecte plus la xsd eCH-0010
		if (isCompleteForAddressInformation(adresse)) {
			fillDestination(a, adresse);
		}

		fillFormattedAddress(a, adresse);
	}

	private static ch.vd.unireg.xml.party.address.v1.OtherPartyAddressType source2typeV1(AdresseGenerique.SourceType source) {

		if (source == null) {
			return null;
		}

		switch (source) {
		case FISCALE:
			return ch.vd.unireg.xml.party.address.v1.OtherPartyAddressType.SPECIFIC;
		case REPRESENTATION:
			return ch.vd.unireg.xml.party.address.v1.OtherPartyAddressType.REPRESENTATIVE;
		case CURATELLE:
			return ch.vd.unireg.xml.party.address.v1.OtherPartyAddressType.WELFARE_ADVOCATE;
		case CONSEIL_LEGAL:
			return ch.vd.unireg.xml.party.address.v1.OtherPartyAddressType.LEGAL_ADVISER;
		case TUTELLE:
			return ch.vd.unireg.xml.party.address.v1.OtherPartyAddressType.GUARDIAN;
		default:
			throw new IllegalArgumentException("Le type de source = [" + source + "] n'est pas représentable comme type d'adresse autre tiers");
		}

	}

	private static ch.vd.unireg.xml.party.address.v2.OtherPartyAddressType source2typeV2(AdresseGenerique.SourceType source) {

		if (source == null) {
			return null;
		}

		switch (source) {
		case FISCALE:
			return ch.vd.unireg.xml.party.address.v2.OtherPartyAddressType.SPECIFIC;
		case REPRESENTATION:
			return ch.vd.unireg.xml.party.address.v2.OtherPartyAddressType.REPRESENTATIVE;
		case CURATELLE:
			return ch.vd.unireg.xml.party.address.v2.OtherPartyAddressType.WELFARE_ADVOCATE;
		case CONSEIL_LEGAL:
			return ch.vd.unireg.xml.party.address.v2.OtherPartyAddressType.LEGAL_ADVISER;
		case TUTELLE:
			return ch.vd.unireg.xml.party.address.v2.OtherPartyAddressType.GUARDIAN;
		default:
			throw new IllegalArgumentException("Le type de source = [" + source + "] n'est pas représentable comme type d'adresse autre tiers");
		}
	}

	private static void fillRecipient(ch.vd.unireg.xml.party.address.v1.Address a, AdresseEnvoiDetaillee adresse) {

		if (adresse.getDestinataire() instanceof PersonnePhysique) {
			final ch.vd.unireg.xml.party.address.v1.PersonMailAddressInfo personInfo = new ch.vd.unireg.xml.party.address.v1.PersonMailAddressInfo();
			personInfo.setMrMrs(DataHelper.salutations2MrMrs(adresse.getSalutations()));
			final List<NomPrenom> nomsPrenoms = adresse.getNomsPrenoms();
			if (!nomsPrenoms.isEmpty()) {
				personInfo.setFirstName(DataHelper.truncate(nomsPrenoms.get(0).getPrenom(), 30));
				personInfo.setLastName(DataHelper.truncate(nomsPrenoms.get(0).getNom(), 30));
			}
			personInfo.setSalutation(adresse.getSalutations());
			personInfo.setFormalGreeting(adresse.getFormuleAppel());
			a.setPerson(personInfo);
		}
		else if (adresse.getDestinataire() instanceof MenageCommun) {
			final ch.vd.unireg.xml.party.address.v1.CoupleMailAddressInfo coupleInfo = new ch.vd.unireg.xml.party.address.v1.CoupleMailAddressInfo();
			coupleInfo.setSalutation(adresse.getSalutations());
			coupleInfo.setFormalGreeting(adresse.getFormuleAppel());
			for (NomPrenom nomPrenom : adresse.getNomsPrenoms()) {
				coupleInfo.getNames().add(new ch.vd.unireg.xml.party.address.v1.PersonName(DataHelper.truncate(nomPrenom.getPrenom(), 30), DataHelper.truncate(nomPrenom.getNom(), 30), null));
			}
			a.setCouple(coupleInfo);
		}
		else {
			final ch.vd.unireg.xml.party.address.v1.OrganisationMailAddressInfo organisationInfo = new ch.vd.unireg.xml.party.address.v1.OrganisationMailAddressInfo();
			final List<String> noms = adresse.getNomsPrenomsOuRaisonsSociales();
			if (!noms.isEmpty()) {
				organisationInfo.setOrganisationName(DataHelper.truncate(noms.get(0), 60));
			}
			if (noms.size() > 1) {
				organisationInfo.setOrganisationNameAddOn1(DataHelper.truncate(noms.get(1), 60));
			}
			if (noms.size() > 2) {
				organisationInfo.setOrganisationNameAddOn2(DataHelper.truncate(noms.get(2), 60));
			}
			if (adresse.getPourAdresse() != null) {
				organisationInfo.setLastName(DataHelper.truncate(adresse.getPourAdresse(), 30));
			}
			organisationInfo.setFormalGreeting(adresse.getFormuleAppel());
			a.setOrganisation(organisationInfo);
		}
	}
	private static void fillRecipient(ch.vd.unireg.xml.party.address.v2.Address a, AdresseEnvoiDetaillee adresse) {

		if (adresse.getDestinataire() instanceof PersonnePhysique) {
			final ch.vd.unireg.xml.party.address.v2.PersonMailAddressInfo personInfo = new ch.vd.unireg.xml.party.address.v2.PersonMailAddressInfo();
			personInfo.setMrMrs(DataHelper.salutations2MrMrs(adresse.getSalutations()));
			final List<NomPrenom> nomsPrenoms = adresse.getNomsPrenoms();
			if (!nomsPrenoms.isEmpty()) {
				personInfo.setFirstName(DataHelper.truncate(nomsPrenoms.get(0).getPrenom(), 30));
				personInfo.setLastName(DataHelper.truncate(nomsPrenoms.get(0).getNom(), 30));
			}
			personInfo.setSalutation(adresse.getSalutations());
			personInfo.setFormalGreeting(adresse.getFormuleAppel());
			a.setPerson(personInfo);
		}
		else if (adresse.getDestinataire() instanceof MenageCommun) {
			final ch.vd.unireg.xml.party.address.v2.CoupleMailAddressInfo coupleInfo = new ch.vd.unireg.xml.party.address.v2.CoupleMailAddressInfo();
			coupleInfo.setSalutation(adresse.getSalutations());
			coupleInfo.setFormalGreeting(adresse.getFormuleAppel());
			for (NomPrenom nomPrenom : adresse.getNomsPrenoms()) {
				coupleInfo.getNames().add(new ch.vd.unireg.xml.party.address.v2.PersonName(DataHelper.truncate(nomPrenom.getPrenom(), 30), DataHelper.truncate(nomPrenom.getNom(), 30), null));
			}
			a.setCouple(coupleInfo);
		}
		else {
			final ch.vd.unireg.xml.party.address.v2.OrganisationMailAddressInfo organisationInfo = new ch.vd.unireg.xml.party.address.v2.OrganisationMailAddressInfo();
			final List<String> noms = adresse.getNomsPrenomsOuRaisonsSociales();
			if (!noms.isEmpty()) {
				organisationInfo.setOrganisationName(DataHelper.truncate(noms.get(0), 60));
			}
			if (noms.size() > 1) {
				organisationInfo.setOrganisationNameAddOn1(DataHelper.truncate(noms.get(1), 60));
			}
			if (noms.size() > 2) {
				organisationInfo.setOrganisationNameAddOn2(DataHelper.truncate(noms.get(2), 60));
			}
			if (adresse.getPourAdresse() != null) {
				organisationInfo.setLastName(DataHelper.truncate(adresse.getPourAdresse(), 30));
			}
			organisationInfo.setFormalGreeting(adresse.getFormuleAppel());
			a.setOrganisation(organisationInfo);
		}
	}

	protected static boolean isCompleteForAddressInformation(AdresseEnvoiDetaillee adresse){
		return adresse.getPays()!=null && adresse.getNpaEtLocalite()!=null && adresse.getTypeAffranchissement()!=null;

	}

	protected static void fillDestination(ch.vd.unireg.xml.party.address.v1.Address to, AdresseEnvoiDetaillee from) {
		final ch.vd.unireg.xml.party.address.v1.AddressInformation info = new ch.vd.unireg.xml.party.address.v1.AddressInformation();

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
					info.setSwissZipCodeId(from.getNumeroOrdrePostal()); // [SIFISC-4320] ne renseigner le swissZipCodeId que sur les adresses suisses.
				}
			}
			else {
				info.setForeignZipCode(npaEtLocalite.getNpa());
			}
			info.setTown(npaEtLocalite.getLocalite());
		}

		final Pays pays = from.getPays();
		if (pays != null) {
			info.setCountry(pays.getCodeIso2());
			info.setCountryName(pays.getNomCourt());
			info.setCountryId(pays.getNoOFS());
		}

		info.setStreetId(from.getNumeroTechniqueRue());
		info.setTariffZone(DataHelper.coreToXMLv1(from.getTypeAffranchissement()));
		info.setEgid(from.getEgid() == null ? null : from.getEgid().longValue());
		info.setEwid(from.getEwid() == null ? null : from.getEwid().longValue());
		info.setMunicipalityId(from.getNoOfsCommuneAdresse());

		to.setAddressInformation(info);
	}

	protected static void fillDestination(ch.vd.unireg.xml.party.address.v2.Address to, AdresseEnvoiDetaillee from) {
		final ch.vd.unireg.xml.party.address.v2.AddressInformation info = new ch.vd.unireg.xml.party.address.v2.AddressInformation();

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
					info.setSwissZipCodeId(from.getNumeroOrdrePostal()); // [SIFISC-4320] ne renseigner le swissZipCodeId que sur les adresses suisses.
				}
			}
			else {
				info.setForeignZipCode(npaEtLocalite.getNpa());
			}
			info.setTown(npaEtLocalite.getLocalite());
		}

		final Pays pays = from.getPays();
		if (pays != null) {
			info.setCountry(pays.getCodeIso2());
			info.setCountryName(pays.getNomCourt());
			info.setCountryId(pays.getNoOFS());
		}

		info.setStreetId(from.getNumeroTechniqueRue());
		info.setTariffZone(DataHelper.coreToXMLv2(from.getTypeAffranchissement()));
		info.setEgid(from.getEgid() == null ? null : from.getEgid().longValue());
		info.setEwid(from.getEwid() == null ? null : from.getEwid().longValue());
		info.setMunicipalityId(from.getNoOfsCommuneAdresse());

		to.setAddressInformation(info);
	}

	private static void fillFormattedAddress(ch.vd.unireg.xml.party.address.v1.Address a, AdresseEnvoiDetaillee adresse) {
		final ch.vd.unireg.xml.party.address.v1.FormattedAddress formattedAdresse = new ch.vd.unireg.xml.party.address.v1.FormattedAddress();
		formattedAdresse.setLine1(adresse.getLigne1());
		formattedAdresse.setLine2(adresse.getLigne2());
		formattedAdresse.setLine3(adresse.getLigne3());
		formattedAdresse.setLine4(adresse.getLigne4());
		formattedAdresse.setLine5(adresse.getLigne5());
		formattedAdresse.setLine6(adresse.getLigne6());
		a.setFormattedAddress(formattedAdresse);
	}

	private static void fillFormattedAddress(ch.vd.unireg.xml.party.address.v2.Address a, AdresseEnvoiDetaillee adresse) {
		final ch.vd.unireg.xml.party.address.v2.FormattedAddress formattedAdresse = new ch.vd.unireg.xml.party.address.v2.FormattedAddress();
		formattedAdresse.setLine1(adresse.getLigne1());
		formattedAdresse.setLine2(adresse.getLigne2());
		formattedAdresse.setLine3(adresse.getLigne3());
		formattedAdresse.setLine4(adresse.getLigne4());
		formattedAdresse.setLine5(adresse.getLigne5());
		formattedAdresse.setLine6(adresse.getLigne6());
		a.setFormattedAddress(formattedAdresse);
	}
}
