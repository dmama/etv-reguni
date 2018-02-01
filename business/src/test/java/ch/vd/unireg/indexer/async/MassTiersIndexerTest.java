package ch.vd.unireg.indexer.async;

import java.util.List;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.avatar.AvatarService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.indexer.GlobalIndex;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexer.Mode;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexerImpl;
import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class MassTiersIndexerTest extends BusinessTest {

	private MassTiersIndexer indexer;
	private TiersDAO tiersDAO;
	private GlobalTiersIndexerImpl gti;

	public MassTiersIndexerTest() {
		setWantIndexationTiers(true);
	}

	private static final String DBUNIT_FILENAME = "MassTiersIndexerTest.xml";
	private static final Integer oidLausanne = MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm();

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new DefaultMockServiceCivil());

		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		final SessionFactory sessionFactory = getBean(SessionFactory.class, "sessionFactory");

		gti = new GlobalTiersIndexerImpl(); // pour éviter le proxy
		gti.setAdresseService(getBean(AdresseService.class, "adresseService"));
		gti.setGlobalIndex(getBean(GlobalIndex.class, "globalTiersIndex"));
		gti.setServiceCivilService(serviceCivil);
		gti.setServiceInfra(serviceInfra);
		gti.setSessionFactory(sessionFactory);
		gti.setTiersDAO(tiersDAO);
		gti.setTiersSearcher(globalTiersSearcher);
		gti.setTiersService(tiersService);
		gti.setAssujettissementService(getBean(AssujettissementService.class, "assujettissementService"));
		gti.setTransactionManager(transactionManager);
		gti.setAvatarService(getBean(AvatarService.class, "avatarService"));

		loadDatabase(DBUNIT_FILENAME);

		indexer = new MassTiersIndexer(gti, transactionManager, sessionFactory, 4, 10, Mode.FULL, dialect, null);
	}

	@Override
	public void onTearDown() throws Exception {

		if (indexer != null) {
			indexer.terminate();
		}

		super.onTearDown();
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAsyncIndexer() throws Exception {

		final long id = 7823L;

		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("bidon");
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(0, list.size());
		}
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNumero(id);
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			// for (TiersIndexedData data : list) {
			// Long id2 = data.getNumero();
			// id2 = null;
			// }
			assertEquals(1, list.size());
		}

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique nh = (PersonnePhysique) tiersDAO.get(id);
				nh.setNom("Bidon");
				nh.setPrenomUsuel("Machin");
				return null;
			}
		});

		globalTiersIndexer.sync(); // on attend que tous les tiers soient indexés

		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNumero(id);
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(1, list.size());
		}
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("bidon");
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			// for (TiersIndexedData data : list) {
			// Long l = data.getNumero();
			// String nom1 = data.getNom1();
			// String nom2 = data.getNom2();
			// l = null;
			// }
			assertEquals(2, list.size());
		}

		gti.removeEntity(id);
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNumero(id);
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(0, list.size());
		}

		indexer.queueTiersForIndexation(id);
		indexer.terminate();

		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("bidon");
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(2, list.size());
		}
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("machin");
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			assertEquals(2, list.size());
		}

	}

	/**
	 * Test que l'office d'impôt est bien mise-à-jour lors de l'indexation asynchrone
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testUpdateOID() throws Exception {

		class Ids {
			public long dupres;
			public long duclou;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// Contribuable sans for
				PersonnePhysique nh = new PersonnePhysique(false);
				nh.setNom("Dupres");
				nh = (PersonnePhysique) tiersDAO.save(nh);
				ids.dupres = nh.getNumero();

				// Contribuable avec for
				nh = new PersonnePhysique(false);
				nh.setNom("Duclou");
				{
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
				}
				nh = (PersonnePhysique) tiersDAO.save(nh);
				ids.duclou = nh.getNumero();

				return null;
			}
		});

		indexer.queueTiersForIndexation(ids.dupres);
		indexer.queueTiersForIndexation(ids.duclou);
		indexer.sync();

		// Contribuable sans for
		Tiers nh = tiersDAO.get(ids.dupres);
		assertNotNull(nh);
		assertNull(nh.getOfficeImpotId());

		// Contribuable avec for
		nh = tiersDAO.get(ids.duclou);
		assertNotNull(nh);
		assertEquals(oidLausanne, nh.getOfficeImpotId());
	}
}
