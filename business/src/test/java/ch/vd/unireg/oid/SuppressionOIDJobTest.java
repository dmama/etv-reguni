package ch.vd.unireg.oid;

import javax.sql.DataSource;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.indexer.tiers.OfficeImpotHibernateInterceptor;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.mouvement.EnvoiDossier;
import ch.vd.unireg.mouvement.EnvoiDossierVersCollectiviteAdministrative;
import ch.vd.unireg.mouvement.EtatMouvementDossier;
import ch.vd.unireg.mouvement.ReceptionDossierClassementGeneral;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tache;
import ch.vd.unireg.tiers.TacheControleDossier;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatTache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class SuppressionOIDJobTest extends BusinessTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(SuppressionOIDJobTest.class);

	private SuppressionOIDJob job;
	private LoggingStatusManager status;
	private OfficeImpotHibernateInterceptor oidInterceptor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		job = new SuppressionOIDJob(1, "");
		job.setAdresseService(getBean(AdresseService.class, "adresseService"));
		job.setDataSource(getBean(DataSource.class, "dataSource"));
		job.setHibernateTemplate(hibernateTemplate);
		job.setTiersDAO(tiersDAO);
		job.setTiersService(tiersService);
		job.setTransactionManager(transactionManager);

		status = new LoggingStatusManager(LOGGER);
		oidInterceptor = getBean(OfficeImpotHibernateInterceptor.class, "officeImpotHibernateInterceptor");
	}

	@Test
	public void testSupprimerOIDOfficeImpotInconnu() throws Exception {
		try {
			job.supprimerOID(666, RegDate.get(), status);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("L'office d'impôt n°666 est introuvable dans la base de donnée !", e.getMessage());
		}
	}

	@Test
	public void testSupprimerOIDBaseVide() throws Exception {
		final SuppressionOIDResults results = job.supprimerOID(ServiceInfrastructureRaw.noACI, RegDate.get(), status);
		assertNotNull(results);
		assertEquals(0, results.total);
		assertEquals(0, results.traites.size());
		assertEquals(0, results.errors.size());
	}

	@Test
	public void testSupprimerOIDUnTiersNonConcerne() throws Exception {

		doInNewTransaction(status -> {
			addNonHabitant("Arnold", "Schönborn", date(1923, 2, 2), Sexe.MASCULIN); // pas de for, pas d'OID;
			return null;
		});

		final SuppressionOIDResults results = job.supprimerOID(ServiceInfrastructureRaw.noACI, RegDate.get(), status);
		assertNotNull(results);
		assertEquals(0, results.total);
		assertEquals(0, results.traites.size());
		assertEquals(0, results.errors.size());
	}

	@Test
	public void testSupprimerOIDUnTiersConcerneEnRaisonTableTiers() throws Exception {

		final long id = doInNewTransactionWithoutOidInterceptor(status -> {
			final PersonnePhysique pp = addNonHabitant("Arnold", "Schönborn", date(1923, 2, 2), Sexe.MASCULIN);
			// pour simuler la suppression de l'oid Lausanne-Ouest, on force l'office d'impôt Lausanne-Ouest alors
			// que le ctb habite Morges : l'oid Lausanne-Ouest est toujours valide, mais l'effet sera le même.
			pp.setOfficeImpotId(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());
			addForPrincipal(pp, date(1950, 3, 23), MotifFor.ARRIVEE_HS, MockCommune.Morges);
			return pp.getId();
		});

		// simule la suppression de l'OID de Lausanne-Ouest
		final SuppressionOIDResults results = job.supprimerOID(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), RegDate.get(), status);
		assertNotNull(results);
		assertEquals(1, results.total);
		assertEquals(0, results.errors.size());
		assertEquals(1, results.traites.size());

		final SuppressionOIDResults.Traite traite0 = results.traites.get(0);
		assertNotNull(traite0);
		assertEquals(id, traite0.noCtb);
		assertEquals("Tables impactées : TIERS", traite0.getDescriptionRaison());

		// on vérifie que les différents offices d'impôt sont bien à jour
		doInNewTransaction(status -> {
			final Tiers t = hibernateTemplate.get(Tiers.class, id);
			assertNotNull(t);
			assertEquals("Fermeture-OID-7-10", t.getLogModifUser());
			assertEquals(MockOfficeImpot.OID_MORGES.getNoColAdm(), t.getOfficeImpotId().intValue());
			return null;
		});
	}

	@Test
	public void testSupprimerOIDUnTiersConcerneEnRaisonTableDeclaration() throws Exception {

		class Ids {
			long pp;
			Long morges;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {
			final CollectiviteAdministrative morges = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_MORGES.getNoColAdm());
			final CollectiviteAdministrative lausanneOuest = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());

			// un contribuable avec un OID théorique à Morges
			final PersonnePhysique pp = addNonHabitant("Arnold", "Schönborn", date(1923, 2, 2), Sexe.MASCULIN);
			addForPrincipal(pp, date(1950, 3, 23), MotifFor.ARRIVEE_HS, MockCommune.Morges);

			// une déclaration d'impôt 2008 sur l'OID Lausanne-Ouest alors que le contribuable est domiciliée à Morges -> la déclaration devra être mis-à-jour
			final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
			final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
			addDeclarationImpot(pp, periode2008, date(2008, 1, 1), date(2008, 12, 31), lausanneOuest, TypeContribuable.VAUDOIS_ORDINAIRE, modele2008);

			ids.pp = pp.getId();
			ids.morges = morges.getId();
			return null;
		});

		// simule la suppression de l'OID de Lausanne-Ouest
		final SuppressionOIDResults results = job.supprimerOID(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), RegDate.get(), status);
		assertNotNull(results);
		assertEquals(1, results.total);
		assertEquals(0, results.errors.size());
		assertEquals(1, results.traites.size());

		final SuppressionOIDResults.Traite traite0 = results.traites.get(0);
		assertNotNull(traite0);
		assertEquals(ids.pp, traite0.noCtb);
		assertEquals("Tables impactées : DOCUMENT_FISCAL", traite0.getDescriptionRaison());

		// on vérifie que les différents offices d'impôt sont bien à jour
		doInNewTransaction(status -> {
			final Tiers t = hibernateTemplate.get(Tiers.class, ids.pp);
			assertNotNull(t);
			assertEquals(MockOfficeImpot.OID_MORGES.getNoColAdm(), t.getOfficeImpotId().intValue());

			final List<Declaration> decl = t.getDeclarationsTriees();
			assertNotNull(decl);
			final DeclarationImpotOrdinaire decl0 = (DeclarationImpotOrdinaire) decl.get(0);
			assertEquals("Fermeture-OID-7-10", decl0.getLogModifUser());
			assertEquals(ids.morges, decl0.getRetourCollectiviteAdministrativeId());
			return null;
		});
	}

	@Test
	public void testSupprimerOIDUnTiersConcerneEnRaisonMouvementDestination() throws Exception {

		class Ids {
			long pp;
			Long morges;
			Long envoi;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {
			final CollectiviteAdministrative yverdon = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_YVERDON.getNoColAdm());
			final CollectiviteAdministrative morges = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_MORGES.getNoColAdm());
			final CollectiviteAdministrative lausanneOuest = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());

			// un contribuable avec un OID théorique à Morges
			final PersonnePhysique pp = addNonHabitant("Arnold", "Schönborn", date(1923, 2, 2), Sexe.MASCULIN);
			addForPrincipal(pp, date(1950, 3, 23), MotifFor.ARRIVEE_HS, MockCommune.Morges);

			// un envoi de dossier à destination de Lausanne-Ouest (incorrect)
			EnvoiDossier envoi = new EnvoiDossierVersCollectiviteAdministrative(lausanneOuest);
			envoi.setContribuable(pp);
			envoi.setCollectiviteAdministrativeEmettrice(yverdon);
			envoi.setEtat(EtatMouvementDossier.A_TRAITER);
			envoi = hibernateTemplate.merge(envoi);

			ids.pp = pp.getId();
			ids.morges = morges.getId();
			ids.envoi = envoi.getId();
			return null;
		});

		// simule la suppression de l'OID de Lausanne-Ouest
		final SuppressionOIDResults results = job.supprimerOID(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), RegDate.get(), status);
		assertNotNull(results);
		assertEquals(1, results.total);
		assertEquals(0, results.errors.size());
		assertEquals(1, results.traites.size());

		final SuppressionOIDResults.Traite traite0 = results.traites.get(0);
		assertNotNull(traite0);
		assertEquals(ids.pp, traite0.noCtb);
		assertEquals("Tables impactées : MOUVEMENT", traite0.getDescriptionRaison());

		// on vérifie que les différents offices d'impôt sont bien à jour
		doInNewTransaction(status -> {
			final Tiers t = hibernateTemplate.get(Tiers.class, ids.pp);
			assertNotNull(t);
			assertEquals(MockOfficeImpot.OID_MORGES.getNoColAdm(), t.getOfficeImpotId().intValue());

			final EnvoiDossierVersCollectiviteAdministrative envoi = hibernateTemplate.get(EnvoiDossierVersCollectiviteAdministrative.class, ids.envoi);
			assertNotNull(envoi);
			assertEquals("Fermeture-OID-7-10", envoi.getLogModifUser());
			assertEquals(ids.morges, envoi.getCollectiviteAdministrativeDestinataire().getId());
			return null;
		});
	}

	@Test
	public void testSupprimerOIDUnTiersConcerneEnRaisonMouvementEmetteur() throws Exception {

		class Ids {
			long pp;
			Long morges;
			Long yverdon;
			Long envoi;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {
			final CollectiviteAdministrative yverdon = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_YVERDON.getNoColAdm());
			final CollectiviteAdministrative morges = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_MORGES.getNoColAdm());
			final CollectiviteAdministrative lausanneOuest = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());

			// un contribuable avec un OID théorique à Morges
			final PersonnePhysique pp = addNonHabitant("Arnold", "Schönborn", date(1923, 2, 2), Sexe.MASCULIN);
			addForPrincipal(pp, date(1950, 3, 23), MotifFor.ARRIVEE_HS, MockCommune.Morges);

			// un envoi de dossier à destination de Yverdon (correct) depuis Lausanne-Ouest (à corriger)
			EnvoiDossier envoi = new EnvoiDossierVersCollectiviteAdministrative(yverdon);
			envoi.setContribuable(pp);
			envoi.setCollectiviteAdministrativeEmettrice(lausanneOuest);
			envoi.setEtat(EtatMouvementDossier.A_TRAITER);
			envoi = hibernateTemplate.merge(envoi);

			ids.pp = pp.getId();
			ids.morges = morges.getId();
			ids.yverdon = yverdon.getId();
			ids.envoi = envoi.getId();
			return null;
		});

		// simule la suppression de l'OID de Lausanne-Ouest
		final SuppressionOIDResults results = job.supprimerOID(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), RegDate.get(), status);
		assertNotNull(results);
		assertEquals(1, results.total);
		assertEquals(0, results.errors.size());
		assertEquals(1, results.traites.size());

		final SuppressionOIDResults.Traite traite0 = results.traites.get(0);
		assertNotNull(traite0);
		assertEquals(ids.pp, traite0.noCtb);
		assertEquals("Tables impactées : MOUVEMENT", traite0.getDescriptionRaison());

		// on vérifie que les différents offices d'impôt sont bien à jour
		doInNewTransaction(status -> {
			final Tiers t = hibernateTemplate.get(Tiers.class, ids.pp);
			assertNotNull(t);
			assertEquals(MockOfficeImpot.OID_MORGES.getNoColAdm(), t.getOfficeImpotId().intValue());

			final EnvoiDossierVersCollectiviteAdministrative envoi = hibernateTemplate.get(EnvoiDossierVersCollectiviteAdministrative.class, ids.envoi);
			assertNotNull(envoi);
			assertEquals("Fermeture-OID-7-10", envoi.getLogModifUser());
			assertEquals(ids.yverdon, envoi.getCollectiviteAdministrativeDestinataire().getId());
			assertEquals(ids.morges, envoi.getCollectiviteAdministrativeEmettrice().getId());
			return null;
		});
	}

	@Test
	public void testSupprimerOIDUnTiersConcerneEnRaisonReceptionDossier() throws Exception {

		class Ids {
			long pp;
			Long morges;
			Long yverdon;
			Long reception;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {
			final CollectiviteAdministrative yverdon = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_YVERDON.getNoColAdm());
			final CollectiviteAdministrative morges = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_MORGES.getNoColAdm());
			final CollectiviteAdministrative lausanneOuest = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());

			// un contribuable avec un OID théorique à Morges
			final PersonnePhysique pp = addNonHabitant("Arnold", "Schönborn", date(1923, 2, 2), Sexe.MASCULIN);
			addForPrincipal(pp, date(1950, 3, 23), MotifFor.ARRIVEE_HS, MockCommune.Morges);

			// un envoi de dossier à destination de Lausanne-Ouest (à corriger)
			ReceptionDossierClassementGeneral reception = new ReceptionDossierClassementGeneral();
			reception.setContribuable(pp);
			reception.setCollectiviteAdministrativeReceptrice(lausanneOuest);
			reception.setEtat(EtatMouvementDossier.A_TRAITER);
			reception = hibernateTemplate.merge(reception);

			ids.pp = pp.getId();
			ids.morges = morges.getId();
			ids.yverdon = yverdon.getId();
			ids.reception = reception.getId();
			return null;
		});

		// simule la suppression de l'OID de Lausanne-Ouest
		final SuppressionOIDResults results = job.supprimerOID(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), RegDate.get(), status);
		assertNotNull(results);
		assertEquals(1, results.total);
		assertEquals(0, results.errors.size());
		assertEquals(1, results.traites.size());

		final SuppressionOIDResults.Traite traite0 = results.traites.get(0);
		assertNotNull(traite0);
		assertEquals(ids.pp, traite0.noCtb);
		assertEquals("Tables impactées : MOUVEMENT", traite0.getDescriptionRaison());

		// on vérifie que les différents offices d'impôt sont bien à jour
		doInNewTransaction(status -> {
			final Tiers t = hibernateTemplate.get(Tiers.class, ids.pp);
			assertNotNull(t);
			assertEquals(MockOfficeImpot.OID_MORGES.getNoColAdm(), t.getOfficeImpotId().intValue());

			final ReceptionDossierClassementGeneral reception = hibernateTemplate.get(ReceptionDossierClassementGeneral.class, ids.reception);
			assertNotNull(reception);
			assertEquals("Fermeture-OID-7-10", reception.getLogModifUser());
			assertEquals(ids.morges, reception.getCollectiviteAdministrativeReceptrice().getId());
			return null;
		});
	}

	@Test
	public void testSupprimerOIDUnTiersConcerneEnRaisonTache() throws Exception {

		class Ids {
			long pp;
			Long morges;
			Long yverdon;
			Long tache;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {
			final CollectiviteAdministrative yverdon = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_YVERDON.getNoColAdm());
			final CollectiviteAdministrative morges = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_MORGES.getNoColAdm());
			final CollectiviteAdministrative lausanneOuest = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());

			// un contribuable avec un OID théorique à Morges
			final PersonnePhysique pp = addNonHabitant("Arnold", "Schönborn", date(1923, 2, 2), Sexe.MASCULIN);
			addForPrincipal(pp, date(1950, 3, 23), MotifFor.ARRIVEE_HS, MockCommune.Morges);

			// un tache de contrôle de dossier assignée à Lausanne-Ouest (à corriger)
			final Tache tache = hibernateTemplate.merge(new TacheControleDossier(TypeEtatTache.EN_INSTANCE, date(2100, 1, 1), pp, lausanneOuest));

			ids.pp = pp.getId();
			ids.morges = morges.getId();
			ids.yverdon = yverdon.getId();
			ids.tache = tache.getId();
			return null;
		});

		// simule la suppression de l'OID de Lausanne-Ouest
		final SuppressionOIDResults results = job.supprimerOID(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), RegDate.get(), status);
		assertNotNull(results);
		assertEquals(1, results.total);
		assertEquals(0, results.errors.size());
		assertEquals(1, results.traites.size());

		final SuppressionOIDResults.Traite traite0 = results.traites.get(0);
		assertNotNull(traite0);
		assertEquals(ids.pp, traite0.noCtb);
		assertEquals("Tables impactées : TACHE", traite0.getDescriptionRaison());

		// on vérifie que les différents offices d'impôt sont bien à jour
		doInNewTransaction(status -> {
			final Tiers t = hibernateTemplate.get(Tiers.class, ids.pp);
			assertNotNull(t);
			assertEquals(MockOfficeImpot.OID_MORGES.getNoColAdm(), t.getOfficeImpotId().intValue());

			final TacheControleDossier tache = hibernateTemplate.get(TacheControleDossier.class, ids.tache);
			assertNotNull(tache);
			assertEquals("Fermeture-OID-7-10", tache.getLogModifUser());
			assertEquals(ids.morges, tache.getCollectiviteAdministrativeAssignee().getId());
			return null;
		});
	}

	@Test
	public void testSupprimerOIDUnTiersConcerneEnRaisonTacheMaisSansForFiscal() throws Exception {

		class Ids {
			long pp;
			Long lausanneOuest;
			Long tache;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {
			final CollectiviteAdministrative lausanneOuest = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());

			// un contribuable sans OID théorique
			final PersonnePhysique pp = addNonHabitant("Arnold", "Schönborn", date(1923, 2, 2), Sexe.MASCULIN);

			// un tache de contrôle de dossier assignée à Lausanne-Ouest (qui devrait être corrigée)
			final Tache tache = hibernateTemplate.merge(new TacheControleDossier(TypeEtatTache.EN_INSTANCE, date(2100, 1, 1), pp, lausanneOuest));

			ids.pp = pp.getId();
			ids.lausanneOuest = lausanneOuest.getId();
			ids.tache = tache.getId();
			return null;
		});

		// simule la suppression de l'OID de Lausanne-Ouest
		final SuppressionOIDResults results = job.supprimerOID(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), RegDate.get(), status);
		assertNotNull(results);
		assertEquals(1, results.total);
		assertEquals(1, results.errors.size());
		assertEquals(0, results.traites.size());

		final SuppressionOIDResults.Erreur error0 = results.errors.get(0);
		assertNotNull(error0);
		assertEquals(ids.pp, error0.noCtb);
		assertEquals("impossible de calculer l'oid courant du tiers.", error0.getDescriptionRaison());

		// on vérifie que les différents offices d'impôt n'ont pas été mis à jour
		doInNewTransaction(status -> {
			final Tiers t = hibernateTemplate.get(Tiers.class, ids.pp);
			assertNotNull(t);
			assertNull(t.getOfficeImpotId()); // pas de for fiscal, pas d'OID

			final TacheControleDossier tache = hibernateTemplate.get(TacheControleDossier.class, ids.tache);
			assertNotNull(tache);
			assertEquals("[UT] SuppressionOIDJobTest", tache.getLogModifUser());
			assertEquals(ids.lausanneOuest, tache.getCollectiviteAdministrativeAssignee().getId());
			return null;
		});
	}

	@Test
	public void testSupprimerOIDUnTiersConcerneEnRaisonPlusieursElements() throws Exception {

		class Ids {
			long pp;
			Long morges;
			Long tache;
		}
		final Ids ids = new Ids();

		doInNewTransactionWithoutOidInterceptor(status -> {
			final CollectiviteAdministrative morges = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_MORGES.getNoColAdm());
			final CollectiviteAdministrative lausanneOuest = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());

			// un contribuable avec un OID théorique à Morges
			final PersonnePhysique pp = addNonHabitant("Arnold", "Schönborn", date(1923, 2, 2), Sexe.MASCULIN);
			addForPrincipal(pp, date(1950, 3, 23), MotifFor.ARRIVEE_HS, MockCommune.Morges);

			// pour simuler la suppression de l'oid Lausanne-Ouest, on force l'office d'impôt Lausanne-Ouest alors
			// que le ctb habite Morges : l'oid Lausanne-Ouest est toujours valide, mais l'effet sera le même.
			pp.setOfficeImpotId(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());

			// une déclaration d'impôt 2008 sur l'OID Lausanne-Ouest alors que le contribuable est domiciliée à Morges -> la déclaration devra être mis-à-jour
			final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
			final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
			addDeclarationImpot(pp, periode2008, date(2008, 1, 1), date(2008, 12, 31), lausanneOuest, TypeContribuable.VAUDOIS_ORDINAIRE, modele2008);

			// un tache de contrôle de dossier assignée à Lausanne-Ouest (à corriger)
			final Tache tache = hibernateTemplate.merge(new TacheControleDossier(TypeEtatTache.EN_INSTANCE, date(2100, 1, 1), pp, lausanneOuest));

			ids.pp = pp.getId();
			ids.morges = morges.getId();
			ids.tache = tache.getId();
			return null;
		});

		// simule la suppression de l'OID de Lausanne-Ouest
		final SuppressionOIDResults results = job.supprimerOID(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), RegDate.get(), status);
		assertNotNull(results);
		assertEquals(1, results.total);
		assertEquals(0, results.errors.size());
		assertEquals(1, results.traites.size());

		final SuppressionOIDResults.Traite traite0 = results.traites.get(0);
		assertNotNull(traite0);
		assertEquals(ids.pp, traite0.noCtb);
		assertEquals("Tables impactées : DOCUMENT_FISCAL, TACHE, TIERS", traite0.getDescriptionRaison());

		// on vérifie que les différents offices d'impôt sont bien à jour
		doInNewTransaction(status -> {
			final Tiers t = hibernateTemplate.get(Tiers.class, ids.pp);
			assertNotNull(t);
			assertEquals("Fermeture-OID-7-10", t.getLogModifUser());
			assertEquals(MockOfficeImpot.OID_MORGES.getNoColAdm(), t.getOfficeImpotId().intValue());

			final List<Declaration> decl = t.getDeclarationsTriees();
			assertNotNull(decl);
			final DeclarationImpotOrdinaire decl0 = (DeclarationImpotOrdinaire) decl.get(0);
			assertEquals("Fermeture-OID-7-10", decl0.getLogModifUser());
			assertEquals(ids.morges, decl0.getRetourCollectiviteAdministrativeId());

			final TacheControleDossier tache = hibernateTemplate.get(TacheControleDossier.class, ids.tache);
			assertNotNull(tache);
			assertEquals("Fermeture-OID-7-10", tache.getLogModifUser());
			assertEquals(ids.morges, tache.getCollectiviteAdministrativeAssignee().getId());
			return null;
		});
	}

	protected <T> T doInNewTransactionWithoutOidInterceptor(TransactionCallback<T> action) throws Exception {
		oidInterceptor.setEnabled(false);
		try {
			return doInNewTransactionAndSession(action);
		}
		finally {
			oidInterceptor.setEnabled(true);
		}
	}
}
