package ch.vd.unireg.indexer.tiers;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockInfrastructureConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TacheNouveauDossier;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEtatTache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings({"JavaDoc"})
public class OfficeImpotHibernateInterceptorTest extends BusinessTest {

	private static final Integer oidLausanne = MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm();
	private TiersDAO tiersDAO;
	private TiersService tiersService;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		tiersService = getBean(TiersService.class, "tiersService");

		serviceCivil.setUp(new DefaultMockIndividuConnector());
		serviceInfra.setUp(new DefaultMockInfrastructureConnector());

		assertNotNull(oidLausanne);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOfficeImpotContribuableSansFor() throws Exception {

		Long id = (Long) doInNewTransaction(status -> {
			PersonnePhysique nh = new PersonnePhysique(false);
			nh.setNom("Dupres");

			nh = (PersonnePhysique) tiersDAO.save(nh);
			return nh.getNumero();
		});

		Tiers nh = tiersDAO.get(id);
		assertNotNull(nh);
		assertNull(nh.getOfficeImpotId());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOfficeImpotContribuableSansForAvecNumeroOID() throws Exception {

		Long id = (Long) doInNewTransaction(status -> {
			PersonnePhysique nh = new PersonnePhysique(false);
			nh.setNom("Dupres");
			nh.setOfficeImpotId(8);

			nh = (PersonnePhysique) tiersDAO.save(nh);
			return nh.getNumero();
		});

		Tiers nh = tiersDAO.get(id);
		assertNotNull(nh);
		assertNull(nh.getOfficeImpotId());
	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOfficeImpotContribuableAvecForPrincipal() throws Exception {

		Long id = doInNewTransaction(status -> {
			PersonnePhysique nh = new PersonnePhysique(false);
			nh.setNom("Dupres");

			ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
			f.setDateDebut(date(2000, 1, 1));
			f.setDateFin(null);
			f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			f.setNumeroOfsAutoriteFiscale(MockCommune.Lausanne.getNoOFS());
			f.setMotifRattachement(MotifRattachement.DOMICILE);
			f.setModeImposition(ModeImposition.ORDINAIRE);
			f.setMotifOuverture(MotifFor.ARRIVEE_HC);
			nh.addForFiscal(f);

			nh = (PersonnePhysique) tiersDAO.save(nh);
			return nh.getNumero();
		});

		Tiers nh = tiersDAO.get(id);
		assertNotNull(nh);
		assertEquals(oidLausanne, nh.getOfficeImpotId());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOfficeImpotContribuableAvecForPrincipalVariante() throws Exception {

		Long id = doInNewTransaction(status -> {
			PersonnePhysique nh = new PersonnePhysique(false);
			nh.setNom("Dupres");

			// variante : le for principal est d'abord sauvé pour lui-même avant d'être ajouté au tiers
			addForPrincipal(nh, date(2000, 1, 1), MotifFor.ARRIVEE_HC, null, null, MockCommune.Lausanne);

			nh = (PersonnePhysique) tiersDAO.save(nh);
			return nh.getNumero();
		});

		Tiers nh = tiersDAO.get(id);
		assertNotNull(nh);
		assertEquals(oidLausanne, nh.getOfficeImpotId());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOfficeImpotContribuableAvecForPrincipalFerme() throws Exception {

		Long id = doInNewTransaction(status -> {
			PersonnePhysique nh = new PersonnePhysique(false);
			nh.setNom("Dupres");

			ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
			f.setDateDebut(date(2000, 1, 1));
			f.setDateFin(date(2008, 1, 1));
			f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			f.setNumeroOfsAutoriteFiscale(MockCommune.Lausanne.getNoOFS());
			f.setMotifRattachement(MotifRattachement.DOMICILE);
			f.setModeImposition(ModeImposition.ORDINAIRE);
			f.setMotifOuverture(MotifFor.ARRIVEE_HC);
			f.setMotifFermeture(MotifFor.DEPART_HS);
			nh.addForFiscal(f);

			nh = (PersonnePhysique) tiersDAO.save(nh);
			return nh.getNumero();
		});

		Tiers nh = tiersDAO.get(id);
		assertNotNull(nh);
		assertEquals(oidLausanne, nh.getOfficeImpotId());
	}
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOfficeImpotContribuableAvecForPrincipalFermeParDepartHC() throws Exception {

		Long id = doInNewTransaction(status -> {
			PersonnePhysique nh = new PersonnePhysique(false);
			nh.setNom("Dupres");

			final ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
			f.setDateDebut(date(2000, 1, 1));
			f.setDateFin(date(2008, 1, 1));
			f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			f.setNumeroOfsAutoriteFiscale(MockCommune.Lausanne.getNoOFS());
			f.setMotifRattachement(MotifRattachement.DOMICILE);
			f.setModeImposition(ModeImposition.ORDINAIRE);
			f.setMotifOuverture(MotifFor.ARRIVEE_HC);
			f.setMotifFermeture(MotifFor.DEPART_HC);
			nh.addForFiscal(f);

			nh = (PersonnePhysique) tiersDAO.save(nh);
			return nh.getNumero();
		});

		Tiers nh = tiersDAO.get(id);
		assertNotNull(nh);
		assertEquals(oidLausanne,nh.getOfficeImpotId());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOfficeImpotContribuableAjoutForPrincipal() throws Exception {

		// Crée un contribuable sans for
		final Long id = doInNewTransaction(status -> {
			PersonnePhysique nh = new PersonnePhysique(false);
			nh.setNom("Dupres");

			nh = (PersonnePhysique) tiersDAO.save(nh);
			return nh.getNumero();
		});
		// L'oid doit être null
		doInNewTransaction(status -> {
			Tiers nh = tiersDAO.get(id);
			assertNotNull(nh);
			assertNull(nh.getOfficeImpotId());
			return null;
		});

		// Recharge le contribuable de la base et ajoute le for
		doInNewTransaction(status -> {
			Tiers nh = tiersDAO.get(id);

			ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
			f.setDateDebut(date(2000, 1, 1));
			f.setDateFin(date(2008, 1, 1));
			f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			f.setNumeroOfsAutoriteFiscale(MockCommune.Lausanne.getNoOFS());
			f.setMotifRattachement(MotifRattachement.DOMICILE);
			f.setModeImposition(ModeImposition.ORDINAIRE);
			f.setMotifOuverture(MotifFor.ARRIVEE_HC);
			f.setMotifFermeture(MotifFor.DEPART_HC);
			nh.addForFiscal(f);

			tiersDAO.save(nh);
			return null;
		});
		// L'oid doit maintenant être renseigné
		doInNewTransaction(status -> {
			Tiers nh = tiersDAO.get(id);
			assertNotNull(nh);
			assertEquals(oidLausanne, nh.getOfficeImpotId());
			return null;
		});
	}

	/**
	 * [UNIREG-2386] Vérifie que l'annulation du dernier for principal provoque bien le recalcul de l'office d'impôt
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOfficeImpotContribuableAnnulationForPrincipal() throws Exception {

		// Crée un contribuable né à Lausanne et ayant déménagé récemment à Orbe
		final Long id = doInNewTransaction(status -> {
			final PersonnePhysique robin = addNonHabitant("Robin", "DesBois", date(1965, 5, 23), Sexe.MASCULIN);
			addForPrincipal(robin, date(1985, 5, 23), MotifFor.MAJORITE, date(2002, 12, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
			addForPrincipal(robin, date(2003, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Orbe);
			return robin.getNumero();
		});

		// L'oid doit être sur Orbe
		doInNewTransaction(status -> {
			final PersonnePhysique robin = (PersonnePhysique) tiersDAO.get(id);
			assertNotNull(robin);
			final Integer oid = robin.getOfficeImpotId();
			assertNotNull(oid);
			assertEquals(MockOfficeImpot.OID_ORBE.getNoColAdm(), oid.intValue());
			return null;
		});

		// Annule le dernier for
		doInNewTransaction(status -> {
			final PersonnePhysique robin = (PersonnePhysique) tiersDAO.get(id);
			assertNotNull(robin);

			final ForFiscalPrincipal ffp = robin.getDernierForFiscalPrincipal();
			assertNotNull(ffp);

			tiersService.annuleForFiscal(ffp);
			return null;
		});

		// L'oid doit maintenant être sur Lausanne
		doInNewTransaction(status -> {
			final PersonnePhysique robin = (PersonnePhysique) tiersDAO.get(id);
			assertNotNull(robin);
			final Integer oid = robin.getOfficeImpotId();
			assertNotNull(oid);
			assertEquals(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), oid.intValue());
			return null;
		});
	}

	@Test
	public void testOfficeImpotContribuableDemenagement() throws Exception {

		// Crée un contribuable à Lausanne avec une tâche
		final long ppId = doInNewTransactionAndSession(status -> {
			PersonnePhysique nh = new PersonnePhysique(false);
			nh.setNom("Dupres");

			final ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
			f.setDateDebut(date(2000, 1, 1));
			f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			f.setNumeroOfsAutoriteFiscale(MockCommune.Lausanne.getNoOFS());
			f.setMotifRattachement(MotifRattachement.DOMICILE);
			f.setModeImposition(ModeImposition.ORDINAIRE);
			f.setMotifOuverture(MotifFor.ARRIVEE_HC);
			nh.addForFiscal(f);

			nh = (PersonnePhysique) tiersDAO.save(nh);
			return nh.getNumero();
		});

		// L'oid doit être celui de Lausanne
		doInNewTransactionAndSession(status -> {
			Tiers nh = tiersDAO.get(ppId);
			assertNotNull(nh);
			assertEquals(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), nh.getOfficeImpotId().intValue());
			return null;
		});


		// Déménagement du contribuable à Bex
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique ctb = hibernateTemplate.get(PersonnePhysique.class, ppId);
			assertNotNull(ctb);

			final ForFiscalPrincipalPP ffp0 = ctb.getForFiscalPrincipalAt(null);
			assertNotNull(ffp0);
			ffp0.setDateFin(date(2009, 5, 1));
			ffp0.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);

			final ForFiscalPrincipalPP ffp1 = new ForFiscalPrincipalPP();
			ffp1.setDateDebut(date(2009, 5, 2));
			ffp1.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
			ffp1.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			ffp1.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			ffp1.setNumeroOfsAutoriteFiscale(MockCommune.Bex.getNoOFS());
			ffp1.setMotifRattachement(MotifRattachement.DOMICILE);
			ffp1.setModeImposition(ModeImposition.ORDINAIRE);
			ctb.addForFiscal(ffp1);
			return null;
		});

		// L'oid doit être celui d'Aigle
		doInNewTransactionAndSession(status -> {
			Tiers nh = tiersDAO.get(ppId);
			assertNotNull(nh);
			assertEquals(MockOfficeImpot.OID_AIGLE.getNoColAdm(), nh.getOfficeImpotId().intValue());
			return null;
		});
	}

	@Test
	public void testContribuableAvecTachesEnInstance() throws Exception {

		class Ids {
			long ctb;
			long lausanne;
			long aigle;
			long tache;
			long aci;
		}

		// Crée les OIDs qui vont bien
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Ids ids1 = new Ids();
			ids1.lausanne = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm()).getNumero();
			ids1.aigle = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_AIGLE.getNoColAdm()).getNumero();
			ids1.aci = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noACI).getNumero();
			return ids1;
		});

		// Crée un contribuable à Lausanne avec une tâche
		doInNewTransactionAndSession(status -> {
			PersonnePhysique nh = new PersonnePhysique(false);
			nh.setNom("Dupres");

			ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
			f.setDateDebut(date(2000, 1, 1));
			f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			f.setNumeroOfsAutoriteFiscale(MockCommune.Lausanne.getNoOFS());
			f.setMotifRattachement(MotifRattachement.DOMICILE);
			f.setModeImposition(ModeImposition.ORDINAIRE);
			f.setMotifOuverture(MotifFor.ARRIVEE_HC);
			nh.addForFiscal(f);

			nh = (PersonnePhysique) tiersDAO.save(nh);
			ids.ctb = nh.getNumero();

			final CollectiviteAdministrative lausanne = hibernateTemplate.get(CollectiviteAdministrative.class, ids.lausanne);
			assertNotNull(lausanne);

			TacheNouveauDossier tache = new TacheNouveauDossier(TypeEtatTache.EN_INSTANCE, date(2010, 1, 1), nh, lausanne);
			tache.setCollectiviteAdministrativeAssignee(lausanne);
			tache = (TacheNouveauDossier) tiersDAO.saveObject(tache);
			ids.tache = tache.getId();
			return null;
		});


		// La tâche doit être sur l'OID de Lausanne
		assertCaTache(ids.lausanne, ids.tache);


		// Déménagement du contribuable à Bex
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique ctb = hibernateTemplate.get(PersonnePhysique.class, ids.ctb);
			assertNotNull(ctb);

			final ForFiscalPrincipal ffp0 = ctb.getForFiscalPrincipalAt(null);
			assertNotNull(ffp0);
			ffp0.setDateFin(date(2009, 5, 1));
			ffp0.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);

			final ForFiscalPrincipalPP ffp1 = new ForFiscalPrincipalPP();
			ffp1.setDateDebut(date(2009, 5, 2));
			ffp1.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
			ffp1.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			ffp1.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			ffp1.setNumeroOfsAutoriteFiscale(MockCommune.Bex.getNoOFS());
			ffp1.setMotifRattachement(MotifRattachement.DOMICILE);
			ffp1.setModeImposition(ModeImposition.ORDINAIRE);
			ctb.addForFiscal(ffp1);
			return null;
		});

		// [UNIREG-2306] La tâche doit être passé sur l'OID d'Aigle
		assertCaTache(ids.aigle, ids.tache);
	}

	private void assertCaTache(final Long caId, final Long tacheId) throws Exception {
		doInNewTransactionAndSession(status -> {
			final TacheNouveauDossier tache = hibernateTemplate.get(TacheNouveauDossier.class, tacheId);
			assertNotNull(tache);
			assertEquals(caId, tache.getCollectiviteAdministrativeAssignee().getNumero());
			return null;
		});
	}
}
