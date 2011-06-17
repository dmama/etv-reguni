package ch.vd.uniregctb.webservices.tiers3.data;

import java.util.List;

import org.apache.log4j.Logger;

import ch.vd.unireg.webservices.tiers3.AddressInformation;
import ch.vd.unireg.webservices.tiers3.Adresse;
import ch.vd.unireg.webservices.tiers3.AdresseAutreTiers;
import ch.vd.unireg.webservices.tiers3.BusinessExceptionCode;
import ch.vd.unireg.webservices.tiers3.CoupleMailAddressInfo;
import ch.vd.unireg.webservices.tiers3.FormattedAddress;
import ch.vd.unireg.webservices.tiers3.MailAddress;
import ch.vd.unireg.webservices.tiers3.MailAddressOtherTiers;
import ch.vd.unireg.webservices.tiers3.OrganisationMailAddressInfo;
import ch.vd.unireg.webservices.tiers3.PersonMailAddressInfo;
import ch.vd.unireg.webservices.tiers3.PersonName;
import ch.vd.unireg.webservices.tiers3.TypeAdressePoursuiteAutreTiers;
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

public class AdresseBuilder {

	private static final Logger LOGGER = Logger.getLogger(AdresseBuilder.class);

	public static Adresse newAdresse(AdresseGenerique adresse, ServiceInfrastructureService serviceInfra) throws WebServiceException {
		final Adresse a = new Adresse();
		fillAdresse(adresse, serviceInfra, a);
		return a;
	}

	public static void fillAdresse(AdresseGenerique adresse, ServiceInfrastructureService serviceInfra, Adresse a) throws WebServiceException {
		a.setDateDebut(DataHelper.coreToWeb(adresse.getDateDebut()));
		a.setDateFin(DataHelper.coreToWeb(adresse.getDateFin()));
		a.setTitre(adresse.getComplement());
		a.setNumeroAppartement(adresse.getNumeroAppartement());
		a.setRue(adresse.getRue());
		a.setNumeroRue(adresse.getNumero());
		a.setCasePostale(adresse.getCasePostale() == null ? null : adresse.getCasePostale().toString());
		a.setLocalite(adresse.getLocalite());
		a.setNumeroPostal(adresse.getNumeroPostal());

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
				a.setPays(p.getNomMinuscule());
			}
		}

		a.setNoOrdrePostal(adresse.getNumeroOrdrePostal());
		a.setNoRue(adresse.getNumeroRue()); // TODO (msi) retourner null si le numéro est égal à 0
		a.setNoPays(noOfsPays);
	}

	public static AdresseAutreTiers newAdresseAutreTiers(AdresseGenerique adresse, ServiceInfrastructureService serviceInfra) throws WebServiceException {
		final AdresseAutreTiers a = new AdresseAutreTiers();
		fillAdresse(adresse, serviceInfra, a);
		a.setType(source2type(adresse.getSource().getType()));
		return a;
	}

	public static TypeAdressePoursuiteAutreTiers source2type(AdresseGenerique.SourceType source) {

		if (source == null) {
			return null;
		}

		switch (source) {
		case FISCALE:
			return TypeAdressePoursuiteAutreTiers.SPECIFIQUE;
		case REPRESENTATION:
			return TypeAdressePoursuiteAutreTiers.MANDATAIRE;
		case CURATELLE:
			return TypeAdressePoursuiteAutreTiers.CURATELLE;
		case CONSEIL_LEGAL:
			return TypeAdressePoursuiteAutreTiers.CONSEIL_LEGAL;
		case TUTELLE:
			return TypeAdressePoursuiteAutreTiers.TUTELLE;
		default:
			throw new IllegalArgumentException("Le type de source = [" + source + "] n'est pas représentable comme type d'adresse autre tiers");
		}

	}

	public static MailAddress newMailAddress(AdresseEnvoiDetaillee adresse) {
		final MailAddress a = new MailAddress();
		fillRecipient(a, adresse);
		fillDestination(a, adresse);
		fillAdresseEnvoi(a, adresse);
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
			if (noms != null) {
				if (noms.size() > 0) {
					organisationInfo.setOrganisationName(noms.get(0));
				}
				if (noms.size() > 1) {
					organisationInfo.setOrganisationNameAddOn1(noms.get(1));
				}
				if (noms.size() > 2) {
					organisationInfo.setOrganisationNameAddOn2(noms.get(2));
				}
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

		info.setComplement(adresse.getComplement());
		info.setPourAdresse(adresse.getPourAdresse());
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
		info.setCountry(adresse.getPays());
		info.setTypeAffranchissement(EnumHelper.coreToWeb(adresse.getTypeAffranchissement()));

		a.setAddressInformation(info);
	}

	public static void fillAdresseEnvoi(MailAddress a, AdresseEnvoiDetaillee adresse) {
		final FormattedAddress formattedAdresse = new FormattedAddress();
		formattedAdresse.setLine1(adresse.getLigne1());
		formattedAdresse.setLine2(adresse.getLigne2());
		formattedAdresse.setLine3(adresse.getLigne3());
		formattedAdresse.setLine4(adresse.getLigne4());
		formattedAdresse.setLine5(adresse.getLigne5());
		formattedAdresse.setLine6(adresse.getLigne6());
		a.setFormattedAddress(formattedAdresse);

	}

	public static MailAddressOtherTiers newMailAddressOtherTiers(AdresseEnvoiDetaillee adresse) {
		final MailAddressOtherTiers a = new MailAddressOtherTiers();
		fillAdresseEnvoi(a, adresse);
		a.setType(source2type(adresse.getSource()));
		return a;
	}
}
