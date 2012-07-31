package ch.vd.uniregctb.evenement.civil.interne.depart;

import java.util.Arrays;

import junit.framework.Assert;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.uniregctb.adresse.AdressesCivilesHisto;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public class DepartDecaleHelperTest extends WithoutSpringTest {

	private static MockAdresse buildAdresse(RegDate dateDebut, @Nullable RegDate dateFin, TypeAdresseCivil type, String rue, String numero, String numeroPostal, String localite) {
		final MockAdresse adresse = new MockAdresse(rue, numero, numeroPostal, localite);
		adresse.setDateDebutValidite(dateDebut);
		adresse.setDateFinValidite(dateFin);
		adresse.setTypeAdresse(type);
		return adresse;
	}

	@Test
	public void testAucuneAdresse() throws Exception {
		Assert.assertNull(DepartDecaleHelper.getAdresseResidenceTerminee(RegDate.get(), 0, null));
		Assert.assertNull(DepartDecaleHelper.getAdresseResidenceTerminee(RegDate.get(), 1, null));
		Assert.assertNull(DepartDecaleHelper.getAdresseResidenceTerminee(RegDate.get(), 10, null));
		Assert.assertNull(DepartDecaleHelper.getAdresseResidenceTerminee(RegDate.get(), 100, null));

		final AdressesCivilesHisto adressesVides = new AdressesCivilesHisto();
		Assert.assertNull(DepartDecaleHelper.getAdresseResidenceTerminee(RegDate.get(), 0, adressesVides));
		Assert.assertNull(DepartDecaleHelper.getAdresseResidenceTerminee(RegDate.get(), 1, adressesVides));
		Assert.assertNull(DepartDecaleHelper.getAdresseResidenceTerminee(RegDate.get(), 10, adressesVides));
		Assert.assertNull(DepartDecaleHelper.getAdresseResidenceTerminee(RegDate.get(), 100, adressesVides));
	}

	@Test
	public void testAucuneAdresseResidence() throws Exception {
		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		final RegDate date = date(2010, 2, 18);
		final Adresse courrier = buildAdresse(date.addMonths(-1), date, TypeAdresseCivil.COURRIER, "Rue du bourg", "12", "9999", "Loin");
		adresses.courriers = Arrays.asList(courrier);
		final Adresse tutelle = buildAdresse(date.addMonths(-1), date, TypeAdresseCivil.TUTEUR, "Rue du bourg", "12", "9999", "Loin");
		adresses.tutelles = Arrays.asList(tutelle);

		Assert.assertNull(DepartDecaleHelper.getAdresseResidenceTerminee(date, 0, adresses));
		Assert.assertNull(DepartDecaleHelper.getAdresseResidenceTerminee(date, 1, adresses));
		Assert.assertNull(DepartDecaleHelper.getAdresseResidenceTerminee(date, 10, adresses));
		Assert.assertNull(DepartDecaleHelper.getAdresseResidenceTerminee(date, 100, adresses));
	}

	@Test
	public void testAdressePrincipaleNonDecalee() throws Exception {
		final RegDate date = date(2012, 5, 23);
		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		final Adresse avant = buildAdresse(date(2000, 1, 1), date.addDays(-1), TypeAdresseCivil.PRINCIPALE, "Rue du bourg", "12", "9999", "Loin");
		final Adresse pendant = buildAdresse(date, date, TypeAdresseCivil.PRINCIPALE, "Rue du bourg", "13", "9999", "Loin");
		final Adresse apres = buildAdresse(date.addDays(1), null, TypeAdresseCivil.PRINCIPALE, "Rue du bourg", "14", "9999", "Loin");
		adresses.principales = Arrays.asList(avant, pendant, apres);

		final Adresse found = DepartDecaleHelper.getAdresseResidenceTerminee(date, 0, adresses);
		Assert.assertNotNull(found);
		Assert.assertEquals("13", found.getNumero());
	}

	@Test
	public void testAdresseSecondaireSeuleNonDecalee() throws Exception {
		final RegDate date = date(2012, 5, 23);
		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		final Adresse avant = buildAdresse(date(2000, 1, 1), date.addDays(-1), TypeAdresseCivil.SECONDAIRE, "Rue du bourg", "12", "9999", "Loin");
		final Adresse pendant = buildAdresse(date, date, TypeAdresseCivil.SECONDAIRE, "Rue du bourg", "13", "9999", "Loin");
		final Adresse apres = buildAdresse(date.addDays(1), null, TypeAdresseCivil.SECONDAIRE, "Rue du bourg", "14", "9999", "Loin");
		adresses.secondaires = Arrays.asList(avant, pendant, apres);

		final Adresse found = DepartDecaleHelper.getAdresseResidenceTerminee(date, 0, adresses);
		Assert.assertNotNull(found);
		Assert.assertEquals("13", found.getNumero());
	}

	@Test
	public void testAdresseSecondaireNonDecaleeAvecAutreAdressePrincipale() throws Exception {
		final RegDate date = date(2012, 5, 23);
		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		final Adresse avant = buildAdresse(date(2000, 1, 1), date.addDays(-1), TypeAdresseCivil.SECONDAIRE, "Rue du bourg", "12", "9999", "Loin");
		final Adresse pendant = buildAdresse(date, date, TypeAdresseCivil.SECONDAIRE, "Rue du bourg", "13", "9999", "Loin");
		final Adresse apres = buildAdresse(date.addDays(1), null, TypeAdresseCivil.SECONDAIRE, "Rue du bourg", "14", "9999", "Loin");
		adresses.secondaires = Arrays.asList(avant, pendant, apres);

		final Adresse prn = buildAdresse(date(2000, 1, 1), null, TypeAdresseCivil.PRINCIPALE, "Rue de la liberté", "56", "8888", "Là");
		adresses.principales = Arrays.asList(prn);

		final Adresse found = DepartDecaleHelper.getAdresseResidenceTerminee(date, 0, adresses);
		Assert.assertNotNull(found);
		Assert.assertEquals("13", found.getNumero());
		Assert.assertEquals(TypeAdresseCivil.SECONDAIRE, found.getTypeAdresse());
	}

	@Test
	public void testAdressesPrincipaleEtSecondaireNonDecalees() throws Exception {
		final RegDate date = date(2012, 5, 23);
		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		final Adresse avant = buildAdresse(date(2000, 1, 1), date.addDays(-1), TypeAdresseCivil.PRINCIPALE, "Rue du bourg", "12", "9999", "Loin");
		final Adresse pendant = buildAdresse(date, date, TypeAdresseCivil.PRINCIPALE, "Rue du bourg", "13", "9999", "Loin");
		final Adresse apres = buildAdresse(date.addDays(1), null, TypeAdresseCivil.PRINCIPALE, "Rue du bourg", "14", "9999", "Loin");
		adresses.principales = Arrays.asList(avant, pendant, apres);

		final Adresse sec = buildAdresse(date(2000, 1, 1), date, TypeAdresseCivil.SECONDAIRE, "Rue de la liberté", "56", "8888", "Là");
		adresses.secondaires = Arrays.asList(sec);

		final Adresse found = DepartDecaleHelper.getAdresseResidenceTerminee(date, 0, adresses);
		Assert.assertNotNull(found);
		Assert.assertEquals("13", found.getNumero());
		Assert.assertEquals(TypeAdresseCivil.PRINCIPALE, found.getTypeAdresse());
	}

	@Test
	public void testAdressePrincipaleDecaleeSeule() throws Exception {
		final RegDate dateAdresse = date(2012, 5, 23);
		final RegDate dateEvenement = dateAdresse.addDays(2);
		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		final Adresse avant = buildAdresse(date(2000, 1, 1), dateAdresse.addDays(-1), TypeAdresseCivil.PRINCIPALE, "Rue du bourg", "12", "9999", "Loin");
		final Adresse pendant = buildAdresse(dateAdresse, dateAdresse, TypeAdresseCivil.PRINCIPALE, "Rue du bourg", "13", "9999", "Loin");
		final Adresse apres = buildAdresse(dateAdresse.addDays(1), null, TypeAdresseCivil.PRINCIPALE, "Rue du bourg", "14", "9999", "Loin");
		adresses.principales = Arrays.asList(avant, pendant, apres);

		// décalage interdit
		{
			final Adresse found = DepartDecaleHelper.getAdresseResidenceTerminee(dateEvenement, 0, adresses);
			Assert.assertNull(found);
		}
		// décalage d'un jour, pas suffisant
		{
			final Adresse found = DepartDecaleHelper.getAdresseResidenceTerminee(dateEvenement, 1, adresses);
			Assert.assertNull(found);
		}
		// décalage de deux jours -> ok
		{
			final Adresse found = DepartDecaleHelper.getAdresseResidenceTerminee(dateEvenement, 2, adresses);
			Assert.assertNotNull(found);
			Assert.assertEquals("13", found.getNumero());
			Assert.assertEquals(TypeAdresseCivil.PRINCIPALE, found.getTypeAdresse());
		}
	}

	@Test
	public void testAdressePrincipaleDecaleeAvecSecondaireMemeDecalage() throws Exception {
		final RegDate dateAdresse = date(2012, 5, 23);
		final RegDate dateEvenement = dateAdresse.addDays(2);
		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		final Adresse avant = buildAdresse(date(2000, 1, 1), dateAdresse.addDays(-1), TypeAdresseCivil.PRINCIPALE, "Rue du bourg", "12", "9999", "Loin");
		final Adresse pendant = buildAdresse(dateAdresse, dateAdresse, TypeAdresseCivil.PRINCIPALE, "Rue du bourg", "13", "9999", "Loin");
		final Adresse apres = buildAdresse(dateAdresse.addDays(1), null, TypeAdresseCivil.PRINCIPALE, "Rue du bourg", "14", "9999", "Loin");
		adresses.principales = Arrays.asList(avant, pendant, apres);

		final Adresse sec = buildAdresse(date(2000, 1, 1), dateAdresse, TypeAdresseCivil.SECONDAIRE, "Rue de la liberté", "56", "8888", "Là");
		adresses.secondaires = Arrays.asList(sec);

		// décalage interdit
		{
			final Adresse found = DepartDecaleHelper.getAdresseResidenceTerminee(dateEvenement, 0, adresses);
			Assert.assertNull(found);
		}
		// décalage d'un jour, pas suffisant
		{
			final Adresse found = DepartDecaleHelper.getAdresseResidenceTerminee(dateEvenement, 1, adresses);
			Assert.assertNull(found);
		}
		// décalage de deux jours -> ok
		{
			final Adresse found = DepartDecaleHelper.getAdresseResidenceTerminee(dateEvenement, 2, adresses);
			Assert.assertNotNull(found);
			Assert.assertEquals("13", found.getNumero());
			Assert.assertEquals(TypeAdresseCivil.PRINCIPALE, found.getTypeAdresse());
		}
	}

	@Test
	public void testAdressePrincipaleDecaleeAvecSecondairePlusProche() throws Exception {
		final RegDate dateAdresse = date(2012, 5, 23);
		final RegDate dateEvenement = dateAdresse.addDays(2);
		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		final Adresse avant = buildAdresse(date(2000, 1, 1), dateAdresse.addDays(-1), TypeAdresseCivil.PRINCIPALE, "Rue du bourg", "12", "9999", "Loin");
		final Adresse pendant = buildAdresse(dateAdresse, dateAdresse, TypeAdresseCivil.PRINCIPALE, "Rue du bourg", "13", "9999", "Loin");
		final Adresse apres = buildAdresse(dateAdresse.addDays(1), null, TypeAdresseCivil.PRINCIPALE, "Rue du bourg", "14", "9999", "Loin");
		adresses.principales = Arrays.asList(avant, pendant, apres);

		final Adresse sec = buildAdresse(date(2000, 1, 1), dateAdresse.addDays(1), TypeAdresseCivil.SECONDAIRE, "Rue de la liberté", "56", "8888", "Là");
		adresses.secondaires = Arrays.asList(sec);

		// décalage interdit
		{
			final Adresse found = DepartDecaleHelper.getAdresseResidenceTerminee(dateEvenement, 0, adresses);
			Assert.assertNull(found);
		}
		// décalage d'un jour, on prend l'adresse secondaire
		{
			final Adresse found = DepartDecaleHelper.getAdresseResidenceTerminee(dateEvenement, 1, adresses);
			Assert.assertNotNull(found);
			Assert.assertEquals("56", found.getNumero());
			Assert.assertEquals(TypeAdresseCivil.SECONDAIRE, found.getTypeAdresse());
		}
		// décalage de deux jours, on prend l'adresse principale
		{
			final Adresse found = DepartDecaleHelper.getAdresseResidenceTerminee(dateEvenement, 2, adresses);
			Assert.assertNotNull(found);
			Assert.assertEquals("13", found.getNumero());
			Assert.assertEquals(TypeAdresseCivil.PRINCIPALE, found.getTypeAdresse());
		}
	}

	@Test
	public void testAdresseDecaleeInverse() throws Exception {
		final RegDate dateAdresse = date(2012, 5, 23);
		final RegDate dateEvenement = dateAdresse.addDays(-2);
		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		final Adresse avant = buildAdresse(date(2000, 1, 1), dateAdresse.addDays(-1), TypeAdresseCivil.PRINCIPALE, "Rue du bourg", "12", "9999", "Loin");
		final Adresse pendant = buildAdresse(dateAdresse, dateAdresse, TypeAdresseCivil.PRINCIPALE, "Rue du bourg", "13", "9999", "Loin");
		final Adresse apres = buildAdresse(dateAdresse.addDays(1), null, TypeAdresseCivil.PRINCIPALE, "Rue du bourg", "14", "9999", "Loin");
		adresses.principales = Arrays.asList(avant, pendant, apres);

		Assert.assertNull(DepartDecaleHelper.getAdresseResidenceTerminee(dateEvenement, 0, adresses));
		Assert.assertNull(DepartDecaleHelper.getAdresseResidenceTerminee(dateEvenement, 1, adresses));
		Assert.assertNull(DepartDecaleHelper.getAdresseResidenceTerminee(dateEvenement, 10, adresses));
		Assert.assertNull(DepartDecaleHelper.getAdresseResidenceTerminee(dateEvenement, 100, adresses));
	}
}
