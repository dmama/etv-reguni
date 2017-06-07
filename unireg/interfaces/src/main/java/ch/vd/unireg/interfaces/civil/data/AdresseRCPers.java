package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;

import ch.ech.ech0007.v4.CantonAbbreviation;
import ch.ech.ech0007.v4.SwissMunicipality;
import ch.ech.ech0010.v4.AddressInformation;
import ch.ech.ech0010.v4.MailAddress;
import ch.ech.ech0010.v4.SwissAddressInformation;
import ch.ech.ech0011.v5.Destination;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.evd0001.v5.Contact;
import ch.vd.evd0001.v5.DwellingAddress;
import ch.vd.evd0001.v5.Residence;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.ServiceCivilException;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public class AdresseRCPers implements Adresse, Serializable {

	private static final long serialVersionUID = 3351399275368181976L;

	private final RegDate dateDebut;
	private RegDate dateFin;
	private final CasePostale casePostale;
	private final String localite;
	private final String numero;
	private final String numeroAppartement;
	private final Integer numeroTechniqueRue;
	private final Integer numeroOrdrePostal;
	private final String numeroPostal;
	private final String numeroPostalComplementaire;
	private final int noOfsPays;
	private final String rue;
	private final String titre;
	private final TypeAdresseCivil typeAdresse;
	private final Integer noOfsCommuneAdresse;
	private final Integer egid;
	private final Integer ewid;
	private final Localisation localisationPrecedente;
	private final Localisation localisationSuivante;

	public static Adresse get(Contact contact, ServiceInfrastructureRaw infraService) {
		if (contact == null) {
			return null;
		}
		return new AdresseRCPers(contact, infraService);
	}

	private static AdresseRCPers get(AddressInformation addressInfo, ServiceInfrastructureRaw infraService) {
		if (addressInfo == null) {
			return null;
		}
		return new AdresseRCPers(addressInfo, null, null, infraService);
	}

	public static AdresseRCPers get(Residence residence, @Nullable Residence next, @Nullable RegDate dateDeces, ServiceInfrastructureRaw infraService) {
		if (residence == null) {
			return null;
		}
		return new AdresseRCPers(residence, next, dateDeces, infraService);
	}

	public AdresseRCPers(Contact contact, ServiceInfrastructureRaw infraService) {
		this(contact.getContact(), XmlUtils.xmlcal2regdate(contact.getContactValidFrom()), XmlUtils.xmlcal2regdate(contact.getContactValidTill()), infraService);
	}

	public AdresseRCPers(MailAddress address, RegDate dateDebut, RegDate dateFin, ServiceInfrastructureRaw infraService) {
		this(address.getAddressInformation(), dateDebut, dateFin, infraService);
	}

	private AdresseRCPers(AddressInformation addressInfo, @Nullable RegDate dateDebut, @Nullable RegDate dateFin, ServiceInfrastructureRaw infraService) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		DateRangeHelper.assertValidRange(this.dateDebut, this.dateFin, ServiceCivilException.class);
		this.casePostale = initCasePostale(addressInfo.getPostOfficeBoxText(), addressInfo.getPostOfficeBoxNumber());
		this.localite = addressInfo.getTown();
		this.numero = addressInfo.getHouseNumber();
		this.numeroAppartement = addressInfo.getDwellingNumber();
		this.numeroTechniqueRue = null; // RcPers ne retourne plus ce numéro technique
		this.numeroOrdrePostal = addressInfo.getSwissZipCodeId();
		this.numeroPostal = initNPA(addressInfo);
		this.numeroPostalComplementaire = addressInfo.getSwissZipCodeAddOn();
		this.noOfsPays = initNoOfsPays(dateDebut, addressInfo.getCountry(), infraService);
		this.rue = addressInfo.getStreet();

		// [SIFISC-13878] on prend en compte la deuxième ligne de complément si la première est vide
		this.titre = StringUtils.isBlank(addressInfo.getAddressLine1()) ? addressInfo.getAddressLine2() : addressInfo.getAddressLine1();

		this.typeAdresse = TypeAdresseCivil.COURRIER;
		this.noOfsCommuneAdresse = null;
		this.egid = null; // les adresses courrier ne possèdent pas d'egid/ewid, par définition
		this.ewid = null;
		this.localisationPrecedente = null; // les adresses courrier ne possèdent pas de localisation précédente/suivante, par définition
		this.localisationSuivante = null;
	}

	public AdresseRCPers(Residence residence, @Nullable Residence next, @Nullable RegDate dateDeces, ServiceInfrastructureRaw infraService) {
		final DwellingAddress dwellingAddress = residence.getDwellingAddress();
		final SwissAddressInformation addressInfo = dwellingAddress.getAddress();
		final DwellingAddress.Dwelling dwelling = dwellingAddress.getDwelling();

		this.dateDebut = initDateDebut(residence); // voir SIREF-1617
		this.dateFin = initDateFin(residence, next, dateDeces); // voir SIREF-1794, SIFISC-13595
		DateRangeHelper.assertValidRange(dateDebut, dateFin, ServiceCivilException.class);
		this.casePostale = null;
		this.localite = addressInfo.getTown();
		this.numero = addressInfo.getHouseNumber();
		this.numeroAppartement = addressInfo.getDwellingNumber();
		this.numeroTechniqueRue = null; // RcPers ne retourne plus ce numéro technique
		this.numeroOrdrePostal = addressInfo.getSwissZipCodeId();
		this.numeroPostal = String.valueOf(addressInfo.getSwissZipCode());
		this.numeroPostalComplementaire = addressInfo.getSwissZipCodeAddOn();
		this.noOfsPays = ServiceInfrastructureRaw.noOfsSuisse; // par définition, RcPers ne retourne que des adresses de domicile dans le canton de Vaud, donc en Suisse.
		this.rue = addressInfo.getStreet();

		// [SIFISC-13878] on prend en compte la deuxième ligne de complément si la première est vide
		this.titre = StringUtils.isBlank(addressInfo.getAddressLine1()) ? addressInfo.getAddressLine2() : addressInfo.getAddressLine1();

		this.typeAdresse = initTypeAdresseResidence(residence);
		this.noOfsCommuneAdresse = residence.getResidenceMunicipality().getMunicipalityId();
		this.egid = dwelling == null || dwelling.getEGID() == null ? null : dwelling.getEGID().intValue();
		this.ewid = dwelling == null || dwelling.getEWID() == null ? null : dwelling.getEWID().intValue();

		if (residence.getDwellingAddress().getMovingDate() != null) {
			this.localisationPrecedente = null; // [SIFISC-4833] en cas de déménagement à l'intérieur de la commune, la localisation précédente doit être nulle
		}
		else {
			this.localisationPrecedente = initLocalisation(getVeille(dateDebut), residence.getComesFrom(), infraService);
		}

		if (next != null && next.getDwellingAddress().getMovingDate() != null) {
			this.localisationSuivante = null; // [SIFISC-4833] en cas de déménagement à l'intérieur de la commune, la localisation suivante doit être nulle
		}
		else if (dateFin == null) {
			// [SIFISC-13595] si la date de fin donnée par RCPers a été ignorée, il faut également ignorer la localisation suivante
			this.localisationSuivante = null;
		}
		else {
			this.localisationSuivante = initLocalisation(getLendemain(dateFin), residence.getGoesTo(), infraService);
		}
	}

	private static RegDate getVeille(@Nullable RegDate date) {
		return date == null ? date : date.getOneDayBefore();
	}

	private static RegDate getLendemain(@Nullable RegDate date) {
		return date == null ? date : date.getOneDayAfter();
	}

	private AdresseRCPers(String localite, int noOfsPays) {
		this.dateDebut = null;
		this.dateFin = null;
		this.casePostale = null;
		this.localite = localite;
		this.numero = null;
		this.numeroAppartement = null;
		this.numeroTechniqueRue = null;
		this.numeroOrdrePostal = null;
		this.numeroPostal = null;
		this.numeroPostalComplementaire = null;
		this.noOfsPays = noOfsPays;
		this.rue = null;
		this.titre = null;
		this.typeAdresse = TypeAdresseCivil.COURRIER;
		this.noOfsCommuneAdresse = null;
		this.egid = null;
		this.ewid = null;
		this.localisationPrecedente = null;
		this.localisationSuivante = null;
	}

	/**
	 * [SIREF-1617] Détermine la date de début effective d'une adresse de résidence.
	 *
	 * @param residence une adresse de résidence
	 * @return la date de début effective de l'adresse de résidence
	 */
	private static RegDate initDateDebut(Residence residence) {
		final DwellingAddress dwellingAddress = residence.getDwellingAddress();
		return XmlUtils.xmlcal2regdate(dwellingAddress.getMovingDate() == null ? residence.getArrivalDate() : dwellingAddress.getMovingDate());
	}

	/**
	 * [SIREF-1794] Détermine la date de fin effective d'une adresse de résidence.
	 * [SIFISC-13595] Si la date de fin de l'adresse de résidence correspond à la date de décès de l'individu, il faut l'ignorer
	 *
	 * @param residence une adresse de résidence
	 * @param next      l'adresse de résidence qui suit dans l'ordre chronologique
	 * @param dateDeces date de décès de l'individu concerné par cette adresse de résidence
	 * @return la date de fin effective de l'adresse de résidence
	 */
	private static RegDate initDateFin(Residence residence, @Nullable Residence next, @Nullable RegDate dateDeces) {
		final RegDate df;
		if (next != null && movingInSameMunicipality(residence, next)) {
			final RegDate nextMovingDate = XmlUtils.xmlcal2regdate(next.getDwellingAddress().getMovingDate());
			df = nextMovingDate.getOneDayBefore();
		}
		else {
			final RegDate candidate = XmlUtils.xmlcal2regdate(residence.getDepartureDate());
			df = (candidate == dateDeces ? null : candidate);
		}
		return df;
	}

	/**
	 * Détermine si les deux adresses de résidence correspondent au même <i>séjour</i> dans une commune.
	 * <p/>
	 * Les dates <i>arrivalDate</i> et <i>DepartureDate</i> exposées par RcPers correspondent aux dates d'arrivée et de départ <b>dans une commune</b> : en cas de déménagement à l'intérieur d'une
	 * commune, un individu possède deux adresses de résidences avec dates d'arrivée et de départ identiques (puisqu'il n'a pas changé de commune) et la seconde adresse possède une <i>movingInDate</i>.
	 *
	 * @param current une adresse de résidence
	 * @param next    une autre adresse de résidence
	 * @return <b>vrai</b> si les adresses correspondent au même <i>séjour</i> dans une commune; <b>faux</b> autrement.
	 */
	private static boolean movingInSameMunicipality(Residence current, Residence next) {
		return current.getResidenceMunicipality().getMunicipalityId().equals(next.getResidenceMunicipality().getMunicipalityId()) &&
				XmlUtils.xmlcal2regdate(current.getArrivalDate()) == XmlUtils.xmlcal2regdate(next.getArrivalDate());
	}

	protected static Localisation initLocalisation(RegDate date, Destination location, ServiceInfrastructureRaw infraService) {

		if (location == null) {
			return null;
		}

		Adresse adresseCourrier = AdresseRCPers.get(location.getMailAddress(), infraService);

		if (location.getUnknown() != null) {
			return new Localisation(LocalisationType.HORS_SUISSE, ServiceInfrastructureRaw.noPaysInconnu, adresseCourrier);
		}

		final Destination.ForeignCountry foreignCountry = location.getForeignCountry();
		if (foreignCountry != null) {
			final int countryId = foreignCountry.getCountry().getCountryId();

			// [SIFISC-977] attention, ce pays peut n'être qu'un territoire et pas un état souverain !
			// si c'est le cas, passer à l'état souverain (c'est ce qui nous intéresse pour les fors)
			final Pays pays = infraService.getPays(countryId, date);
			final int ofsEtatSouverain = pays != null ? pays.getNoOfsEtatSouverain() : ServiceInfrastructureRaw.noPaysInconnu;

			if (adresseCourrier == null && StringUtils.isNotBlank(foreignCountry.getTown())) {
				// on essaie très fort de mettre quelque chose dans l'adresse
				adresseCourrier = new AdresseRCPers(foreignCountry.getTown(), ofsEtatSouverain);
			}

			return new Localisation(LocalisationType.HORS_SUISSE, ofsEtatSouverain, adresseCourrier);
		}

		final SwissMunicipality swissTown = location.getSwissTown();
		if (swissTown != null) {
			if (swissTown.getCantonAbbreviation() == CantonAbbreviation.VD) {
				return new Localisation(LocalisationType.CANTON_VD, swissTown.getMunicipalityId(), adresseCourrier);
			}
			else {
				return new Localisation(LocalisationType.HORS_CANTON, swissTown.getMunicipalityId(), adresseCourrier);
			}
		}

		return null;
	}

	private static String initNPA(AddressInformation addressInfo) {
		final Long swissZipCode = addressInfo.getSwissZipCode();
		if (swissZipCode != null) {
			return String.valueOf(swissZipCode);
		}
		return addressInfo.getForeignZipCode();
	}

	private static int initNoOfsPays(RegDate date, String countryCode, ServiceInfrastructureRaw infraService) {

		if ("CH".equals(countryCode)) {
			// short path : 90% des adresses sont en Suisse
			return ServiceInfrastructureRaw.noOfsSuisse;
		}

		final Pays pays = infraService.getPays(countryCode, date);
		if (pays == null) {
			return 0;
		}
		return pays.getNoOFS();
	}

	private static CasePostale initCasePostale(String text, Long number) {
		if (number == null && StringUtils.isBlank(text)) {
			return null;
		}
		if (number == null) {
			return CasePostale.parse(text);
		}
		else {
			return new CasePostale(text, number);
		}
	}

	private static TypeAdresseCivil initTypeAdresseResidence(Residence residence) {
		// voir SIREF-1622
		if (residence.getMainResidence() != null) {
			return TypeAdresseCivil.PRINCIPALE;
		}
		else if (residence.getSecondaryResidence() != null) {
			return TypeAdresseCivil.SECONDAIRE;
		}
		else if (residence.getOtherResidence() != null) {
			// les 'other' residences sont des adresses secondaires dont l'adresse principale n'est pas connu ou hors-Suisse, c'est tout.
			return TypeAdresseCivil.SECONDAIRE;
		}
		throw new IllegalArgumentException("L'adresse de résidence ne possède pas de type défini !");
	}

	public static TypeAdresseCivil getTypeAdresseResidence(Residence residence) {
		return initTypeAdresseResidence(residence);
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	@Override
	public CasePostale getCasePostale() {
		return casePostale;
	}

	@Override
	public String getLocalite() {
		return localite;
	}

	@Override
	public String getNumero() {
		return numero;
	}

	@Override
	public String getNumeroAppartement() {
		return numeroAppartement;
	}

	@Override
	public Integer getNumeroRue() {
		return numeroTechniqueRue;
	}

	@Override
	public Integer getNumeroOrdrePostal() {
		return numeroOrdrePostal;
	}

	@Override
	public String getNumeroPostal() {
		return numeroPostal;
	}

	@Override
	public String getNumeroPostalComplementaire() {
		return numeroPostalComplementaire;
	}

	@Override
	public Integer getNoOfsPays() {
		return noOfsPays;
	}

	@Override
	public String getRue() {
		return rue;
	}

	@Override
	public String getTitre() {
		return titre;
	}

	@Override
	public TypeAdresseCivil getTypeAdresse() {
		return typeAdresse;
	}

	@Nullable
	@Override
	public Integer getNoOfsCommuneAdresse() {
		return noOfsCommuneAdresse;
	}

	@Override
	public Integer getEgid() {
		return egid;
	}

	@Override
	public Integer getEwid() {
		return ewid;
	}

	@Override
	public Localisation getLocalisationPrecedente() {
		return localisationPrecedente;
	}

	@Override
	public Localisation getLocalisationSuivante() {
		return localisationSuivante;
	}
}
