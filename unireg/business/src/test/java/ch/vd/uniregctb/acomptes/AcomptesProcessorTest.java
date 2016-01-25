package ch.vd.uniregctb.acomptes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.ListesResults;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersDAOImpl;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.TiersServiceImpl;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AcomptesProcessorTest extends BusinessTest {

	public static final Logger LOGGER = LoggerFactory.getLogger(AcomptesProcessorTest.class);

	private AcomptesProcessor processor;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		final TiersService tiersService = getBean(TiersServiceImpl.class, "tiersService");
		final TiersDAO tiersDAO = getBean(TiersDAOImpl.class, "tiersDAO");
		final ServiceCivilCacheWarmer serviceCivilCacheWarmer = getBean(ServiceCivilCacheWarmer.class, "serviceCivilCacheWarmer");
		final AssujettissementService assujettissementService = getBean(AssujettissementService.class, "assujettissementService");
		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");

		// création du processeur à la main de manière à pouvoir appeler les méthodes protégées
		processor = new AcomptesProcessor(hibernateTemplate, tiersService, serviceCivilCacheWarmer, transactionManager, tiersDAO, assujettissementService, adresseService);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCreateIteratorOnIDsOfCtbs() throws Exception {

		 class Ids {
			 final long idOrdinaire;
			 final long idDepense;
			 final long idHcImmeuble;
			 final long idHsActiviteIndependante;
			 final long idSourcierPur;
			 final long idOrdinaireParti2009;
			 final long idOrdinaireParti2008;
			 final long idDiplomateEtranger;
			 final long idSourcierMixte1;
			 final long idSourcierMixte2;

			 private Ids(long idOrdinaire, long idDepense, long idHcImmeuble, long idHsActiviteIndependante, long idSourcierPur,
			             long idOrdinaireParti2009, long idOrdinaireParti2008, long idDiplomateEtranger, long idSourcierMixte1, long idSourcierMixte2) {
				 this.idOrdinaire = idOrdinaire;
				 this.idDepense = idDepense;
				 this.idHcImmeuble = idHcImmeuble;
				 this.idHsActiviteIndependante = idHsActiviteIndependante;
				 this.idSourcierPur = idSourcierPur;
				 this.idOrdinaireParti2009 = idOrdinaireParti2009;
				 this.idOrdinaireParti2008 = idOrdinaireParti2008;
				 this.idDiplomateEtranger = idDiplomateEtranger;
				 this.idSourcierMixte1 = idSourcierMixte1;
				 this.idSourcierMixte2 = idSourcierMixte2;
			 }
		 }

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final PersonnePhysique ordinaire = addNonHabitant("Vaudois", "Ordinaire", null, Sexe.MASCULIN);
				addForPrincipal(ordinaire, date(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

				final PersonnePhysique depense = addNonHabitant("Vaudois", "Dépense", null, Sexe.FEMININ);
				addForPrincipal(depense, date(2001, 4, 26), MotifFor.ARRIVEE_HS, null, null, MockCommune.VufflensLaVille, MotifRattachement.DOMICILE, ModeImposition.DEPENSE);

				final PersonnePhysique hcImmeuble = addNonHabitant("Bernois", "Immeuble", null, Sexe.MASCULIN);
				addForPrincipal(hcImmeuble, date(2002, 7, 9), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(hcImmeuble, date(2002, 7, 9), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aigle.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				final PersonnePhysique hsActiviteIndependante = addNonHabitant("Allemand", "Activité Indépendante", null, Sexe.FEMININ);
				addForPrincipal(hsActiviteIndependante, date(2003, 12, 3), MotifFor.DEBUT_EXPLOITATION, MockPays.Allemagne);
				addForSecondaire(hsActiviteIndependante, date(2003, 12, 3), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);

				final PersonnePhysique sourcier = addNonHabitant("Vaudois", "Sourcier Pur", null, Sexe.MASCULIN);
				addForPrincipal(sourcier, date(2004, 11, 24), MotifFor.ARRIVEE_HS, null, null, MockCommune.Renens, MotifRattachement.DOMICILE, ModeImposition.SOURCE);

				final PersonnePhysique ordinaireParti2009 = addNonHabitant("Vaudois", "Ordinaire Parti 2009", null, Sexe.FEMININ);
				addForPrincipal(ordinaireParti2009, date(2005, 8, 1), MotifFor.ARRIVEE_HS, date(2009, 12, 28), MotifFor.DEPART_HS, MockCommune.Bex);

				final PersonnePhysique ordinaireParti2008 = addNonHabitant("Vaudois", "Ordinaire Parti 2008", null, Sexe.FEMININ);
				addForPrincipal(ordinaireParti2008, date(2005, 8, 1), MotifFor.ARRIVEE_HS, date(2008, 12, 28), MotifFor.DEPART_HS, MockCommune.Bex);

				final PersonnePhysique diplomateEtranger = addNonHabitant("Diplomate", "Etranger", null, Sexe.MASCULIN);
				addForPrincipal(diplomateEtranger, date(2006, 6, 13), MotifFor.INDETERMINE, null, null, MockPays.Colombie, MotifRattachement.DIPLOMATE_ETRANGER);

				final PersonnePhysique sourcierMixte1 = addNonHabitant("Vaudois", "Mixte 1", null, Sexe.FEMININ);
				addForPrincipal(sourcierMixte1, date(2007, 4, 25), MotifFor.ARRIVEE_HS, null, null, MockCommune.Bex, MotifRattachement.DOMICILE, ModeImposition.MIXTE_137_1);
				addForSecondaire(sourcierMixte1, date(2007, 4, 25), MotifFor.ARRIVEE_HS, MockCommune.Bex.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				final PersonnePhysique sourcierMixte2 = addNonHabitant("Vaudois", "Mixte 2", null, Sexe.MASCULIN);
				addForPrincipal(sourcierMixte2, date(2008, 5, 23), MotifFor.ARRIVEE_HS, null, null, MockCommune.Bussigny, MotifRattachement.DOMICILE, ModeImposition.MIXTE_137_2);

				return new Ids(ordinaire.getNumero(), depense.getNumero(), hcImmeuble.getNumero(), hsActiviteIndependante.getNumero(),
				               sourcier.getNumero(), ordinaireParti2009.getNumero(), ordinaireParti2008.getNumero(), diplomateEtranger.getNumero(),
				               sourcierMixte1.getNumero(), sourcierMixte2.getNumero());
			}
		});

		hibernateTemplate.executeWithNewSession(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException {
				final Iterator<Long> idIterator = processor.createIteratorOnIDsOfCtbs(session, 2010);
				assertNotNull(idIterator);

				// les sourciers purs et les diplomates étrangers ne font pas partie de la base acompte

				//For principal vaudois, mode d'imposition ordinaire
				assertNextCtb(idIterator, ids.idOrdinaire);
				//For principal vaudois, mode d'imposition à la dépense
				assertNextCtb(idIterator, ids.idDepense);
				//For principal hors canton, for secondaire immeuble
				assertNextCtb(idIterator, ids.idHcImmeuble);
				//For principal hors suisse, for secondaire 'Activité indépendante'
				assertNextCtb(idIterator, ids.idHsActiviteIndependante);
				//Contribuable parti en 2009, donc assujetti à l'IFD 2009
				assertNextCtb(idIterator, ids.idOrdinaireParti2009);
				//Sourcier mixte 1
				assertNextCtb(idIterator, ids.idSourcierMixte1);

				// un peu de log pour comprendre qui il y a en plus...
				if (idIterator.hasNext()) {
					final StringBuilder b = new StringBuilder();
					while (idIterator.hasNext()) {
						if (b.length() > 0) {
							b.append(", ");
						}
						b.append(idIterator.next());
					}
					fail("Encore des contribuables trouvés ? (" + b.toString() + ')');
				}
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
			@Override
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
			@Override
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final Iterator<Long> iterator = processor.createIteratorOnIDsOfCtbs(getCurrentSession(), anneeReference);
				final List<Long> ctbs = new ArrayList<>(10);
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
		if (!erreurs.isEmpty()) {
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
				addForSecondaire(vdSans, date(2005, 12, 1), MotifFor.ACHAT_IMMOBILIER, date(anneeAcomptes - 1, 12, 25), MotifFor.VENTE_IMMOBILIER, MockCommune.Lonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				// vaudois avec un for secondaire sur la même commune que celle du for principal
				final PersonnePhysique vdAvecMeme = addHabitant(idIndividuVaudoisAvecForSecondaireMemeCommune);
				addForPrincipal(vdAvecMeme, date(2005, 5, 12), MotifFor.ARRIVEE_HS, MockCommune.Aigle);
				addForSecondaire(vdAvecMeme, date(2005, 5, 12), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aigle.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				// vaudois avec un for secondaire sur une autre commune que celle du for principal
				final PersonnePhysique vdAvecAutre = addHabitant(idIndividuVaudoisAvecForSecondaireCommuneDifferente);
				addForPrincipal(vdAvecAutre, date(2005, 5, 12), MotifFor.ARRIVEE_HS, MockCommune.Aigle);
				addForSecondaire(vdAvecAutre, date(2005, 5, 12), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				// vaudois avec deux fors secondaires
				final PersonnePhysique vdAvecDeux = addHabitant(idIndividuVaudoisAvecDeuxForsSecondaires);
				addForPrincipal(vdAvecDeux, date(2005, 5, 12), MotifFor.ARRIVEE_HS, MockCommune.Aigle);
				addForSecondaire(vdAvecDeux, date(2005, 5, 12), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				addForSecondaire(vdAvecDeux, date(2008, 5, 12), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bex.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				// hors-canton
				final PersonnePhysique hc = addNonHabitant("Gaspard", "Lekanar", date(1980, 10, 25), Sexe.MASCULIN);
				addForPrincipal(hc, date(2006, 7, 11), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(hc, date(2006, 7, 11), MotifFor.ACHAT_IMMOBILIER, MockCommune.Croy.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				// hors-Suisse
				final PersonnePhysique hs = addNonHabitant("Lucie", "Lafourmi", date(1985, 8, 1), Sexe.FEMININ);
				addForPrincipal(hs, date(2006, 11, 1), MotifFor.ACHAT_IMMOBILIER, MockPays.France);
				addForSecondaire(hs, date(2006, 11, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				addForSecondaire(hs, date(2008, 12, 25), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

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
			Assert.assertEquals(MockCommune.Aigle.getNoOFS(), (long) ctb.getAssujettissementIcc().noOfsForPrincipal);
			Assert.assertTrue(ctb.getAssujettissementIcc().ofsForsSecondaires.contains(MockCommune.Aigle.getNoOFS()));
			Assert.assertNotNull(ctb.getAssujettissementIfd());
			Assert.assertEquals(1, ctb.getAssujettissementIfd().ofsForsSecondaires.size());
			Assert.assertEquals(MockCommune.Aigle.getNoOFS(), (long) ctb.getAssujettissementIfd().noOfsForPrincipal);
			Assert.assertTrue(ctb.getAssujettissementIfd().ofsForsSecondaires.contains(MockCommune.Aigle.getNoOFS()));
		}

		// vaudois avec un for secondaire sur une autre commune que celle du for principal
		{
			final AcomptesResults.InfoContribuableAssujetti ctb = assujettis.get(2);
			Assert.assertEquals(ids.idVaudoisAvecForSecondaireCommuneDifferente, ctb.getNumeroCtb());
			Assert.assertNotNull(ctb.getAssujettissementIcc());
			Assert.assertEquals(1, ctb.getAssujettissementIcc().ofsForsSecondaires.size());
			Assert.assertEquals(MockCommune.Aigle.getNoOFS(), (long) ctb.getAssujettissementIcc().noOfsForPrincipal);
			Assert.assertTrue(ctb.getAssujettissementIcc().ofsForsSecondaires.contains(MockCommune.Aubonne.getNoOFS()));
			Assert.assertNotNull(ctb.getAssujettissementIfd());
			Assert.assertEquals(1, ctb.getAssujettissementIfd().ofsForsSecondaires.size());
			Assert.assertEquals(MockCommune.Aigle.getNoOFS(), (long) ctb.getAssujettissementIfd().noOfsForPrincipal);
			Assert.assertTrue(ctb.getAssujettissementIfd().ofsForsSecondaires.contains(MockCommune.Aubonne.getNoOFS()));
		}

		// vaudois avec deux fors secondaires
		{
			final AcomptesResults.InfoContribuableAssujetti ctb = assujettis.get(3);
			Assert.assertEquals(ids.idVaudoisAvecDeuxForsSecondaires, ctb.getNumeroCtb());
			Assert.assertNotNull(ctb.getAssujettissementIcc());
			Assert.assertEquals(2, ctb.getAssujettissementIcc().ofsForsSecondaires.size());
			Assert.assertEquals(MockCommune.Aigle.getNoOFS(), (long) ctb.getAssujettissementIcc().noOfsForPrincipal);
			Assert.assertTrue(ctb.getAssujettissementIcc().ofsForsSecondaires.contains(MockCommune.Aubonne.getNoOFS()));
			Assert.assertTrue(ctb.getAssujettissementIcc().ofsForsSecondaires.contains(MockCommune.Bex.getNoOFS()));
			Assert.assertNotNull(ctb.getAssujettissementIfd());
			Assert.assertEquals(2, ctb.getAssujettissementIfd().ofsForsSecondaires.size());
			Assert.assertEquals(MockCommune.Aigle.getNoOFS(), (long) ctb.getAssujettissementIfd().noOfsForPrincipal);
			Assert.assertTrue(ctb.getAssujettissementIfd().ofsForsSecondaires.contains(MockCommune.Aubonne.getNoOFS()));
			Assert.assertTrue(ctb.getAssujettissementIfd().ofsForsSecondaires.contains(MockCommune.Bex.getNoOFS()));
		}

		// hors-canton
		{
			final AcomptesResults.InfoContribuableAssujetti ctb = assujettis.get(4);
			Assert.assertEquals(ids.idHorsCanton, ctb.getNumeroCtb());
			Assert.assertNotNull(ctb.getAssujettissementIcc());
			Assert.assertEquals(1, ctb.getAssujettissementIcc().ofsForsSecondaires.size());
			Assert.assertNull(ctb.getAssujettissementIcc().noOfsForPrincipal);
			Assert.assertTrue(ctb.getAssujettissementIcc().ofsForsSecondaires.contains(MockCommune.Croy.getNoOFS()));
			Assert.assertNull(ctb.getAssujettissementIfd());
		}

		// hors-Suisse
		{
			final AcomptesResults.InfoContribuableAssujetti ctb = assujettis.get(5);
			Assert.assertEquals(ids.idHorsSuisse, ctb.getNumeroCtb());
			Assert.assertNotNull(ctb.getAssujettissementIcc());
			Assert.assertEquals(2, ctb.getAssujettissementIcc().ofsForsSecondaires.size());
			Assert.assertNull(ctb.getAssujettissementIcc().noOfsForPrincipal);
			Assert.assertTrue(ctb.getAssujettissementIcc().ofsForsSecondaires.contains(MockCommune.Lonay.getNoOFS()));
			Assert.assertTrue(ctb.getAssujettissementIcc().ofsForsSecondaires.contains(MockCommune.Bussigny.getNoOFS()));
			Assert.assertNotNull(ctb.getAssujettissementIfd());
			Assert.assertEquals(2, ctb.getAssujettissementIfd().ofsForsSecondaires.size());
			Assert.assertNull(ctb.getAssujettissementIfd().noOfsForPrincipal);
			Assert.assertTrue(ctb.getAssujettissementIfd().ofsForsSecondaires.contains(MockCommune.Lonay.getNoOFS()));
			Assert.assertTrue(ctb.getAssujettissementIfd().ofsForsSecondaires.contains(MockCommune.Bussigny.getNoOFS()));
		}
	}

	@Test
	public void testSourcierMixte1() throws Exception {

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastair", "Rogers", null, Sexe.MASCULIN);
				addForPrincipal(pp, date(2000, 1, 1), MotifFor.ARRIVEE_HS, null, null, MockCommune.Bex, MotifRattachement.DOMICILE, ModeImposition.MIXTE_137_1);
				return pp.getId();
			}
		});

		final AcomptesResults results = processor.run(RegDate.get(), 1, 2010, null);
		Assert.assertNotNull(results);

		final List<AcomptesResults.InfoContribuableAssujetti> assujettis = results.getListeContribuablesAssujettis();
		Assert.assertNotNull(assujettis);
		Assert.assertEquals(1, assujettis.size());

		final AcomptesResults.InfoContribuableAssujetti assujetti = assujettis.get(0);
		Assert.assertNotNull(assujetti);
		Assert.assertEquals(id, assujetti.getNumeroCtb());
		Assert.assertNotNull(assujetti.getAssujettissementIcc());
		Assert.assertNotNull(assujetti.getAssujettissementIfd());

		// ICC : 2010
		{
			final AcomptesResults.InfoAssujettissementContribuable a = assujetti.getAssujettissementIcc();
			Assert.assertEquals(MockCommune.Bex.getNoOFS(), (long) a.noOfsForPrincipal);
			Assert.assertEquals(2010, a.anneeFiscale);
			Assert.assertEquals(AcomptesResults.TypeContribuableAcompte.VAUDOIS_MIXTE_137_1, a.typeContribuable);
		}
		// IFD : 2009
		{
			final AcomptesResults.InfoAssujettissementContribuable a = assujetti.getAssujettissementIfd();
			Assert.assertEquals(MockCommune.Bex.getNoOFS(), (long) a.noOfsForPrincipal);
			Assert.assertEquals(2009, a.anneeFiscale);
			Assert.assertEquals(AcomptesResults.TypeContribuableAcompte.VAUDOIS_MIXTE_137_1, a.typeContribuable);
		}
	}

	/**
	 * Ce test est là pour décrire le comportement d'unireg en cas de déménagement au 31.12.XXXX lors du calcul
	 * de la population assujettie aux acomptes ICC de l'année XXXX + 1 (= paramètre à passer au batch d'extraction).
	 * <p/>
	 * En effet, la spécification demande de regarder l'assujettissement au 31.12.XXXX, et le code semble plutôt regarder l'assujettissement
	 * l'année suivante. Cela ne pose pas de problème tant que l'année XXXX n'est pas terminée (l'absence de fors ouverts dans le futur
	 * permet de rendre équivalentes les analyses des assujettissements XXXX et XXXX + 1), ce qui est toujours le cas lors des tirs de
	 * novembre et décembre, mais cela n'est plus vrai dès que janvier arrive...
	 * <p/>
	 * D'un autre côté, les acomptes ICC sont justement ceux qui concerneront l'année XXXX + 1, donc la commune qui nous intéresse est bien
	 * celle du for existant en XXXX + 1...
	 * <p/>
	 * En résumé, on ne change rien du comportement actuel et ce test est là pour détecter les éventuels changements futurs et la non-régression.
	 * En particulier, lors d'une fusion de commune (ou d'un déménagement au 31.12...) :
	 * <ul>
	 *     <li>avant que l'année XXXX ne se termine, les fors de l'extraction "XXXX + 1" sont ceux <b>d'avant</b> la fusion/déménagement ;</li>
	 *     <li>une fois l'année XXXX terminée, les fors de l'extraction "XXXX + 1" sont ceux <b>d'après</b> la fusion/déménagement ;</li>
	 * </ul>
	 */
	@Test
	public void testDemenagementFinAnnee() throws Exception {

		final int anneeAcomptes = 2011;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				// déménagement au 31.12 de l'année précédent les acomptes
				final PersonnePhysique pp = addNonHabitant("Carole", "Luciole", date(1985, 8, 1), Sexe.FEMININ);
				addForPrincipal(pp, date(2005, 5, 12), MotifFor.ARRIVEE_HS, date(anneeAcomptes - 1, 12, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Aigle);
				addForPrincipal(pp, date(anneeAcomptes, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
				return pp.getNumero();
			}
		});

		final AcomptesResults results = processor.run(RegDate.get(), 1, anneeAcomptes, null);
		Assert.assertNotNull(results);

		final List<AcomptesResults.InfoContribuableAssujetti> assujettis = results.getListeContribuablesAssujettis();
		Assert.assertNotNull(assujettis);
		Assert.assertEquals(1, assujettis.size());

		// vérification des résultats pour le contribuable
		final AcomptesResults.InfoContribuableAssujetti info = assujettis.get(0);
		Assert.assertNotNull(info);
		Assert.assertEquals(ppId, info.getNumeroCtb());

		final AcomptesResults.InfoAssujettissementContribuable icc = info.getAssujettissementIcc();
		Assert.assertNotNull(icc);
		Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), icc.noOfsForGestion);
		Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), icc.noOfsForPrincipal);
	}

	/**
	 * Cas soulevé par M. Rime du Data Warehouse ACI : contribuable VD parti HS dans l'année précédent les acomptes... Il n'apparaît bien-évidemment pas dans
	 * la partie ICC (plus là en fin d'année), ni dans la partie IFD (pareil), mais il devrait être dans la liste des ignorés, non ?
	 */
	@Test
	public void testDepartHSEnCoursAnnee() throws Exception {

		final int anneeAcomptes = 2011;
		final RegDate dateDepart = date(anneeAcomptes - 1, 8, 24);

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				// départ HS dans l'année n-1
				final PersonnePhysique pp = addNonHabitant("Carole", "Luciole", date(1985, 8, 1), Sexe.FEMININ);
				addForPrincipal(pp, date(2005, 5, 12), MotifFor.ARRIVEE_HS, dateDepart, MotifFor.DEPART_HS, MockCommune.Aigle);
				addForPrincipal(pp, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.Espagne);
				return pp.getNumero();
			}
		});

		final AcomptesResults results = processor.run(RegDate.get(), 1, anneeAcomptes, null);
		Assert.assertNotNull(results);

		final List<AcomptesResults.InfoContribuableAssujetti> assujettis = results.getListeContribuablesAssujettis();
		Assert.assertNotNull(assujettis);
		Assert.assertEquals(0, assujettis.size());

		final List<AcomptesResults.InfoContribuableIgnore> ignores = results.getContribuablesIgnores();
		Assert.assertNotNull(ignores);
		Assert.assertEquals(2, ignores.size());

		// vérification des résultats pour le contribuable ignoré
		final List<AcomptesResults.InfoContribuableIgnore> ignoresTries = new ArrayList<>(ignores);
		Collections.sort(ignoresTries, new Comparator<AcomptesResults.InfoContribuableIgnore>() {
			@Override
			public int compare(AcomptesResults.InfoContribuableIgnore o1, AcomptesResults.InfoContribuableIgnore o2) {
				int compare = Long.compare(o1.getNumeroCtb(), o2.getNumeroCtb());
				if (compare == 0) {
					compare = o1.getAnneeFiscale() - o2.getAnneeFiscale();
				}
				return compare;
			}
		});

		{
			final AcomptesResults.InfoContribuableIgnore info = ignoresTries.get(0);
			Assert.assertNotNull(info);
			Assert.assertEquals(AcomptesResults.InfoContribuableNonAssujetti.class, info.getClass());
			Assert.assertEquals(ppId, info.getNumeroCtb());
			Assert.assertEquals(anneeAcomptes - 1, info.getAnneeFiscale());
		}
		{
			final AcomptesResults.InfoContribuableIgnore info = ignoresTries.get(1);
			Assert.assertNotNull(info);
			Assert.assertEquals(AcomptesResults.InfoContribuableNonAssujetti.class, info.getClass());
			Assert.assertEquals(ppId, info.getNumeroCtb());
			Assert.assertEquals(anneeAcomptes, info.getAnneeFiscale());
		}
	}

	@Test
	public void testAcomptesAnneeCouranteAvecArriveeAnneeAnterieure() throws Exception {
		final int anneeAcomptes = RegDate.get().year();

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		// mise en place fiscale
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastair", "Rogers", null, Sexe.MASCULIN);
				addForPrincipal(pp, date(2000, 1, 1), MotifFor.ARRIVEE_HS, null, null, MockCommune.Bex, MotifRattachement.DOMICILE);
				return pp.getId();
			}
		});

		// calcul des acomptes
		final AcomptesResults results = processor.run(RegDate.get(), 1, anneeAcomptes, null);
		Assert.assertNotNull(results);

		final List<AcomptesResults.InfoContribuableAssujetti> assujettis = results.getListeContribuablesAssujettis();
		Assert.assertNotNull(assujettis);
		Assert.assertEquals(1, assujettis.size());

		final AcomptesResults.InfoContribuableAssujetti ctb = assujettis.get(0);
		Assert.assertEquals(id, ctb.getNumeroCtb());

		Assert.assertNotNull(ctb.getAssujettissementIcc());
		Assert.assertEquals(anneeAcomptes, ctb.getAssujettissementIcc().anneeFiscale);
		Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), ctb.getAssujettissementIcc().noOfsForGestion);
		Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), ctb.getAssujettissementIcc().noOfsForPrincipal);
		Assert.assertEquals(AcomptesResults.TypeContribuableAcompte.VAUDOIS_ORDINAIRE, ctb.getAssujettissementIcc().typeContribuable);
		Assert.assertEquals(0, ctb.getAssujettissementIcc().ofsForsSecondaires.size());

		Assert.assertNotNull(ctb.getAssujettissementIfd());
		Assert.assertEquals(anneeAcomptes - 1, ctb.getAssujettissementIfd().anneeFiscale);
		Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), ctb.getAssujettissementIfd().noOfsForGestion);
		Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), ctb.getAssujettissementIfd().noOfsForPrincipal);
		Assert.assertEquals(AcomptesResults.TypeContribuableAcompte.VAUDOIS_ORDINAIRE, ctb.getAssujettissementIfd().typeContribuable);
		Assert.assertEquals(0, ctb.getAssujettissementIfd().ofsForsSecondaires.size());
	}

	/**
	 * La population arrivée après le début d'année doit être listée quand-même (après coup = rattrapage)
	 */
	@Test
	public void testAcomptesAnneeCouranteAvecArriveeMemeAnnee() throws Exception {
		final int anneeAcomptes = RegDate.get().year();
		final RegDate arrivee = date(anneeAcomptes, 1, 1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		// mise en place fiscale
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastair", "Rogers", null, Sexe.MASCULIN);
				addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HS, null, null, MockCommune.Bex, MotifRattachement.DOMICILE);
				return pp.getId();
			}
		});

		// calcul des acomptes
		final AcomptesResults results = processor.run(RegDate.get(), 1, anneeAcomptes, null);
		Assert.assertNotNull(results);

		final List<AcomptesResults.InfoContribuableAssujetti> assujettis = results.getListeContribuablesAssujettis();
		Assert.assertNotNull(assujettis);
		Assert.assertEquals(1, assujettis.size());

		final AcomptesResults.InfoContribuableAssujetti ctb = assujettis.get(0);
		Assert.assertEquals(id, ctb.getNumeroCtb());

		// assujettissement ICC ok...
		Assert.assertNotNull(ctb.getAssujettissementIcc());
		Assert.assertEquals(anneeAcomptes, ctb.getAssujettissementIcc().anneeFiscale);
		Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), ctb.getAssujettissementIcc().noOfsForGestion);
		Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), ctb.getAssujettissementIcc().noOfsForPrincipal);
		Assert.assertEquals(AcomptesResults.TypeContribuableAcompte.VAUDOIS_ORDINAIRE, ctb.getAssujettissementIcc().typeContribuable);
		Assert.assertEquals(0, ctb.getAssujettissementIcc().ofsForsSecondaires.size());

		// ... mais pas d'IFD (on calcule ici la population pour les acomptes IFD de l'année dernière, et le contribuable n'était pas encore là...)
		Assert.assertNull(ctb.getAssujettissementIfd());
	}
}