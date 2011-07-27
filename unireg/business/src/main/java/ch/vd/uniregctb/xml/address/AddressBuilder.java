package ch.vd.uniregctb.xml.address;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import ch.vd.unireg.xml.party.address.v1.Address;
import ch.vd.unireg.xml.party.address.v1.AddressInformation;
import ch.vd.unireg.xml.party.address.v1.AddressOtherParty;
import ch.vd.unireg.xml.party.address.v1.AddressType;
import ch.vd.unireg.xml.party.address.v1.CoupleMailAddressInfo;
import ch.vd.unireg.xml.party.address.v1.FormattedAddress;
import ch.vd.unireg.xml.party.address.v1.OrganisationMailAddressInfo;
import ch.vd.unireg.xml.party.address.v1.OtherPartyAddressType;
import ch.vd.unireg.xml.party.address.v1.PersonMailAddressInfo;
import ch.vd.unireg.xml.party.address.v1.PersonName;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.common.CasePostale;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.common.NpaEtLocalite;
import ch.vd.uniregctb.common.RueEtNumero;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.xml.DataHelper;

public class AddressBuilder {

//	private static final Logger LOGGER = Logger.getLogger(AddressBuilder.class);

	public static Address newAddress(AdresseEnvoiDetaillee adresse, AddressType type) {
		final Address a = new Address();
		fillAddress(adresse, a, type);
		return a;
	}

	public static AddressOtherParty newOtherPartyAddress(AdresseEnvoiDetaillee adresse, AddressType type) {
		final AddressOtherParty a = new AddressOtherParty();
		final Address base = new Address();
		fillAddress(adresse, base, type);
		a.setBase(base);
		a.setOtherPartyType(source2type(adresse.getSource()));
		return a;
	}

	private static void fillAddress(AdresseEnvoiDetaillee adresse, Address a, AddressType type) {
		a.setDateFrom(DataHelper.coreToXML(adresse.getDateDebut()));
		a.setDateTo(DataHelper.coreToXML(adresse.getDateFin()));
		a.setType(type);

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
				coupleInfo.getNames().add(new PersonName(nomPrenom.getPrenom(), nomPrenom.getNom(), null));
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

		final Pays pays = from.getPays();
		if (pays != null) {
			info.setCountry(pays.getCodeIso2());
			info.setCountryName(pays.getNomMinuscule());
			info.setCountryId(pays.getNoOFS());
		}

		info.setSwissZipCodeId(from.getNumeroOrdrePostal());
		info.setStreetId(from.getNumeroTechniqueRue());
		info.setTariffZone(DataHelper.coreToXML(from.getTypeAffranchissement()));

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
