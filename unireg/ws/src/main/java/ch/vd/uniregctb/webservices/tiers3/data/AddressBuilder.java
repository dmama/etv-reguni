package ch.vd.uniregctb.webservices.tiers3.data;

import java.util.List;

import org.apache.log4j.Logger;

import ch.vd.unireg.webservices.tiers3.Address;
import ch.vd.unireg.webservices.tiers3.AddressInformation;
import ch.vd.unireg.webservices.tiers3.BusinessExceptionCode;
import ch.vd.unireg.webservices.tiers3.CoupleMailAddressInfo;
import ch.vd.unireg.webservices.tiers3.FormattedAddress;
import ch.vd.unireg.webservices.tiers3.MailAddress;
import ch.vd.unireg.webservices.tiers3.MailAddressOtherParty;
import ch.vd.unireg.webservices.tiers3.OrganisationMailAddressInfo;
import ch.vd.unireg.webservices.tiers3.OtherPartyAddress;
import ch.vd.unireg.webservices.tiers3.OtherPartyAddressType;
import ch.vd.unireg.webservices.tiers3.PersonMailAddressInfo;
import ch.vd.unireg.webservices.tiers3.PersonName;
import ch.vd.unireg.webservices.tiers3.WebServiceException;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.common.CasePostale;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.common.RueEtNumero;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.ExceptionHelper;

public class AddressBuilder {

	private static final Logger LOGGER = Logger.getLogger(AddressBuilder.class);

	public static Address newAddress(AdresseGenerique adresse, ServiceInfrastructureService serviceInfra) throws WebServiceException {
		final Address a = new Address();
		fillAddress(adresse, serviceInfra, a);
		return a;
	}

	public static void fillAddress(AdresseGenerique adresse, ServiceInfrastructureService serviceInfra, Address a) throws WebServiceException {
		a.setDateFrom(DataHelper.coreToWeb(adresse.getDateDebut()));
		a.setDateTo(DataHelper.coreToWeb(adresse.getDateFin()));
		a.setTitle(adresse.getComplement());
		a.setDwellingNumber(adresse.getNumeroAppartement());
		a.setStreet(adresse.getRue());
		a.setHouseNumber(adresse.getNumero());
		a.setPostOfficeBox(adresse.getCasePostale() == null ? null : adresse.getCasePostale().toString());
		a.setTown(adresse.getLocalite());
		a.setZipCode(adresse.getNumeroPostal());

		final Integer noOfsPays = adresse.getNoOfsPays();
		if (noOfsPays != null) {
			ch.vd.uniregctb.interfaces.model.Pays p;
			try {
				p = serviceInfra.getPays(noOfsPays);
			}
			catch (ServiceInfrastructureException e) {
				LOGGER.error(e, e);
				throw ExceptionHelper.newBusinessException(e.getMessage(), BusinessExceptionCode.INFRASTRUCTURE);
			}
			if (p != null && !p.isSuisse()) {
				a.setCountry(p.getNomMinuscule());
			}
		}

		a.setSwissZipCodeId(adresse.getNumeroOrdrePostal());
		a.setStreetId(adresse.getNumeroRue()); // TODO (msi) retourner null si le numéro est égal à 0
		a.setCountryId(noOfsPays);
	}

	public static OtherPartyAddress newOtherPartyAddress(AdresseGenerique adresse, ServiceInfrastructureService serviceInfra) throws WebServiceException {
		final OtherPartyAddress a = new OtherPartyAddress();
		fillAddress(adresse, serviceInfra, a);
		a.setType(source2type(adresse.getSource().getType()));
		return a;
	}

	public static OtherPartyAddressType source2type(AdresseGenerique.SourceType source) {

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

	public static MailAddress newMailAddress(AdresseEnvoiDetaillee adresse) {
		final MailAddress a = new MailAddress();
		fillRecipient(a, adresse);
		fillDestination(a, adresse);
		fillFormattedAddress(a, adresse);
		return a;
	}

	private static void fillRecipient(MailAddress a, AdresseEnvoiDetaillee adresse) {

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

	private static void fillDestination(MailAddress a, AdresseEnvoiDetaillee adresse) {
		final AddressInformation info = new AddressInformation();

		info.setComplementaryInformation(adresse.getComplement());
		info.setCareOf(adresse.getPourAdresse());
		final RueEtNumero rueEtNumero = adresse.getRueEtNumero();
		if (rueEtNumero != null) {
			info.setStreet(rueEtNumero.getRue());
			info.setHouseNumber(rueEtNumero.getNumero());
		}
		final CasePostale casePostale = adresse.getCasePostale();
		if (casePostale != null) {
			info.setPostOfficeBoxText(casePostale.getType().format());
			info.setPostOfficeBoxNumber(casePostale.getNumero() == null ? null : casePostale.getNumero().longValue());
		}
		info.setTown(adresse.getNpaEtLocalite());
		info.setCountry(adresse.getPays()); // FIXME (msi) il faut renseigner le code ISO sur 2 positions du pays, et pas son nom complet !
		info.setTariffZone(EnumHelper.coreToWeb(adresse.getTypeAffranchissement()));

		a.setAddressInformation(info);
	}

	public static void fillFormattedAddress(MailAddress a, AdresseEnvoiDetaillee adresse) {
		final FormattedAddress formattedAdresse = new FormattedAddress();
		formattedAdresse.setLine1(adresse.getLigne1());
		formattedAdresse.setLine2(adresse.getLigne2());
		formattedAdresse.setLine3(adresse.getLigne3());
		formattedAdresse.setLine4(adresse.getLigne4());
		formattedAdresse.setLine5(adresse.getLigne5());
		formattedAdresse.setLine6(adresse.getLigne6());
		a.setFormattedAddress(formattedAdresse);

	}

	public static MailAddressOtherParty newMailAddressOtherTiers(AdresseEnvoiDetaillee adresse) {
		final MailAddressOtherParty a = new MailAddressOtherParty();
		fillFormattedAddress(a, adresse);
		a.setType(source2type(adresse.getSource()));
		return a;
	}
}
