package ch.vd.unireg.evenement.reqdes.reception;

import ch.ech.ech0010.v4.AddressInformation;
import ch.ech.ech0010.v4.SwissAddressInformation;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.xml.event.reqdes.v1.SwissResidence;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.type.TypeAdresseCivil;

/**
 * Adresse de résidence telle que renvoyée par les messages ReqDes
 */
public final class ReqDesAdresseResidence implements Adresse {

	private final Integer noOfsCommune;
	private final int noOfsPays;
	private final CasePostale casePostale;
	private final String localite;
	private final String numeroMaison;
	private final String npa;
	private final String npaComplementaire;
	private final Integer numeroOrdrePoste;
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
		this.numeroOrdrePoste = dwellingAddress.getSwissZipCodeId();
		this.rue = dwellingAddress.getStreet();
		this.numeroAppartement = dwellingAddress.getDwellingNumber();

		// [SIFISC-13878] on prend en compte la deuxième ligne de complément si la première est vide
		this.titre = StringUtils.isBlank(dwellingAddress.getAddressLine1()) ? dwellingAddress.getAddressLine2() : dwellingAddress.getAddressLine1();
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
		this.numeroOrdrePoste = addressInfo.getSwissZipCodeId();
		this.npa = addressInfo.getSwissZipCode() != null ? String.valueOf(addressInfo.getSwissZipCode()) : addressInfo.getForeignZipCode();
		this.npaComplementaire = addressInfo.getSwissZipCodeAddOn();
		this.rue = addressInfo.getStreet();

		// [SIFISC-13878] on prend en compte la deuxième ligne de complément si la première est vide
		this.titre = StringUtils.isBlank(addressInfo.getAddressLine1()) ? addressInfo.getAddressLine2() : addressInfo.getAddressLine1();
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
	public Integer getNumeroOrdrePostal() {
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
