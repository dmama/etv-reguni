package ch.vd.uniregctb.tiers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.collection.PersistentSet;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseAutreTiers;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.performance.PerformanceLog;
import ch.vd.uniregctb.performance.PerformanceLogsRepository;
import ch.vd.uniregctb.tiers.TiersDAO.Parts;
import ch.vd.uniregctb.type.CategorieIdentifiant;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TarifImpotSource;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author
 */
@SuppressWarnings({"JavaDoc"})
public class TiersDAOTest extends CoreDAOTest {

	protected static final Logger LOGGER = Logger.getLogger(TiersDAOTest.class);

	private static final String DAO_NAME = "tiersDAO";

	private static final long NOMBRE_ADRESSES_PREMIER_TIERS = 2L;

	private static final long NOMBRE_ADRESSES_TIERS_SUIVANTS = 2L;

	private static final long NOMBRE_FORS_FISCAUX = 3L;

	/**
	 * Le DAO.
	 */
	private TiersDAO dao;

	/**
	 * @throws Exception
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		dao = getBean(TiersDAO.class, DAO_NAME);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void getTiersInRangeBounded() throws Exception {

		{
			List<Long> list = dao.getTiersInRange(10001200, 10012832);
			assertEquals(0, list.size());
		}

		loadDatabase();

		{
			List<Long> list = dao.getTiersInRange(10007890, 10008901);
			assertEquals(2, list.size());
		}

		{
			List<Long> list = dao.getTiersInRange(10007888, 10008905);
			assertEquals(2, list.size());
		}

		{
			List<Long> list = dao.getTiersInRange(10007891, 10008905);
			assertEquals(1, list.size());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void getTiersInRangeUnboundedRight() throws Exception {

		loadDatabase();

		List<Long> list = dao.getTiersInRange(10000000, -1);
		assertEquals(9, list.size());
		assertTrue(list.contains(10000001L));
		assertTrue(list.contains(10000002L));
		assertTrue(list.contains(10000004L));
		assertTrue(list.contains(10000005L));
		assertTrue(list.contains(10000010L));
		assertTrue(list.contains(10001111L));
		assertTrue(list.contains(10006789L));
		assertTrue(list.contains(10007890L));
		assertTrue(list.contains(10008901L));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void getTiersInRangeUnboundedLeft() throws Exception {

		loadDatabase();

		List<Long> list = dao.getTiersInRange(-1, 10000000);
		assertEquals(3, list.size());
		assertTrue(list.contains(9876L));
		assertTrue(list.contains(1001234L));
		assertTrue(list.contains(2002222L));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void getTiersInRangeUnbounded() throws Exception {

		loadDatabase();
		int total = dao.getAll().size();

		List<Long> list = dao.getTiersInRange(-1, -1);
		assertEquals(total, list.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void getAllIds() throws Exception {

		{
			List<Long> list = dao.getAllIds();
			assertEquals(0, list.size());
		}

		loadDatabase();

		{
			List<Long> list = dao.getAllIds();
			assertEquals(12, list.size());
		}
		{
			List<Long> list = dao.getAllNumeroIndividu();
			assertTrue(list.size() == 7);
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testExists() throws Exception {

		assertFalse(dao.exists(1234456567L));
		assertFalse(dao.exists(10007890L));

		loadDatabase();

		assertFalse(dao.exists(1234456567L));
		assertTrue(dao.exists(10007890L));
	}

	/**
	 * Teste que les numéros générés pour les Tiers est dans le bon range
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testNumeroContribuable() throws Exception {

		loadDatabase();

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumeroIndividu(12L);
		insertAndTestNumeroTiers(hab, Contribuable.CTB_GEN_FIRST_ID, Contribuable.CTB_GEN_LAST_ID);
		PersonnePhysique nh = new PersonnePhysique(false);
		nh.setNom("bla");
		insertAndTestNumeroTiers(nh, Contribuable.CTB_GEN_FIRST_ID, Contribuable.CTB_GEN_LAST_ID);
		insertAndTestNumeroTiers(new MenageCommun(), Contribuable.CTB_GEN_FIRST_ID, Contribuable.CTB_GEN_LAST_ID);
	}

	/**
	 * Teste que les numéros générés pour les Tiers est dans le bon range
	 */
	@ExpectedException(IllegalArgumentException.class)
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testNumeroEntrepriseNOK() throws Exception {
		insertAndTestNumeroTiers(new Entreprise(), Entreprise.FIRST_ID, Entreprise.LAST_ID);
	}

