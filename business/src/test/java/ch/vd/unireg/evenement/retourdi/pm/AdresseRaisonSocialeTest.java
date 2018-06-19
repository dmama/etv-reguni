package ch.vd.unireg.evenement.retourdi.pm;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockRue;

/**
 * Quelques tests autour des algorithmes vus dans cette classe d'adresse
 */
public class AdresseRaisonSocialeTest extends BusinessTest {

	@Test
	public void testSplitAdresseBrutteSimple() throws Exception {

		final AdresseRaisonSociale.Brutte brutte = new AdresseRaisonSociale.Brutte("Alphonse Tartempion SARL", "Chemin de Mornex 25bis", null, null, null,null, "1003", "Lausanne");

		final Pair<NomAvecCivilite, Adresse> resultat = brutte.split(serviceInfra, tiersService, RegDate.get());
		Assert.assertNotNull(resultat);
		Assert.assertNotNull(resultat.getLeft());
		Assert.assertNull(resultat.getLeft().getCivilite());
		Assert.assertEquals("Alphonse Tartempion SARL", resultat.getLeft().getNomRaisonSociale());

		final Adresse adresse = resultat.getRight();
		Assert.assertNotNull(adresse);
		Assert.assertEquals(MockRue.Lausanne.CheminDeMornex.getNoRue(), adresse.getNumeroRue());
		Assert.assertEquals("25bis", adresse.getNumero());
		Assert.assertEquals("1003", adresse.getNumeroPostal());
		Assert.assertEquals(MockLocalite.Lausanne1003.getNoOrdre(), adresse.getNumeroOrdrePostal());
	}

	@Test
	public void testSplitAdresseBrutteAutreChoseApresNumeroMaison() throws Exception {

		final AdresseRaisonSociale.Brutte brutte = new AdresseRaisonSociale.Brutte("Alphonse Tartempion SARL", "Chemin de Mornex 25bis - machin", null, null, null,null, "1003", "Lausanne");

		final Pair<NomAvecCivilite, Adresse> resultat = brutte.split(serviceInfra, tiersService, RegDate.get());
		Assert.assertNotNull(resultat);
		Assert.assertNotNull(resultat.getLeft());
		Assert.assertNull(resultat.getLeft().getCivilite());
		Assert.assertEquals("Alphonse Tartempion SARL", resultat.getLeft().getNomRaisonSociale());

		final Adresse adresse = resultat.getRight();
		Assert.assertNotNull(adresse);
		Assert.assertEquals(MockRue.Lausanne.CheminDeMornex.getNoRue(), adresse.getNumeroRue());
		Assert.assertEquals("25bis", adresse.getNumero());
		Assert.assertEquals("1003", adresse.getNumeroPostal());
		Assert.assertEquals(MockLocalite.Lausanne1003.getNoOrdre(), adresse.getNumeroOrdrePostal());
	}

	@Test
	public void testSplitAdresseBrutteRaisonSocialeSurPlusieursLignes() throws Exception {

		final AdresseRaisonSociale.Brutte brutte = new AdresseRaisonSociale.Brutte("Alphonse Tartempion", "SARL", "Chemin de Mornex 25bis", null, null, null,"1003", "Lausanne");

		final Pair<NomAvecCivilite, Adresse> resultat = brutte.split(serviceInfra, tiersService, RegDate.get());
		Assert.assertNotNull(resultat);
		Assert.assertNotNull(resultat.getLeft());
		Assert.assertNull(resultat.getLeft().getCivilite());
		Assert.assertEquals("Alphonse Tartempion SARL", resultat.getLeft().getNomRaisonSociale());

		final Adresse adresse = resultat.getRight();
		Assert.assertNotNull(adresse);
		Assert.assertEquals(MockRue.Lausanne.CheminDeMornex.getNoRue(), adresse.getNumeroRue());
		Assert.assertEquals("25bis", adresse.getNumero());
		Assert.assertEquals("1003", adresse.getNumeroPostal());
		Assert.assertEquals(MockLocalite.Lausanne1003.getNoOrdre(), adresse.getNumeroOrdrePostal());
	}

