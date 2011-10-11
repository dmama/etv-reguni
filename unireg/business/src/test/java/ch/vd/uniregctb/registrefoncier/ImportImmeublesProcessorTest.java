package ch.vd.uniregctb.registrefoncier;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.rf.Immeuble;
import ch.vd.uniregctb.rf.ImmeubleDAO;
import ch.vd.uniregctb.rf.PartPropriete;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class ImportImmeublesProcessorTest extends BusinessTest {

	private ImportImmeublesProcessor processor;
	private InputStream is;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		processor = new ImportImmeublesProcessor(hibernateTemplate, getBean(ImmeubleDAO.class, "immeubleDAO"), transactionManager, tiersDAO);
	}

	@Override
	public void onTearDown() throws Exception {
		if (is != null) {
			is.close();
		}
		super.onTearDown();
	}

	@Test
	public void testRunFichierVide() throws Exception {

		is = new ByteArrayInputStream(new byte[0]);
		try {
			processor.run(is, "ISO-8859-15", null);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("Le fichier est vide !", e.getMessage());
		}
	}

	@Test
	public void testRunFichierAvecEnteteSeulement() throws Exception {

		is = new FileInputStream(getFile("immeubles_vide.csv"));
		final ImportImmeublesResults res = processor.run(is, "UTF-8", null);
		assertNotNull(res);
		assertEquals(0, res.nbLignes);
		assertEquals(0, res.traites.size());
		assertEquals(0, res.ignores.size());
		assertEquals(0, res.erreurs.size());
	}

	@Test
	public void testRunFichierAvecUnImmeubleContribuableInconnu() throws Exception {

		is = new FileInputStream(getFile("un_immeuble.csv"));
		final ImportImmeublesResults res = processor.run(is, "UTF-8", null);
		assertNotNull(res);
		assertEquals(1, res.nbLignes);
		assertEquals(0, res.traites.size());
		assertEquals(0, res.ignores.size());
		assertEquals(1, res.erreurs.size());

		final ImportImmeublesResults.Erreur erreur0 = res.erreurs.get(0);
		assertNotNull(erreur0);
		assertEquals("132/3129", erreur0.getNoImmeuble());
		assertEquals("Le contribuable n'existe pas", erreur0.getDescriptionRaison());
		assertEquals("Le contribuable n°12345678 n'existe pas.", erreur0.getDetails());
	}

	@Test
	public void testRunFichierAvecUnImmeubleContribuableNull() throws Exception {

		is = new FileInputStream(getFile("un_immeuble_ctb_nul.csv"));
		final ImportImmeublesResults res = processor.run(is, "UTF-8", null);
		assertNotNull(res);
		assertEquals(1, res.nbLignes);
		assertEquals(0, res.traites.size());
		assertEquals(1, res.ignores.size());
		assertEquals(0, res.erreurs.size());

		final ImportImmeublesResults.Ignore ignore0 = res.ignores.get(0);
		assertNotNull(ignore0);
		assertEquals("132/3129", ignore0.getNoImmeuble());
		assertEquals("Le contribuable n'est pas renseigné", ignore0.getDescriptionRaison());
	}

	@Test
	public void testRunFichierAvecUnImmeubleEtContribuableConnu() throws Exception {

		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				addNonHabitant(12345678L, "Madeleine", "Chtöpötz", date(1945, 1, 1), Sexe.FEMININ);
				return null;
			}
		});

		is = new FileInputStream(getFile("un_immeuble.csv"));
		final ImportImmeublesResults res = processor.run(is, "UTF-8", null);
		assertNotNull(res);
		assertEquals(1, res.nbLignes);
		assertEquals(1, res.traites.size());
		assertEquals(0, res.ignores.size());
		assertEquals(0, res.erreurs.size());

		final ImportImmeublesResults.Import traite0 = res.traites.get(0);
		assertNotNull(traite0);
		assertEquals(Long.valueOf(12345678), traite0.getNoContribuable());
		assertEquals("132/3129", traite0.getNoImmeuble());

		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(12345678L);
				assertNotNull(pp);

				final Set<Immeuble> immeubles = pp.getImmeubles();
				assertNotNull(immeubles);
				assertEquals(1, immeubles.size());

				final Immeuble immeuble0 = immeubles.iterator().next();
				assertNotNull(immeuble0);
				assertEquals("132/3129", immeuble0.getNumero());
				assertEquals(date(2001, 1, 9), immeuble0.getDateDebut());
				assertNull(immeuble0.getDateFin());
				assertEquals("Revêtement dur", immeuble0.getNature());
				assertEquals(1200000, immeuble0.getEstimationFiscale());
				assertNull(immeuble0.getDateEstimationFiscale());
				assertEquals(GenrePropriete.INDIVIDUELLE, immeuble0.getGenrePropriete());
				assertEquals(new PartPropriete(1, 1), immeuble0.getPartPropriete());
				return null;
			}
		});
	}
}
