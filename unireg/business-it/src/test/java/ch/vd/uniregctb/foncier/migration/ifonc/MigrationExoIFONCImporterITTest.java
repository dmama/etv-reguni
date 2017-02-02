package ch.vd.uniregctb.foncier.migration.ifonc;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

import org.hibernate.FlushMode;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.foncier.ExonerationIFONC;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.registrefoncier.dao.CommuneRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.tache.TacheSynchronizerInterceptor;
import ch.vd.uniregctb.tiers.Entreprise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MigrationExoIFONCImporterITTest extends BusinessItTest {

	private MigrationExoIFONCImporter importer;
	private CommuneRFDAO communeRFDAO;
	private ImmeubleRFDAO immeubleRFDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		communeRFDAO = getBean(CommuneRFDAO.class, "communeRFDAO");
		immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		final ServiceInfrastructureService infraService = getBean(ServiceInfrastructureService.class, "serviceInfrastructureService");
		final ImmeubleRFDAO immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		final GlobalTiersIndexer tiersIndexer = getBean(GlobalTiersIndexer.class, "globalTiersIndexer");
		final TacheSynchronizerInterceptor tacheSynchronizerInterceptor = getBean(TacheSynchronizerInterceptor.class, "tacheSynchronizerInterceptor");
		importer = new MigrationExoIFONCImporter(infraService, transactionManager, immeubleRFDAO, validationInterceptor, tiersIndexer, tacheSynchronizerInterceptor, hibernateTemplate);
	}

	@Test
	public void testLoadCSV() throws Exception {

		doInNewTransaction(status -> {
			// on crée les entreprises concernées
			Stream.of(161, 982, 3777, 4145, 5092, 5919, 7803).map(Entreprise::new).forEach(tiersDAO::save);

			// on crée les communes concernées
			final CommuneRF ollon = communeRFDAO.save(new CommuneRF(59, "Ollon", 5409));
			final CommuneRF montreux = communeRFDAO.save(new CommuneRF(907, "Montreux", 5886));
			final CommuneRF nyon = communeRFDAO.save(new CommuneRF(626, "Nyon", 5724));
			final CommuneRF lausanne = communeRFDAO.save(new CommuneRF(356, "Lausanne", 5586));
			final CommuneRF montanaire = communeRFDAO.save(new CommuneRF(283, "Montanaire", 5693));

			// on crée les immeubles concernés
			immeubleRFDAO.save(newImmeuble("01faeee", ollon, 1137, null, null, null));
			immeubleRFDAO.save(newImmeuble("02faeee", ollon, 14141, null, null, null));
			immeubleRFDAO.save(newImmeuble("03faeee", montreux, 2637, null, null, null));
			immeubleRFDAO.save(newImmeuble("04faeee", nyon, 5113, null, null, null));
			immeubleRFDAO.save(newImmeuble("05faeee", lausanne, 4337, null, null, null));
			immeubleRFDAO.save(newImmeuble("06faeee", lausanne, 4338, null, null, null));
			immeubleRFDAO.save(newImmeuble("07faeee", lausanne, 5705, null, null, null));
			immeubleRFDAO.save(newImmeuble("08faeee", montanaire, 1011, null, null, null));

			return null;
		});

		// on lance l'importation du CSV
		final MigrationExoIFONCImporterResults results;
		try (InputStream is = getClass().getResourceAsStream("exonerations_ifonc_small.csv")) {
			results = importer.loadCSV(is, "UTF-8", 1, null);
		}

		assertNotNull(results);
		assertEmpty(results.getLignesEnErreur());
		assertEquals(11, results.getNbLignesLues());
		assertEquals(8, results.getNbExonerationsTraitees());
		assertEmpty(results.getExonerationsEnErreur());

		// vérification en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final List<ExonerationIFONC> all = hibernateTemplate.find("FROM ExonerationIFONC", FlushMode.AUTO);
				assertEquals(8, all.size());
			}
		});
	}

	@NotNull
	private static ImmeubleRF newImmeuble(String idRF, CommuneRF commune, int noParcelle, Integer index1, Integer index2, Integer index3) {
		final SituationRF situation = new SituationRF();
		situation.setDateDebut(RegDate.get(2000, 1, 1));
		situation.setNoParcelle(noParcelle);
		situation.setIndex1(index1);
		situation.setIndex2(index2);
		situation.setIndex3(index3);
		situation.setCommune(commune);

		ImmeubleRF im0 = new BienFondRF();
		im0.setIdRF(idRF);
		im0.addSituation(situation);
		return im0;
	}

}
