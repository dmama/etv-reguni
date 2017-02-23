package ch.vd.uniregctb.foncier.migration.ici;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessItTest;
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
import static org.junit.Assert.assertTrue;

public class MigrationDDImporterITTest extends BusinessItTest {

	private MigrationDDImporter importer;
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
		importer = new MigrationDDImporter(tiersDAO, hibernateTemplate, infraService, immeubleRFDAO, tiersIndexer, validationInterceptor, tacheSynchronizerInterceptor, transactionManager);
	}

	@Test
	public void testLoadCSV() throws Exception {

		doInNewTransaction(status -> {
			// on crée les entreprises concernées
			Arrays.asList(9, 16, 42, 47, 73, 94, 8143).forEach(id -> tiersDAO.save(new Entreprise(id)));

			// on crée les communes concernées
			final CommuneRF laSarraz = communeRFDAO.save(new CommuneRF(61, "La Sarraz", 5498));
			final CommuneRF gland = communeRFDAO.save(new CommuneRF(242, "Gland", 5721));
			final CommuneRF gingins = communeRFDAO.save(new CommuneRF(240, "Gingins", 5719));
			final CommuneRF laTourDePeilz = communeRFDAO.save(new CommuneRF(347, "La Tour-de-Peilz", 5889));
			final CommuneRF aigle = communeRFDAO.save(new CommuneRF(1, "Aigle", 5401));
			final CommuneRF bex = communeRFDAO.save(new CommuneRF(2, "Bex", 5402));

			// on crée les immeubles concernés
			immeubleRFDAO.save(newImmeuble("01faeee", laSarraz, 579, null, null, null));
			immeubleRFDAO.save(newImmeuble("02faeee", gland, 4298, null, null, null));
			immeubleRFDAO.save(newImmeuble("03faeee", gingins, 536, null, null, null));
			immeubleRFDAO.save(newImmeuble("04faeee", gingins, 540, null, null, null));
			immeubleRFDAO.save(newImmeuble("05faeee", gingins, 508, 2, null, null));
			immeubleRFDAO.save(newImmeuble("06faeee", laTourDePeilz, 2065, null, null, null));
			immeubleRFDAO.save(newImmeuble("07faeee", laTourDePeilz, 2066, null, null, null));
			immeubleRFDAO.save(newImmeuble("08faeee", aigle, 1053, 6, null, null));
			immeubleRFDAO.save(newImmeuble("09faeee", bex, 1, null, null, null));
			immeubleRFDAO.save(newImmeuble("10faeee", bex, 2245, null, null, null));

			return null;
		});

		// on lance l'importation du CSV
		final MigrationDDImporterResults results;
		try (InputStream is = getClass().getResourceAsStream("demandes_degrevement_small.csv")) {
			results = importer.loadCSV(is, "UTF-8", 1, null);
		}

		assertNotNull(results);
		assertEmpty(results.getLignesEnErreur());
		assertEquals(13, results.getNbLignes());
		assertEquals(10, results.getTraites().size());  // la demande EGICA-2013 est ignorée car il existe une demande pour 2014
		assertEmpty(results.getErreurs());

		final List<MigrationDDImporterResults.Ignore> ignorees = results.getDonneesIgnorees();
		assertEquals(1, ignorees.size());
		final MigrationDDImporterResults.Ignore ignore0 = ignorees.get(0);
		assertTrue(ignore0.getMessage(), ignore0.getMessage().startsWith("Une donnée pour une PF plus récente (2014) est présente."));
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