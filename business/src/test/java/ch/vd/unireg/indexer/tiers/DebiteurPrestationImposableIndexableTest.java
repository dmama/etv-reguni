package ch.vd.unireg.indexer.tiers;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.tiers.ContactImpotSource;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.TypeAdresseTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

@SuppressWarnings({"JavaDoc"})
public class DebiteurPrestationImposableIndexableTest extends BusinessTest {

	private TiersDAO dao;

	public DebiteurPrestationImposableIndexableTest() {
		setWantIndexationTiers(true);
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		dao = getBean(TiersDAO.class, "tiersDAO");
	}

	/**
	 * Vérifie qu'un débiteur est bien indexé en fonction du nom du contribuable auquel il est associé.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIndexationDebiteurEtContribuable() throws Exception {

		/* Création d'un contribuable et d'un débiteur lié au contribuable */
		final class Numeros {
			Long noCtbNh;
			Long noCtbDpi;
		}
		final Numeros numeros = new Numeros();
		doInNewTransaction(status -> {
			DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
			dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.MENSUEL, null, date(2000, 1, 1), null));
			dpi = (DebiteurPrestationImposable) dao.save(dpi);
			numeros.noCtbDpi = dpi.getNumero();

			addAdresseSuisse(dpi, TypeAdresseTiers.COURRIER, date(2000, 1, 1), null, MockRue.Lausanne.BoulevardGrancy);

			PersonnePhysique nh = new PersonnePhysique(false);
			nh.setNom("Entrepreneur");
			nh = (PersonnePhysique) dao.save(nh);
			numeros.noCtbNh = nh.getNumero();

