package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import ch.ech.ech0010.v6.AddressInformation;
import ch.ech.ech0010.v6.Country;

import ch.vd.evd0022.v3.Address;
import ch.vd.unireg.interfaces.entreprise.data.AdresseAnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.rcent.RCEntAnnonceIDEHelper;

/**
 * @author Raphaël Marmier, 2016-08-29, <raphael.marmier@vd.ch>
 */
public class NoticeRequestAddressConverter {

	public AdresseAnnonceIDE convert(Address address) {
		final AddressInformation addressInformation = address.getAddressInformation();

		final Country country = addressInformation.getCountry();

		return RCEntAnnonceIDEHelper
				.createAdresseAnnonceIDERCEnt(addressInformation.getStreet(),
				                              addressInformation.getHouseNumber(),
				                              addressInformation.getDwellingNumber(),
				                              addressInformation.getSwissZipCode() == null ? null : toInt(addressInformation.getSwissZipCode()),
				                              addressInformation.getForeignZipCode(),
				                              addressInformation.getSwissZipCodeId(),
				                              addressInformation.getTown(),
				                              country.getCountryId(),
				                              country.getCountryIdISO2(),
				                              country.getCountryNameShort(),
				                              toInt(addressInformation.getPostOfficeBoxNumber()),
				                              addressInformation.getPostOfficeBoxText(),
				                              toInt(address.getFederalBuildingId()));
	}

	public Address convert(AdresseAnnonceIDE adresse) {
		final Address address = new Address();
		final AddressInformation addressInformation = new AddressInformation();

		addressInformation.setStreet(adresse.getRue());

		addressInformation.setHouseNumber(adresse.getNumero());
		addressInformation.setDwellingNumber(adresse.getNumeroAppartement());
		final Integer npa = adresse.getNpa();
		addressInformation.setSwissZipCode(npa == null ? null : npa.longValue());
		addressInformation.setForeignZipCode(adresse.getNpaEtranger());
		addressInformation.setSwissZipCodeId(adresse.getNumeroOrdrePostal());
		addressInformation.setTown(adresse.getVille());
		final AdresseAnnonceIDE.Pays pays = adresse.getPays();
		if (pays != null) {
			addressInformation.setCountry(new Country(pays.getNoOfs(), pays.getCodeISO2(), pays.getNomCourt()));
		}

		final Integer numeroCasePostale = adresse.getNumeroCasePostale();
		addressInformation.setPostOfficeBoxNumber(numeroCasePostale == null ? null : numeroCasePostale.longValue());
		addressInformation.setPostOfficeBoxText(adresse.getTexteCasePostale());

		final Integer egid = adresse.getEgid();
		address.setFederalBuildingId(egid == null ? null : egid.longValue());

		address.setAddressInformation(addressInformation);
		return address;
	}

	private static Integer toInt(Long l) {
		if (l == null) {
			return null;
		}
		if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
			return l.intValue();
		}
		throw new IllegalArgumentException("Value " + l + " not castable to int");
	}

}
