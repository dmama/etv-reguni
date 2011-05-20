package ch.vd.uniregctb.acomptes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.ListesResults;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersDAOImpl;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.TiersServiceImpl;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class AcomptesProcessorTest extends BusinessTest {

	public static final Logger LOGGER = Logger.getLogger(AcomptesProcessorTest.class);

	private final static String DB_UNIT_DATA_FILE = "classpath:ch/vd/uniregctb/acomptes/AcomptesProcessorTest.xml";

	private HibernateTemplate hibernateTemplate;
	private AcomptesProcessor processor;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		hibernateTemplate = getBean(HibernateTemplate.class, "hibernateTemplate");

		final PlatformTransactionManager transactionManager = getBean(PlatformTransactionManager.class, "transactionManager");
		final TiersService tiersService = getBean(TiersServiceImpl.class, "tiersService");
		final TiersDAO tiersDAO = getBean(TiersDAOImpl.class, "tiersDAO");
		final ServiceCivilCacheWarmer serviceCivilCacheWarmer = getBean(ServiceCivilCacheWarmer.class, "serviceCivilCacheWarmer");

		// création du processeur à la main de manière à pouvoir appeler les méthodes protégées
		processor = new AcomptesProcessor(hibernateTemplate, tiersService, serviceCivilCacheWarmer, transactionManager, tiersDAO);
	}

	@Test
	public void testCreateIteratorOnIDsOfCtbs() throws Exception {
		loadDatabase(DB_UNIT_DATA_FILE);
		hibernateTemplate.executeWithNewSession(new HibernateCallback<Object>() {
		public Object doInHibernate(Session session) throws HibernateException {
			final Iterator<Long> idIterator = processor.createIteratorOnIDsOfCtbs(session, 2010);
			assertNotNull(idIterator);
			//12600004 à la source ne fait pas partie de la population pour les bases acomptes
			//12600003 qui a un for fermé en 2010 ne fait pas partie de la population pour les bases acomptes
			//12600001 qui a un for principal à l'étranger avec motif de rattachement 'Diplômate étranger' ne fait pas partie de la population pour les bases acomptes
			//For principal vaudois, mode d'imposition ordinaire
			assertNextCtb(idIterator, 12600009L);
			//For principal vaudois, mode d'imposition à la dépense
			assertNextCtb(idIterator, 12900001L);
			//For principal hors canton, for secondaire immeuble
			assertNextCtb(idIterator, 34807810L);
			//For principal hors suisse, for secondaire 'Activité indépendante'
			assertNextCtb(idIterator, 86006202L);

			return null;
		}
	});
	}

	private void assertNextCtb(final Iterator<Long> iter, Long numeroCtbExpected) {
		assertTrue(iter.hasNext());
		final Long actual = iter.next();
		assertNotNull(actual);
		assertEquals(numeroCtbExpected, actual);
	}

	@Test
	@NotTransactional
	public void testMenageFermeAnneePrecedente() throws Exception {

		final class Ids {
			long idMenage;
			long idMonsieur;
			long idMadame;
		}
		final Ids ids = new Ids();

		final int anneeReference = 2010;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		// mise en place
		doInNewTransaction(new TransactionCallback<Object>() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique m = addNonHabitant("Jules", "César", date(1945, 3, 1), Sexe.MASCULIN);
				final PersonnePhysique mme = addNonHabitant("Julie", "César", date(1945, 3, 1), Sexe.FEMININ);

				final RegDate dateClotureMenage = RegDate.get(anneeReference - 1, 12, 5);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(m, mme, date(1968, 5, 5), dateClotureMenage);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, date(1980, 8, 12), MotifFor.ARRIVEE_HC, dateClotureMenage, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Bussigny);
				addForPrincipal(m, dateClotureMenage.getOneDayAfter(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Bex);
				addForPrincipal(mme, dateClotureMenage.getOneDayAfter(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Neuchatel);

				ids.idMonsieur = m.getNumero();
				ids.idMadame= mme.getNumero();
				ids.idMenage= mc.getNumero();

				return null;
			}
		});

		// le contribuable ménage avait un for l'année dernière, donc il sera pris dans la requête initiale ...
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final Iterator<Long> iterator = processor.createIteratorOnIDsOfCtbs(hibernateTemplate.getSessionFactory().getCurrentSession(), anneeReference);
				final List<Long> ctbs = new ArrayList<Long>(10);
				while (iterator.hasNext()) {
					ctbs.add(iterator.next());
				}
				Assert.assertEquals(2, ctbs.size());        // le ménage et monsieur, madame n'ayant pas de for vaudois en propre

				Assert.assertTrue(ctbs.contains(ids.idMonsieur));
				Assert.assertTrue(ctbs.contains(ids.idMenage));
				Assert.assertFalse(ctbs.contains(ids.idMadame));
				return null;
			}
		});

		// ... mais il doit finir en "ignoré non assujetti", pas en "manque les liens d'appartenance ménage" (les liens sont effectivement inactifs au 31.12.2009)
		final AcomptesResults results = processor.run(RegDate.get(), 1, anneeReference, null);
		Assert.assertNotNull(results);

		final List<ListesResults.Erreur> erreurs = results.getListeErreurs();
		Assert.assertNotNull(erreurs);
		if (erreurs.size() > 0) {
			for (ListesResults.Erreur erreur : erreurs) {
				LOGGER.error(String.format("Trouvé erreur sur le contribuable %s : %s (%s)", erreur.noCtb, erreur.getDescriptionRaison(), erreur.details));
			}
			Assert.fail();
		}

		final List<AcomptesResults.InfoContribuableIgnore> infosIgnores = results.getContribuablesIgnores();
		Assert.assertNotNull(infosIgnores);
		Assert.assertEquals(2, infosIgnores.size());        // le contribuable ménage apparait une fois par année fiscale testée (l'année dernière et cette année)
		for (AcomptesResults.InfoContribuableIgnore info : infosIgnores) {
			Assert.assertEquals(ids.idMenage, info.getNumeroCtb());
			Assert.assertTrue(info.getCauseIgnorance().contains("non-assujetti"));
		}

		final List<AcomptesResults.InfoContribuableAssujetti> assujettis = results.getListeContribuablesAssujettis();
		Assert.assertNotNull(assujettis);
		Assert.assertEquals(1, assujettis.size());

		final AcomptesResults.InfoContribuableAssujetti assujetti = assujettis.get(0);
		Assert.assertNotNull(assujetti);
		Assert.assertEquals(ids.idMonsieur, assujetti.getNumeroCtb());
		Assert.assertNotNull(assujetti.getAssujettissementIcc());
		Assert.assertEquals(0, assujetti.getAssujettissementIcc().ofsForsSecondaires.size());
		Assert.assertNotNull(assujetti.getAssujettissementIfd());
		Assert.assertEquals(0, assujetti.getAssujettissementIfd().ofsForsSecondaires.size());
	}

	@Test
	@NotTransactional
	public void testForsSecondaires() throws Exception {

		final long idIndividuVaudoisSansForSecondaire = 3564712513467L;
		final long idIndividuVaudoisAvecForSecondaireMemeCommune = 867782441236782L;
		final long idIndividuVaudoisAvecForSecondaireCommuneDifferente = 325612431L;
		final long idIndividuVaudoisAvecDeuxForsSecondaires = 26734522L;

		final class Ids {
			long idVaudoisSansForSecondaire;
			long idVaudoisAvecForSecondaireMemeCommune;
			long idVaudoisAvecForSecondaireCommuneDifferente;
			long idVaudoisAvecDeuxForsSecondaires;
			long idHorsCanton;
			long idHorsSuisse;
		}

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(idIndividuVaudoisSansForSecondaire, date(1974, 11, 4), "Romanova", "Anasthasia", false);
				addIndividu(idIndividuVaudoisAvecForSecondaireMemeCommune, date(1972, 1, 31), "Granger", "Hermione", false);
				addIndividu(idIndividuVaudoisAvecForSecondaireCommuneDifferente, date(1973, 5, 12), "Weasley", "Ronald", true);
				addIndividu(idIndividuVaudoisAvecDeuxForsSecondaires, date(1970, 2, 24), "Weasley", "Percy", true);
			}
		});

		final int anneeAcomptes = 2011;

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				// vaudois sans for secondaire (en fait, il en a un, mais celui-ci n'est pas valide au moment du calcul des acomptes)
				final PersonnePhysique vdSans = addHabitant(idIndividuVaudoisSansForSecondaire);
				addForPrincipal(vdSans, date(2005, 5, 12), MotifFor.ARRIVEE_HS, MockCommune.Aigle);
				addForSecondaire(vdSans, date(2005, 12, 1), MotifFor.ACHAT_IMMOBILIER, date(anneeAcomptes - 1, 12, 25), MotifFor.VENTE_IMMOBILIER, MockCommune.Lonay.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

				// vaudois avec un for secondaire sur la même commune que celle du for principal
				final PersonnePhysique vdAvecMeme = addHabitant(idIndividuVaudoisAvecForSecondaireMemeCommune);
				addForPrincipal(vdAvecMeme, date(2005, 5, 12), MotifFor.ARRIVEE_HS, MockCommune.Aigle);
				addForSecondaire(vdAvecMeme, date(2005, 5, 12), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aigle.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

				// vaudois avec un for secondaire sur une autre commune que celle du for principal
				final PersonnePhysique vdAvecAutre = addHabitant(idIndividuVaudoisAvecForSecondaireCommuneDifferente);
				addForPrincipal(vdAvecAutre, date(2005, 5, 12), MotifFor.ARRIVEE_HS, MockCommune.Aigle);
				addForSecondaire(vdAvecAutre, date(2005, 5, 12), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

				// vaudois avec deux fors secondaires
				final PersonnePhysique vdAvecDeux = addHabitant(idIndividuVaudoisAvecDeuxForsSecondaires);
				addForPrincipal(vdAvecDeux, date(2005, 5, 12), MotifFor.ARRIVEE_HS, MockCommune.Aigle);
				addForSecondaire(vdAvecDeux, date(2005, 5, 12), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				addForSecondaire(vdAvecDeux, date(2008, 5, 12), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bex.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

				// hors-canton
				final PersonnePhysique hc = addNonHabitant("Gaspard", "Lekanar", date(1980, 10, 25), Sexe.MASCULIN);
				addForPrincipal(hc, date(2006, 7, 11), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(hc, date(2006, 7, 11), MotifFor.ACHAT_IMMOBILIER, MockCommune.Croy.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

				// hors-Suisse
				final PersonnePhysique hs = addNonHabitant("Lucie", "Lafourmi", date(1985, 8, 1), Sexe.FEMININ);
				addForPrincipal(hs, date(2006, 11, 1), MotifFor.ACHAT_IMMOBILIER, MockPays.France);
				addForSecondaire(hs, date(2006, 11, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lonay.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				addForSecondaire(hs, date(2008, 12, 25), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

				final Ids ids = new Ids();
				ids.idVaudoisSansForSecondaire = vdSans.getNumero();
				ids.idVaudoisAvecForSecondaireMemeCommune = vdAvecMeme.getNumero();
				ids.idVaudoisAvecForSecondaireCommuneDifferente = vdAvecAutre.getNumero();
				ids.idVaudoisAvecDeuxForsSecondaires = vdAvecDeux.getNumero();
				ids.idHorsCanton = hc.getNumero();
				ids.idHorsSuisse = hs.getNumero();
				return ids;
			}
		});

		final AcomptesResults results = processor.run(RegDate.get(), 1, anneeAcomptes, null);
		Assert.assertNotNull(results);

		final List<AcomptesResults.InfoContribuableAssujetti> assujettis = results.getListeContribuablesAssujettis();
		Assert.assertNotNull(assujettis);
		Assert.assertEquals(6, assujettis.size());

		// vaudois sans for secondaire (en fait, il en a un, mais celui-ci n'est pas valide au moment du calcul des acomptes)
		{
			final AcomptesResults.InfoContribuableAssujetti ctb = assujettis.get(0);
			Assert.assertEquals(ids.idVaudoisSansForSecondaire, ctb.getNumeroCtb());
			Assert.assertNotNull(ctb.getAssujettissementIcc());
			Assert.assertEquals(0, ctb.getAssujettissementIcc().ofsForsSecondaires.size());
			Assert.assertNotNull(ctb.getAssujettissementIfd());
			Assert.assertEquals(0, ctb.getAssujettissementIfd().ofsForsSecondaires.size());
		}

		// vaudois avec un for secondaire sur la même commune que celle du for principal
		{
			final AcomptesResults.InfoContribuableAssujetti ctb = assujettis.get(1);
			Assert.assertEquals(ids.idVaudoisAvecForSecondaireMemeCommune, ctb.getNumeroCtb());
			Assert.assertNotNull(ctb.getAssujettissementIcc());
			Assert.assertEquals(1, ctb.getAssujettissementIcc().ofsForsSecondaires.size());
			Assert.assertEquals(MockCommune.Aigle.getNoOFSEtendu(), (long) ctb.getAssujettissementIcc().noOfsForPrincipal);
			Assert.assertTrue(ctb.getAssujettissementIcc().ofsForsSecondaires.contains(MockCommune.Aigle.getNoOFSEtendu()));
			Assert.assertNotNull(ctb.getAssujettissementIfd());
			Assert.assertEquals(1, ctb.getAssujettissementIfd().ofsForsSecondaires.size());
			Assert.assertEquals(MockCommune.Aigle.getNoOFSEtendu(), (long) ctb.getAssujettissementIfd().noOfsForPrincipal);
			Assert.assertTrue(ctb.getAssujettissementIfd().ofsForsSecondaires.contains(MockCommune.Aigle.getNoOFSEtendu()));
		}

		// vaudois avec un for secondaire sur une autre commune que celle du for principal
		{
			final AcomptesResults.InfoContribuableAssujetti ctb = assujettis.get(2);
			Assert.assertEquals(ids.idVaudoisAvecForSecondaireCommuneDifferente, ctb.getNumeroCtb());
			Assert.assertNotNull(ctb.getAssujettissementIcc());
			Assert.assertEquals(1, ctb.getAssujettissementIcc().ofsForsSecondaires.size());
			Assert.assertEquals(MockCommune.Aigle.getNoOFSEtendu(), (long) ctb.getAssujettissementIcc().noOfsForPrincipal);
			Assert.assertTrue(ctb.getAssujettissementIcc().ofsForsSecondaires.contains(MockCommune.Aubonne.getNoOFSEtendu()));
			Assert.assertNotNull(ctb.getAssujettissementIfd());
			Assert.assertEquals(1, ctb.getAssujettissementIfd().ofsForsSecondaires.size());
			Assert.assertEquals(MockCommune.Aigle.getNoOFSEtendu(), (long) ctb.getAssujettissementIfd().noOfsForPrincipal);
			Assert.assertTrue(ctb.getAssujettissementIfd().ofsForsSecondaires.contains(MockCommune.Aubonne.getNoOFSEtendu()));
		}

		// vaudois avec deux fors secondaires
		{
			final AcomptesResults.InfoContribuableAssujetti ctb = assujettis.get(3);
			Assert.assertEquals(ids.idVaudoisAvecDeuxForsSecondaires, ctb.getNumeroCtb());
			Assert.assertNotNull(ctb.getAssujettissementIcc());
			Assert.assertEquals(2, ctb.getAssujettissementIcc().ofsForsSecondaires.size());
			Assert.assertEquals(MockCommune.Aigle.getNoOFSEtendu(), (long) ctb.getAssujettissementIcc().noOfsForPrincipal);
			Assert.assertTrue(ctb.getAssujettissementIcc().ofsForsSecondaires.contains(MockCommune.Aubonne.getNoOFSEtendu()));
			Assert.assertTrue(ctb.getAssujettissementIcc().ofsForsSecondaires.contains(MockCommune.Bex.getNoOFSEtendu()));
			Assert.assertNotNull(ctb.getAssujettissementIfd());
			Assert.assertEquals(2, ctb.getAssujettissementIfd().ofsForsSecondaires.size());
			Assert.assertEquals(MockCommune.Aigle.getNoOFSEtendu(), (long) ctb.getAssujettissementIfd().noOfsForPrincipal);
			Assert.assertTrue(ctb.getAssujettissementIfd().ofsForsSecondaires.contains(MockCommune.Aubonne.getNoOFSEtendu()));
			Assert.assertTrue(ctb.getAssujettissementIfd().ofsForsSecondaires.contains(MockCommune.Bex.getNoOFSEtendu()));
		}

		// hors-canton
		{
			final AcomptesResults.InfoContribuableAssujetti ctb = assujettis.get(4);
			Assert.assertEquals(ids.idHorsCanton, ctb.getNumeroCtb());
			Assert.assertNotNull(ctb.getAssujettissementIcc());
			Assert.assertEquals(1, ctb.getAssujettissementIcc().ofsForsSecondaires.size());
			Assert.assertNull(ctb.getAssujettissementIcc().noOfsForPrincipal);
			Assert.assertTrue(ctb.getAssujettissementIcc().ofsForsSecondaires.contains(MockCommune.Croy.getNoOFSEtendu()));
			Assert.assertNull(ctb.getAssujettissementIfd());
		}

		// hors-Suisse
		{
			final AcomptesResults.InfoContribuableAssujetti ctb = assujettis.get(5);
			Assert.assertEquals(ids.idHorsSuisse, ctb.getNumeroCtb());
			Assert.assertNotNull(ctb.getAssujettissementIcc());
			Assert.assertEquals(2, ctb.getAssujettissementIcc().ofsForsSecondaires.size());
			Assert.assertNull(ctb.getAssujettissementIcc().noOfsForPrincipal);
			Assert.assertTrue(ctb.getAssujettissementIcc().ofsForsSecondaires.contains(MockCommune.Lonay.getNoOFSEtendu()));
			Assert.assertTrue(ctb.getAssujettissementIcc().ofsForsSecondaires.contains(MockCommune.Bussigny.getNoOFSEtendu()));
			Assert.assertNotNull(ctb.getAssujettissementIfd());
			Assert.assertEquals(2, ctb.getAssujettissementIfd().ofsForsSecondaires.size());
			Assert.assertNull(ctb.getAssujettissementIfd().noOfsForPrincipal);
			Assert.assertTrue(ctb.getAssujettissementIfd().ofsForsSecondaires.contains(MockCommune.Lonay.getNoOFSEtendu()));
			Assert.assertTrue(ctb.getAssujettissementIfd().ofsForsSecondaires.contains(MockCommune.Bussigny.getNoOFSEtendu()));
		}
	}
}