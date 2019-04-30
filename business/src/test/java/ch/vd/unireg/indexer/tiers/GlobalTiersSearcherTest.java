package ch.vd.unireg.indexer.tiers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.common.Fuse;
import ch.vd.unireg.indexer.EmptySearchCriteriaException;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.entreprise.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.parametrage.ParametreEnum;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.TiersCriteria.TypeRecherche;
import ch.vd.unireg.tiers.TiersCriteria.TypeTiers;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.NatureJuridique;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.type.TypeGenerationEtatEntreprise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Classe pour tester la recherche de tiers
 *
 * @author <a href="mailto:jean-eric.cuendet@vd.ch">Jean-Eric Cuendet</a>
 */
@SuppressWarnings({"JavaDoc"})
public class GlobalTiersSearcherTest extends BusinessTest {

	public GlobalTiersSearcherTest() {
		setWantIndexationTiers(true);
	}

	private static int getDefaultCollectivitesAdministrativesNumber() {
		return MockCollectiviteAdministrative.getAll().size();
	}

	@Test
	public void testRechercheParNumeroContribuable() throws Exception {

		final class Ids {
			long idNestle;
			long idBcv;
		}

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				addEntreprise(MockEntrepriseFactory.NESTLE);
				addEntreprise(MockEntrepriseFactory.BCV);
			}
		});

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// rien...
			}
		});

		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise nestle = addEntrepriseConnueAuCivil(MockEntrepriseFactory.NESTLE.getNumeroEntreprise());
			final Entreprise bcv = addEntrepriseConnueAuCivil(MockEntrepriseFactory.BCV.getNumeroEntreprise());
			final Ids ids1 = new Ids();
			ids1.idBcv = bcv.getNumero();
			ids1.idNestle = nestle.getNumero();
			return ids1;
		});
		globalTiersIndexer.sync();

		final int c = globalTiersSearcher.getExactDocCount();
		assertEquals(2 + getDefaultCollectivitesAdministrativesNumber(), c);

		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNumero(ids.idNestle);
			final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(1, list.size());
			assertEquals((Long) ids.idNestle, list.get(0).getNumero());
		}
		{
			final TiersCriteria criteria = new TiersCriteria();
			// Le numero prime sur tout le reste
			criteria.setNumero(ids.idBcv);
			// Donc le nom n'est pas utilisé
			criteria.setNomRaison("Bla bla");
			final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(1, list.size());
			assertEquals((Long) ids.idBcv, list.get(0).getNumero());
		}
		{
			final TiersCriteria criteria = new TiersCriteria();
			// Le numero prime sur tout le reste
			criteria.setNumero(ids.idNestle);
			// Donc le type de tiers n'est pas utilisé
			criteria.setTypeTiers(TypeTiers.AUTRE_COMMUNAUTE);
			final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(1, list.size());
			assertEquals((Long) ids.idNestle, list.get(0).getNumero());
		}
		{
			TiersCriteria criteria = new TiersCriteria();
			// Le numero prime sur tout le reste
			criteria.setNumero(ids.idNestle);
			// mais le type de tiers impératif est utilisé
			criteria.setTypeTiersImperatif(TypeTiers.AUTRE_COMMUNAUTE);
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(0, list.size());
		}
		{
			TiersCriteria criteria = new TiersCriteria();
			// Le numero prime sur tout le reste
			criteria.setNumero(1234456L); // Inexistant
			criteria.setNomRaison("Nestlé"); // critère non-utilisé
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(0, list.size());
		}
	}

	@Test
	public void testSearchMenage() throws Exception {

		final class Ids {
			long idmc;
			long idlui;
		}

		final long noIndividuLui = 481L;
		final long noIndividuElle = 451L;
		final RegDate dateMariage = date(2001, 1, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndividuLui, null, "Bolomey", "Marcel", Sexe.MASCULIN);
				final MockIndividu elle = addIndividu(noIndividuElle, null, "Pittet", "Julie", Sexe.FEMININ);
				marieIndividus(lui, elle, dateMariage);
			}
		});

		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addHabitant(noIndividuLui);
			addForPrincipal(lui, date(1998, 7, 12), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

			final PersonnePhysique elle = addHabitant(noIndividuElle);
			addForPrincipal(elle, date(1997, 5, 14), MotifFor.ARRIVEE_HS, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Morges);

			final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
			addForPrincipal(couple.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Moudon);

			final Ids ids1 = new Ids();
			ids1.idlui = lui.getNumero();
			ids1.idmc = couple.getMenage().getNumero();
			return ids1;
		});
		globalTiersIndexer.sync();


		final int c = globalTiersSearcher.getExactDocCount();
		assertEquals(3 + getDefaultCollectivitesAdministrativesNumber(), c);

		// Recherche le couple par numéro
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNumero(ids.idmc);
			final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(1, list.size());
			assertEquals((Long) ids.idmc, list.get(0).getNumero());
		}

		// Recherche le mari par for principal fermé
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Bolomey");
			criteria.setNoOfsFor(String.valueOf(MockCommune.Lausanne.getNoOFS()));
			final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(1, list.size());
			final TiersIndexedData data = list.get(0);
			assertEquals("Lausanne", data.getForPrincipal());
			assertEquals((Long) ids.idlui, data.getNumero());
		}

		// Recherche le couple par for principal
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Bolomey");
			criteria.setNoOfsFor(String.valueOf(MockCommune.Moudon.getNoOFS()));
			final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(1, list.size());
			final TiersIndexedData data = list.get(0);
			// String nom1 = data.getNom1();
			// String nom2 = data.getNom2();
			assertEquals("Moudon", data.getForPrincipal());
			assertEquals((Long) ids.idmc, data.getNumero());
		}

		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Bolomey");
			final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(2, list.size());

			final List<TiersIndexedData> listeTriee = new ArrayList<>(list);
			Collections.sort(listeTriee, new Comparator<TiersIndexedData>() {
				@Override
				public int compare(TiersIndexedData o1, TiersIndexedData o2) {
					return Long.compare(o1.getNumero(), o2.getNumero());
				}
			});
			final TiersIndexedData marcel = listeTriee.get(0);
			final TiersIndexedData couple = listeTriee.get(1);

			assertEquals(Long.valueOf(ids.idlui), marcel.getNumero());
			assertEquals("Marcel Bolomey", marcel.getNom1());
			assertEquals(Long.valueOf(ids.idmc), couple.getNumero());
			assertEquals("Marcel Bolomey", couple.getNom1());
			assertEquals("Julie Pittet", couple.getNom2());
		}
	}

	@Test
	public void testRechercheParNumeroAVS() throws Exception {

		final class Ids {
			long idelle;
			long idlui;
		}

		final long noIndividuLui = 481L;
		final long noIndividuElle = 451L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndividuLui, null, "Bolomey", "Marcel", Sexe.MASCULIN);
				lui.setNoAVS11("98765432109");

				final MockIndividu elle = addIndividu(noIndividuElle, null, "Pittet", "Julie", Sexe.FEMININ);
				elle.setNoAVS11("11111111113");
				elle.setNouveauNoAVS("1234567891023");
			}
		});

		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addHabitant(noIndividuLui);
			addForPrincipal(lui, date(1998, 7, 12), MotifFor.MAJORITE, MockCommune.Lausanne);

			final PersonnePhysique elle = addHabitant(noIndividuElle);
			addForPrincipal(elle, date(1997, 5, 14), MotifFor.ARRIVEE_HS, MockCommune.Morges);

			final Ids ids1 = new Ids();
			ids1.idlui = lui.getNumero();
			ids1.idelle = elle.getNumero();
			return ids1;
		});
		globalTiersIndexer.sync();

		final int c = globalTiersSearcher.getExactDocCount();
		assertEquals(2 + getDefaultCollectivitesAdministrativesNumber(), c);

		// Ancien numero
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNumeroAVS("987.65.432.109");
			final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			//dumpResults(list);
			assertEquals(1, list.size());
			assertEquals((Long) ids.idlui, list.get(0).getNumero());
		}
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNumeroAVS("98765432109");
			final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			//dumpResults(list);
			assertEquals(1, list.size());
			assertEquals((Long) ids.idlui, list.get(0).getNumero());
		}
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNumeroAVS("987654321");
			final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			//dumpResults(list);
			assertEquals(0, list.size());
		}

		// Nouveau numero
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNumeroAVS("1234567891023");
			final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			//dumpResults(list);
			assertEquals(1, list.size());
			assertEquals((Long) ids.idelle, list.get(0).getNumero());
		}
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNumeroAVS("12345678910");
			final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			//dumpResults(list);
			assertEquals(0, list.size());
		}
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNumeroAVS("123.4567.8910.23");
			final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			//dumpResults(list);
			assertEquals(1, list.size());
			assertEquals((Long) ids.idelle, list.get(0).getNumero());
		}
	}

	private void rechercheParTypeTiers(String nom, TypeTiers type, int expected) throws Exception {

		final TiersCriteria criteria = new TiersCriteria() {
			@Override
			public boolean isEmpty() {
				// on veut pouvoir exécuter les recherches sans limitation
				return false;
			}
		};
		criteria.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.CONTIENT);
		criteria.setNomRaison(nom);
		criteria.setTypeTiers(type);
		List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
		//dumpResults(list);
		assertEquals(expected, list.size());
	}

	// Recherche sur le type de tiers
	@Test
	public void testRechercheParTypeTiers() throws Exception {

		final long noIndividuDespont = 4278234L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividuDespont, null, "Despont", "Alain", Sexe.MASCULIN);
			}
		});

		doInNewTransactionAndSession(status -> {
			addHabitant(noIndividuDespont);

			final PersonnePhysique marcel = addNonHabitant("Marcel", "Bolomido", null, Sexe.MASCULIN);
			final PersonnePhysique claudine = addNonHabitant("Claudine", "Desplatanes", null, Sexe.FEMININ);
			addEnsembleTiersCouple(marcel, claudine, date(2012, 4, 14), null);

			addDebiteur(CategorieImpotSource.CREANCIERS_HYPOTHECAIRES, PeriodiciteDecompte.ANNUEL, date(2009, 1, 1));
			return null;
		});
		globalTiersIndexer.sync();

		final int c = globalTiersSearcher.getExactDocCount();
		assertEquals(5 + getDefaultCollectivitesAdministrativesNumber(), c);

		rechercheParTypeTiers("despont", TypeTiers.HABITANT, 1); // Despont Alain
		rechercheParTypeTiers(null, TypeTiers.NON_HABITANT, 2);
		rechercheParTypeTiers("desp", TypeTiers.PERSONNE_PHYSIQUE, 2); // Despont Alain et Desplatanes Claudine
		rechercheParTypeTiers("desp", TypeTiers.CONTRIBUABLE, 3); // Despont Alain, Desplatanes Claudine + (Bolomido Marcel & Desplatanes Claudine)
		rechercheParTypeTiers(null, TypeTiers.DEBITEUR_PRESTATION_IMPOSABLE, 1);
		rechercheParTypeTiers(null, TypeTiers.COLLECTIVITE_ADMINISTRATIVE, getDefaultCollectivitesAdministrativesNumber());
	}

	@Test
	public void testNatureJuridique() throws Exception {

		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Alfredo", "Parnentiel", null, Sexe.MASCULIN);
			return pp.getNumero();
		});
		globalTiersIndexer.sync();

		final int c = globalTiersSearcher.getExactDocCount();
		assertEquals(1 + getDefaultCollectivitesAdministrativesNumber(), c);

		// Tiers par Nature Juridique
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Parnentiel");
			criteria.setNatureJuridique(NatureJuridique.PP.name());
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(1, list.size());

			TiersIndexedData data = list.get(0);
			assertEquals(Long.valueOf(ppId), data.getNumero());
		}
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Parnentiel");
			criteria.setNatureJuridique(NatureJuridique.PM.name());
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(0, list.size());
		}
	}

	@Test
	public void testRechercheParAdresse() throws Exception {

		final long noIndividuAndre = 436742L;
		final long noIndividuMartine = 81541897L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu andre = addIndividu(noIndividuAndre, null, "Duval", "André", Sexe.MASCULIN);
				addAdresse(andre, TypeAdresseCivil.COURRIER, MockRue.Prilly.RueDesMetiers, null, date(2001, 1, 1), null);

				final MockIndividu martine = addIndividu(noIndividuMartine, null, "Duval", "Martine", Sexe.FEMININ);
				addAdresse(martine, TypeAdresseCivil.COURRIER, MockRue.Renens.QuatorzeAvril, null, date(2001, 1, 1), null);
			}
		});

		doInNewTransactionAndSession(status -> {
			addHabitant(noIndividuAndre);
			addHabitant(noIndividuMartine);
			return null;
		});
		globalTiersIndexer.sync();

		final int c = globalTiersSearcher.getExactDocCount();
		assertEquals(2 + getDefaultCollectivitesAdministrativesNumber(), c);

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setLocaliteOuPays("Prilly");
		List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
		assertEquals(1, list.size());
	}

	@Test
	public void testRechercheParNpa() throws Exception {

		final long noIndividuRichard = 436742L;
		final long noIndividuClaudine = 81541897L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu richard = addIndividu(noIndividuRichard, null, "Bolomey", "Richard", Sexe.MASCULIN);
				addAdresse(richard, TypeAdresseCivil.COURRIER, MockRue.Prilly.RueDesMetiers, null, date(2001, 1, 1), null);

				final MockIndividu claudine = addIndividu(noIndividuClaudine, null, "Duchene", "Claudine", Sexe.FEMININ);
				addAdresse(claudine, TypeAdresseCivil.COURRIER, MockRue.Renens.QuatorzeAvril, null, date(2001, 1, 1), null);
				marieIndividu(claudine, date(2001, 1, 1));
			}
		});

		final class Ids {
			long idMarcel;
			long idRichard;
			long idClaudine;
			long idMenage;
		}

		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique marcel = addNonHabitant("Marcel", "Bolomido", null, Sexe.MASCULIN);
			final PersonnePhysique richard = addHabitant(noIndividuRichard);
			final PersonnePhysique claudine = addHabitant(noIndividuClaudine);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(marcel, claudine, date(2001, 1, 1), null);

			final Ids ids1 = new Ids();
			ids1.idMarcel = marcel.getNumero();
			ids1.idRichard = richard.getNumero();
			ids1.idClaudine = claudine.getNumero();
			ids1.idMenage = couple.getMenage().getNumero();
			return ids1;
		});
		globalTiersIndexer.sync();

		final int c = globalTiersSearcher.getExactDocCount();
		assertEquals(4 + getDefaultCollectivitesAdministrativesNumber(), c);

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNpaCourrier(String.valueOf(MockLocalite.Renens.getNPA()));
		final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);

		assertEquals(2, list.size());
		final Set<Long> found = new HashSet<>();
		for (TiersIndexedData data : list) {
			found.add(data.getNumero());
		}
		assertTrue(found.contains(Long.valueOf(ids.idMenage))); // ménage commun Bolomido Marcel + Duchene Claudine -> mr non habitant le ménage prend l'adresse de mme
		assertTrue(found.contains(Long.valueOf(ids.idClaudine))); // habitant Duchene Claudine
	}

	@Test
	public void testRechercheVideException() throws Exception {

		try {
			TiersCriteria criteria = new TiersCriteria();
			globalTiersSearcher.search(criteria);
			fail();
		}
		catch (Exception e) {
			assertEquals("Les critères de recherche sont vides", e.getMessage());
		}
	}

	@Test
	public void testRechercheParAdresseZeroTrouve() throws Exception {

		final long noIndividuAndre = 436742L;
		final long noIndividuMartine = 81541897L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu andre = addIndividu(noIndividuAndre, null, "Duval", "André", Sexe.MASCULIN);
				addAdresse(andre, TypeAdresseCivil.COURRIER, MockRue.Prilly.RueDesMetiers, null, date(2001, 1, 1), null);

				final MockIndividu martine = addIndividu(noIndividuMartine, null, "Duval", "Martine", Sexe.FEMININ);
				addAdresse(martine, TypeAdresseCivil.COURRIER, MockRue.Renens.QuatorzeAvril, null, date(2001, 1, 1), null);
			}
		});

		doInNewTransactionAndSession(status -> {
			addHabitant(noIndividuAndre);
			addHabitant(noIndividuMartine);
			return null;
		});
		globalTiersIndexer.sync();

		final int c = globalTiersSearcher.getExactDocCount();
		assertEquals(2 + getDefaultCollectivitesAdministrativesNumber(), c);

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setLocaliteOuPays("Montreux"); // Ne devrait pas etre trouvé!
		final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
		assertEmpty(list);
	}

	@Test
	public void testRechercheParFors() throws Exception {

		final long noIndividuAndre = 436742L;
		final long noIndividuMartine = 81541897L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu andre = addIndividu(noIndividuAndre, null, "Duval", "André", Sexe.MASCULIN);
				addAdresse(andre, TypeAdresseCivil.COURRIER, MockRue.Prilly.RueDesMetiers, null, date(2001, 1, 1), null);

				final MockIndividu martine = addIndividu(noIndividuMartine, null, "Duval", "Martine", Sexe.FEMININ);
				addAdresse(martine, TypeAdresseCivil.COURRIER, MockRue.Renens.QuatorzeAvril, null, date(2001, 1, 1), null);
			}
		});

		final long idFred = doInNewTransactionAndSession(status -> {
			final PersonnePhysique andre = addHabitant(noIndividuAndre);
			addForPrincipal(andre, date(2005, 4, 2), MotifFor.ARRIVEE_HS, MockCommune.Prilly);

			final PersonnePhysique martine = addHabitant(noIndividuMartine);
			addForPrincipal(martine, date(2005, 4, 2), MotifFor.ARRIVEE_HS, MockCommune.Renens);

			final PersonnePhysique fred = addNonHabitant("Fred", "Gnagna", null, Sexe.MASCULIN);
			addForPrincipal(fred, date(2001, 4, 7), MotifFor.INDETERMINE, MockCommune.Prilly);
			return fred.getNumero();
		});
		globalTiersIndexer.sync();

		final int c = globalTiersSearcher.getExactDocCount();
		assertEquals(3 + getDefaultCollectivitesAdministrativesNumber(), c);

		final TiersCriteria criteria = new TiersCriteria();

		// 2 tiers à Prilly
		{
			criteria.setNoOfsFor(String.valueOf(MockCommune.Prilly.getNoOFS()));
			final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(2, list.size());
		}

		// 1 tiers à Renens
		{
			criteria.setNoOfsFor(String.valueOf(MockCommune.Renens.getNoOFS()));
			final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(1, list.size());
		}

		// 1 tiers pour recherche par numéro (= le for est ignoré)
		{
			criteria.setNumero(idFred);
			criteria.setNoOfsFor(String.valueOf(MockCommune.Renens.getNoOFS()));
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(1, list.size());
			TiersIndexedData data = list.get(0);
			assertEquals("Prilly", data.getForPrincipal());
		}
	}

	@Test
	public void testRechercheParDateNaissance() throws Exception {

		final long noIndividuAndre = 436742L;
		final long noIndividuMartine = 81541897L;
		final RegDate dateNaissanceAndre = date(1965, 4, 12);
		final RegDate dateNaissanceMartine = date(1966, 4, 30);
		final RegDate dateNaissanceFred = date(1966, 4, 22);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu andre = addIndividu(noIndividuAndre, dateNaissanceAndre, "Duval", "André", Sexe.MASCULIN);
				addAdresse(andre, TypeAdresseCivil.COURRIER, MockRue.Prilly.RueDesMetiers, null, date(2001, 1, 1), null);

				final MockIndividu martine = addIndividu(noIndividuMartine, dateNaissanceMartine, "Duval", "Martine", Sexe.FEMININ);
				addAdresse(martine, TypeAdresseCivil.COURRIER, MockRue.Renens.QuatorzeAvril, null, date(2001, 1, 1), null);
			}
		});

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique andre = addHabitant(noIndividuAndre);
			addForPrincipal(andre, date(2005, 4, 2), MotifFor.ARRIVEE_HS, MockCommune.Prilly);

			final PersonnePhysique martine = addHabitant(noIndividuMartine);
			addForPrincipal(martine, date(2005, 4, 2), MotifFor.ARRIVEE_HS, MockCommune.Renens);

			final PersonnePhysique fred = addNonHabitant("Fred", "Gnagna", dateNaissanceFred, Sexe.MASCULIN);
			addForPrincipal(fred, date(2001, 4, 7), MotifFor.INDETERMINE, MockCommune.Prilly);
			return null;
		});
		globalTiersIndexer.sync();

		int c = globalTiersSearcher.getExactDocCount();
		assertEquals(3 + getDefaultCollectivitesAdministrativesNumber(), c);

		// Recherche sur la date de naissance (complète)
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setDateNaissanceInscriptionRC(dateNaissanceAndre);

			final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(1, list.size());
			final TiersIndexedData data = list.get(0);
			assertEquals(dateNaissanceAndre, RegDateHelper.indexStringToDate(data.getDateNaissanceInscriptionRC()));
			assertEquals("André Duval", data.getNom1());
		}
		// Recherche sur la date de naissance (partielle)
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setDateNaissanceInscriptionRC(RegDate.get(1966, 4));

			final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(2, list.size());

			final List<TiersIndexedData> listeTriee = new ArrayList<>(list);
			Collections.sort(listeTriee, new Comparator<TiersIndexedData>() {
				@Override
				public int compare(TiersIndexedData o1, TiersIndexedData o2) {
					return o1.getDateNaissanceInscriptionRC().compareTo(o2.getDateNaissanceInscriptionRC());
				}
			});
			{
				final TiersIndexedData data = listeTriee.get(0);
				assertEquals(dateNaissanceFred, RegDateHelper.indexStringToDate(data.getDateNaissanceInscriptionRC()));
				assertEquals("Fred Gnagna", data.getNom1());
			}
			{
				final TiersIndexedData data = listeTriee.get(1);
				assertEquals(dateNaissanceMartine, RegDateHelper.indexStringToDate(data.getDateNaissanceInscriptionRC()));
				assertEquals("Martine Duval", data.getNom1());
			}
		}
	}

	/**
	 * Effectue une recherche complexe basée sur le nom.
	 */
	@Test
	public void testRechercheNomContient() throws Exception {

		final long noIndividuAndre = 436742L;
		final long noIndividuMartine = 81541897L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu andre = addIndividu(noIndividuAndre, null, "Duval", "André", Sexe.MASCULIN);
				addAdresse(andre, TypeAdresseCivil.COURRIER, MockRue.Prilly.RueDesMetiers, null, date(2001, 1, 1), null);

				final MockIndividu martine = addIndividu(noIndividuMartine, null, "Duval", "Martine", Sexe.FEMININ);
				addAdresse(martine, TypeAdresseCivil.COURRIER, MockRue.Renens.QuatorzeAvril, null, date(2001, 1, 1), null);
			}
		});

		final class Ids {
			long idAndre;
			long idMartine;
			long idDpi;
		}

		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique andre = addHabitant(noIndividuAndre);
			addForPrincipal(andre, date(2005, 4, 2), MotifFor.ARRIVEE_HS, MockCommune.Prilly);

			final PersonnePhysique martine = addHabitant(noIndividuMartine);
			addForPrincipal(martine, date(2005, 4, 2), MotifFor.ARRIVEE_HS, MockCommune.Renens);

			final PersonnePhysique fred = addNonHabitant("Fred", "Gnagna", null, Sexe.MASCULIN);
			addForPrincipal(fred, date(2001, 4, 7), MotifFor.INDETERMINE, MockCommune.Prilly);

			final DebiteurPrestationImposable dpi = addDebiteur("Debiteur IS", martine, date(2010, 5, 1));

			final Ids ids1 = new Ids();
			ids1.idAndre = andre.getNumero();
			ids1.idMartine = martine.getNumero();
			ids1.idDpi = dpi.getNumero();
			return ids1;
		});
		globalTiersIndexer.sync();

		final int c = globalTiersSearcher.getExactDocCount();
		assertEquals(4 + getDefaultCollectivitesAdministrativesNumber(), c);

		// Recherche "contient" sur le nom
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("duv");
		criteria.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.CONTIENT);
		final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
		assertEquals(3, list.size());

		final List<TiersIndexedData> listeTriee = new ArrayList<>(list);
		Collections.sort(listeTriee, new Comparator<TiersIndexedData>() {
			@Override
			public int compare(TiersIndexedData o1, TiersIndexedData o2) {
				return Long.compare(o1.getNumero(), o2.getNumero());
			}
		});
		TiersIndexedData dpi = listeTriee.get(0);
		TiersIndexedData andre = listeTriee.get(1);
		TiersIndexedData martine = listeTriee.get(2);

		assertEquals(Long.valueOf(ids.idAndre), andre.getNumero());
		assertEquals("André Duval", andre.getNom1());
		assertEquals(Long.valueOf(ids.idMartine), martine.getNumero());
		assertEquals("Martine Duval", martine.getNom1());
		assertEquals(Long.valueOf(ids.idDpi), dpi.getNumero());
		assertEquals("Martine Duval", dpi.getNom1()); // [UNIREG-1376] on va chercher les infos sur le contribuable si elles n'existent pas sur le débiteur
	}

	@Test
	public void testRechercheAutresNoms() throws Exception {

		final class Ids {
			long idAlain;
			long idDpi;
		}

		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique alain = addNonHabitant("Alain", "Despont", null, Sexe.MASCULIN);
			final PersonnePhysique martine = addNonHabitant("Martine", "Dupont", null, Sexe.FEMININ);
			final DebiteurPrestationImposable dpi = addDebiteur("Débiteur", alain, date(2000, 1, 1));

			final Ids ids1 = new Ids();
			ids1.idAlain = alain.getNumero();
			ids1.idDpi = dpi.getNumero();
			return ids1;
		});
		globalTiersIndexer.sync();

		int c = globalTiersSearcher.getExactDocCount();
		assertEquals(3 + getDefaultCollectivitesAdministrativesNumber(), c);

		// Recherche "contient" sur le nom
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("pon ain");
		criteria.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.CONTIENT);

		final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
		assertEquals(2, list.size());

		final List<TiersIndexedData> listeTriee = new ArrayList<>(list);
		Collections.sort(listeTriee, new Comparator<TiersIndexedData>() {
			@Override
			public int compare(TiersIndexedData o1, TiersIndexedData o2) {
				return Long.compare(o1.getNumero(), o2.getNumero());
			}
		});

		final TiersIndexedData debiteur = listeTriee.get(0);
		final TiersIndexedData contribuable = listeTriee.get(1);

		assertEquals(Long.valueOf(ids.idAlain), contribuable.getNumero());
		assertEquals("Alain Despont", contribuable.getNom1());
		assertEquals(Long.valueOf(ids.idDpi), debiteur.getNumero());
		assertEquals("Alain Despont", debiteur.getNom1());
	}

	/**
	 * Effectue une recherche complexe basée sur le nom.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRechercheNomRessemble() throws Exception {

		final class Ids {
			long idAlain;
			long idDpi;
		}

		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique alain = addNonHabitant("Alain", "Despont", null, Sexe.MASCULIN);
			final PersonnePhysique martine = addNonHabitant("Martine", "Dupont", null, Sexe.FEMININ);
			final DebiteurPrestationImposable dpi = addDebiteur("Débiteur", alain, date(2000, 1, 1));

			final Ids ids1 = new Ids();
			ids1.idAlain = alain.getNumero();
			ids1.idDpi = dpi.getNumero();
			return ids1;
		});
		globalTiersIndexer.sync();

		int c = globalTiersSearcher.getExactDocCount();
		assertEquals(3 + getDefaultCollectivitesAdministrativesNumber(), c);

		// Recherche "phonetique" sur le nom
		TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("despant");
		criteria.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.PHONETIQUE);

		final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
		assertEquals(2, list.size());

		final List<TiersIndexedData> listeTriee = new ArrayList<>(list);
		Collections.sort(listeTriee, new Comparator<TiersIndexedData>() {
			@Override
			public int compare(TiersIndexedData o1, TiersIndexedData o2) {
				return Long.compare(o1.getNumero(), o2.getNumero());
			}
		});

		final TiersIndexedData debiteur = listeTriee.get(0);
		final TiersIndexedData contribuable = listeTriee.get(1);

		assertEquals(Long.valueOf(ids.idAlain), contribuable.getNumero());
		assertEquals("Alain Despont", contribuable.getNom1());
		assertEquals(Long.valueOf(ids.idDpi), debiteur.getNumero());
		assertEquals("Alain Despont", debiteur.getNom1());
	}

	/**
	 * Teste la recherche sur le prénom.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSearchIndividuParPrenom() throws Exception {

		final class Ids {
			long idAlain;
			long idDpi;
		}

		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique alain = addNonHabitant("Alain", "Despont", null, Sexe.MASCULIN);
			final PersonnePhysique martine = addNonHabitant("Martine", "Dupont", null, Sexe.FEMININ);
			final DebiteurPrestationImposable dpi = addDebiteur("Débiteur", alain, date(2000, 1, 1));

			final Ids ids1 = new Ids();
			ids1.idAlain = alain.getNumero();
			ids1.idDpi = dpi.getNumero();
			return ids1;
		});
		globalTiersIndexer.sync();

		int c = globalTiersSearcher.getExactDocCount();
		assertEquals(3 + getDefaultCollectivitesAdministrativesNumber(), c);

		// Recherche "phonetique" sur le nom
		TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("alain");
		criteria.setTypeRechercheDuNom(TypeRecherche.EST_EXACTEMENT);

		final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
		assertEquals(2, list.size());

		final List<TiersIndexedData> listeTriee = new ArrayList<>(list);
		Collections.sort(listeTriee, new Comparator<TiersIndexedData>() {
			@Override
			public int compare(TiersIndexedData o1, TiersIndexedData o2) {
				return Long.compare(o1.getNumero(), o2.getNumero());
			}
		});

		final TiersIndexedData debiteur = listeTriee.get(0);
		final TiersIndexedData contribuable = listeTriee.get(1);

		assertEquals(Long.valueOf(ids.idAlain), contribuable.getNumero());
		assertEquals("Alain Despont", contribuable.getNom1());
		assertEquals(Long.valueOf(ids.idDpi), debiteur.getNumero());
		assertEquals("Alain Despont", debiteur.getNom1());
	}

	@Test
	public void testSearchEntreprise() throws Exception {

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				addEntreprise(MockEntrepriseFactory.BANQUE_COOP);
			}
		});

		final long idpm = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseConnueAuCivil(MockEntrepriseFactory.BANQUE_COOP.getNumeroEntreprise());
			addNonHabitant("Roger", "Rabbit", null, Sexe.MASCULIN);
			return entreprise.getNumero();
		});
		globalTiersIndexer.sync();

		final int c = globalTiersSearcher.getExactDocCount();
		assertEquals(2 + getDefaultCollectivitesAdministrativesNumber(), c);
		
		// Par numero
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNumero(idpm);
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(1, list.size());
			assertEquals(Long.valueOf(idpm), list.get(0).getNumero());
		}

		// Par Nature Juridique
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNatureJuridique(NatureJuridique.PM.name());
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(1, list.size());
			assertEquals(Long.valueOf(idpm), list.get(0).getNumero());
		}
	}

	@SuppressWarnings({"unchecked"})
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRechercheTropDeResultats() throws Exception {

		// Le nombre de resultats est limité dans la recherche
		final int nbMaxParListe = new Integer(ParametreEnum.nbMaxParListe.getDefaut());
		final int nbDocs = nbMaxParListe + 20;

		final List<Long> ids = doInNewTransactionAndSession(status -> {
			final List<Long> ids1 = new ArrayList<>(2000);
			for (long i = 0; i < nbDocs; i++) {
				final PersonnePhysique pp = addNonHabitant("Bimbo", "Maluna", date(1970, 1, 1), Sexe.MASCULIN);
				ids1.add(pp.getNumero());
			}
			return ids1;
		});

		globalTiersIndexer.schedule(ids);
		globalTiersIndexer.sync();

		TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("maluNa");
		try {
			globalTiersSearcher.search(criteria);
			fail(); // renvoie trop de resultats
		}
		catch (Exception e) {
			assertContains("Le nombre max de résultats ne peut pas excéder " +
					               ParametreEnum.nbMaxParListe.getDefaut() + ". Hits: " + nbDocs, e.getMessage());
		}
	}

	/**
	 * [UNIREG-2597] Vérifie que la recherche de tous les ids n'est pas limité par le paramètre maxHits.
	 */
	@SuppressWarnings({"unchecked"})
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetAllIds() throws Exception {

		globalTiersIndexer.overwriteIndex();

		// Le nombre de resultats est limité dans la recherche
		final int nbMaxParListe = new Integer(ParametreEnum.nbMaxParListe.getDefaut());
		final int nbDocs = nbMaxParListe + 20;

		final Set<Long> idsDb = doInNewTransactionAndSession(status -> {
			final Set<Long> ids = new HashSet<>();
			for (long i = 0; i < nbDocs; i++) {
				PersonnePhysique pp = addNonHabitant("Alfred", "Fodor", date(1970, 1, 1), Sexe.MASCULIN);
				ids.add(pp.getNumero());
			}
			return ids;
		});

		globalTiersIndexer.sync();

		// La méthode 'getAllIds' ne devrait pas être limitée par le paramètre maxHists, donc les deux ensembles d'ids devraient être égaux
		final Set<Long> idsIndexer = globalTiersSearcher.getAllIds();
		assertNotNull(idsIndexer);
		assertEquals(idsDb.size(), idsIndexer.size());
		assertEquals(idsDb, idsIndexer);
	}

	/**
	 * [UNIREG-2592] Vérifie qu'une recherche avec un nom de raison égal à un espace (" ") ne plante pas.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRechercheNomRaisonUnEspace() throws Exception {

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison(" ");

		try {
			globalTiersSearcher.search(criteria);
			fail("Les critères de recherche sont comme vides...");
		}
		catch (EmptySearchCriteriaException e) {
			// ok...
		}
	}

	/**
	 * [UNIREG-3157] Vérifie que la recherche avec le mode de visualisation limitée fonctionne correctement.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRechercheVisualisationLimitee()  throws Exception {

		class Ids {
			Long ramon;
			Long julien;
		}
		final Ids ids = new Ids();

		// Crée deux ctbs, dont un est un débiteur inactif
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique ramon = addNonHabitant("Ramon", "Zarrate", date(1930, 3, 2), Sexe.MASCULIN);
			ramon.setDebiteurInactif(true);
			addForPrincipal(ramon, date(1980, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockPays.Espagne);
			addForSecondaire(ramon, date(1980, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny, MotifRattachement.IMMEUBLE_PRIVE);
			ids.ramon = ramon.getId();

			final PersonnePhysique julien = addNonHabitant("Julien", "Zarrate", date(1930, 3, 2), Sexe.MASCULIN);
			addForPrincipal(julien, date(1980, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockPays.Espagne);
			addForSecondaire(julien, date(1980, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny, MotifRattachement.IMMEUBLE_PRIVE);
			ids.julien = julien.getId();
			return null;
		});

		globalTiersIndexer.sync();

		// Effectue une recherche avec un type de visualisation limité : seuls les débiteur actifs doivent être retournés
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setTypeVisualisation(TiersCriteria.TypeVisualisation.LIMITEE);
		criteria.setNomRaison("Zarrate");

		final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals("Julien Zarrate", list.get(0).getNom1());
	}

	/**
	 * Encode les 0-9 en A-J pour éviter de mettre des chiffres dans les noms/prénoms des personnes physiques
	 */
	private static String encodeDigitsInName(String originalName) {
		final StringBuilder b = new StringBuilder();
		for (char c : originalName.toCharArray()) {
			if (Character.isDigit(c)) {
				b.append((char) (c - '0' + 'A'));
			}
			else {
				b.append(c);
			}
		}
		return b.toString();
	}

	/**
	 * [UNIREG-1386] Vérifie que le moteur de recherche supprime automatiquement les termes trop communs sur le champ 'nom/raison' lorsqu'une exception BooleanQuery.TooManyClause est levée par lucene.
	 */
	@SuppressWarnings({"unchecked"})
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRechercheCriteresTropCommuns() throws Exception {

		final List<Long> ids = doInNewTransactionAndSession(status -> {
			final List<Long> ids1 = new ArrayList<>(2000);

			// Charge 2000 personnes dans l'index. Ces 2000 personnes possèdent toutes un nom de famille commençant par "Du Pont".
			for (int i = 0; i < 2000; ++i) {

				final String nom;
				final String prenom;
				final String localite;

				if (i == 0) {
					// Cas spécial pour le premier
					nom = "Du Pont";
					prenom = "Michel";
					localite = "Romanel-s-Morges";
				}
				else {
					nom = String.format("Du Pont%04d", i); // "Du Pont0001".."Du Pont1999"
					prenom = String.format("Michel%02d", i % 50); // 40 * (Michel01..Michel49)
					localite = String.format("Romanel-s%04d-Lausanne", i); // "Romanel-s0001-Lausanne".."Romanel-s1999-Lausanne"
				}

				final PersonnePhysique pp = addNonHabitant(encodeDigitsInName(prenom), encodeDigitsInName(nom), date(1970, 1, 1), Sexe.MASCULIN);
				addAdresseEtrangere(pp, TypeAdresseTiers.COURRIER, date(1970, 1, 1), null, "chemin du devin", encodeDigitsInName(localite), MockPays.Suisse);
				ids1.add(pp.getNumero());
			}
			return ids1;
		});

		globalTiersIndexer.schedule(ids);
		globalTiersIndexer.sync();

		// Recherche les 40 personnes nommées "MichelCC Du Pont*"
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("MichelCC Du Pont");
			criteria.setTypeRechercheDuNom(TypeRecherche.CONTIENT);

			final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertNotNull(list);
			assertEquals(40, list.size());

			// Trie par ordre des noms croissant
			Collections.sort(list, new Comparator<TiersIndexedData>() {
				@Override
				public int compare(TiersIndexedData o1, TiersIndexedData o2) {
					return o1.getNom1().compareTo(o2.getNom1());
				}
			});

			int i = 0;
			for (TiersIndexedData d : list) {
				final String nomAttendu = encodeDigitsInName(String.format("MichelCC Du Pont%04d", (i++ * 50 + 22)));
				assertEquals(nomAttendu, d.getNom1());
			}
		}

		{
			// [UNIREG-2142] teste la recherche sur les localités
			final TiersCriteria criteria = new TiersCriteria();
			// le 's' vers s'étendre en 's*' -> lucene va lever une exception parce que le nombre de termes est dépassé. Unireg devrait là-dessus filtrer les termes les plus courts et re-essayer.
			criteria.setLocaliteOuPays(encodeDigitsInName("Romanel-s-Morges"));

			final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertNotNull(list);
			assertEquals(1, list.size());

			final TiersIndexedData d = list.get(0);
			assertEquals("Romanel-s-Morges", d.getLocalite());
		}
	}

	@SuppressWarnings("unused")
	private void dumpResults(List<TiersIndexedData> values) {
		for (TiersIndexedData v : values) {
			System.out.println("Numero: " + v.getNumero());
			System.out.println("Nom1: " + v.getNom1());
			System.out.println("Nom2: " + v.getNom2());
			System.out.println("Date naissance: " + v.getDateNaissanceInscriptionRC());
		}
	}

	/**
	 * [SIFISC-5846] Vérifie qu'il est possible de recherche quelqu'un à partir de son ancien numéro de sourcier.
	 */
	@Test
	public void testRechercheParAncienNumeroDeSourcier() throws Exception {

		class Ids {
			long marcel;
			long jules;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique marcel = addNonHabitant("Marcel", "Espol", date(1934, 3, 12), Sexe.MASCULIN);
			marcel.setAncienNumeroSourcier(333111L);
			ids.marcel = marcel.getId();

			final PersonnePhysique jules = addNonHabitant("Jules", "Espol", date(1936, 8, 22), Sexe.MASCULIN);
			ids.jules = jules.getId();
			return null;
		});

		globalTiersIndexer.sync();

		// numéro connu
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setAncienNumeroSourcier(333111L);

			final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertNotNull(list);
			assertEquals(1, list.size());

			final TiersIndexedData d = list.get(0);
			assertEquals(ids.marcel, d.getNumero().longValue());
		}

		// numéro inconnu
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setAncienNumeroSourcier(111333L);

			assertEmpty(globalTiersSearcher.search(criteria));
		}

		// pas de critère sur le numéro
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Jules");

			final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertNotNull(list);
			assertEquals(1, list.size());

			final TiersIndexedData d = list.get(0);
			assertEquals(ids.jules, d.getNumero().longValue());
		}
	}

	@Test(timeout = 20000)
	public void testFlowSearchFusibleAvecResultats() throws Exception {
		final BlockingQueue<TiersIndexedData> queue = new SynchronousQueue<>();
		final Fuse fusible = new Fuse();

		assertTrue(fusible.isNotBlown());

		final long noIndividuDespont = 4278234L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividuDespont, null, "Despont", "Alain", Sexe.MASCULIN);
			}
		});

		doInNewTransactionAndSession(status -> {
			addHabitant(noIndividuDespont);

			final PersonnePhysique marcel = addNonHabitant("Marcel", "Bolomido", null, Sexe.MASCULIN);
			final PersonnePhysique claudine = addNonHabitant("Claudine", "Desplatanes", null, Sexe.FEMININ);
			addEnsembleTiersCouple(marcel, claudine, date(2012, 4, 14), null);

			addDebiteur(CategorieImpotSource.CREANCIERS_HYPOTHECAIRES, PeriodiciteDecompte.ANNUEL, date(2009, 1, 1));
			return null;
		});
		globalTiersIndexer.sync();

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("Alain Despont");

		final Thread fuseBlowingThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(2000);
					fusible.blow();
				}
				catch (InterruptedException e) {
					// on sort... de toute façon, si le <i>sleep</i> a sauté, le fusible va rester et le test partira en timeout
				}
			}
		});

		final long start = System.nanoTime();
		fuseBlowingThread.start();
		try {
			globalTiersSearcher.flowSearch(criteria, queue, fusible);
		}
		finally {
			final long end = System.nanoTime();
			assertTrue(end - start > TimeUnit.MILLISECONDS.toNanos(2000));
		}

		fuseBlowingThread.join();
	}

	@Test(timeout = 20000)
	public void testFlowSearchFusibleDejaGrilleAvecResultats() throws Exception {
		final BlockingQueue<TiersIndexedData> queue = new SynchronousQueue<>();
		final Fuse fusible = new Fuse();

		assertTrue(fusible.isNotBlown());
		fusible.blow();
		assertTrue(fusible.isBlown());

		final long noIndividuDespont = 4278234L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividuDespont, null, "Despont", "Alain", Sexe.MASCULIN);
			}
		});

		doInNewTransactionAndSession(status -> {
			addHabitant(noIndividuDespont);

			final PersonnePhysique marcel = addNonHabitant("Marcel", "Bolomido", null, Sexe.MASCULIN);
			final PersonnePhysique claudine = addNonHabitant("Claudine", "Desplatanes", null, Sexe.FEMININ);
			addEnsembleTiersCouple(marcel, claudine, date(2012, 4, 14), null);

			addDebiteur(CategorieImpotSource.CREANCIERS_HYPOTHECAIRES, PeriodiciteDecompte.ANNUEL, date(2009, 1, 1));
			return null;
		});
		globalTiersIndexer.sync();

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("Alain Dupont");

		// fusible grillé, donc même s'il y a des résulats, on n'est pas bloqué
		globalTiersSearcher.flowSearch(criteria, queue, fusible);
	}

	@Test(timeout = 20000)
	public void testFlowSearchFusibleSansResultats() throws Exception {
		final BlockingQueue<TiersIndexedData> queue = new SynchronousQueue<>();
		final Fuse fusible = new Fuse();

		assertTrue(fusible.isNotBlown());

		final long noIndividuDespont = 4278234L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividuDespont, null, "Despont", "Alain", Sexe.MASCULIN);
			}
		});

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique alain = addHabitant(noIndividuDespont);

			final PersonnePhysique marcel = addNonHabitant("Marcel", "Bolomido", null, Sexe.MASCULIN);
			final PersonnePhysique claudine = addNonHabitant("Claudine", "Desplatanes", null, Sexe.FEMININ);
			addEnsembleTiersCouple(marcel, claudine, date(2012, 4, 14), null);

			addDebiteur("Débiteur IS", alain, date(2009, 1, 1));
			return null;
		});
		globalTiersIndexer.sync();

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("Robert Pittet");

		// pas de résultat, pas de blocage, même si la queue de sortie n'est pas consommée
		globalTiersSearcher.flowSearch(criteria, queue, fusible);
	}

	@Test(timeout = 20000)
	public void testFlowSearchAvecResultats() throws Exception {
		final BlockingQueue<TiersIndexedData> queue = new SynchronousQueue<>();
		final Fuse fusible = new Fuse();

		final long noIndividuDespont = 4278234L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividuDespont, null, "Despont", "Alain", Sexe.MASCULIN);
			}
		});

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique alain = addHabitant(noIndividuDespont);

			final PersonnePhysique marcel = addNonHabitant("Marcel", "Bolomido", null, Sexe.MASCULIN);
			final PersonnePhysique claudine = addNonHabitant("Claudine", "Desplatanes", null, Sexe.FEMININ);
			addEnsembleTiersCouple(marcel, claudine, date(2012, 4, 14), null);

			addDebiteur("Débiteur IS", alain, date(2009, 1, 1));
			return null;
		});
		globalTiersIndexer.sync();

		final List<TiersIndexedData> found = new ArrayList<>();
		final Fuse done = new Fuse();
		final Thread listener = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (done.isNotBlown()) {
						final TiersIndexedData data = queue.poll(10, TimeUnit.MILLISECONDS);
						if (data != null) {
							found.add(data);
						}
					}
				}
				catch (InterruptedException e) {
					// on sort...
				}
			}
		});
		listener.start();

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("Alain Despont");

		globalTiersSearcher.flowSearch(criteria, queue, fusible);
		done.blow();
		listener.join();

		assertEquals(2, found.size());      // une PP et un DPI
	}

	@Test
	public void testRechercheSexe() throws Exception {

		class Ids {
			long lui;
			long elle;
		}

		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique elle = addNonHabitant("Albertine", "Cochin", null, Sexe.FEMININ);
			final PersonnePhysique lui = addNonHabitant("Robert", "Cochin", null, Sexe.MASCULIN);
			final Ids ids1 = new Ids();
			ids1.elle = elle.getNumero();
			ids1.lui = lui.getNumero();
			return ids1;
		});

		globalTiersIndexer.sync();

		// recherche elle
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Cochin");
			criteria.setSexe(Sexe.FEMININ);
			final List<TiersIndexedData> res = globalTiersSearcher.search(criteria);
			assertEquals(1, res.size());
			assertEquals((Long) ids.elle, res.get(0).getNumero());
		}

		// recherche lui
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Cochin");
			criteria.setSexe(Sexe.MASCULIN);
			final List<TiersIndexedData> res = globalTiersSearcher.search(criteria);
			assertEquals(1, res.size());
			assertEquals((Long) ids.lui, res.get(0).getNumero());
		}

		// recherche les deux
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Cochin");
			final List<TiersIndexedData> res = globalTiersSearcher.search(criteria);
			assertEquals(2, res.size());
			Collections.sort(res, new Comparator<TiersIndexedData>() {
				@Override
				public int compare(TiersIndexedData o1, TiersIndexedData o2) {
					return Long.compare(o1.getNumero(), o2.getNumero());
				}
			});
			assertEquals((Long) ids.elle, res.get(0).getNumero());
			assertEquals((Long) ids.lui, res.get(1).getNumero());
		}
	}

	@Test
	public void testRechercheDateNaissancePartielle() throws Exception {

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		class Ids {
			long pp1;
			long pp2;
			long pp3;
			long pp4;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp1 = addNonHabitant("Albert", "Tartempion", RegDate.get(1965), Sexe.MASCULIN);
			final PersonnePhysique pp2 = addNonHabitant("Albert", "Tartempion", RegDate.get(1965, 2), Sexe.MASCULIN);
			final PersonnePhysique pp3 = addNonHabitant("Albert", "Tartempion", RegDate.get(1965, 2, 21), Sexe.MASCULIN);
			final PersonnePhysique pp4 = addNonHabitant("Albert", "Tartempion", RegDate.get(1965, 3), Sexe.MASCULIN);

			final Ids ids1 = new Ids();
			ids1.pp1 = pp1.getNumero();
			ids1.pp2 = pp2.getNumero();
			ids1.pp3 = pp3.getNumero();
			ids1.pp4 = pp4.getNumero();
			return ids1;
		});

		final Comparator<TiersIndexedData> comparator = new Comparator<TiersIndexedData>() {
			@Override
			public int compare(TiersIndexedData o1, TiersIndexedData o2) {
				return Long.compare(o1.getNumero(), o2.getNumero());
			}
		};

		// on attend la fin de l'indexation des bonshommes
		globalTiersIndexer.sync();

		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Tartempion");
			criteria.setDateNaissanceInscriptionRC(RegDate.get(1965, 4, 12));
			final List<TiersIndexedData> res = globalTiersSearcher.search(criteria);
			assertEquals(1, res.size());
			assertEquals((Long) ids.pp1, res.get(0).getNumero());
		}
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Tartempion");
			criteria.setDateNaissanceInscriptionRC(RegDate.get(1965, 2, 12));
			final List<TiersIndexedData> res = globalTiersSearcher.search(criteria);
			assertEquals(2, res.size());
			Collections.sort(res, comparator);
			assertEquals((Long) ids.pp1, res.get(0).getNumero());
			assertEquals((Long) ids.pp2, res.get(1).getNumero());
		}
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Tartempion");
			criteria.setDateNaissanceInscriptionRC(RegDate.get(1965, 2, 21));
			final List<TiersIndexedData> res = globalTiersSearcher.search(criteria);
			assertEquals(3, res.size());
			Collections.sort(res, comparator);
			assertEquals((Long) ids.pp1, res.get(0).getNumero());
			assertEquals((Long) ids.pp2, res.get(1).getNumero());
			assertEquals((Long) ids.pp3, res.get(2).getNumero());
		}
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Tartempion");
			criteria.setDateNaissanceInscriptionRC(RegDate.get(1965, 4));
			final List<TiersIndexedData> res = globalTiersSearcher.search(criteria);
			assertEquals(1, res.size());
			assertEquals((Long) ids.pp1, res.get(0).getNumero());
		}
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Tartempion");
			criteria.setDateNaissanceInscriptionRC(RegDate.get(1965, 3));
			final List<TiersIndexedData> res = globalTiersSearcher.search(criteria);
			assertEquals(2, res.size());
			Collections.sort(res, comparator);
			assertEquals((Long) ids.pp1, res.get(0).getNumero());
			assertEquals((Long) ids.pp4, res.get(1).getNumero());
		}
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Tartempion");
			criteria.setDateNaissanceInscriptionRC(RegDate.get(1965, 2));
			final List<TiersIndexedData> res = globalTiersSearcher.search(criteria);
			assertEquals(3, res.size());
			Collections.sort(res, comparator);
			assertEquals((Long) ids.pp1, res.get(0).getNumero());
			assertEquals((Long) ids.pp2, res.get(1).getNumero());
			assertEquals((Long) ids.pp3, res.get(2).getNumero());
		}
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Tartempion");
			criteria.setDateNaissanceInscriptionRC(RegDate.get(1965));
			final List<TiersIndexedData> res = globalTiersSearcher.search(criteria);
			assertEquals(4, res.size());
			Collections.sort(res, comparator);
			assertEquals((Long) ids.pp1, res.get(0).getNumero());
			assertEquals((Long) ids.pp2, res.get(1).getNumero());
			assertEquals((Long) ids.pp3, res.get(2).getNumero());
			assertEquals((Long) ids.pp4, res.get(3).getNumero());
		}
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Tartempion");
			criteria.setDateNaissanceInscriptionRC(RegDate.get(1966));
			final List<TiersIndexedData> res = globalTiersSearcher.search(criteria);
			assertEquals(0, res.size());
		}
	}

	@Test
	public void testEntrepriseSaufEtatEntreprisePasse() throws Exception {

		final class Ids {
			long pm1;
			long pm2;
			long pm3;
			long pm4;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise e1 = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e1, date(2000, 1, 1), null, "Toto fondée");
			addEtatEntreprise(e1, date(2000, 1, 1), TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.MANUELLE);

			final Entreprise e2 = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e2, date(2000, 1, 1), null, "Toto inscrite RC");
			addEtatEntreprise(e2, date(2000, 1, 1), TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.MANUELLE);
			addEtatEntreprise(e2, date(2000, 1, 1).addDays(3), TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.MANUELLE);

			final Entreprise e3 = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e3, date(2000, 1, 1), null, "Toto Radiée RC");
			addEtatEntreprise(e3, date(2000, 1, 1), TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.MANUELLE);
			addEtatEntreprise(e3, date(2000, 1, 1).addDays(3), TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.MANUELLE);
			addEtatEntreprise(e3, date(2010, 1, 1), TypeEtatEntreprise.RADIEE_RC, TypeGenerationEtatEntreprise.MANUELLE);

			final Entreprise e4 = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e4, date(2000, 1, 1), null, "Toto Absorbée Radiée RC");
			addEtatEntreprise(e4, date(2000, 1, 1), TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.MANUELLE);
			addEtatEntreprise(e4, date(2000, 1, 1).addDays(3), TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.MANUELLE);
			addEtatEntreprise(e4, date(2009, 10, 1), TypeEtatEntreprise.ABSORBEE, TypeGenerationEtatEntreprise.MANUELLE);
			addEtatEntreprise(e4, date(2010, 1, 1), TypeEtatEntreprise.RADIEE_RC, TypeGenerationEtatEntreprise.MANUELLE);

			final Ids ids1 = new Ids();
			ids1.pm1 = e1.getNumero();
			ids1.pm2 = e2.getNumero();
			ids1.pm3 = e3.getNumero();
			ids1.pm4 = e4.getNumero();
			return ids1;
		});

		// on attend la fin de l'indexation des nouveaux contribuables
		globalTiersIndexer.sync();

		// recherche de base sur le nom -> tous les 4 s'appellent "toto"
		{
			final TiersCriteria tiersCriteria = new TiersCriteria();
			tiersCriteria.setNomRaison("toto");

			final List<TiersIndexedData> results = globalTiersSearcher.search(tiersCriteria);
			assertNotNull(results);
			assertEquals(4, results.size());
			final Set<Long> idsRetrouves = new HashSet<>(results.size());
			for (TiersIndexedData data : results) {
				idsRetrouves.add(data.getNumero());
			}
			assertEquals(new HashSet<>(Arrays.asList(ids.pm1, ids.pm2, ids.pm3, ids.pm4)), idsRetrouves);
		}

		// recherche de base sur le nom, mais on ne veut pas les INSCRITE_RC -> 1 seul résultat
		{
			final TiersCriteria tiersCriteria = new TiersCriteria();
			tiersCriteria.setNomRaison("toto");
			tiersCriteria.setEtatsEntrepriseInterdits(EnumSet.of(TypeEtatEntreprise.INSCRITE_RC));

			final List<TiersIndexedData> results = globalTiersSearcher.search(tiersCriteria);
			assertNotNull(results);
			assertEquals(1, results.size());
			final Set<Long> idsRetrouves = new HashSet<>(results.size());
			for (TiersIndexedData data : results) {
				idsRetrouves.add(data.getNumero());
			}
			assertEquals(new HashSet<>(Collections.singletonList(ids.pm1)), idsRetrouves);
		}

		// recherche de base sur le nom mais on ne veut pas les radiées/dissoutes -> 2 résultats
		{
			final TiersCriteria tiersCriteria = new TiersCriteria();
			tiersCriteria.setNomRaison("toto");
			tiersCriteria.setEtatsEntrepriseInterdits(EnumSet.of(TypeEtatEntreprise.RADIEE_RC, TypeEtatEntreprise.DISSOUTE));

			final List<TiersIndexedData> results = globalTiersSearcher.search(tiersCriteria);
			assertNotNull(results);
			assertEquals(2, results.size());
			final Set<Long> idsRetrouves = new HashSet<>(results.size());
			for (TiersIndexedData data : results) {
				idsRetrouves.add(data.getNumero());
			}
			assertEquals(new HashSet<>(Arrays.asList(ids.pm1, ids.pm2)), idsRetrouves);
		}

	}

	@Test
	public void testEntrepriseSelonEtatVisAVisRC() throws Exception {

		final class Ids {
			long pm1;
			long pm2;
			long pm3;
			long pm4;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise e1 = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e1, date(2000, 1, 1), null, "Toto fondée");
			addEtatEntreprise(e1, date(2000, 1, 1), TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.MANUELLE);

			final Entreprise e2 = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e2, date(2000, 1, 1), null, "Toto inscrite RC");
			addEtatEntreprise(e2, date(2000, 1, 1), TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.MANUELLE);
			addEtatEntreprise(e2, date(2000, 1, 1).addDays(3), TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.MANUELLE);

			final Entreprise e3 = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e3, date(2000, 1, 1), null, "Toto Radiée RC");
			addEtatEntreprise(e3, date(2000, 1, 1), TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.MANUELLE);
			addEtatEntreprise(e3, date(2000, 1, 1).addDays(3), TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.MANUELLE);
			addEtatEntreprise(e3, date(2010, 1, 1), TypeEtatEntreprise.RADIEE_RC, TypeGenerationEtatEntreprise.MANUELLE);

			final Entreprise e4 = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e4, date(2000, 1, 1), null, "Toto Absorbée Radiée RC");
			addEtatEntreprise(e4, date(2000, 1, 1), TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.MANUELLE);
			addEtatEntreprise(e4, date(2000, 1, 1).addDays(3), TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.MANUELLE);
			addEtatEntreprise(e4, date(2009, 10, 1), TypeEtatEntreprise.ABSORBEE, TypeGenerationEtatEntreprise.MANUELLE);
			addEtatEntreprise(e4, date(2010, 1, 1), TypeEtatEntreprise.RADIEE_RC, TypeGenerationEtatEntreprise.MANUELLE);

			final Ids ids1 = new Ids();
			ids1.pm1 = e1.getNumero();
			ids1.pm2 = e2.getNumero();
			ids1.pm3 = e3.getNumero();
			ids1.pm4 = e4.getNumero();
			return ids1;
		});

		// on attend la fin de l'indexation des nouveaux contribuables
		globalTiersIndexer.sync();

		// recherche de base sur le nom -> tous les 4 s'appellent "toto"
		{
			final TiersCriteria tiersCriteria = new TiersCriteria();
			tiersCriteria.setNomRaison("toto");

			final List<TiersIndexedData> results = globalTiersSearcher.search(tiersCriteria);
			assertNotNull(results);
			assertEquals(4, results.size());
			final Set<Long> idsRetrouves = new HashSet<>(results.size());
			for (TiersIndexedData data : results) {
				idsRetrouves.add(data.getNumero());
			}
			assertEquals(new HashSet<>(Arrays.asList(ids.pm1, ids.pm2, ids.pm3, ids.pm4)), idsRetrouves);
		}

		// recherche de base sur le nom, mais on ne veut que celles qui ont une inscription RC (inscrite ou radiée, peu importe)
		{
			final TiersCriteria tiersCriteria = new TiersCriteria();
			tiersCriteria.setNomRaison("toto");
			tiersCriteria.setEtatInscriptionRC(TiersCriteria.TypeInscriptionRC.AVEC_INSCRIPTION);

			final List<TiersIndexedData> results = globalTiersSearcher.search(tiersCriteria);
			assertNotNull(results);
			assertEquals(3, results.size());
			final Set<Long> idsRetrouves = new HashSet<>(results.size());
			for (TiersIndexedData data : results) {
				idsRetrouves.add(data.getNumero());
			}
			assertEquals(new HashSet<>(Arrays.asList(ids.pm2, ids.pm3, ids.pm4)), idsRetrouves);
		}

		// recherche de base sur le nom, mais on ne veut que celles qui ont une inscription RC actuellement active
		{
			final TiersCriteria tiersCriteria = new TiersCriteria();
			tiersCriteria.setNomRaison("toto");
			tiersCriteria.setEtatInscriptionRC(TiersCriteria.TypeInscriptionRC.INSCRIT_ACTIF);

			final List<TiersIndexedData> results = globalTiersSearcher.search(tiersCriteria);
			assertNotNull(results);
			assertEquals(1, results.size());
			final Set<Long> idsRetrouves = new HashSet<>(results.size());
			for (TiersIndexedData data : results) {
				idsRetrouves.add(data.getNumero());
			}
			assertEquals(new HashSet<>(Collections.singletonList(ids.pm2)), idsRetrouves);
		}

		// recherche de base sur le nom, mais on ne veut que celles qui ont une inscription RC actuellement radiée
		{
			final TiersCriteria tiersCriteria = new TiersCriteria();
			tiersCriteria.setNomRaison("toto");
			tiersCriteria.setEtatInscriptionRC(TiersCriteria.TypeInscriptionRC.INSCRIT_RADIE);

			final List<TiersIndexedData> results = globalTiersSearcher.search(tiersCriteria);
			assertNotNull(results);
			assertEquals(2, results.size());
			final Set<Long> idsRetrouves = new HashSet<>(results.size());
			for (TiersIndexedData data : results) {
				idsRetrouves.add(data.getNumero());
			}
			assertEquals(new HashSet<>(Arrays.asList(ids.pm3, ids.pm4)), idsRetrouves);
		}

		// recherche de base sur le nom, mais on ne veut que celles qui n'ont aucune inscription RC
		{
			final TiersCriteria tiersCriteria = new TiersCriteria();
			tiersCriteria.setNomRaison("toto");
			tiersCriteria.setEtatInscriptionRC(TiersCriteria.TypeInscriptionRC.SANS_INSCRIPTION);

			final List<TiersIndexedData> results = globalTiersSearcher.search(tiersCriteria);
			assertNotNull(results);
			assertEquals(1, results.size());
			final Set<Long> idsRetrouves = new HashSet<>(results.size());
			for (TiersIndexedData data : results) {
				idsRetrouves.add(data.getNumero());
			}
			assertEquals(new HashSet<>(Collections.singletonList(ids.pm1)), idsRetrouves);
		}

	}
}