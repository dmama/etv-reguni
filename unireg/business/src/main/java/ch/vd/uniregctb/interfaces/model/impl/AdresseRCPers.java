package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.ech.ech0007.v4.CantonAbbreviation;
import ch.ech.ech0007.v4.SwissMunicipality;
import ch.ech.ech0010.v4.AddressInformation;
import ch.ech.ech0010.v4.MailAddress;
import ch.ech.ech0010.v4.SwissAddressInformation;
import ch.ech.ech0011.v5.Destination;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.evd0001.v3.DwellingAddress;
import ch.vd.evd0001.v3.HistoryContact;
import ch.vd.evd0001.v3.Residence;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.CasePostale;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Localisation;
import ch.vd.uniregctb.interfaces.model.LocalisationType;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public class AdresseRCPers implements Adresse, Serializable {

	private static final long serialVersionUID = 6370371735246030462L;
	
	private final RegDate dateDebut;
	private RegDate dateFin;
	private final CasePostale casePostale;
	private final String localite;
	private final String numero;
	private final String numeroAppartement;
	private final Integer numeroTechniqueRue;
	private final int numeroOrdrePostal;
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

	public static Adresse get(HistoryContact contact, ServiceInfrastructureService infraService) {
		if (contact == null) {
			return null;
		}
		return new AdresseRCPers(contact, infraService);
	}

	public static AdresseRCPers get(Residence residence, @Nullable Residence next, ServiceInfrastructureService infraService) {
		if (residence == null) {
			return null;
		}
		return new AdresseRCPers(residence, next, infraService);
	}

	public AdresseRCPers(HistoryContact contact, ServiceInfrastructureService infraService) {
		final MailAddress address = contact.getContact();
		final AddressInformation addressInfo = address.getAddressInformation();

		this.dateDebut = XmlUtils.xmlcal2regdate(contact.getContactValidFrom());
		this.dateFin = XmlUtils.xmlcal2regdate(contact.getContactValidTill());
		DateRangeHelper.assertValidRange(dateDebut, dateFin);
		this.casePostale = initCasePostale(addressInfo.getPostOfficeBoxText(), addressInfo.getPostOfficeBoxNumber());
		this.localite = addressInfo.getTown();
		this.numero = addressInfo.getHouseNumber();
		this.numeroAppartement = addressInfo.getDwellingNumber();
		this.numeroTechniqueRue = null; // RcPers ne retourne plus ce numéro technique
		this.numeroOrdrePostal = addressInfo.getSwissZipCodeId() == null ? 0 : addressInfo.getSwissZipCodeId();
		this.numeroPostal = initNPA(addressInfo);
		this.numeroPostalComplementaire = addressInfo.getSwissZipCodeAddOn();
		this.noOfsPays = initNoOfsPays(addressInfo.getCountry(), infraService);
		this.rue = addressInfo.getStreet();
		this.titre = addressInfo.getAddressLine1(); // TODO (msi) que faire d'addressLine2 ?
		this.typeAdresse = TypeAdresseCivil.COURRIER;
		this.noOfsCommuneAdresse = null;
		this.egid = null; // les adresses courrier ne possèdent pas d'egid/ewid, par définition
		this.ewid = null;
		this.localisationPrecedente = null; // les adresses courrier ne possèdent pas de localisation précédente/suivante, par définition
		this.localisationSuivante = null;
	}

	public AdresseRCPers(Residence residence, @Nullable Residence next, ServiceInfrastructureService infraService) {
		final DwellingAddress dwellingAddress = residence.getDwellingAddress();
		final SwissAddressInformation addressInfo = dwellingAddress.getAddress();

		this.dateDebut = initDateDebut(residence); // voir SIREF-1617
		this.dateFin = initDateFin(residence, next); // voir SIREF-1794
		DateRangeHelper.assertValidRange(dateDebut, dateFin);
		this.casePostale = null;
		this.localite = addressInfo.getTown();
		this.numero = addressInfo.getHouseNumber();
		this.numeroAppartement = addressInfo.getDwellingNumber();
		this.numeroTechniqueRue = null; // RcPers ne retourne plus ce numéro technique
		this.numeroOrdrePostal = addressInfo.getSwissZipCodeId() == null ? 0 : addressInfo.getSwissZipCodeId();
		this.numeroPostal = String.valueOf(addressInfo.getSwissZipCode());
		this.numeroPostalComplementaire = addressInfo.getSwissZipCodeAddOn();
		this.noOfsPays = initNoOfsPays(addressInfo.getCountry(), infraService);
		this.rue = addressInfo.getStreet();
		this.titre = addressInfo.getAddressLine1(); // TODO (msi) que faire d'addressLine2 ?
		this.typeAdresse = initTypeAdresseResidence(residence);
		this.noOfsCommuneAdresse = residence.getResidenceMunicipality().getMunicipalityId();
		this.egid = dwellingAddress.getEGID() == null ? null : dwellingAddress.getEGID().intValue();
		this.ewid = dwellingAddress.getEWID() == null ? null : dwellingAddress.getEWID().intValue();
		this.localisationPrecedente = initLocalisation(residence.getComesFrom());
		this.localisationSuivante = initLocalisation(residence.getGoesTo());
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
	 *
	 * @param residence une adresse de résidence
	 * @param next      l'adresse de résidence qui suit dans l'ordre chronologique
	 * @return la date de fin effective de l'adresse de résidence
	 */
	private static RegDate initDateFin(Residence residence, @Nullable Residence next) {
		final RegDate df;
		if (next != null && movingInSameMunicipality(residence, next)) {
			final RegDate nextMovingDate = XmlUtils.xmlcal2regdate(next.getDwellingAddress().getMovingDate());
			df = nextMovingDate.getOneDayBefore();
		}
		else {
			df = XmlUtils.xmlcal2regdate(residence.getDepartureDate());
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

	private static Localisation initLocalisation(Destination location) {

		if (location == null) {
			return null;
		}

		if(location.getUnknown()!=null){
			return new Localisation(LocalisationType.HORS_SUISSE,ServiceInfrastructureService.noPaysInconnu);

		}

		final Destination.ForeignCountry foreignCountry = location.getForeignCountry();
		if (foreignCountry != null) {
			return new Localisation(LocalisationType.HORS_SUISSE, foreignCountry.getCountry().getCountryId());
		}

		final SwissMunicipality swissTown = location.getSwissTown();
		if (swissTown != null) {
			if (swissTown.getCantonAbbreviation() == CantonAbbreviation.VD) {
				return new Localisation(LocalisationType.CANTON_VD, swissTown.getMunicipalityId());
			}
			else {
				return new Localisation(LocalisationType.HORS_CANTON, swissTown.getMunicipalityId());
			}
		}

		return null;
	}

	private static String initNPA(AddressInformation addressInfo) {
		final Long swissZipCode = addressInfo.getSwissZipCode();
		if (swissZipCode != null) {
			return String.valueOf(swissZipCode);
		}
		String foreignZipCode = addressInfo.getForeignZipCode();
		if ("inconnu".equals(foreignZipCode)) {
			foreignZipCode = null; // FIXME (rcpers) en attente de la résolution de SIREF-1798
		}
		return foreignZipCode;
	}

	private static int initNoOfsPays(String countryCode, ServiceInfrastructureService infraService) {
		final Pays pays = infraService.getPays(countryCode);
		if (pays == null) {
			return 0;
		}
		return pays.getNoOFS();
	}

	private static CasePostale initCasePostale(String text, Long number) {
		if (number == null && StringUtils.isBlank(text)) {
			return null;
		}
		return new CasePostale(text, number);
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
	public int getNumeroOrdrePostal() {
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

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}
}