			addAdresseSuisse(nh, TypeAdresseTiers.COURRIER, date(2000, 1, 1), null, MockRue.Chamblon.RueDesUttins);

			ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, nh, dpi);
			hibernateTemplate.merge(contact);
			return null;
		});

		/* On vérifie que les liens sont bien établis */
		{
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) dao.get(numeros.noCtbDpi);
			assertNotNull(dpi);
			assertEquals(PeriodiciteDecompte.MENSUEL, dpi.getPeriodiciteAt(RegDate.get()).getPeriodiciteDecompte());
			assertNotNull(tiersService.getContribuable(dpi));

			final PersonnePhysique nh = (PersonnePhysique) dao.get(numeros.noCtbNh);
			assertNotNull(nh);
			assertEquals("Entrepreneur", nh.getNom());

			final Set<DebiteurPrestationImposable> debiteurs = tiersService.getDebiteursPrestationImposable(nh);
			assertEquals(1, debiteurs.size());

			final DebiteurPrestationImposable debiteur = debiteurs.iterator().next();
			assertSame(dpi, debiteur);
		}

		globalTiersIndexer.sync();

		/* On vérifie que le débiteur et le contribuables sont bien indexés */
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Entrepreneur");
			final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
			assertNotNull(resultats);
			assertEquals(2, resultats.size());

			TiersIndexedData data0 = resultats.get(0);
			TiersIndexedData data1;
			if (data0.getNumero().equals(numeros.noCtbDpi)) {
				data0 = resultats.get(0);
				data1 = resultats.get(1);
			}
			else {
				data0 = resultats.get(1);
				data1 = resultats.get(0);
			}
			assertNotNull(data0);
			assertNotNull(data1);

			final TiersIndexedData dataNh = (data0.getNumero().equals(numeros.noCtbNh) ? data0 : data1);
			final TiersIndexedData dataDpi = (data0.getNumero().equals(numeros.noCtbNh) ? data1 : data0);
			assertEquals(numeros.noCtbNh, dataNh.getNumero());
			assertEquals("Chamblon", dataNh.getLocaliteOuPays());
			assertEquals(numeros.noCtbDpi, dataDpi.getNumero());
			assertEquals("Chamblon Lausanne", dataDpi.getLocaliteOuPays()); // les localités du débiteur et du contribuable associé doivent apparaître
		}
	}

	@Test
	public void testIndexationDebiteurEtEntreprise() throws Exception {

		/* Création d'un contribuable et d'un débiteur lié au contribuable */
		final class Numeros {
			Long noCtbEnt;
			Long noCtbDpi;
		}

		final Numeros numeros = doInNewTransaction(status -> {
			final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.ADMINISTRATEURS, PeriodiciteDecompte.MENSUEL, date(2011, 1, 1));

			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, date(2011, 1, 1), null, "Toto SA");
			addFormeJuridique(entreprise, date(2011, 1, 1), null, FormeJuridiqueEntreprise.SA);

			final ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, entreprise, dpi);
			hibernateTemplate.merge(contact);

			final Numeros n = new Numeros();
			n.noCtbDpi = dpi.getNumero();
			n.noCtbEnt = entreprise.getNumero();
			return n;
		});

		globalTiersIndexer.sync();

		// On vérifie que le débiteur et le contribuables sont bien indexés
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Toto");
			final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
			assertNotNull(resultats);
			assertEquals(2, resultats.size());
			Collections.sort(resultats, new Comparator<TiersIndexedData>() {
				@Override
				public int compare(TiersIndexedData o1, TiersIndexedData o2) {
					return Long.compare(o1.getNumero(), o2.getNumero());
				}
			});

			final TiersIndexedData dataEnt = resultats.get(0);
			final TiersIndexedData dataDpi = resultats.get(1);
			assertEquals(numeros.noCtbEnt, dataEnt.getNumero());
			assertEquals(numeros.noCtbDpi, dataDpi.getNumero());
		}

		// On modifie l'entreprise et on cherche sur la modif => 2 hits
		doInNewTransaction(status -> {
			final Entreprise entreprise = (Entreprise) dao.get(numeros.noCtbEnt);
			assertNotNull(entreprise);
			tiersService.addRaisonSocialeFiscale(entreprise, "Tata SA", date(2016, 1, 1), null);
			return null;
		});

		globalTiersIndexer.sync();
		
		// On vérifie que le débiteur et le contribuable sont bien indexés
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Tata");
			final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
			assertNotNull(resultats);
			assertEquals(2, resultats.size());
		}
	}

	@Test
	public void testIndexationDebiteurEtEtablissementPrincipal() throws Exception {

		/* Création d'un contribuable et d'un débiteur lié au contribuable */
		final class Numeros {
			Long noCtbEnt;
			Long noCtbEtb;
			Long noCtbDpi;
		}
		final Numeros numeros = doInNewTransaction(status -> {
			final RegDate dateDebut = date(2011, 1, 1);
			final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.CONFERENCIERS_ARTISTES_SPORTIFS, PeriodiciteDecompte.MENSUEL, dateDebut);

			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Toto SA");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);

			final Etablissement principal = addEtablissement();
			principal.setRaisonSociale("Toto SA");
			addDomicileEtablissement(principal, dateDebut, null, MockCommune.Lausanne);
			addActiviteEconomique(entreprise, principal, dateDebut, null, true);

			ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, principal, dpi);
			hibernateTemplate.merge(contact);

			final Numeros n = new Numeros();
			n.noCtbDpi = dpi.getNumero();
			n.noCtbEnt = entreprise.getNumero();
			n.noCtbEtb = principal.getNumero();
			return n;
		});

		globalTiersIndexer.sync();

		/* On vérifie que le débiteur et le contribuables sont bien indexés */
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Toto");
			final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
			assertNotNull(resultats);
			assertEquals(3, resultats.size());
			Collections.sort(resultats, new Comparator<TiersIndexedData>() {
				@Override
				public int compare(TiersIndexedData o1, TiersIndexedData o2) {
					return Long.compare(o1.getNumero(), o2.getNumero());
				}
			});

			final TiersIndexedData dataEntreprise = resultats.get(0);
			final TiersIndexedData dataDpi = resultats.get(1);
			final TiersIndexedData dataEtablissement = resultats.get(2);
			assertEquals(numeros.noCtbEnt, dataEntreprise.getNumero());
			assertEquals(numeros.noCtbEtb, dataEtablissement.getNumero());
			assertEquals(numeros.noCtbDpi, dataDpi.getNumero());
		}

		// On modifie l'établissement et on cherche sur la modif => 2 hits
		doInNewTransaction(status -> {
			final Etablissement etablissement = (Etablissement) dao.get(numeros.noCtbEtb);
			assertNotNull(etablissement);
			etablissement.setRaisonSociale("Tata SA");
			return null;
		});

		globalTiersIndexer.sync();

		/* On vérifie que le débiteur et le contribuables sont bien indexés */
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Tata");
			final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
			assertNotNull(resultats);
			assertEquals(2, resultats.size());
		}
	}

	@Test
	public void testIndexationDebiteurEtEtablissementSecondaire() throws Exception {

		/* Création d'un contribuable et d'un débiteur lié au contribuable */
		final class Numeros {
			Long noCtbEnt;
			Long noCtbEtbPrn;
			Long noCtbEtbSec;
			Long noCtbDpi;
		}
		final Numeros numeros = doInNewTransaction(status -> {
			final RegDate dateDebut = date(2011, 1, 1);
			final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.CONFERENCIERS_ARTISTES_SPORTIFS, PeriodiciteDecompte.MENSUEL, dateDebut);

			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Toto SA");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);

			final Etablissement principal = addEtablissement();
			principal.setRaisonSociale("Toto SA");
			addDomicileEtablissement(principal, dateDebut, null, MockCommune.Lausanne);
			addActiviteEconomique(entreprise, principal, dateDebut, null, true);

			final Etablissement secondaire = addEtablissement();
			secondaire.setRaisonSociale("Toto SA, Orbe");
			addDomicileEtablissement(secondaire, dateDebut, null, MockCommune.Orbe);
			addActiviteEconomique(entreprise, secondaire, dateDebut, null, false);

			ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, secondaire, dpi);
			hibernateTemplate.merge(contact);

			final Numeros n = new Numeros();
			n.noCtbDpi = dpi.getNumero();
			n.noCtbEnt = entreprise.getNumero();
			n.noCtbEtbPrn = principal.getNumero();
			n.noCtbEtbSec = secondaire.getNumero();
			return n;
		});

		globalTiersIndexer.sync();

		/* On vérifie que le débiteur et le contribuables sont bien indexés */
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Toto");
			final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
			assertNotNull(resultats);
			assertEquals(4, resultats.size());
			Collections.sort(resultats, new Comparator<TiersIndexedData>() {
				@Override
				public int compare(TiersIndexedData o1, TiersIndexedData o2) {
					return Long.compare(o1.getNumero(), o2.getNumero());
				}
			});

			final TiersIndexedData dataEntreprise = resultats.get(0);
			final TiersIndexedData dataDpi = resultats.get(1);
			final TiersIndexedData dataEtablissementPrincipal = resultats.get(2);
			final TiersIndexedData dataEtablissementSecondaire = resultats.get(3);
			assertEquals(numeros.noCtbEnt, dataEntreprise.getNumero());
			assertEquals(numeros.noCtbEtbPrn, dataEtablissementPrincipal.getNumero());
			assertEquals(numeros.noCtbEtbSec, dataEtablissementSecondaire.getNumero());
			assertEquals(numeros.noCtbDpi, dataDpi.getNumero());
		}

		// On modifie l'établissement et on cherche sur la modif => 2 hits
		doInNewTransaction(status -> {
			final Etablissement etablissement = (Etablissement) dao.get(numeros.noCtbEtbSec);
			assertNotNull(etablissement);
			etablissement.setRaisonSociale("Tata SA");
			return null;
		});

		globalTiersIndexer.sync();

		/* On vérifie que le débiteur et le contribuables sont bien indexés */
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Tata");
			final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
			assertNotNull(resultats);
			assertEquals(2, resultats.size());
		}
	}

	/**
	 * [UNIREG-1376] Indexation d'un débiteur lié à une autre communauté.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIndexationDebiteurEtAutreCommunaute() throws Exception {

		final long idDpi = 1500040L;
		final long idAC = 2001400L; // Bollet SA

		// Création d'un contribuable et d'un débiteur lié au contribuable
		doInNewTransaction(status -> {
			DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
			dpi.setNumero(idDpi);
			dpi.setPersonneContact("Jean-François Burnier");
			dpi.setPeriodiciteDecompteAvantMigration(PeriodiciteDecompte.MENSUEL);
			dpi = (DebiteurPrestationImposable) dao.save(dpi);

			addAdresseSuisse(dpi, TypeAdresseTiers.COURRIER, date(2009, 1, 1), null, MockRue.Lausanne.AvenueDeBeaulieu);
			addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);

			AutreCommunaute ac = new AutreCommunaute();
			ac.setNumero(idAC);
			ac.setNom("Bollet SA");
			ac.setComplementNom("Vive les champignons !");
			ac = (AutreCommunaute) dao.save(ac);

			ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, ac, dpi);
			hibernateTemplate.merge(contact);
			return null;
		});

		globalTiersIndexer.sync();
		
		// On vérifie que le débiteur et l'autre communauté sont bien indexés
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Bollet");
			final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
			assertNotNull(resultats);
			assertEquals(2, resultats.size());

			final TiersIndexedData dataDpi = (resultats.get(0).getNumero() == idDpi ? resultats.get(0) : resultats.get(1));
			assertEquals(idDpi, dataDpi.getNumero().longValue());
			assertEquals("Bollet SA", dataDpi.getNom1());
			assertEquals("Vive les champignons !", dataDpi.getNom2());
			assertEquals("1003", dataDpi.getNpa());
			assertEquals("Lausanne", dataDpi.getForPrincipal());
			assertEquals("", dataDpi.getDateNaissanceInscriptionRC());
			assertEquals("", dataDpi.getDateDeces());

			final TiersIndexedData dataAC = (resultats.get(0).getNumero() == idAC ? resultats.get(0) : resultats.get(1));
			assertEquals(idAC, dataAC.getNumero().longValue());
			assertEquals("Bollet SA", dataAC.getNom1());
			assertEquals("Vive les champignons !", dataAC.getNom2());
			assertEquals("", dataAC.getNpa());
			assertEquals("", dataAC.getForPrincipal());
			assertEquals("", dataAC.getDateNaissanceInscriptionRC());
			assertEquals("", dataAC.getDateDeces());
		}
	}

	// [UNIREG-2907] Vérifique la catérorie IS sur un débiteur est bien recherchable
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIndexationDebiteurCategorieIS() throws Exception {

		// Création d'un débiteur avec catégorie impôt source prestation de prévoyance
		final Long idDpi = doInNewTransaction(status -> {
			DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
			dpi.setPersonneContact("Jean-François Burnier");
			dpi.setPeriodiciteDecompteAvantMigration(PeriodiciteDecompte.MENSUEL);
			dpi.setCategorieImpotSource(CategorieImpotSource.PRESTATIONS_PREVOYANCE);
			dpi = (DebiteurPrestationImposable) dao.save(dpi);

			addAdresseSuisse(dpi, TypeAdresseTiers.COURRIER, date(2009, 1, 1), null, MockRue.Lausanne.AvenueDeBeaulieu);
			addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);
			return dpi.getNumero();
		});

		globalTiersIndexer.sync();

		// On vérifie qu'il est possible de retouver le débiteur à partir de sa catégorie impôt source
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setCategorieDebiteurIs(CategorieImpotSource.PRESTATIONS_PREVOYANCE);
			final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
			assertNotNull(resultats);
			assertEquals(1, resultats.size());

			final TiersIndexedData dataDpi = resultats.get(0);
			assertEquals(idDpi, dataDpi.getNumero());
		}
	}

	/**
	 * [SIFISC-19217] pour les DPI, ce qui est indexé comme date de premier for vaudois est en fait
	 * la date de début de la dernière période d'activité vaudoise continue
	 */
	@Test
	public void testDatesForsVaudois() throws Exception {

		// création d'un DPI
		final Long id = doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2010, 1, 1));
			addForDebiteur(dpi, date(2010, 1, 1), MotifFor.DEBUT_PRESTATION_IS, date(2010, 12, 31), MotifFor.DEMENAGEMENT_SIEGE, MockCommune.Lausanne);
			addForDebiteur(dpi, date(2011, 1, 1), MotifFor.DEMENAGEMENT_SIEGE, date(2011, 10, 31), MotifFor.DEMENAGEMENT_SIEGE, MockCommune.YverdonLesBains);
			addForDebiteur(dpi, date(2011, 11, 1), MotifFor.DEMENAGEMENT_SIEGE, date(2011, 12, 31), MotifFor.DEMENAGEMENT_SIEGE, MockCommune.Bern);
			addForDebiteur(dpi, date(2012, 1, 1), MotifFor.DEMENAGEMENT_SIEGE, date(2012, 12, 31), MotifFor.DEMENAGEMENT_SIEGE, MockCommune.Cossonay);
			addForDebiteur(dpi, date(2013, 1, 1), MotifFor.DEMENAGEMENT_SIEGE, null, null, MockCommune.Echallens);
			return dpi.getNumero();
		});

		globalTiersIndexer.sync();

		// vérification des dates stockées dans l'indexeur
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNumero(id);
		final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
		assertNotNull(resultats);
		assertEquals(1, resultats.size());

		final TiersIndexedData data = resultats.get(0);
		assertEquals(id, data.getNumero());
		assertEquals(date(2012, 1, 1), RegDateHelper.get(data.getDateOuvertureForVd()));
		assertNull(data.getDateFermetureForVd());
		assertEquals(date(2013, 1, 1), RegDateHelper.get(data.getDateOuvertureFor()));
		assertNull(data.getDateFermetureFor());
	}
}
