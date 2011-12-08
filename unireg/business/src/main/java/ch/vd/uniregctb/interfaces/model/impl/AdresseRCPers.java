package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.ech.ech0010.v4.AddressInformation;
import ch.ech.ech0010.v4.MailAddress;
import ch.ech.ech0010.v4.SwissAddressInformation;
import org.apache.commons.lang.StringUtils;

import ch.vd.evd0001.v3.DwellingAddress;
import ch.vd.evd0001.v3.HistoryContact;
import ch.vd.evd0001.v3.Residence;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.CasePostale;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public class AdresseRCPers implements Adresse, Serializable {

	private static final long serialVersionUID = 4429511432785369598L;
	
	private final RegDate dateDebut;
	private final RegDate dateFin;
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
	private final Commune communeAdresse;
	private final Integer egid;
	private final Integer ewid;

	public static Adresse get(HistoryContact contact, ServiceInfrastructureService infraService) {
		if (contact == null) {
			return null;
		}
		return new AdresseRCPers(contact, infraService);
	}

	public static Adresse get(Residence residence, ServiceInfrastructureService infraService) {
		if (residence == null) {
			return null;
		}
		return new AdresseRCPers(residence, infraService);
	}

	public AdresseRCPers(HistoryContact contact, ServiceInfrastructureService infraService) {
		final MailAddress address = contact.getContact();
		final AddressInformation addressInfo = address.getAddressInformation();

		this.dateDebut = XmlUtils.xmlcal2regdate(contact.getDate());
		this.dateFin = null; // TODO (rcpers)
		this.casePostale = initCasePostale(addressInfo.getPostOfficeBoxText(), addressInfo.getPostOfficeBoxNumber());
		this.localite = addressInfo.getTown();
		this.numero = addressInfo.getHouseNumber();
		this.numeroAppartement = addressInfo.getDwellingNumber();
		this.numeroTechniqueRue = null; // TODO (msi) check this
		this.numeroOrdrePostal = addressInfo.getSwissZipCodeId() == null ? 0 : addressInfo.getSwissZipCodeId();
		this.numeroPostal = addressInfo.getSwissZipCode() == null ? null : String.valueOf(addressInfo.getSwissZipCode());
		this.numeroPostalComplementaire = addressInfo.getSwissZipCodeAddOn();
		this.noOfsPays = initNoOfsPays(addressInfo.getCountry(), infraService);
		this.rue = addressInfo.getStreet();
		this.titre = addressInfo.getAddressLine1(); // TODO (msi) que faire d'addressLine2 ?
		this.typeAdresse = TypeAdresseCivil.COURRIER;
		this.communeAdresse = null;
		this.egid = null; // les adresses courrier ne possèdent pas d'egid/ewid, par définition
		this.ewid = null;
	}

	public AdresseRCPers(Residence residence, ServiceInfrastructureService infraService) {
		final DwellingAddress dwellingAddress = residence.getDwellingAddress();
		final SwissAddressInformation addressInfo = dwellingAddress.getAddress();

		this.dateDebut = XmlUtils.xmlcal2regdate(residence.getArrivalDate());
		this.dateFin = XmlUtils.xmlcal2regdate(residence.getDepartureDate());
		this.casePostale = null;
		this.localite = addressInfo.getTown();
		this.numero = addressInfo.getHouseNumber();
		this.numeroAppartement = addressInfo.getDwellingNumber();
		this.numeroTechniqueRue = null; // TODO (msi) check this
		this.numeroOrdrePostal = addressInfo.getSwissZipCodeId() == null ? 0 : addressInfo.getSwissZipCodeId();
		this.numeroPostal = String.valueOf(addressInfo.getSwissZipCode());
		this.numeroPostalComplementaire = addressInfo.getSwissZipCodeAddOn();
		this.noOfsPays = initNoOfsPays(addressInfo.getCountry(), infraService);
		this.rue = addressInfo.getStreet();
		this.titre = addressInfo.getAddressLine1(); // TODO (msi) que faire d'addressLine2 ?
		this.typeAdresse = TypeAdresseCivil.PRINCIPALE;
		this.communeAdresse = initCommune(residence.getResidenceMunicipality().getMunicipalityId(), this.dateDebut, infraService);
		this.egid = dwellingAddress.getEGID() == null ? null : dwellingAddress.getEGID().intValue();
		this.ewid = dwellingAddress.getEWID() == null ? null : dwellingAddress.getEWID().intValue();
	}

	private static Commune initCommune(Integer municipalityId, RegDate date, ServiceInfrastructureService infraService) {
		if (municipalityId == null){
			return null;
		}
		return infraService.getCommuneByNumeroOfsEtendu(municipalityId, date);
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

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
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

	@Override
	public Commune getCommuneAdresse() {
		return communeAdresse;
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
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}
}
