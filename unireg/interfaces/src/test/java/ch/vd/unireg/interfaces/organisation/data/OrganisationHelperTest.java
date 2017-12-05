package ch.vd.unireg.interfaces.organisation.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesRC;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesREE;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesRegistreIDE;
import ch.vd.uniregctb.common.UniregJUnit4Runner;
import ch.vd.uniregctb.type.TypeAdresseCivil;

import static ch.vd.unireg.interfaces.civil.data.IndividuRCPersTest.assertAdresse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(UniregJUnit4Runner.class)
public class OrganisationHelperTest {

	/**
	 * [SIFISC-24996] Vérifie que les adresses principales, courriers et cases postales sont bien extraites des données historiques RCEnt.
	 */
	@Test
	public void testGetAdresses() throws Exception {

		final MockDonneesRC donneesRc = new MockDonneesRC();
		final MockDonneesRegistreIDE donneesIde = new MockDonneesRegistreIDE();
		final MockDonneesREE donneesRee = new MockDonneesREE();
		donneesRc.addAdresseLegale(new AdresseLegaleRCEnt(RegDate.get(2000, 1, 1), null, "Lausanne", null, null, 150, "1003", null, null, "Avenue de la Gare", null, null, null));
		donneesIde.addAdresseEffective(new AdresseEffectiveRCEnt(RegDate.get(2001, 1, 1), null, "Morges", null, null, 254, "1110", null, null, "Rue des Alpes", null, null, null));
		donneesIde.addAdresseBoitePostale(new AdresseBoitePostaleRCEnt(RegDate.get(2002, 1, 1), null, "Romainmôtier", null, null, 564, "1323", null, null, "Rue du Bourg", null, null, null));

		final SiteOrganisationRCEnt site = new SiteOrganisationRCEnt(10L, null, null, null,
		                                                             Collections.singletonList(new DateRanged<>(null, null, TypeDeSite.ETABLISSEMENT_PRINCIPAL)), null, null, null,
		                                                             donneesRc, donneesIde, donneesRee, null, null, null, null, null);

		final Map<Long, SiteOrganisation> sites = new HashMap<>();
		sites.put(1L, site);

		final List<Adresse> adresses = OrganisationHelper.getAdresses(sites);
		assertNotNull(adresses);
		assertEquals(3, adresses.size());
		assertAdresse(TypeAdresseCivil.PRINCIPALE, RegDate.get(2000, 1, 1), null, "Avenue de la Gare", "Lausanne", adresses.get(0));
		assertAdresse(TypeAdresseCivil.COURRIER, RegDate.get(2001, 1, 1), null, "Rue des Alpes", "Morges", adresses.get(1));
		assertAdresse(TypeAdresseCivil.CASE_POSTALE, RegDate.get(2002, 1, 1), null, "Rue du Bourg", "Romainmôtier", adresses.get(2));
	}

	/**
	 * [SIFISC-24996] Vérifie que les adresses principales, courriers et cases postales sont bien extraites des données RCEnt.
	 */
	@Test
	public void testGetAdressesPourSite() throws Exception {

		final MockDonneesRC donneesRc = new MockDonneesRC();
		final MockDonneesRegistreIDE donneesIde = new MockDonneesRegistreIDE();
		final MockDonneesREE donneesRee = new MockDonneesREE();
		donneesRc.addAdresseLegale(new AdresseLegaleRCEnt(RegDate.get(2000, 1, 1), null, "Lausanne", null, null, 150, "1003", null, null, "Avenue de la Gare", null, null, null));
		donneesIde.addAdresseEffective(new AdresseEffectiveRCEnt(RegDate.get(2001, 1, 1), null, "Morges", null, null, 254, "1110", null, null, "Rue des Alpes", null, null, null));
		donneesIde.addAdresseBoitePostale(new AdresseBoitePostaleRCEnt(RegDate.get(2002, 1, 1), null, "Romainmôtier", null, null, 564, "1323", null, null, "Rue du Bourg", null, null, null));

		final SiteOrganisationRCEnt site = new SiteOrganisationRCEnt(10L, null, null, null,
		                                                             Collections.singletonList(new DateRanged<>(null, null, TypeDeSite.ETABLISSEMENT_PRINCIPAL)), null, null, null,
		                                                             donneesRc, donneesIde, donneesRee, null, null, null, null, null);

		final List<Adresse> adresses = OrganisationHelper.getAdressesPourSite(site);
		assertNotNull(adresses);
		assertEquals(3, adresses.size());
		assertAdresse(TypeAdresseCivil.PRINCIPALE, RegDate.get(2000, 1, 1), null, "Avenue de la Gare", "Lausanne", adresses.get(0));
		assertAdresse(TypeAdresseCivil.COURRIER, RegDate.get(2001, 1, 1), null, "Rue des Alpes", "Morges", adresses.get(1));
		assertAdresse(TypeAdresseCivil.CASE_POSTALE, RegDate.get(2002, 1, 1), null, "Rue du Bourg", "Romainmôtier", adresses.get(2));
	}
}