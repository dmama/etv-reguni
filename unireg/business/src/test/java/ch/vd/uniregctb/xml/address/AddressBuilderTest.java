package ch.vd.uniregctb.xml.address;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.party.address.v1.Address;
import ch.vd.unireg.xml.party.address.v1.AddressInformation;
import ch.vd.unireg.xml.party.address.v1.TariffZone;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.common.NpaEtLocalite;
import ch.vd.uniregctb.common.RueEtNumero;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.interfaces.model.TypeAffranchissement;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings("JavaDoc")
public class AddressBuilderTest extends WithoutSpringTest {

	/**
	 * [SIFISC-4320] cas du contribuable n°10749312 dont l'adresse courrier en provenance du registre civil possède à la fois un numéro d'ordre poste (= adresse suisse) et la France comme pays (= adresse
	 * hors-suisse). Dans ce cas-là, le swissZipCodeId était renseigné alors que le swissZipCode ne l'était pas et le message retourné ne passait pas la validation XML.
	 */
	@Test
	public void testFillDestination() throws Exception {

		// l'adresse bizarre
		final AdresseEnvoiDetaillee from =
				new AdresseEnvoiDetaillee(RegDate.get(2011, 10, 1), null, "Monsieur", "Monsieur", new NomPrenom("Porte", "Jean-Claude"), new RueEtNumero("Rue Aimé Pinel", "42"),
						new NpaEtLocalite("1110", "38230"), MockPays.France, TypeAffranchissement.EUROPE, 254, 0, AdresseGenerique.SourceType.CIVILE);
		
		final Address to = new Address();
		AddressBuilder.fillDestination(to, from);
		
		// on vérifie que le swissZipCodeId n'est pas renseigné  
		final AddressInformation info = to.getAddressInformation();
		assertNotNull(info);

		// spécifique eCH-0010
		assertNull(info.getAddressLine1());
		assertNull(info.getAddressLine2());
		assertEquals("FR", info.getCountry());
		assertNull(info.getDwellingNumber());
		assertEquals("1110", info.getForeignZipCode());
		assertEquals("42", info.getHouseNumber());
		assertNull(info.getLocality());
		assertNull(info.getPostOfficeBoxNumber());
		assertNull(info.getPostOfficeBoxText());
		assertEquals("Rue Aimé Pinel", info.getStreet());
		assertNull(info.getSwissZipCode());
		assertNull(info.getSwissZipCodeAddOn());
		assertNull(info.getSwissZipCodeId()); // <--- ne doit pas être renseigné !
		assertEquals("38230", info.getTown());

		// spécifique Unireg
		assertNull(info.getCareOf());
		assertNull(info.getComplementaryInformation());
		assertEquals(Integer.valueOf(8212), info.getCountryId());
		assertEquals("France", info.getCountryName());
		assertNull(info.getEgid());
		assertNull(info.getEwid());
		assertEquals(Integer.valueOf(0), info.getStreetId());
		assertEquals(TariffZone.EUROPE, info.getTariffZone());
	}
}
