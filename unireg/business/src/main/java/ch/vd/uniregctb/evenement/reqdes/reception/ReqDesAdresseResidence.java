package ch.vd.uniregctb.evenement.reqdes.reception;

import ch.ech.ech0010.v4.AddressInformation;
import ch.ech.ech0010.v4.SwissAddressInformation;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.civil.data.CasePostale;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.xml.event.reqdes.v1.SwissResidence;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.TypeAdresseCivil;

/**
 * Adresse de résidence telle que renvoyée par les messages eReqDes
 */
public final class ReqDesAdresseResidence implements Adresse {

	private final Integer noOfsCommune;
	private final int noOfsPays;
	private final CasePostale casePostale;
	private final String localite;
	private final String numeroMaison;
	private final String npa;
	private final String npaComplementaire;
	private final int numeroOrdrePoste;
	private final String rue;
	private final String numeroAppartement;
	private final String titre;

	public ReqDesAdresseResidence(SwissResidence swissResidence) {
		this.noOfsCommune = swissResidence.getMunicipalityId();
		this.noOfsPays = ServiceInfrastructureService.noOfsSuisse;
		this.casePostale = null;

		final SwissAddressInformation dwellingAddress = swissResidence.getDwellingAddress();
		this.localite = dwellingAddress.getTown();
		this.numeroMaison = dwellingAddress.getHouseNumber();
		this.npa = String.valueOf(dwellingAddress.getSwissZipCode());
		this.npaComplementaire = dwellingAddress.getSwissZipCodeAddOn();
		this.numeroOrdrePoste = dwellingAddress.getSwissZipCodeId() == null ? 0 : dwellingAddress.getSwissZipCodeId();
		this.rue = dwellingAddress.getStreet();
		this.numeroAppartement = dwellingAddress.getDwellingNumber();
		this.titre = dwellingAddress.getAddressLine1();     // TODO que faire d'addressLine2 ?
	}

	public ReqDesAdresseResidence(AddressInformation addressInfo, ServiceInfrastructureService infraService) {
		this.noOfsCommune = null;

		final Pays pays = infraService.getPays(addressInfo.getCountry(), null);
		if (pays == null) {
			throw new IllegalArgumentException(String.format("Pays inconnu avec le code '%s'", addressInfo.getCountry()));
		}
		this.noOfsPays = pays.getNoOFS();

		this.casePostale = initCasePostale(addressInfo.getPostOfficeBoxText(), addressInfo.getPostOfficeBoxNumber());
		this.localite = addressInfo.getTown();
		this.numeroMaison = addressInfo.getHouseNumber();
		this.numeroAppartement = addressInfo.getDwellingNumber();
		this.numeroOrdrePoste = addressInfo.getSwissZipCodeId() == null ? 0 : addressInfo.getSwissZipCodeId();
		this.npa = addressInfo.getSwissZipCode() != null ? String.valueOf(addressInfo.getSwissZipCode()) : addressInfo.getForeignZipCode();
		this.npaComplementaire = addressInfo.getSwissZipCodeAddOn();
		this.rue = addressInfo.getStreet();
		this.titre = addressInfo.getAddressLine1();         // TODO que faire d'addressLine2 ?
	}

	private static CasePostale initCasePostale(String text, Long number) {
		if (number == null && StringUtils.isBlank(text)) {
			return null;
		}
		return new CasePostale(text, number);
	}

	@Override
	public CasePostale getCasePostale() {
		return casePostale;
	}

	@Override
	public RegDate getDateDebut() {
		return null;
	}

	@Override
	public RegDate getDateFin() {
		return null;
	}

	@Override
	public String getLocalite() {
		return localite;
	}

	@Override
	public String getNumero() {
		return numeroMaison;
	}

	@Override
	public int getNumeroOrdrePostal() {
		return numeroOrdrePoste;
	}

	@Override
	public String getNumeroPostal() {
		return npa;
	}

	@Override
	public String getNumeroPostalComplementaire() {
		return npaComplementaire;
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
	public Integer getNumeroRue() {
		return null;
	}

	@Override
	public String getNumeroAppartement() {
		return numeroAppartement;
	}

	@Override
	public String getTitre() {
		return titre;
	}

	@Override
	public TypeAdresseCivil getTypeAdresse() {
		return TypeAdresseCivil.PRINCIPALE;
	}

	@Override
	public Integer getEgid() {
		return null;
	}

	@Override
	public Integer getEwid() {
		return null;
	}

	@Nullable
	@Override
	public Localisation getLocalisationPrecedente() {
		return null;
	}

	@Nullable
	@Override
	public Localisation getLocalisationSuivante() {
		return null;
	}

	@Nullable
	@Override
	public Integer getNoOfsCommuneAdresse() {
		return noOfsCommune;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return true;
	}
}
