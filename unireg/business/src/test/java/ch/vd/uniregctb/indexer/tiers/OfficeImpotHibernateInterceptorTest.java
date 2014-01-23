package ch.vd.uniregctb.indexer.tiers;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockServiceInfrastructureService;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TacheNouveauDossier;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEtatTache;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
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

		serviceCivil.setUp(new DefaultMockServiceCivil());
		serviceInfra.setUp(new DefaultMockServiceInfrastructureService());

		assertNotNull(oidLausanne);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOfficeImpotContribuableSansFor() throws Exception {

		Long id = (Long) doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique nh = new PersonnePhysique(false);
				nh.setNom("Dupres");

				nh = (PersonnePhysique) tiersDAO.save(nh);
				return nh.getNumero();
			}
		});

		Tiers nh = tiersDAO.get(id);
		assertNotNull(nh);
		assertNull(nh.getOfficeImpotId());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOfficeImpotContribuableSansForAvecNumeroOID() throws Exception {

		Long id = (Long) doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique nh = new PersonnePhysique(false);
				nh.setNom("Dupres");
				nh.setOfficeImpotId(8);

				nh = (PersonnePhysique) tiersDAO.save(nh);
				return nh.getNumero();
			}
		});

		Tiers nh = tiersDAO.get(id);
		assertNotNull(nh);
		assertNull(nh.getOfficeImpotId());
	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOfficeImpotContribuableAvecForPrincipal() throws Exception {

		Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				PersonnePhysique nh = new PersonnePhysique(false);
				nh.setNom("Dupres");

				ForFiscalPrincipal f = new ForFiscalPrincipal();
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
			}
		});

		Tiers nh = tiersDAO.get(id);
		assertNotNull(nh);
		assertEquals(oidLausanne, nh.getOfficeImpotId());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOfficeImpotContribuableAvecForPrincipalVariante() throws Exception {

		Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				PersonnePhysique nh = new PersonnePhysique(false);
				nh.setNom("Dupres");

				// variante : le for principal est d'abord sauvé pour lui-même avant d'être ajouté au tiers
				addForPrincipal(nh, date(2000, 1, 1), MotifFor.ARRIVEE_HC, null, null, MockCommune.Lausanne);

				nh = (PersonnePhysique) tiersDAO.save(nh);
				return nh.getNumero();
			}
		});

		Tiers nh = tiersDAO.get(id);
		assertNotNull(nh);
		assertEquals(oidLausanne, nh.getOfficeImpotId());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOfficeImpotContribuableAvecForPrincipalFerme() throws Exception {

		Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				PersonnePhysique nh = new PersonnePhysique(false);
				nh.setNom("Dupres");

				ForFiscalPrincipal f = new ForFiscalPrincipal();
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
			}
		});

		Tiers nh = tiersDAO.get(id);
		assertNotNull(nh);
		assertEquals(oidLausanne, nh.getOfficeImpotId());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOfficeImpotContribuableAjoutForPrincipal() throws Exception {

		// Crée un contribuable sans for
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				PersonnePhysique nh = new PersonnePhysique(false);
				nh.setNom("Dupres");

				nh = (PersonnePhysique) tiersDAO.save(nh);
				return nh.getNumero();
			}
		});
		// L'oid doit être null
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Tiers nh = tiersDAO.get(id);
				assertNotNull(nh);
				assertNull(nh.getOfficeImpotId());
				return null;
			}
		});

		// Recharge le contribuable de la base et ajoute le for
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				Tiers nh = tiersDAO.get(id);

				ForFiscalPrincipal f = new ForFiscalPrincipal();
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
			}
		});
		// L'oid doit maintenant être renseigné
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Tiers nh = tiersDAO.get(id);
				assertNotNull(nh);
				assertEquals(oidLausanne, nh.getOfficeImpotId());
				return null;
			}
		});
	}

	/**
	 * [UNIREG-2386] Vérifie que l'annulation du dernier for principal provoque bien le recalcul de l'office d'impôt
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOfficeImpotContribuableAnnulationForPrincipal() throws Exception {

		// Crée un contribuable né à Lausanne et ayant déménagé récemment à Orbe
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique robin = addNonHabitant("Robin", "DesBois", date(1965, 5, 23), Sexe.MASCULIN);
				addForPrincipal(robin, date(1985,5,23), MotifFor.MAJORITE, date(2002,12,31), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				addForPrincipal(robin, date(2003,1,1), MotifFor.DEMENAGEMENT_VD, MockCommune.Orbe);
				return robin.getNumero();
			}
		});

		// L'oid doit être sur Orbe
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique robin = (PersonnePhysique) tiersDAO.get(id);
				assertNotNull(robin);
				final Integer oid = robin.getOfficeImpotId();
				assertNotNull(oid);
				assertEquals(MockOfficeImpot.OID_ORBE.getNoColAdm(), oid.intValue());
				return null;
			}
		});

		// Annule le dernier for
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique robin = (PersonnePhysique) tiersDAO.get(id);
				assertNotNull(robin);

				final ForFiscalPrincipal ffp = robin.getDernierForFiscalPrincipal();
				assertNotNull(ffp);

				tiersService.annuleForFiscal(ffp);
				return null;
			}
		});

		// L'oid doit maintenant être sur Lausanne
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique robin = (PersonnePhysique) tiersDAO.get(id);
				assertNotNull(robin);
				final Integer oid = robin.getOfficeImpotId();
				assertNotNull(oid);
				assertEquals(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), oid.intValue());
				return null;
			}
		});
	}

	@Test
	public void testOfficeImpotContribuableDemenagement() throws Exception {

		class Ids {
			long ctb;
			long lausanne;
			long aigle;
		}
		final Ids ids = new Ids();

		// Crée les OIDs qui vont bien
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				ids.lausanne = addCollAdm(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm()).getNumero();
				ids.aigle = addCollAdm(MockOfficeImpot.OID_AIGLE.getNoColAdm()).getNumero();
				return null;
			}
		});

		// Crée un contribuable à Lausanne avec une tâche
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique nh = new PersonnePhysique(false);
				nh.setNom("Dupres");

				ForFiscalPrincipal f = new ForFiscalPrincipal();
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

				return null;
			}
		});

		// L'oid doit être celui de Lausanne
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Tiers nh = tiersDAO.get(ids.ctb);
				assertNotNull(nh);
				assertEquals(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), nh.getOfficeImpotId().intValue());
				return null;
			}
		});


		// Déménagement du contribuable à Bex
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique ctb = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, ids.ctb);
				assertNotNull(ctb);

				final ForFiscalPrincipal ffp0 = ctb.getForFiscalPrincipalAt(null);
				assertNotNull(ffp0);
				ffp0.setDateFin(date(2009,5,1));
				ffp0.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);

				final ForFiscalPrincipal ffp1 = new ForFiscalPrincipal();
				ffp1.setDateDebut(date(2009, 5, 2));
				ffp1.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
				ffp1.setGenreImpot(GenreImpot.REVENU_FORTUNE);
				ffp1.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
				ffp1.setNumeroOfsAutoriteFiscale(MockCommune.Bex.getNoOFS());
				ffp1.setMotifRattachement(MotifRattachement.DOMICILE);
				ffp1.setModeImposition(ModeImposition.ORDINAIRE);
				ctb.addForFiscal(ffp1);

				return null;
			}
		});

		// L'oid doit être celui d'Aigle
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Tiers nh = tiersDAO.get(ids.ctb);
				assertNotNull(nh);
				assertEquals(MockOfficeImpot.OID_AIGLE.getNoColAdm(), nh.getOfficeImpotId().intValue());
				return null;
			}
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
		final Ids ids = new Ids();

		// Crée les OIDs qui vont bien
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				ids.lausanne = addCollAdm(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm()).getNumero();
				ids.aigle = addCollAdm(MockOfficeImpot.OID_AIGLE.getNoColAdm()).getNumero();
				ids.aci = addCollAdm(ServiceInfrastructureService.noACI).getNumero();

				return null;
			}
		});

		// Crée un contribuable à Lausanne avec une tâche
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique nh = new PersonnePhysique(false);
				nh.setNom("Dupres");

				ForFiscalPrincipal f = new ForFiscalPrincipal();
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

				final CollectiviteAdministrative lausanne = (CollectiviteAdministrative) hibernateTemplate.get(CollectiviteAdministrative.class, ids.lausanne);
				assertNotNull(lausanne);

				TacheNouveauDossier tache = new TacheNouveauDossier(TypeEtatTache.EN_INSTANCE, date(2010, 1, 1), nh, lausanne);
				tache.setCollectiviteAdministrativeAssignee(lausanne);
				tache = (TacheNouveauDossier) tiersDAO.saveObject(tache);
				ids.tache = tache.getId();

				return null;
			}
		});


		// La tâche doit être sur l'OID de Lausanne
		assertCaTache(ids.lausanne, ids.tache);


		// Déménagement du contribuable à Bex
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique ctb = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, ids.ctb);
				assertNotNull(ctb);

				final ForFiscalPrincipal ffp0 = ctb.getForFiscalPrincipalAt(null);
				assertNotNull(ffp0);
				ffp0.setDateFin(date(2009,5,1));
				ffp0.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);

				final ForFiscalPrincipal ffp1 = new ForFiscalPrincipal();
				ffp1.setDateDebut(date(2009, 5, 2));
				ffp1.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
				ffp1.setGenreImpot(GenreImpot.REVENU_FORTUNE);
				ffp1.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
				ffp1.setNumeroOfsAutoriteFiscale(MockCommune.Bex.getNoOFS());
				ffp1.setMotifRattachement(MotifRattachement.DOMICILE);
				ffp1.setModeImposition(ModeImposition.ORDINAIRE);
				ctb.addForFiscal(ffp1);

				return null;
			}
		});

		// [UNIREG-2306] La tâche doit être passé sur l'OID d'Aigle
		assertCaTache(ids.aigle, ids.tache);
	}

	private void assertCaTache(final Long caId, final Long tacheId) throws Exception {
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final TacheNouveauDossier tache = (TacheNouveauDossier) hibernateTemplate.get(TacheNouveauDossier.class, tacheId);
				assertNotNull(tache);
				assertEquals(caId, tache.getCollectiviteAdministrativeAssignee().getNumero());
				return null;
			}
		});
	}
}
