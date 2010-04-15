package ch.vd.uniregctb.indexer.tiers;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import ch.vd.uniregctb.tiers.*;
import ch.vd.uniregctb.type.*;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockOfficeImpot;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;

public class OfficeImpotHibernateInterceptorTest extends BusinessTest {

	private static final Integer oidLausanne = MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm();
	private TiersDAO tiersDAO;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		tiersDAO = getBean(TiersDAO.class, "tiersDAO");

		serviceCivil.setUp(new DefaultMockServiceCivil());
		serviceInfra.setUp(new DefaultMockServiceInfrastructureService());

		assertNotNull(oidLausanne);
	}

	@Test
	public void testOfficeImpotContribuableSansFor() throws Exception {

		Long id = (Long) doInNewTransaction(new TxCallback() {
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
	public void testOfficeImpotContribuableAvecForPrincipal() throws Exception {

		Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

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
	public void testOfficeImpotContribuableAvecForPrincipalVariante() throws Exception {

		Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique nh = new PersonnePhysique(false);
				nh.setNom("Dupres");

				// variante : le for principal est d'abord sauvé pour lui-même avant d'être ajouté au tiers
				addForPrincipal(nh, date(2000, 1, 1), MotifFor.ARRIVEE_HC, null, MotifFor.DEPART_HC, MockCommune.Lausanne);

				nh = (PersonnePhysique) tiersDAO.save(nh);
				return nh.getNumero();
			}
		});

		Tiers nh = tiersDAO.get(id);
		assertNotNull(nh);
		assertEquals(oidLausanne, nh.getOfficeImpotId());
	}

	@Test
	public void testOfficeImpotContribuableAvecForPrincipalFerme() throws Exception {

		Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

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
	public void testOfficeImpotContribuableAjoutForPrincipal() throws Exception {

		// Crée un contribuable sans for
		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique nh = new PersonnePhysique(false);
				nh.setNom("Dupres");

				nh = (PersonnePhysique) tiersDAO.save(nh);
				return nh.getNumero();
			}
		});
		// L'oid doit être null
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Tiers nh = tiersDAO.get(id);
				assertNotNull(nh);
				assertNull(nh.getOfficeImpotId());
				return null;
			}
		});

		// Recharge le contribuable de la base et ajoute le for
		doInNewTransaction(new TxCallback() {
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
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Tiers nh = tiersDAO.get(id);
				assertNotNull(nh);
				assertEquals(oidLausanne, nh.getOfficeImpotId());
				return null;
			}
		});
	}

	@Test
	@NotTransactional
	public void testOfficeImpotContribuableDemenagement() throws Exception {

		class Ids {
			long ctb;
			long lausanne;
			long aigle;
		}
		final Ids ids = new Ids();

		// Crée les OIDs qui vont bien
		doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				ids.lausanne = addCollAdm(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm()).getNumero();
				ids.aigle = addCollAdm(MockOfficeImpot.OID_AIGLE.getNoColAdm()).getNumero();
				return null;
			}
		});

		// Crée un contribuable à Lausanne avec une tâche
		doInNewTransactionAndSession(new TxCallback() {
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
		doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Tiers nh = tiersDAO.get(ids.ctb);
				assertNotNull(nh);
				assertEquals(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), nh.getOfficeImpotId().intValue());
				return null;
			}
		});


		// Déménagement du contribuable à Bex
		doInNewTransactionAndSession(new TxCallback() {
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
		doInNewTransactionAndSession(new TxCallback() {
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
	@NotTransactional
	public void testContribuableAvecTachesEnInstance() throws Exception {

		class Ids {
			long ctb;
			long lausanne;
			long aigle;
			long tache;
		}
		final Ids ids = new Ids();

		// Crée les OIDs qui vont bien
		doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				ids.lausanne = addCollAdm(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm()).getNumero();
				ids.aigle = addCollAdm(MockOfficeImpot.OID_AIGLE.getNoColAdm()).getNumero();
				return null;
			}
		});

		// Crée un contribuable à Lausanne avec une tâche
		doInNewTransactionAndSession(new TxCallback() {
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
		doInNewTransactionAndSession(new TxCallback() {
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

		// La tâche doit être passé sur l'OID d'Aigle
		assertCaTache(ids.aigle, ids.tache);
	}

	private void assertCaTache(final Long caId, final Long tacheId) throws Exception {
		doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final TacheNouveauDossier tache = (TacheNouveauDossier) hibernateTemplate.get(TacheNouveauDossier.class, tacheId);
				assertNotNull(tache);
				assertEquals(caId, tache.getCollectiviteAdministrativeAssignee().getNumero());
				return null;
			}
		});
	}

	private CollectiviteAdministrative addCollAdm(int numero) {
		CollectiviteAdministrative ca = new CollectiviteAdministrative();
		ca.setNumeroCollectiviteAdministrative(numero);
		ca = (CollectiviteAdministrative) hibernateTemplate.merge(ca);
		return ca;
	}
}