	@Test
	public void testSplitAdresseBrutteRaisonSocialeAbsente() throws Exception {

		final AdresseRaisonSociale.Brutte brutte = new AdresseRaisonSociale.Brutte("Chemin de Mornex 25bis", null, null, null, null, null,"1003", "Lausanne");

		final Pair<NomAvecCivilite, Adresse> resultat = brutte.split(serviceInfra, tiersService, RegDate.get());
		Assert.assertNotNull(resultat);
		Assert.assertNotNull(resultat.getLeft());
		Assert.assertNull(resultat.getLeft().getCivilite());
		Assert.assertNull(resultat.getLeft().getNomRaisonSociale());

		final Adresse adresse = resultat.getRight();
		Assert.assertNotNull(adresse);
		Assert.assertEquals(MockRue.Lausanne.CheminDeMornex.getNoRue(), adresse.getNumeroRue());
		Assert.assertEquals("25bis", adresse.getNumero());
		Assert.assertEquals("1003", adresse.getNumeroPostal());
		Assert.assertEquals(MockLocalite.Lausanne1003.getNoOrdre(), adresse.getNumeroOrdrePostal());
	}

	@Test
	public void testSplitAdresseBrutteAdresseSurPlusieursLignes() throws Exception {

		final AdresseRaisonSociale.Brutte brutte = new AdresseRaisonSociale.Brutte("Alphonse Tartempion SARL", "Chemin de", "Mornex 25bis", null, null, null,"1003", "Lausanne");

		final Pair<NomAvecCivilite, Adresse> resultat = brutte.split(serviceInfra, tiersService, RegDate.get());
		Assert.assertNotNull(resultat);
		Assert.assertNotNull(resultat.getLeft());
		Assert.assertNull(resultat.getLeft().getCivilite());
		Assert.assertEquals("Alphonse Tartempion SARL", resultat.getLeft().getNomRaisonSociale());

		final Adresse adresse = resultat.getRight();
		Assert.assertNotNull(adresse);
		Assert.assertEquals(MockRue.Lausanne.CheminDeMornex.getNoRue(), adresse.getNumeroRue());
		Assert.assertEquals("25bis", adresse.getNumero());
		Assert.assertEquals("1003", adresse.getNumeroPostal());
		Assert.assertEquals(MockLocalite.Lausanne1003.getNoOrdre(), adresse.getNumeroOrdrePostal());
	}

	@Test
	public void testSplitAdresseBrutteInconnue() throws Exception {
		final AdresseRaisonSociale.Brutte brutte = new AdresseRaisonSociale.Brutte("Alphonse Tartempion SARL", "Chemin des petits bisous 14", null, null, null,null, "1003", "Lausanne");
		final Pair<NomAvecCivilite, Adresse> resultat = brutte.split(serviceInfra, tiersService, RegDate.get());
		Assert.assertNull(resultat);
	}

	@Test
	public void testSplitAdresseBrutteMauvaisNPA() throws Exception {
		final AdresseRaisonSociale.Brutte brutte = new AdresseRaisonSociale.Brutte("Alphonse Tartempion SARL", "Chemin des petits bisous 14", null, null, null, null,"3", "Mirage city");
		final Pair<NomAvecCivilite, Adresse> resultat = brutte.split(serviceInfra, tiersService, RegDate.get());
		Assert.assertNull(resultat);
	}

	@Test
	public void testSplitAdresseStructureeEntreprise() throws Exception {
		final String raisonSociale = "Turlututu chapeau pointu SA";
		final DestinataireAdresse destinataire = new DestinataireAdresse.Entreprise(null, raisonSociale, null, null, null);
		final AdresseRaisonSociale.StructureeSuisse input = new AdresseRaisonSociale.StructureeSuisse(destinataire, null, null, MockRue.Echallens.GrandRue.getNoRue(), null, "42ter", null, null, null, null, MockLocalite.Echallens.getNoOrdre());
		final Pair<NomAvecCivilite, Adresse> resultat = input.split(serviceInfra, tiersService, RegDate.get());
		Assert.assertNotNull(resultat);
		Assert.assertNotNull(resultat.getLeft());
		Assert.assertNull(resultat.getLeft().getCivilite());
		Assert.assertEquals("Turlututu chapeau pointu SA", resultat.getLeft().getNomRaisonSociale());

		final Adresse adresse = resultat.getRight();
		Assert.assertNotNull(adresse);
		Assert.assertEquals(MockRue.Echallens.GrandRue.getNoRue(), adresse.getNumeroRue());
		Assert.assertEquals("42ter", adresse.getNumero());
		Assert.assertEquals(MockLocalite.Echallens.getNoOrdre(), adresse.getNumeroOrdrePostal());
	}
}
