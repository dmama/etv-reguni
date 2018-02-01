package ch.vd.unireg.indexer.jobs;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockServiceInfrastructureService;
import ch.vd.unireg.interfaces.infra.mock.MockCollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexer.Mode;
import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DatabaseIndexerJobTest extends BusinessTest {

	private TiersDAO tiersDAO;

	public DatabaseIndexerJobTest() {
		setWantIndexationTiers(true);
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		tiersDAO = getBean(TiersDAO.class, "tiersDAO");

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(MockOrganisationFactory.NESTLE);
				addOrganisation(MockOrganisationFactory.BCV);
				addOrganisation(MockOrganisationFactory.KPMG);
				addOrganisation(MockOrganisationFactory.CURIA_TREUHAND);
				addOrganisation(MockOrganisationFactory.JAL_HOLDING);
				addOrganisation(MockOrganisationFactory.BANQUE_COOP);
			}
		});

		serviceCivil.setUp(new MockServiceCivil() {

			@Override
			protected void init() {
				MockIndividu alain = addIndividu(9876, RegDate.get(1976, 2, 27), "Dupont", "Alain", true);
				MockIndividu richard = addIndividu(9734, RegDate.get(1942, 12, 7), "Bolomey", "Richard", true);
				MockIndividu james = addIndividu(1373, RegDate.get(1992, 1, 14), "Dean", "James", true);
				MockIndividu francois = addIndividu(403399, RegDate.get(1961, 3, 12), "Lestourgie", "Francois", true);
				MockIndividu claudine = addIndividu(222, RegDate.get(1975, 11, 30), "Duchene", "Claudine", false);
				MockIndividu alain2 = addIndividu(111, RegDate.get(1965, 5, 21), "Dupont", "Alain", true);
				MockIndividu miro = addIndividu(333, RegDate.get(1972, 7, 15), "Boillat dupain", "Miro", true);
				MockIndividu claudine2 = addIndividu(444, RegDate.get(1922, 2, 12), "Duchene", "Claudine", false);

				addFieldsIndividu(richard, "1234567891023", "98765432109", null);

				addDefaultAdressesTo(alain);
				addDefaultAdressesTo(richard);
				addDefaultAdressesTo(james);
				addDefaultAdressesTo(francois);
				addDefaultAdressesTo(claudine);
				addDefaultAdressesTo(alain2);
				addDefaultAdressesTo(miro);
				addDefaultAdressesTo(claudine2);
			}

			private void addDefaultAdressesTo(MockIndividu individu) {
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 11, 2), null);
				addAdresse(individu, TypeAdresseCivil.COURRIER, MockRue.Bex.CheminDeLaForet, null, RegDate.get(1980, 11, 2), null);
			}
		});

		serviceInfra.setUp(new DefaultMockServiceInfrastructureService());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testReindexationJob() throws Exception {

		final class Ids {
			long idNestle;
			long idBcv;
			long idKpmg;
			long idCuriaTreuhand;
			long idJalHolding;
			long idBanqueCoop;
		}

		// tout d'abord quelques tiers qui se réindexent
		final Ids ids;
		globalTiersIndexer.onTheFlyIndexationSwitch().setEnabled(true);
		try {
			ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
				@Override
				public Ids doInTransaction(TransactionStatus status) {
					final Entreprise nestle = addEntrepriseConnueAuCivil(MockOrganisationFactory.NESTLE.getNumeroOrganisation());
					addRegimeFiscalVD(nestle, MockOrganisationFactory.NESTLE.getNumeroIDE().get(0).getDateDebut(), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
					addRegimeFiscalCH(nestle, MockOrganisationFactory.NESTLE.getNumeroIDE().get(0).getDateDebut(), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
					addForPrincipal(nestle, MockOrganisationFactory.NESTLE.getNumeroIDE().get(0).getDateDebut(), MotifFor.DEBUT_EXPLOITATION, MockCommune.Vevey);

					final Entreprise bcv = addEntrepriseConnueAuCivil(MockOrganisationFactory.BCV.getNumeroOrganisation());
					addRegimeFiscalVD(bcv, MockOrganisationFactory.BCV.getNumeroIDE().get(0).getDateDebut(), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
					addRegimeFiscalCH(bcv, MockOrganisationFactory.BCV.getNumeroIDE().get(0).getDateDebut(), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
					addForPrincipal(bcv, MockOrganisationFactory.BCV.getNumeroIDE().get(0).getDateDebut(), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);

					final Entreprise kpmg = addEntrepriseConnueAuCivil(MockOrganisationFactory.KPMG.getNumeroOrganisation());
					addRegimeFiscalVD(kpmg, MockOrganisationFactory.KPMG.getNumeroIDE().get(0).getDateDebut(), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
					addRegimeFiscalCH(kpmg, MockOrganisationFactory.KPMG.getNumeroIDE().get(0).getDateDebut(), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
					addForPrincipal(kpmg, MockOrganisationFactory.KPMG.getNumeroIDE().get(0).getDateDebut(), MotifFor.DEBUT_EXPLOITATION, MockCommune.Zurich);

					final Entreprise curiaTreuhand = addEntrepriseConnueAuCivil(MockOrganisationFactory.CURIA_TREUHAND.getNumeroOrganisation());
					addRegimeFiscalVD(curiaTreuhand, MockOrganisationFactory.CURIA_TREUHAND.getNumeroIDE().get(0).getDateDebut(), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
					addRegimeFiscalCH(curiaTreuhand, MockOrganisationFactory.CURIA_TREUHAND.getNumeroIDE().get(0).getDateDebut(), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
					addForPrincipal(curiaTreuhand, MockOrganisationFactory.CURIA_TREUHAND.getNumeroIDE().get(0).getDateDebut(), MotifFor.DEBUT_EXPLOITATION, MockCommune.Chur);

					final Entreprise jalHolding = addEntrepriseConnueAuCivil(MockOrganisationFactory.JAL_HOLDING.getNumeroOrganisation());
					addRegimeFiscalVD(jalHolding, MockOrganisationFactory.JAL_HOLDING.getNumeroIDE().get(0).getDateDebut(), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
					addRegimeFiscalCH(jalHolding, MockOrganisationFactory.JAL_HOLDING.getNumeroIDE().get(0).getDateDebut(), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
					addForPrincipal(jalHolding, MockOrganisationFactory.JAL_HOLDING.getNumeroIDE().get(0).getDateDebut(), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);

					final Entreprise banqueCoop = addEntrepriseConnueAuCivil(MockOrganisationFactory.BANQUE_COOP.getNumeroOrganisation());
					addRegimeFiscalVD(banqueCoop, MockOrganisationFactory.BANQUE_COOP.getNumeroIDE().get(0).getDateDebut(), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
					addRegimeFiscalCH(banqueCoop, MockOrganisationFactory.BANQUE_COOP.getNumeroIDE().get(0).getDateDebut(), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
					addForPrincipal(banqueCoop, MockOrganisationFactory.BANQUE_COOP.getNumeroIDE().get(0).getDateDebut(), MotifFor.DEBUT_EXPLOITATION, MockCommune.Bale);

					final Ids ids = new Ids();
					ids.idNestle = nestle.getNumero();
					ids.idBcv = bcv.getNumero();
					ids.idKpmg = kpmg.getNumero();
					ids.idCuriaTreuhand = curiaTreuhand.getNumero();
					ids.idJalHolding = jalHolding.getNumero();
					ids.idBanqueCoop = banqueCoop.getNumero();
					return ids;
				}
			});
		}
		finally {
			globalTiersIndexer.sync();
			globalTiersIndexer.onTheFlyIndexationSwitch().setEnabled(false);
		}

		globalTiersIndexer.onTheFlyIndexationSwitch().setEnabled(false);
		doInNewTransaction(new TxCallback<Object>() {

			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Ajout d'un Habitant qui ne se reindexe pas
				PersonnePhysique hab = new PersonnePhysique(true);
				hab.setNumero(12345678L);
				hab.setNumeroIndividu(123456L);     // ce numéro n'existe pas dans le mock civil
				tiersDAO.save(hab);
				return null;
			}

		});
		globalTiersIndexer.onTheFlyIndexationSwitch().setEnabled(true);

		// Le tiers est chargé
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNumero(ids.idNestle);
			final List<TiersIndexedData> l = globalTiersSearcher.search(criteria);
			assertEquals(1, l.size());
			assertEquals((Long) ids.idNestle, l.get(0).getNumero());
		}

		// L'index est vidé
		globalTiersIndexer.overwriteIndex();

		// L'index est vide
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNumero(ids.idNestle);
			final List<TiersIndexedData> l = globalTiersSearcher.search(criteria);
			assertEquals(0, l.size());

			final Set<Long> allIds = globalTiersSearcher.getAllIds();
			assertEquals(0, allIds.size());
		}

		// On index dabord avec 1 Thread pour mettre LOG_MDATE et INDEX_DIRTY comme il faut
		globalTiersIndexer.indexAllDatabase(Mode.FULL, 1, null);

		// Puis avec 4 pour vérifier que le multi-threading marche bien
		globalTiersIndexer.indexAllDatabase(Mode.FULL, 4, null);

		// De nouveau trouvé
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNumero(ids.idKpmg);
			final List<TiersIndexedData> l = globalTiersSearcher.search(criteria);
			assertEquals(1, l.size());

			final Tiers tiers = tiersDAO.get(ids.idKpmg);
			assertFalse(tiers.isDirty());
		}

		// Tiers non trouvé.
		{
			long id = 12345678L;

			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNumero(id);
			final List<TiersIndexedData> l = globalTiersSearcher.search(criteria);
			assertEquals(0, l.size());

			final Tiers tiers = tiersDAO.get(id);
			assertTrue(tiers.isDirty());
		}

		// Nombre de tiers indexés
		{
			final Set<Long> allIds = globalTiersSearcher.getAllIds();
			assertTrue(allIds.contains(Long.valueOf(ids.idBanqueCoop)));
			assertTrue(allIds.contains(Long.valueOf(ids.idBcv)));
			assertTrue(allIds.contains(Long.valueOf(ids.idCuriaTreuhand)));
			assertTrue(allIds.contains(Long.valueOf(ids.idJalHolding)));
			assertTrue(allIds.contains(Long.valueOf(ids.idKpmg)));
			assertTrue(allIds.contains(Long.valueOf(ids.idNestle)));

			int nb = globalTiersSearcher.getExactDocCount();
			assertEquals(6 + MockCollectiviteAdministrative.getAll().size(), nb);
		}
	}

}
