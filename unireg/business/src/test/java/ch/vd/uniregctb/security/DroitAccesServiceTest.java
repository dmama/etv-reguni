package ch.vd.uniregctb.security;

import java.util.regex.Pattern;

import junit.framework.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.TypeDroitAcces;

public class DroitAccesServiceTest extends BusinessTest {

	private DroitAccesServiceImpl service;
	private DroitAccesDAO droitAccesDAO;
	private TiersDAO tiersDAO;

	private static final long noIndOperateurSource = 1L;
	private static final long noIndOperateurDestination = 2L;

	private RegDate aujourdhui = RegDate.get();
	private RegDate demain = aujourdhui.getOneDayAfter();
	private RegDate hier = aujourdhui.getOneDayBefore();

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		droitAccesDAO = getBean(DroitAccesDAO.class, "droitAccesDAO");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");

		service = new DroitAccesServiceImpl();
		service.setDroitAccesDAO(droitAccesDAO);
		service.setTiersDAO(tiersDAO);
	}

	private PersonnePhysique createTiersPourDossier() {
		final PersonnePhysique nonhabitant = new PersonnePhysique(false);
		nonhabitant.setNom("Khan");
		nonhabitant.setPrenom("Gengis");
		return (PersonnePhysique) tiersDAO.save(nonhabitant);
	}

	private DroitAcces createDroitAcces(RegDate dateDebut, RegDate dateFin, Niveau niveau, TypeDroitAcces type, long noIndOperateur, PersonnePhysique dossier) {
		final DroitAcces da = new DroitAcces();
		da.setDateDebut(dateDebut);
		da.setDateFin(dateFin);
		da.setNiveau(niveau);
		da.setType(type);
		da.setNoIndividuOperateur(noIndOperateur);
		da.setTiers(dossier);
		return droitAccesDAO.save(da);
	}

	@Test
	public void testCopieSimple() throws Exception {
		final PersonnePhysique dossier = createTiersPourDossier();
		createDroitAcces(RegDate.get(2000,1,1), null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier);

		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui);

		service.copieDroitsAcces(noIndOperateurSource, noIndOperateurDestination);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), demain, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), hier);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
	}

	@Test
	public void testCopieDroitEchu() throws Exception {
		final PersonnePhysique dossier = createTiersPourDossier();
		createDroitAcces(RegDate.get(2000,1,1), RegDate.get(2009,12,31), Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier);

		final RegDate milieu = RegDate.get(2008, 1, 1);
		assertSansDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), milieu, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), milieu);

		// un droit échu ne doit pas être copié !
		service.copieDroitsAcces(noIndOperateurSource, noIndOperateurDestination);
		assertSansDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), milieu, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), milieu);
	}

	@Test
	public void testTransfertDroitEchu() throws Exception {
		final PersonnePhysique dossier = createTiersPourDossier();
		createDroitAcces(RegDate.get(2000,1,1), RegDate.get(2009,12,31), Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier);

		final RegDate milieu = RegDate.get(2008, 1, 1);
		assertSansDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), milieu, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), milieu);

		// un droit échu ne doit pas être transféré !
		service.transfereDroitsAcces(noIndOperateurSource, noIndOperateurDestination);
		assertSansDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), milieu, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), milieu);
	}

	@Test
	public void testTransfertSimple() throws Exception {
		final PersonnePhysique dossier = createTiersPourDossier();
		createDroitAcces(RegDate.get(2000,1,1), null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier);

		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui);

		service.transfereDroitsAcces(noIndOperateurSource, noIndOperateurDestination);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurSource, dossier.getNumero(), demain);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), hier);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
	}

	@Test
	public void testCopieAvecChevauchement() throws Exception {
		final PersonnePhysique dossier = createTiersPourDossier();
		createDroitAcces(RegDate.get(2000,1,1), null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier);
		createDroitAcces(aujourdhui, null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurDestination, dossier);

		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);

		service.copieDroitsAcces(noIndOperateurSource, noIndOperateurDestination);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), demain, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), hier);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
	}

	@Test
	public void testTransfertAvecChevauchement() throws Exception {
		final PersonnePhysique dossier = createTiersPourDossier();
		createDroitAcces(RegDate.get(2000,1,1), null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier);
		createDroitAcces(aujourdhui, null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurDestination, dossier);

		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);

		service.transfereDroitsAcces(noIndOperateurSource, noIndOperateurDestination);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurSource, dossier.getNumero(), demain);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), hier);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
	}

	@Test
	public void testCopieAvecChevauchementChangementDateDebut() throws Exception {
		final PersonnePhysique dossier = createTiersPourDossier();
		createDroitAcces(RegDate.get(2000,1,1), null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier);
		createDroitAcces(demain, null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurDestination, dossier);

		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), demain, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);

		service.copieDroitsAcces(noIndOperateurSource, noIndOperateurDestination);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), demain, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), hier);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
	}

	@Test
	public void testTransfertAvecChevauchementChangementDateDebut() throws Exception {
		final PersonnePhysique dossier = createTiersPourDossier();
		createDroitAcces(RegDate.get(2000,1,1), null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier);
		createDroitAcces(demain, null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurDestination, dossier);

		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), demain, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);

		service.transfereDroitsAcces(noIndOperateurSource, noIndOperateurDestination);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurSource, dossier.getNumero(), demain);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), hier);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
	}

	@Test
	public void testCopieAvecConflitDeNiveau() throws Exception {
		final PersonnePhysique dossier = createTiersPourDossier();
		createDroitAcces(RegDate.get(2000,1,1), null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier);
		createDroitAcces(RegDate.get(2002,1,1), null, Niveau.LECTURE, TypeDroitAcces.AUTORISATION, noIndOperateurDestination, dossier);

		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.LECTURE);

		try {
			service.copieDroitsAcces(noIndOperateurSource, noIndOperateurDestination);
			Assert.fail("Il y a un conflit sur le niveau d'autorisation, on n'aurait pas dû accepter la copie !");
		}
		catch (DroitAccesException e) {
			final Pattern pattern = Pattern.compile("Impossible d'ajouter le droit d'accès \\w+/\\w+ sur le dossier [0-9\\.]+ à l'opérateur \\d+ car celui-ci entrerait en conflit avec un droit \\w+/\\w+ existant");
			Assert.assertTrue(e.getMessage(), pattern.matcher(e.getMessage()).matches());
		}

		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.LECTURE);
	}

	@Test
	public void testTransfertAvecConflitDeNiveau() throws Exception {
		final PersonnePhysique dossier = createTiersPourDossier();
		createDroitAcces(RegDate.get(2000,1,1), null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier);
		createDroitAcces(RegDate.get(2002,1,1), null, Niveau.LECTURE, TypeDroitAcces.AUTORISATION, noIndOperateurDestination, dossier);

		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.LECTURE);

		try {
			service.transfereDroitsAcces(noIndOperateurSource, noIndOperateurDestination);
			Assert.fail("Il y a un conflit sur le niveau d'autorisation, on n'aurait pas dû accepter le transfert !");
		}
		catch (DroitAccesException e) {
			final Pattern pattern = Pattern.compile("Impossible d'ajouter le droit d'accès \\w+/\\w+ sur le dossier [0-9\\.]+ à l'opérateur \\d+ car celui-ci entrerait en conflit avec un droit \\w+/\\w+ existant");
			Assert.assertTrue(e.getMessage(), pattern.matcher(e.getMessage()).matches());
		}

		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.LECTURE);
	}

	@Test
	public void testCopieAvecConflitDeType() throws Exception {
		final PersonnePhysique dossier = createTiersPourDossier();
		createDroitAcces(RegDate.get(2000,1,1), null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier);
		createDroitAcces(RegDate.get(2002,1,1), null, Niveau.ECRITURE, TypeDroitAcces.INTERDICTION, noIndOperateurDestination, dossier);

		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui, TypeDroitAcces.INTERDICTION, Niveau.ECRITURE);

		try {
			service.copieDroitsAcces(noIndOperateurSource, noIndOperateurDestination);
			Assert.fail("Il y a un conflit sur le niveau d'autorisation, on n'aurait pas dû accepter la copie !");
		}
		catch (DroitAccesException e) {
			final Pattern pattern = Pattern.compile("Impossible d'ajouter le droit d'accès \\w+/\\w+ sur le dossier [0-9\\.]+ à l'opérateur \\d+ car celui-ci entrerait en conflit avec un droit \\w+/\\w+ existant");
			Assert.assertTrue(e.getMessage(), pattern.matcher(e.getMessage()).matches());
		}

		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui, TypeDroitAcces.INTERDICTION, Niveau.ECRITURE);
	}

	@Test
	public void testTransfertAvecConflitDeType() throws Exception {
		final PersonnePhysique dossier = createTiersPourDossier();
		createDroitAcces(RegDate.get(2000,1,1), null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier);
		createDroitAcces(RegDate.get(2002,1,1), null, Niveau.ECRITURE, TypeDroitAcces.INTERDICTION, noIndOperateurDestination, dossier);

		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui, TypeDroitAcces.INTERDICTION, Niveau.ECRITURE);

		try {
			service.transfereDroitsAcces(noIndOperateurSource, noIndOperateurDestination);
			Assert.fail("Il y a un conflit sur le niveau d'autorisation, on n'aurait pas dû accepter le transfert !");
		}
		catch (DroitAccesException e) {
			final Pattern pattern = Pattern.compile("Impossible d'ajouter le droit d'accès \\w+/\\w+ sur le dossier [0-9\\.]+ à l'opérateur \\d+ car celui-ci entrerait en conflit avec un droit \\w+/\\w+ existant");
			Assert.assertTrue(e.getMessage(), pattern.matcher(e.getMessage()).matches());
		}

		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui, TypeDroitAcces.INTERDICTION, Niveau.ECRITURE);
	}

	@Test
	public void testCopieAvecConflitSurDroitAnnule() throws Exception {
		final PersonnePhysique dossier = createTiersPourDossier();
		createDroitAcces(RegDate.get(2000,1,1), null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier);
		final DroitAcces daAnnule = createDroitAcces(RegDate.get(2002,1,1), null, Niveau.LECTURE, TypeDroitAcces.AUTORISATION, noIndOperateurDestination, dossier);
		daAnnule.setAnnule(true);

		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui);

		service.copieDroitsAcces(noIndOperateurSource, noIndOperateurDestination);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), demain, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), hier);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
	}

	private void assertSansDroit(long noIndOperateur, long dossier, RegDate date) {
		final DroitAcces da = droitAccesDAO.getDroitAcces(noIndOperateur, dossier, date);
		if (da != null) {
			Assert.assertNotNull(da.getDateFin());
			Assert.assertTrue(da.getDateFin().isBefore(date) || da.getDateDebut().isAfter(date));
		}
	}

	private void assertAvecDroit(long noIndOperateur, long dossier, RegDate date, TypeDroitAcces type, Niveau niveau) {
		final DroitAcces da = droitAccesDAO.getDroitAcces(noIndOperateur, dossier, date);
		Assert.assertNotNull(da);
		Assert.assertEquals(type, da.getType());
		Assert.assertEquals(niveau, da.getNiveau());
	}
}
