package ch.vd.uniregctb.security;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
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
		service.setServiceSecuriteService(getBean(ServiceSecuriteService.class, "serviceSecuriteService"));
	}

	private PersonnePhysique createTiersPourDossier() {
		final PersonnePhysique nonhabitant = new PersonnePhysique(false);
		nonhabitant.setNom("Khan");
		nonhabitant.setPrenomUsuel("Gengis");
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
	@Transactional(rollbackFor = Throwable.class)
	public void testCopieSimple() throws Exception {
		final PersonnePhysique dossier = createTiersPourDossier();
		createDroitAcces(RegDate.get(2000,1,1), null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier);

		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui);

		final List<DroitAccesConflit> conflits = service.copieDroitsAcces(noIndOperateurSource, noIndOperateurDestination);
		assertEmpty(conflits);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), demain, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), hier);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCopieDroitEchu() throws Exception {
		final PersonnePhysique dossier = createTiersPourDossier();
		createDroitAcces(RegDate.get(2000,1,1), RegDate.get(2009,12,31), Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier);

		final RegDate milieu = RegDate.get(2008, 1, 1);
		assertSansDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), milieu, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), milieu);

		// un droit échu ne doit pas être copié !
		final List<DroitAccesConflit> conflits = service.copieDroitsAcces(noIndOperateurSource, noIndOperateurDestination);
		assertEmpty(conflits);
		assertSansDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), milieu, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), milieu);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTransfertDroitEchu() throws Exception {
		final PersonnePhysique dossier = createTiersPourDossier();
		createDroitAcces(RegDate.get(2000,1,1), RegDate.get(2009,12,31), Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier);

		final RegDate milieu = RegDate.get(2008, 1, 1);
		assertSansDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), milieu, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), milieu);

		// un droit échu ne doit pas être transféré !
		final List<DroitAccesConflit> conflits = service.transfereDroitsAcces(noIndOperateurSource, noIndOperateurDestination);
		assertEmpty(conflits);
		assertSansDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), milieu, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), milieu);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTransfertSimple() throws Exception {
		final PersonnePhysique dossier = createTiersPourDossier();
		createDroitAcces(RegDate.get(2000,1,1), null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier);

		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui);

		final List<DroitAccesConflit> conflits = service.transfereDroitsAcces(noIndOperateurSource, noIndOperateurDestination);
		assertEmpty(conflits);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurSource, dossier.getNumero(), demain);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), hier);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCopieAvecChevauchement() throws Exception {
		final PersonnePhysique dossier = createTiersPourDossier();
		createDroitAcces(RegDate.get(2000,1,1), null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier);
		createDroitAcces(aujourdhui, null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurDestination, dossier);

		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);

		final List<DroitAccesConflit> conflits = service.copieDroitsAcces(noIndOperateurSource, noIndOperateurDestination);
		assertEmpty(conflits);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), demain, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), hier);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTransfertAvecChevauchement() throws Exception {
		final PersonnePhysique dossier = createTiersPourDossier();
		createDroitAcces(RegDate.get(2000,1,1), null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier);
		createDroitAcces(aujourdhui, null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurDestination, dossier);

		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);

		final List<DroitAccesConflit> conflits = service.transfereDroitsAcces(noIndOperateurSource, noIndOperateurDestination);
		assertEmpty(conflits);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurSource, dossier.getNumero(), demain);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), hier);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCopieAvecChevauchementChangementDateDebut() throws Exception {
		final PersonnePhysique dossier = createTiersPourDossier();
		createDroitAcces(RegDate.get(2000,1,1), null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier);
		createDroitAcces(demain, null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurDestination, dossier);

		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), demain, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);

		final List<DroitAccesConflit> conflits = service.copieDroitsAcces(noIndOperateurSource, noIndOperateurDestination);
		assertEmpty(conflits);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), demain, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), hier);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTransfertAvecChevauchementChangementDateDebut() throws Exception {
		final PersonnePhysique dossier = createTiersPourDossier();
		createDroitAcces(RegDate.get(2000,1,1), null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier);
		createDroitAcces(demain, null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurDestination, dossier);

		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), demain, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);

		final List<DroitAccesConflit> conflits = service.transfereDroitsAcces(noIndOperateurSource, noIndOperateurDestination);
		assertEmpty(conflits);
		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurSource, dossier.getNumero(), demain);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), hier);
		assertAvecDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCopieAvecConflitDeNiveau() throws Exception {
		final PersonnePhysique dossier1 = createTiersPourDossier();
		final PersonnePhysique dossier2 = createTiersPourDossier();
		createDroitAcces(RegDate.get(2000, 1, 1), null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier1);
		createDroitAcces(RegDate.get(2000, 1, 1), null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier2);
		createDroitAcces(RegDate.get(2002, 1, 1), null, Niveau.LECTURE, TypeDroitAcces.AUTORISATION, noIndOperateurDestination, dossier1);

		assertAvecDroit(noIndOperateurSource, dossier1.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurSource, dossier2.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurDestination, dossier1.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.LECTURE);

		final List<DroitAccesConflit> conflits = service.copieDroitsAcces(noIndOperateurSource, noIndOperateurDestination);
		Assert.assertEquals(1, conflits.size());

		final DroitAccesConflit conflit = conflits.get(0);
		Assert.assertNotNull(conflit);
		Assert.assertEquals(dossier1.getNumero(), (Long) conflit.getNoContribuable());
		Assert.assertEquals(Niveau.LECTURE, conflit.getAccesPreexistant().getNiveau());
		Assert.assertEquals(TypeDroitAcces.AUTORISATION, conflit.getAccesPreexistant().getType());
		Assert.assertEquals(Niveau.ECRITURE, conflit.getAccesCopie().getNiveau());
		Assert.assertEquals(TypeDroitAcces.AUTORISATION, conflit.getAccesCopie().getType());

		assertAvecDroit(noIndOperateurSource, dossier1.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurSource, dossier1.getNumero(), demain, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurSource, dossier2.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurSource, dossier2.getNumero(), demain, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurDestination, dossier1.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.LECTURE);
		assertSansDroit(noIndOperateurDestination, dossier2.getNumero(), hier);
		assertAvecDroit(noIndOperateurDestination, dossier2.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTransfertAvecConflitDeNiveau() throws Exception {
		final PersonnePhysique dossier1 = createTiersPourDossier();
		final PersonnePhysique dossier2 = createTiersPourDossier();
		createDroitAcces(RegDate.get(2000, 1, 1), null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier1);
		createDroitAcces(RegDate.get(2000, 1, 1), null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier2);
		createDroitAcces(RegDate.get(2002, 1, 1), null, Niveau.LECTURE, TypeDroitAcces.AUTORISATION, noIndOperateurDestination, dossier1);

		assertAvecDroit(noIndOperateurSource, dossier1.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurSource, dossier2.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurDestination, dossier1.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.LECTURE);

		final List<DroitAccesConflit> conflits = service.transfereDroitsAcces(noIndOperateurSource, noIndOperateurDestination);
		Assert.assertEquals(1, conflits.size());

		final DroitAccesConflit conflit = conflits.get(0);
		Assert.assertNotNull(conflit);
		Assert.assertEquals(dossier1.getNumero(), (Long) conflit.getNoContribuable());
		Assert.assertEquals(Niveau.LECTURE, conflit.getAccesPreexistant().getNiveau());
		Assert.assertEquals(TypeDroitAcces.AUTORISATION, conflit.getAccesPreexistant().getType());
		Assert.assertEquals(Niveau.ECRITURE, conflit.getAccesCopie().getNiveau());
		Assert.assertEquals(TypeDroitAcces.AUTORISATION, conflit.getAccesCopie().getType());

		assertAvecDroit(noIndOperateurSource, dossier1.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurSource, dossier1.getNumero(), demain);
		assertAvecDroit(noIndOperateurSource, dossier2.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurSource, dossier2.getNumero(), demain);
		assertAvecDroit(noIndOperateurDestination, dossier1.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.LECTURE);
		assertSansDroit(noIndOperateurDestination, dossier2.getNumero(), hier);
		assertAvecDroit(noIndOperateurDestination, dossier2.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCopieAvecConflitDeType() throws Exception {
		final PersonnePhysique dossier1 = createTiersPourDossier();
		final PersonnePhysique dossier2 = createTiersPourDossier();
		createDroitAcces(RegDate.get(2000, 1, 1), null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier1);
		createDroitAcces(RegDate.get(2000, 1, 1), null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier2);
		createDroitAcces(RegDate.get(2002, 1, 1), null, Niveau.ECRITURE, TypeDroitAcces.INTERDICTION, noIndOperateurDestination, dossier1);

		assertAvecDroit(noIndOperateurSource, dossier1.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurSource, dossier2.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurDestination, dossier1.getNumero(), aujourdhui, TypeDroitAcces.INTERDICTION, Niveau.ECRITURE);

		final List<DroitAccesConflit> conflits = service.copieDroitsAcces(noIndOperateurSource, noIndOperateurDestination);
		Assert.assertEquals(1, conflits.size());

		final DroitAccesConflit conflit = conflits.get(0);
		Assert.assertNotNull(conflit);
		Assert.assertEquals(dossier1.getNumero(), (Long) conflit.getNoContribuable());
		Assert.assertEquals(Niveau.ECRITURE, conflit.getAccesPreexistant().getNiveau());
		Assert.assertEquals(TypeDroitAcces.INTERDICTION, conflit.getAccesPreexistant().getType());
		Assert.assertEquals(Niveau.ECRITURE, conflit.getAccesCopie().getNiveau());
		Assert.assertEquals(TypeDroitAcces.AUTORISATION, conflit.getAccesCopie().getType());

		assertAvecDroit(noIndOperateurSource, dossier1.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurSource, dossier1.getNumero(), demain, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurSource, dossier2.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurSource, dossier2.getNumero(), demain, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurDestination, dossier1.getNumero(), aujourdhui, TypeDroitAcces.INTERDICTION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier2.getNumero(), hier);
		assertAvecDroit(noIndOperateurDestination, dossier2.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTransfertAvecConflitDeType() throws Exception {
		final PersonnePhysique dossier1 = createTiersPourDossier();
		final PersonnePhysique dossier2 = createTiersPourDossier();
		createDroitAcces(RegDate.get(2000, 1, 1), null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier1);
		createDroitAcces(RegDate.get(2000, 1, 1), null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier2);
		createDroitAcces(RegDate.get(2002, 1, 1), null, Niveau.ECRITURE, TypeDroitAcces.INTERDICTION, noIndOperateurDestination, dossier1);

		assertAvecDroit(noIndOperateurSource, dossier1.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurSource, dossier2.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertAvecDroit(noIndOperateurDestination, dossier1.getNumero(), aujourdhui, TypeDroitAcces.INTERDICTION, Niveau.ECRITURE);

		final List<DroitAccesConflit> conflits = service.transfereDroitsAcces(noIndOperateurSource, noIndOperateurDestination);
		Assert.assertEquals(1, conflits.size());

		final DroitAccesConflit conflit = conflits.get(0);
		Assert.assertNotNull(conflit);
		Assert.assertEquals(dossier1.getNumero(), (Long) conflit.getNoContribuable());
		Assert.assertEquals(Niveau.ECRITURE, conflit.getAccesPreexistant().getNiveau());
		Assert.assertEquals(TypeDroitAcces.INTERDICTION, conflit.getAccesPreexistant().getType());
		Assert.assertEquals(Niveau.ECRITURE, conflit.getAccesCopie().getNiveau());
		Assert.assertEquals(TypeDroitAcces.AUTORISATION, conflit.getAccesCopie().getType());

		assertAvecDroit(noIndOperateurSource, dossier1.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurSource, dossier1.getNumero(), demain);
		assertAvecDroit(noIndOperateurSource, dossier2.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurSource, dossier2.getNumero(), demain);
		assertAvecDroit(noIndOperateurDestination, dossier1.getNumero(), aujourdhui, TypeDroitAcces.INTERDICTION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier2.getNumero(), hier);
		assertAvecDroit(noIndOperateurDestination, dossier2.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCopieAvecConflitSurDroitAnnule() throws Exception {
		final PersonnePhysique dossier = createTiersPourDossier();
		createDroitAcces(RegDate.get(2000,1,1), null, Niveau.ECRITURE, TypeDroitAcces.AUTORISATION, noIndOperateurSource, dossier);
		final DroitAcces daAnnule = createDroitAcces(RegDate.get(2002,1,1), null, Niveau.LECTURE, TypeDroitAcces.AUTORISATION, noIndOperateurDestination, dossier);
		daAnnule.setAnnule(true);

		assertAvecDroit(noIndOperateurSource, dossier.getNumero(), aujourdhui, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE);
		assertSansDroit(noIndOperateurDestination, dossier.getNumero(), aujourdhui);

		final List<DroitAccesConflit> conflits = service.copieDroitsAcces(noIndOperateurSource, noIndOperateurDestination);
		assertEmpty(conflits);
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