	/**
	 * Teste que les numéros générés pour les Tiers est dans le bon range
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testNumeroEntrepriseOK() throws Exception {

		Entreprise ent = new Entreprise();
		ent.setNumero(1004L);
		insertAndTestNumeroTiers(ent, Entreprise.FIRST_ID, Entreprise.LAST_ID);
	}

	/**
	 * Teste que les numéros générés pour les Tiers est dans le bon range
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testNumeroAutreCommunaute() throws Exception {

		AutreCommunaute ac = new AutreCommunaute();
		ac.setNom("Une entreprise super");
		insertAndTestNumeroTiers(ac, Entreprise.PM_GEN_FIRST_ID, Entreprise.PM_GEN_LAST_ID);
	}

	/**
	 * Teste que les numéros générés pour les Tiers est dans le bon range
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testNumeroCollectiviteAdministrative() throws Exception {

		insertAndTestNumeroTiers(new CollectiviteAdministrative(), Entreprise.PM_GEN_FIRST_ID, Entreprise.PM_GEN_LAST_ID);
	}

	/**
	 * Teste que les numéros générés pour les Tiers est dans le bon range
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testNumeroDebiteurPrestationImposable() throws Exception {

		DebiteurPrestationImposable debiteur = new DebiteurPrestationImposable();
		insertAndTestNumeroTiers(debiteur, DebiteurPrestationImposable.FIRST_ID, DebiteurPrestationImposable.LAST_ID);
	}

	private void insertAndTestNumeroTiers(final Tiers tiers, long first, long last) throws Exception {

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final Tiers t = dao.save(tiers);
				return t.getId();
			}
		});

		{
			final Tiers t = dao.get(id);
			assertTrue("Le numéro de Tiers (id=" + id + " tiers=" + t + ") doit être dans le range " + first + " => " + last, first <= t.getId() && t.getId() < last);
		}
	}

	public void _testPerf() {
		Map<String, PerformanceLog> logs = PerformanceLogsRepository.getInstance().getLogs("dao");
		for (Map.Entry<String, PerformanceLog> entry : logs.entrySet()) {
			PerformanceLog log = entry.getValue();

			LOGGER.warn("Item: " + entry.getKey() + " Log: " + log);
		}

		logs = null;
	}

	/**
	 * L'idée est de changer une propriété, der ne PAS faire de save(), de committer puis de recharger le tiers La modif doit être persistée
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testModifyTiersWithoutSave() throws Exception {

		loadDatabase();

		final long id = 10001111L;
		final RegDate date1 = RegDate.get(1970, 1, 23);
		final RegDate date2 = RegDate.get(1969, 3, 1);

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique nonHab = (PersonnePhysique) dao.get(id);
				assertEquals(date1, nonHab.getDateNaissance());
				nonHab.setDateNaissance(date2);
				return null;
			}
		});

		{
			PersonnePhysique nonHab = (PersonnePhysique) dao.get(id);
			assertEquals(date2, nonHab.getDateNaissance());
		}
	}

	/**
	 * Teste la methode findByNumero.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetContribuableByNumero() throws Exception {

		loadDatabase();

		Contribuable contribuable = dao.getContribuableByNumero(10006789L);
		assertNotNull(contribuable);
		assertEquals(new Long(10006789L), contribuable.getNumero());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetHabitantsByNumeroIndividu() throws Exception {

		loadDatabase();

		PersonnePhysique tiers = dao.getHabitantByNumeroIndividu(282315L);
		assertNotNull(tiers);
		assertTrue(tiers.getNumero() == 10006789);
		assertTrue(tiers.getNumeroIndividu().intValue() == 282315);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetNonHabitant() throws Exception {

		loadDatabase();

		PersonnePhysique nonHab = (PersonnePhysique) dao.get(10001111L);
		assertEquals(new Long(10001111L), nonHab.getNumero());
		assertEquals("Conchita", nonHab.getNom());
		assertEquals("Andrea", nonHab.getPrenom());
		assertFalse(nonHab.isHabitantVD());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSaveNonHabitant() throws Exception {

		final long id = 12345678L;

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique nonHab = new PersonnePhysique(false);
				nonHab.setNumero(id);
				nonHab.setNom("Bla");
				nonHab.setPrenom("Bli");

				dao.save(nonHab);
				return null;
			}
		});

		{
			PersonnePhysique nonHab = (PersonnePhysique) dao.get(id);
			assertEquals(new Long(id), nonHab.getNumero());
			assertEquals("Bla", nonHab.getNom());
			assertEquals("Bli", nonHab.getPrenom());
			assertFalse(nonHab.isHabitantVD());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPersonnePhysiqueSansForByNumeroIndividu() throws Exception {

		final long noIndividu = 1234567890L;

		// mise en place
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				addHabitant(noIndividu);
				return null;
			}
		});

		final PersonnePhysique pp = dao.getPPByNumeroIndividu(noIndividu);
		assertNotNull(pp);
		assertEquals(Long.valueOf(noIndividu), pp.getNumeroIndividu());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPersonnePhysiqueDesactiveeByNumeroIndividu() throws Exception {

		final long noIndividu = 1234567890L;

		// mise en place
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(2001, 12, 4), MotifFor.MAJORITE, date(2009, 5, 12), MotifFor.ANNULATION, 2434, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE);
				return null;
			}
		});

		final PersonnePhysique pp = dao.getPPByNumeroIndividu(noIndividu);
		assertNull(pp);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPersonnePhysiqueReactiveeByNumeroIndividu() throws Exception {

		final long noIndividu = 1234567890L;

		// mise en place
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(2001, 12, 4), MotifFor.MAJORITE, date(2009, 5, 12), MotifFor.ANNULATION, 2434, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE);
				addForPrincipal(pp, date(2010, 1, 1), MotifFor.REACTIVATION, date(2010, 6, 30), MotifFor.DEPART_HS, 2434, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE);
				return null;
			}
		});

		final PersonnePhysique pp = dao.getPPByNumeroIndividu(noIndividu);
		assertNotNull(pp);
		assertEquals(Long.valueOf(noIndividu), pp.getNumeroIndividu());
	}

	/**
	 * Teste la methode find.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFind() throws Exception {

		loadDatabase();

		List<Tiers> list = dao.getAll();
		assertNotNull(list);
		int count = list.size();
		assertEquals(12, count);
		for (Tiers tiers : list) {
			if (tiers.getNumero().equals(9876L)) {
				count = tiers.getAdressesTiers().size();
				assertEquals(1, count);
			}

			if (tiers.getNumero().equals(10006789L)) {
				count = tiers.getAdressesTiers().size();
				assertEquals(2, count);
				Contribuable ctb = (Contribuable) tiers;
				assertEquals(2, ctb.getForsFiscaux().size());

				// Recherche l'imposition fermé
				boolean foundImpositionFerme = false;
				ForFiscal forOuvert = null;
				for (ForFiscal forFiscal : ctb.getForsFiscaux()) {
					LOGGER.info("Ouv:" + forFiscal.getDateDebut() + " Ferm:" + forFiscal.getDateFin() + " Autre:"
							+ RegDate.get(2006, 3, 1));
					if (forFiscal.getDateDebut().equals(RegDate.get(2006, 3, 1))) {
						assertTrue(RegDate.get(2006, 8, 31).equals(forFiscal.getDateFin()));
						foundImpositionFerme = true;
					}
					else {
						forOuvert = forFiscal;
					}
				}
				assertTrue(foundImpositionFerme);
				assertNotNull(forOuvert);
			}

			// Verifie les adresses
			for (AdresseTiers adresses : tiers.getAdressesTiers()) {
				Tiers adresse = adresses.getTiers();
				assertTrue(tiers == adresse);
			}
		}
		// setComplete();
	}

	/**
	 * @throws Exception
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testInsertTiers() throws Exception {

		final int NOMBRE_TIERS = 23;

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				doInsertTiers(NOMBRE_TIERS);
				return null;
			}
		});


		List<Tiers> tierss = dao.getAll();
		int nombreTiers = tierss.size();
		assertEquals(NOMBRE_TIERS, nombreTiers);

		int tiersIndex = 0;
		for (Tiers tiers : tierss) {
			tiersIndex++;

			{
				String user = tiers.getLogCreationUser();
				assertEquals("[UT] TiersDAOTest", user);
				Date date = tiers.getLogCreationDate();
				assertNotNull(date);
			}

			// Controle le nombre d'adresses
			Set<AdresseTiers> adresses = tiers.getAdressesTiers();
			int nombreAdresses = adresses.size();
			// LOGGER.debug("nbAdr:" + nbAdr);
			// Le premier tiers de la liste est suppossé avoir
			// NOMBRE_ADRESSES_PREMIER_TIERS
			if (tiersIndex == 1) {
				assertEquals(NOMBRE_ADRESSES_PREMIER_TIERS, nombreAdresses);
			}
			else {
				// Les tiers suivants ont NOMBRE_ADRESSES_TIERS_SUIVANTS
				assertEquals(NOMBRE_ADRESSES_TIERS_SUIVANTS, nombreAdresses);
			}
			if (tiers instanceof Contribuable) {
				// Controle le nombre de fors
				Set<ForFiscal> forsFiscaux = tiers.getForsFiscaux();
				int nombreForsFiscaux = forsFiscaux.size();
				assertEquals(NOMBRE_FORS_FISCAUX, nombreForsFiscaux);
			}
		}
		assertEquals(NOMBRE_TIERS, tiersIndex);
	}

	private void doInsertTiers(int nbTiers) throws Exception {

		PersonnePhysique ppPrecedent = null;

		// Database is empty at method start
		for (int i = 0; i < nbTiers; i++) {
			Tiers tiers = null;
			int modulo = i % 7;
			switch (modulo) {
			case 0:
				PersonnePhysique hab = new PersonnePhysique(true);
				hab.setNumeroIndividu(i + 43L);
				tiers = hab;
				break;
			case 1:
				PersonnePhysique nh = new PersonnePhysique(false);
				nh.setNom("Bli");
				tiers = nh;
				break;
			case 2:
				tiers = new MenageCommun();
				break;
			case 3:
				AutreCommunaute ac = new AutreCommunaute();
				ac.setNom("sa");
				tiers = ac;
				break;
			case 4:
				tiers = new CollectiviteAdministrative();
				break;
			case 5:
				tiers = new Entreprise();
				tiers.setNumero((long) 1000 + i);
				break;
			case 6:
				tiers = new DebiteurPrestationImposable();
				break;
			default:
				fail();
				tiers = null;
				break;
			}

			LOGGER.debug("Enregistrement du tiers=" + tiers);
			tiers.setLogCreationUser("User XXX"); // Cet appel ne sert a rien, la valeur de la session est mise a la place
			tiers = dao.save(tiers);

			if (tiers instanceof PersonnePhysique) {
				ppPrecedent = (PersonnePhysique) tiers;
			}

			// Adresses Postales
			Set<AdresseTiers> adressesPostales = new HashSet<AdresseTiers>();

			AdresseSuisse adresseSuisse = new AdresseSuisse();
			adresseSuisse.setDateDebut(RegDate.get(2005, 2, 1));
			adresseSuisse.setUsage(TypeAdresseTiers.COURRIER);
			adresseSuisse.setComplement("supplement");
			adresseSuisse.setNumeroMaison("12b");
			adresseSuisse.setNumeroAppartement("6bis");
			adresseSuisse.setNumeroOrdrePoste(5423);
			adresseSuisse.setNumeroRue(87321);
			adressesPostales.add(adresseSuisse);

			AdresseEtrangere adresseEtrangere = new AdresseEtrangere();
			adresseEtrangere.setDateDebut(RegDate.get(2005, 2, 1));
			adresseEtrangere.setUsage(TypeAdresseTiers.DOMICILE);
			adresseEtrangere.setRue("Chemin des moineaux");
			adresseEtrangere.setComplementLocalite("Paris");
			adresseEtrangere.setNumeroMaison("87");
			adresseEtrangere.setNumeroPostalLocalite("65000 cedex");
			adresseEtrangere.setNumeroOfsPays(543);
			adressesPostales.add(adresseEtrangere);

			tiers.setAdressesTiers(adressesPostales);

			// Rapports entre tiers
			if (tiers instanceof MenageCommun) {
				// Nécessaire à la validation des fors sur le ménage commun
				MenageCommun mc = (MenageCommun) tiers;
				RapportEntreTiers rapport = dao.save(new AppartenanceMenage(RegDate.get(2005, 8, 12), null, ppPrecedent, mc));
				mc.addRapportObjet(rapport);
			}

			// Fors fiscaux
			Set<ForFiscal> fors = new HashSet<ForFiscal>();

			{
				ForFiscalAutreImpot forFiscal = new ForFiscalAutreImpot();
				forFiscal.setGenreImpot(GenreImpot.DROIT_MUTATION);
				forFiscal.setDateDebut(RegDate.get(2006, 6, 1));
				forFiscal.setDateFin(RegDate.get(2007, 2, 28));
				forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
				forFiscal.setNumeroOfsAutoriteFiscale(1001234);
				fors.add(forFiscal);
			}

			{
				ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
				forFiscal.setDateDebut(RegDate.get(2005, 8, 12));
				forFiscal.setDateFin(RegDate.get(2007, 2, 28));
				forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
				forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.PAYS_HS);
				forFiscal.setNumeroOfsAutoriteFiscale(1001234);
				forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
				fors.add(forFiscal);
			}

			{
				ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
				forFiscal.setDateDebut(RegDate.get(2007, 3, 1));
				forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
				forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
				forFiscal.setNumeroOfsAutoriteFiscale(563);
				forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
				fors.add(forFiscal);
			}

			if (tiers instanceof Contribuable) {
				Contribuable contribuable = (Contribuable) tiers;
				contribuable.setForsFiscaux(fors);
			}

			Long id = tiers.getId();
			assertNotNull(id);
		}

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetRapportCouple() throws Exception {

		loadDatabase();

		Tiers ctb1 = dao.get(10006789L);
		Tiers ctb2 = dao.get(10007890L);
		Tiers couple = dao.get(10008901L);

		// CTB1
		{
			assertEquals(0, ctb1.getRapportsObjet().size());
			RapportEntreTiers rap = ctb1.getRapportSujetValidAt(null, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
			assertEquals(ctb1.getId(), rap.getSujetId());
			assertEquals(couple.getId(), rap.getObjetId());
		}

		// CTB2
		{
			assertEquals(0, ctb2.getRapportsObjet().size());
			RapportEntreTiers rap = ctb2.getRapportSujetValidAt(null, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
			assertEquals(ctb2.getId(), rap.getSujetId());
			assertEquals(couple.getId(), rap.getObjetId());
		}

		// Couple
		{
			Set<RapportEntreTiers> objets = couple.getRapportsObjet();
			Set<RapportEntreTiers> sujets = couple.getRapportsSujet();
			assertEquals(2, objets.size());
			assertEquals(0, sujets.size());

			Iterator<RapportEntreTiers> iter = objets.iterator();
			RapportEntreTiers rctb1 = iter.next();
			RapportEntreTiers rctb2 = iter.next();

			assertEquals(couple.getId(), rctb1.getObjetId());
			if (rctb1.getSujetId().equals(10006789L)) {
				assertEquals(new Long(10006789L), rctb1.getSujetId());
				assertEquals(new Long(10007890L), rctb2.getSujetId());
			}
			else {
				assertEquals(new Long(10007890L), rctb1.getSujetId());
				assertEquals(new Long(10006789L), rctb2.getSujetId());
			}
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testInsertMenageCommun() throws Exception {

		final class Numeros {
			Long numeroCtb1;
			Long numeroCtb2;
			Long numeroMenage;
		}

		Numeros numeros = doInNewTransaction(new TxCallback<Numeros>() {
			@Override
			public Numeros execute(TransactionStatus status) throws Exception {

				Numeros numeros = new Numeros();

				// Pour le rattachement
				{
					// CTB 1
					PersonnePhysique ctb1;
					{
						PersonnePhysique ctb = new PersonnePhysique(true);
						ctb1 = ctb;
						ctb.setNumeroIndividu(12345L);

						// For fermé
						HashSet<ForFiscal> fors = new HashSet<ForFiscal>();
						ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
						forFiscal.setDateDebut(RegDate.get(2002, 1, 1));
						forFiscal.setDateFin(RegDate.get(2006, 11, 30));
						forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
						forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
						forFiscal.setNumeroOfsAutoriteFiscale(563);
						forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
						forFiscal.setMotifOuverture(MotifFor.ARRIVEE_HC);
						forFiscal.setMotifFermeture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
						fors.add(forFiscal);
						ctb.setForsFiscaux(fors);

						LOGGER.debug("Enregistrement du tiers=" + ctb);
						ctb1 = (PersonnePhysique) dao.save(ctb1);
						numeros.numeroCtb1 = ctb1.getNumero();
					}

					// CTB 2
					PersonnePhysique ctb2;
					{
						PersonnePhysique ctb = new PersonnePhysique(true);
						ctb2 = ctb;
						ctb.setNumeroIndividu(23456L);

						// For fermé
						HashSet<ForFiscal> fors = new HashSet<ForFiscal>();
						ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
						forFiscal.setDateDebut(RegDate.get(2004, 1, 1));
						forFiscal.setDateFin(RegDate.get(2006, 11, 30));
						forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
						forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
						forFiscal.setNumeroOfsAutoriteFiscale(876);
						forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
						forFiscal.setMotifOuverture(MotifFor.ARRIVEE_HC);
						forFiscal.setMotifFermeture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
						fors.add(forFiscal);
						ctb.setForsFiscaux(fors);

						LOGGER.debug("Enregistrement du tiers=" + ctb);
						ctb2 = (PersonnePhysique) dao.save(ctb2);
						numeros.numeroCtb2 = ctb2.getNumero();
					}

					// setComplete();
					// endTransaction();
					// startNewTransaction();

					// Menage
					MenageCommun menage;
					{
						menage = new MenageCommun();
						menage = (MenageCommun) dao.save(menage);

						numeros.numeroMenage = menage.getNumero();
					}

					// Rattachement
					RapportEntreTiers rapport1;
					RapportEntreTiers rapport2;
					{
						// CTB1 <=> Couple
						rapport1 = new AppartenanceMenage(RegDate.get(2002, 2, 1), null, ctb1, menage);
						rapport1 = dao.save(rapport1);
						menage.addRapportObjet(rapport1);

						// CTB2 <=> Couple
						rapport2 = new AppartenanceMenage(RegDate.get(2002, 2, 1), null, ctb2, menage);
						rapport2 = dao.save(rapport2);
						menage.addRapportObjet(rapport2);
					}

					// For ouvert sur le ménage (on ne peut l'ajouter qu'après avoir définit les rapport-entre-tiers, autrement le
					// ménage-commun ne valide pas)
					ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
					forFiscal.setDateDebut(RegDate.get(2006, 12, 1));
					forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
					forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
					forFiscal.setNumeroOfsAutoriteFiscale(563);
					forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
					forFiscal.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
					menage.addForFiscal(forFiscal);
				}
				return numeros;
			}
		});

		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 3, dao.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 2, dao.getCount(RapportEntreTiers.class));
			assertEquals("Nombre de fors incorrect", 3, dao.getCount(ForFiscal.class));
		}

		{
			PersonnePhysique ctb1 = (PersonnePhysique) dao.get(numeros.numeroCtb1);
			PersonnePhysique ctb2 = (PersonnePhysique) dao.get(numeros.numeroCtb2);
			MenageCommun menage = (MenageCommun) dao.get(numeros.numeroMenage);

			assertTrue(ctb1.getRapportsObjet() == null || ctb1.getRapportsObjet().isEmpty());
			assertNotNull(ctb1.getRapportsSujet());
			assertEquals(1L, ctb1.getRapportsSujet().size());
			RapportEntreTiers rapport1 = ctb1.getRapportsSujet().iterator().next();
			assertEquals(ctb1.getId(), rapport1.getSujetId());
			assertEquals(menage.getId(), rapport1.getObjetId());

			assertTrue(ctb2.getRapportsObjet() == null || ctb2.getRapportsObjet().isEmpty());
			assertNotNull(ctb2.getRapportsSujet());
			assertEquals(1L, ctb2.getRapportsSujet().size());
			RapportEntreTiers rapport2 = ctb2.getRapportsSujet().iterator().next();
			assertEquals(ctb2.getId(), rapport2.getSujetId());
			assertEquals(menage.getId(), rapport2.getObjetId());

			assertNotNull(menage.getRapportsObjet());
			assertEquals(2L, menage.getRapportsObjet().size());
			RapportEntreTiers rapportMenage1 = (RapportEntreTiers) menage.getRapportsObjet().toArray()[0];
			RapportEntreTiers rapportMenage2 = (RapportEntreTiers) menage.getRapportsObjet().toArray()[1];
			assertEquals(menage.getId(), rapportMenage1.getObjetId());
			assertEquals(menage.getId(), rapportMenage2.getObjetId());
			assertTrue(rapportMenage1.getSujetId().equals(numeros.numeroCtb1) || rapportMenage1.getSujetId().equals(numeros.numeroCtb2));
			assertTrue(rapportMenage2.getSujetId().equals(numeros.numeroCtb1) || rapportMenage2.getSujetId().equals(numeros.numeroCtb2));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testInsertTutelle() throws Exception {

		final class Tierss {

			PersonnePhysique tuteur;
			PersonnePhysique pupille;
			RapportEntreTiers rapport1;

		}

		// Pour le rattachement
		Tierss tierss = doInNewTransaction(new TxCallback<Tierss>() {
			@Override
			public Tierss execute(TransactionStatus status) throws Exception {

				Tierss tierss = new Tierss();

				// CTB 1
				{
					PersonnePhysique ctb = new PersonnePhysique(true);
					tierss.tuteur = ctb;
					ctb.setNumeroIndividu(12345L);

					// For fermé
					HashSet<ForFiscal> fors = new HashSet<ForFiscal>();
					ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
					forFiscal.setDateDebut(RegDate.get(2002, 1, 1));
					forFiscal.setDateFin(null);
					forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
					forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
					forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
					forFiscal.setNumeroOfsAutoriteFiscale(563);
					forFiscal.setMotifOuverture(MotifFor.ARRIVEE_HC);
					fors.add(forFiscal);
					ctb.setForsFiscaux(fors);

					LOGGER.debug("Enregistrement du tiers=" + ctb);
					tierss.tuteur = (PersonnePhysique) dao.save(tierss.tuteur);
				}

				// CTB 2
				{
					PersonnePhysique ctb = new PersonnePhysique(true);
					tierss.pupille = ctb;
					ctb.setNumeroIndividu(23456L);

					// For fermé
					HashSet<ForFiscal> fors = new HashSet<ForFiscal>();
					ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
					forFiscal.setDateDebut(RegDate.get(2004, 1, 1));
					forFiscal.setDateFin(null);
					forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
					forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
					forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
					forFiscal.setNumeroOfsAutoriteFiscale(876);
					forFiscal.setMotifOuverture(MotifFor.ARRIVEE_HC);
					fors.add(forFiscal);
					ctb.setForsFiscaux(fors);

					LOGGER.debug("Enregistrement du tiers=" + ctb);
					tierss.pupille = (PersonnePhysique) dao.save(tierss.pupille);
				}

				{
					// pupille <=> tuteur
					tierss.rapport1 = addTutelle(tierss.pupille, tierss.tuteur, null, RegDate.get(2002, 2, 1), null);
				}
				return tierss;
			}
		});

		{
			tierss.pupille = dao.getHabitantByNumeroIndividu(23456L);
			tierss.tuteur = dao.getHabitantByNumeroIndividu(12345L);

			assertFalse(tierss.tuteur.getRapportsObjet().isEmpty());
			assertTrue(tierss.tuteur.getRapportsSujet().isEmpty());
			assertEquals(1L, tierss.tuteur.getRapportsObjet().size());
			assertEquals(tierss.pupille.getId(), tierss.tuteur.getRapportsObjet().iterator().next().getSujetId());

			assertTrue(tierss.pupille.getRapportsObjet().isEmpty());
			assertFalse(tierss.pupille.getRapportsSujet().isEmpty());
			assertEquals(1L, tierss.pupille.getRapportsSujet().size());
			assertEquals(tierss.pupille.getId(), tierss.tuteur.getRapportsObjet().iterator().next().getSujetId());
			assertEquals(tierss.tuteur.getId(), tierss.pupille.getRapportsSujet().iterator().next().getObjetId());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSituationFamille() throws Exception {

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				HashSet<SituationFamille> sit = new HashSet<SituationFamille>();
				SituationFamilleMenageCommun s = new SituationFamilleMenageCommun();
				s.setDateDebut(RegDate.get(2002, 11, 1));
				s.setDateFin(RegDate.get(2005, 1, 31));
				s.setNombreEnfants(2);
				s.setTarifApplicable(TarifImpotSource.NORMAL);
				sit.add(s);
				s = new SituationFamilleMenageCommun();
				s.setDateDebut(RegDate.get(2005, 2, 20));
				s.setDateFin(RegDate.get(2007, 12, 31));
				s.setNombreEnfants(3);
				s.setTarifApplicable(TarifImpotSource.DOUBLE_GAIN);
				sit.add(s);

				PersonnePhysique nh1 = new PersonnePhysique(false);
				nh1.setNom("titi");
				nh1.setSituationsFamille(sit);

				dao.save(nh1);
				return null;
			}
		});

		{
			List<Tiers> l = dao.getAll();
			assertEquals(1, l.size());
			PersonnePhysique nh = (PersonnePhysique) l.get(0);
			Set<SituationFamille> sfs = nh.getSituationsFamille();
			assertEquals(2, sfs.size());
			Iterator<SituationFamille> iter = sfs.iterator();
			SituationFamille sf = iter.next();
			if (sf.getNombreEnfants() == 2) {
				sf = iter.next();
			}
			assertEquals(3, sf.getNombreEnfants());
			assertEquals(RegDate.get(2007, 12, 31), sf.getDateFin());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSituationFamilleMapping() throws Exception {

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique nh1 = new PersonnePhysique(false);
				nh1.setNom("nh-un");
				nh1 = (PersonnePhysique) dao.save(nh1);

				PersonnePhysique nh2 = new PersonnePhysique(false);
				nh2.setNom("nh-deux");
				nh2 = (PersonnePhysique) dao.save(nh2);

				MenageCommun mc = new MenageCommun();
				mc = (MenageCommun) dao.save(mc);

				RapportEntreTiers rapport1 = new AppartenanceMenage();
				rapport1.setDateDebut(RegDate.get(2000, 1, 1));
				rapport1.setSujet(nh1);
				rapport1.setObjet(mc);
				rapport1 = dao.save(rapport1);

				RapportEntreTiers rapport2 = new AppartenanceMenage();
				rapport2.setDateDebut(RegDate.get(2000, 1, 1));
				rapport2.setSujet(nh2);
				rapport2.setObjet(mc);
				rapport2 = dao.save(rapport2);

				{
					HashSet<SituationFamille> sit = new HashSet<SituationFamille>();
					SituationFamilleMenageCommun s = new SituationFamilleMenageCommun();
					s.setDateDebut(RegDate.get(2002, 11, 1));
					s.setDateFin(RegDate.get(2005, 1, 31));
					s.setNombreEnfants(2);
					s.setTarifApplicable(TarifImpotSource.NORMAL);
					s.setContribuablePrincipalId(nh2.getId());
					sit.add(s);
					s = new SituationFamilleMenageCommun();
					s.setDateDebut(RegDate.get(2005, 2, 20));
					s.setDateFin(RegDate.get(2007, 12, 31));
					s.setNombreEnfants(3);
					s.setTarifApplicable(TarifImpotSource.DOUBLE_GAIN);
					s.setContribuablePrincipalId(nh2.getId());
					sit.add(s);

					nh1.setSituationsFamille(sit);
				}

				{
					HashSet<SituationFamille> sit = new HashSet<SituationFamille>();
					SituationFamilleMenageCommun s = new SituationFamilleMenageCommun();
					s.setDateDebut(RegDate.get(2002, 11, 1));
					s.setDateFin(RegDate.get(2005, 1, 31));
					s.setNombreEnfants(2);
					s.setTarifApplicable(TarifImpotSource.NORMAL);
					s.setContribuablePrincipalId(nh2.getId());
					sit.add(s);
					s = new SituationFamilleMenageCommun();
					s.setDateDebut(RegDate.get(2005, 2, 20));
					s.setDateFin(RegDate.get(2007, 12, 31));
					s.setNombreEnfants(3);
					s.setTarifApplicable(TarifImpotSource.DOUBLE_GAIN);
					s.setContribuablePrincipalId(nh2.getId());
					sit.add(s);

					nh2.setSituationsFamille(sit);
				}
				return null;
			}
		});

		{
			List<Tiers> l = dao.getAll();
			assertEquals(3, l.size());

			PersonnePhysique nh1 = null;
			PersonnePhysique nh2 = null;
			for (int i = 0; i < 3; i++) {
				Tiers t = l.get(i);
				if (t instanceof PersonnePhysique) {
					PersonnePhysique nh = (PersonnePhysique) t;
					if (nh.getNom().equals("nh-deux")) {
						nh2 = nh;
					}
					else {
						nh1 = nh;
					}
				}
			}

			{
				Set<SituationFamille> sfs1 = nh1.getSituationsFamille();
				assertEquals(2, sfs1.size());

				Iterator<SituationFamille> iter = sfs1.iterator();
				SituationFamilleMenageCommun sf1 = (SituationFamilleMenageCommun) iter.next();
				SituationFamilleMenageCommun sf2 = (SituationFamilleMenageCommun) iter.next();
				if (sf1.getNombreEnfants() == 2) {
					SituationFamilleMenageCommun sf = sf1;
					sf1 = sf2;
					sf2 = sf;
				}
				assertEquals(3, sf1.getNombreEnfants());
				assertEquals(RegDate.get(2007, 12, 31), sf1.getDateFin());
				assertEquals(2, sf2.getNombreEnfants());
				assertEquals(RegDate.get(2002, 11, 1), sf2.getDateDebut());

				assertEquals(nh2.getId(), sf1.getContribuablePrincipalId());
				assertEquals(nh2.getId(), sf2.getContribuablePrincipalId());
			}
			{
				Set<SituationFamille> sfs = nh2.getSituationsFamille();
				assertEquals(2, sfs.size());

				Iterator<SituationFamille> iter = sfs.iterator();
				SituationFamilleMenageCommun sf1 = (SituationFamilleMenageCommun) iter.next();
				SituationFamilleMenageCommun sf2 = (SituationFamilleMenageCommun) iter.next();
				if (sf1.getNombreEnfants() == 2) {
					SituationFamilleMenageCommun sf = sf1;
					sf1 = sf2;
					sf2 = sf;
				}
				assertEquals(3, sf1.getNombreEnfants());
				assertEquals(RegDate.get(2007, 12, 31), sf1.getDateFin());
				assertEquals(2, sf2.getNombreEnfants());
				assertEquals(RegDate.get(2002, 11, 1), sf2.getDateDebut());

				assertEquals(nh2.getId(), sf1.getContribuablePrincipalId());
				assertEquals(nh2.getId(), sf2.getContribuablePrincipalId());
			}
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAddAdresseAutreTiers() throws Exception {

		final class Tierss {
			Long numeroCtb1;
			Long numeroCtb2;
		}

		Tierss tierss = doInNewTransaction(new TxCallback<Tierss>() {
			@Override
			public Tierss execute(TransactionStatus status) throws Exception {

				Tierss tierss = new Tierss();
				{
					PersonnePhysique ctb1 = new PersonnePhysique(true);
					ctb1.setNumeroIndividu(12345L);
					ctb1 = (PersonnePhysique) dao.save(ctb1);
					tierss.numeroCtb1 = ctb1.getNumero();

					PersonnePhysique ctb2 = new PersonnePhysique(true);
					ctb2.setNumeroIndividu(23456L);
					ctb2 = (PersonnePhysique) dao.save(ctb2);
					tierss.numeroCtb2 = ctb2.getNumero();

					AdresseAutreTiers adresse = new AdresseAutreTiers();
					adresse.setDateDebut(RegDate.get(2000, 1, 1));
					adresse.setDateFin(null);
					adresse.setUsage(TypeAdresseTiers.COURRIER);
					adresse.setType(TypeAdresseTiers.COURRIER);
					adresse.setAutreTiersId(ctb2.getId());
					ctb1.addAdresseTiers(adresse);
				}
				return tierss;
			}
		});

		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 2, dao.getCount(Tiers.class));
			assertEquals("Nombre d'adresses tiers incorrect", 1, dao.getCount(AdresseTiers.class));

			final Tiers tiers1 = dao.get(tierss.numeroCtb1);
			assertNotNull(tiers1);

			final Tiers tiers2 = dao.get(tierss.numeroCtb2);
			assertNotNull(tiers2);

			final Set<AdresseTiers> adresses = tiers1.getAdressesTiers();
			assertNotNull(adresses);
			assertEquals(1, adresses.size());

			final AdresseAutreTiers adresseAutreTiers = (AdresseAutreTiers) adresses.iterator().next();
			assertNotNull(adresseAutreTiers);
			assertEquals(tiers1, adresseAutreTiers.getTiers());
			assertEquals(tiers2.getId(), adresseAutreTiers.getAutreTiersId());
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetBatch() throws Exception {
		loadDatabase();

		final List<Long> ids = Arrays.asList(10006789L, 1001234L, 9876L, 10007890L, 10008901L, 10001111L, 2002222L, 10000010L, 10000001L, 10000002L, 10000004L,
				10000005L);
		final Set<Parts> parts = new HashSet<Parts>(Arrays.asList(Parts.ADRESSES, Parts.DECLARATIONS, Parts.FORS_FISCAUX, Parts.RAPPORTS_ENTRE_TIERS, Parts.SITUATIONS_FAMILLE));

		// charge les tiers en passant par la méthode batch (dans une transaction séparée pour éviter de partager la session hibernate)
		final List<Tiers> listBatch = doInNewTransaction(new TxCallback<List<Tiers>>() {
			@Override
			public List<Tiers> execute(TransactionStatus status) throws Exception {
				return dao.getBatch(ids, parts);
			}
		});
		assertNotNull(listBatch);
		assertEquals(ids.size(), listBatch.size());

		// charge les même tiers en passant par la méthode normale
		final List<Tiers> listNormal = new ArrayList<Tiers>();
		for (Long id : ids) {
			Tiers tiers = dao.get(id);
			listNormal.add(tiers);
		}
		assertNotNull(listNormal);
		assertEquals(ids.size(), listNormal.size());

		Comparator<Tiers> c = new Comparator<Tiers>() {
			@Override
			public int compare(Tiers o1, Tiers o2) {
				return o1.getId().compareTo(o2.getId());
			}
		};
		Collections.sort(listNormal, c);
		Collections.sort(listBatch, c);

		// vérifie que les tiers des deux listes sont égaux
		final int count = ids.size();
		for (int i = 0; i < count; ++i) {
			final Tiers normal = listNormal.get(i);
			final Tiers batch = listBatch.get(i);
			assertNotSame(batch, normal); // autrement, il y a un problème de session hibernate et le test ne rime à rien
			assertTiersEquals(normal, batch);
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetBatchMaximumDepasse() throws Exception {
		final int size = 1100;          // la limite d'Oracle se situe à 1000
		final List<Long> ids = new ArrayList<Long>(size);
		for (int i = 0; i < size; ++i) {
			ids.add((long) i);
		}
		final List<Tiers> tiers = dao.getBatch(ids, new HashSet<Parts>(Arrays.asList(Parts.values())));
		assertNotNull(tiers);
		assertEquals(0, tiers.size());      // la base est vide, donc c'est normal, mais l'important c'est qu'il n'y ait pas eu d'explosion
	}

	/**
	 * [UNIREG-1985] on s'assure que les collections des tiers liés ne sont pas remplies avec des HashSet vides lorsque les rapports-entre-tiers sont demandés.
	 */
	@SuppressWarnings("unchecked")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetBatchAvecRapportEntreTiersEtForsFiscaux() throws Exception {

		final HibernateTemplate hibernateTemplate = dao.getHibernateTemplate();

		// Crée un couple normal, assujetti vaudois ordinaire
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final RegDate dateMariage = date(1990, 1, 1);
				final RegDate veilleMariage = dateMariage.getOneDayBefore();

				PersonnePhysique paul = new PersonnePhysique(false);
				paul.setNom("Paul");
				{
					ForFiscalPrincipal f = new ForFiscalPrincipal();
					f.setDateDebut(date(1974, 3, 31));
					f.setMotifOuverture(MotifFor.MAJORITE);
					f.setDateFin(veilleMariage);
					f.setMotifFermeture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
					f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
					f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
					f.setNumeroOfsAutoriteFiscale(5586);
					f.setMotifRattachement(MotifRattachement.DOMICILE);
					f.setModeImposition(ModeImposition.ORDINAIRE);
					paul.addForFiscal(f);
				}
				paul = hibernateTemplate.merge(paul);

				PersonnePhysique janine = new PersonnePhysique(false);
				janine.setNom("Janine");
				{
					ForFiscalPrincipal f = new ForFiscalPrincipal();
					f.setDateDebut(date(1974, 3, 31));
					f.setMotifOuverture(MotifFor.MAJORITE);
					f.setDateFin(veilleMariage);
					f.setMotifFermeture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
					f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
					f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
					f.setNumeroOfsAutoriteFiscale(5586);
					f.setMotifRattachement(MotifRattachement.DOMICILE);
					f.setModeImposition(ModeImposition.ORDINAIRE);
					janine.addForFiscal(f);
				}
				janine = hibernateTemplate.merge(janine);

				MenageCommun menage = hibernateTemplate.merge(new MenageCommun());
				{
					RapportEntreTiers rapport = new AppartenanceMenage();
					rapport.setDateDebut(dateMariage);
					rapport.setObjet(menage);
					rapport.setSujet(paul);
					rapport = hibernateTemplate.merge(rapport);

					menage.addRapportObjet(rapport);
					paul.addRapportSujet(rapport);

					rapport = new AppartenanceMenage();
					rapport.setDateDebut(dateMariage);
					rapport.setObjet(menage);
					rapport.setSujet(janine);
					rapport = hibernateTemplate.merge(rapport);

					menage.addRapportObjet(rapport);
					janine.addRapportSujet(rapport);

					ForFiscalPrincipal f = new ForFiscalPrincipal();
					f.setDateDebut(dateMariage);
					f.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
					f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
					f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
					f.setNumeroOfsAutoriteFiscale(5586);
					f.setMotifRattachement(MotifRattachement.DOMICILE);
					f.setModeImposition(ModeImposition.ORDINAIRE);
					menage.addForFiscal(f);
				}

				return paul.getNumero();
			}
		});

		// Charge le contribuable principal à travers getBatch et en demandant les rapports-entre-tiers (va tirer le ménage commun) et la liste des fors fiscaux
		final List<Tiers> list = dao.getBatch(Arrays.asList(id), new HashSet<Parts>(Arrays.asList(Parts.FORS_FISCAUX, Parts.RAPPORTS_ENTRE_TIERS)));
		assertEquals(1, list.size());

		final PersonnePhysique paul = (PersonnePhysique) list.get(0);
		assertNotNull(paul);
		assertEquals(id, paul.getNumero());

		// Les fors fiscaux doivent être renseignés sur le contribuable demandé
		assertEquals(1, paul.getForsFiscaux().size());

		final Set<RapportEntreTiers> rapports = paul.getRapportsSujet();
		assertEquals(1, rapports.size());

		final AppartenanceMenage rapport0 = (AppartenanceMenage) rapports.iterator().next();
		final MenageCommun menage = (MenageCommun) dao.get(rapport0.getObjetId());
		assertNotNull(menage);

		// [UNIREG-1985] Le ménage commun doit être chargé sans que les fors fiscaux soient initialisés : la collection doit être un persistent set non-initialisé
		final Set<ForFiscal> forsMenage = menage.getForsFiscaux();
		assertNotNull(forsMenage);
		assertTrue(forsMenage instanceof PersistentSet);
		assertFalse(((PersistentSet) forsMenage).wasInitialized());

		// Un appel à size doit lazy-initialiser le persistent set et retourner un for fiscal
		assertEquals(1, forsMenage.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetBatchDebiteursAvecBeaucoupDeSourciers() throws Exception {

		final int size = 1200;

		// crée un débiteur avec 600 sourciers
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
				dpi = (DebiteurPrestationImposable) dao.save(dpi);

				for (int i = 0; i < size; ++i) {
					PersonnePhysique pp = new PersonnePhysique(false);
					pp.setNom(buildNomSourcier(i));
					pp.setDateNaissance(RegDate.get(2000, 1, 1));
					pp = (PersonnePhysique) dao.save(pp);

					RapportPrestationImposable r = new RapportPrestationImposable(RegDate.get(2000, 1, 1), null, pp, dpi);
					dao.save(r);
				}

				return dpi.getNumero();
			}
		});

		// charge le débiteur
		final Set<Parts> parts = new HashSet<Parts>();
		parts.add(Parts.RAPPORTS_ENTRE_TIERS);
		final List<Tiers> tiers = dao.getBatch(Arrays.asList(id), parts);
		assertEquals(1, tiers.size());

		final Tiers t0 = tiers.get(0);
		assertNotNull(t0);
		assertEquals(id, t0.getNumero());

		final Set<RapportEntreTiers> rapports = t0.getRapportsObjet();
		assertNotNull(rapports);
		assertEquals(size, rapports.size());
	}

	private static String buildNomSourcier(int index) {
		final StringBuilder b = new StringBuilder("pp-");
		final String indexStr = Long.toString(index);
		for (char c : indexStr.toCharArray()) {
			b.append((char) ((c - '0') + 'A'));
		}
		return b.toString();
	}

	/**
	 * Vérifie que vider la collection d'identifications de personnes (sur PersonnePhysique) supprime bien les instances de IdentificationPersonne correspondantes.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOrphaningIdentificationPersonne() throws Exception {

		// Crée une personne physique avec deux identifications existantes
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique pp = new PersonnePhysique(false);
				pp.setNom("John");

				final Set<IdentificationPersonne> idents = new HashSet<IdentificationPersonne>();
				{
					final IdentificationPersonne ident = new IdentificationPersonne();
					ident.setCategorieIdentifiant(CategorieIdentifiant.CH_AHV_AVS);
					ident.setIdentifiant("123456-654321");
					idents.add(ident);
				}
				{
					final IdentificationPersonne ident = new IdentificationPersonne();
					ident.setCategorieIdentifiant(CategorieIdentifiant.CH_ZAR_RCE);
					ident.setIdentifiant("abcde-edcba");
					idents.add(ident);
				}
				pp.setIdentificationsPersonnes(idents);
				pp = (PersonnePhysique) dao.save(pp);
				return pp.getNumero();
			}
		});

		// On a bien deux identifications au total dans la base
		assertEquals(2, dao.getCount(IdentificationPersonne.class));

		// Vide la collection d'identifications sur la personne physique
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique pp = (PersonnePhysique) dao.get(id);
				assertNotNull(pp);

				final Set<IdentificationPersonne> idents = pp.getIdentificationsPersonnes();
				assertNotNull(idents);
				assertEquals(2, idents.size());

				idents.clear();
				return null;
			}
		});

		// Les deux identifications précédentes doivent avoir été supprimées
		assertEquals(0, dao.getCount(IdentificationPersonne.class));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetListContribuablesModifies() throws Exception{

		loadDatabase("tiersCtbModifies.xml");
		final Date debut = DateHelper.getDate(2010, 12, 21);

		final Calendar cal = DateHelper.getCalendar(2010, 12, 21);
		cal.add(Calendar.SECOND,6);
		final Date fin1 = cal.getTime();

		List<Long> listId = tiersDAO.getListeCtbModifies(debut,fin1);
		Assert.assertNotNull(listId);
		Assert.assertEquals(1, listId.size());

		cal.add(Calendar.MINUTE, 35);
		final Date fin2 = cal.getTime();

		listId = tiersDAO.getListeCtbModifies(debut,fin2);
		Assert.assertNotNull(listId);
		Assert.assertEquals(2, listId.size());

		cal.add(Calendar.HOUR_OF_DAY, 16);
		final Date fin3 = cal.getTime();

		listId = tiersDAO.getListeCtbModifies(debut,fin3);
		Assert.assertNotNull(listId);
		Assert.assertEquals(3, listId.size());

		cal.add(Calendar.DAY_OF_WEEK, 1);
		final Date fin4 = cal.getTime();

		listId = tiersDAO.getListeCtbModifies(debut,fin4);
		Assert.assertNotNull(listId);
		Assert.assertEquals(4, listId.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetRelatedIdsTiersInconnu() throws Exception {
		Set<Long> ids = tiersDAO.getRelatedIds(12345678, 2);
		assertNotNull(ids);
		assertEquals(1, ids.size());
		assertEquals(Long.valueOf(12345678), ids.iterator().next());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetRelatedIdsTiersNonLies() throws Exception {

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				addNonHabitant("Arnold", "Terminator", date(1945, 3, 4), Sexe.MASCULIN);
				addNonHabitant("Jean", "Quinquin", date(1932, 1, 23), Sexe.MASCULIN);
				final PersonnePhysique gudrun = addNonHabitant("Gudrun", "Schnitzel", date(1939, 9, 11), Sexe.FEMININ);
				return gudrun.getNumero();
			}
		});

		final Set<Long> ids = tiersDAO.getRelatedIds(id, 2);
		assertNotNull(ids);
		assertEquals(1, ids.size());
		assertEquals(id, ids.iterator().next());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetRelatedIdsDeuxTiersLies() throws Exception {

		class Ids {
			long jean;
			long gudrun;
			long menage;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				addNonHabitant("Arnold", "Terminator", date(1945, 3, 4), Sexe.MASCULIN);
				final PersonnePhysique jean = addNonHabitant("Jean", "Quinquin", date(1932, 1, 23), Sexe.MASCULIN);
				final PersonnePhysique gudrun = addNonHabitant("Gudrun", "Schnitzel", date(1939, 9, 11), Sexe.FEMININ);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(jean, gudrun, date(1960, 4, 12), null);
				ids.jean = jean.getNumero();
				ids.gudrun = gudrun.getNumero();
				ids.menage = ensemble.getMenage().getNumero();
				return null;
			}
		});

		// 1 niveau de profondeur
		{
			final Set<Long> related = tiersDAO.getRelatedIds(ids.jean, 1);
			assertNotNull(related);
			assertEquals(2, related.size());
			assertTrue(related.contains(ids.jean));
			assertTrue(related.contains(ids.menage));
		}

		// 2 niveaux de profondeur
		{
			final Set<Long> related = tiersDAO.getRelatedIds(ids.jean, 2);
			assertNotNull(related);
			assertEquals(3, related.size());
			assertTrue(related.contains(ids.jean));
			assertTrue(related.contains(ids.gudrun));
			assertTrue(related.contains(ids.menage));
		}

		// 3 niveaux de profondeur
		{
			final Set<Long> related = tiersDAO.getRelatedIds(ids.jean, 3);
			assertNotNull(related);
			assertEquals(3, related.size());
			assertTrue(related.contains(ids.jean));
			assertTrue(related.contains(ids.gudrun));
			assertTrue(related.contains(ids.menage));
		}
	}

	private void assertTiersEquals(Tiers expected, Tiers actual) {
		assertTrue("Le n°" + expected.getId() + " n'est pas égal au tiers n°" + actual.getId(), expected.equalsTo(actual));
	}

	@SuppressWarnings({"unchecked", "UnusedAssignment"})
	private void loadDatabase() throws Exception {
		TiersBasic.loadDatabase(hibernateTemplate, transactionManager);
	}
}
