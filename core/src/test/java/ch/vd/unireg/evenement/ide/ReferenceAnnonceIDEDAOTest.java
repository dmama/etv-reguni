package ch.vd.unireg.evenement.ide;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.common.CoreDAOTest;
import ch.vd.unireg.tiers.Etablissement;

/**
 * @author Raphaël Marmier, 2016-08-31, <raphael.marmier@vd.ch>
 */
public class ReferenceAnnonceIDEDAOTest extends CoreDAOTest {

	protected static final Logger LOGGER = LoggerFactory.getLogger(ReferenceAnnonceIDEDAOTest.class);

	private static final String DAO_NAME = "referenceAnnonceIDEDAO";

	private ReferenceAnnonceIDEDAO dao;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		dao = getBean(ReferenceAnnonceIDEDAO.class, DAO_NAME);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	public void testReferencesAnnonceIDEPourEtablissement() throws Exception {

		final Long etablissement1Id = 3000001L;
		final Long etablissement2Id = 3000002L;
		final Long etablissement3Id = 3000003L;
		final Long etablissement4Id = 3000004L;

		doInNewTransaction(status -> {
			final Etablissement etablissement1 = addEtablissement(etablissement1Id);
			addReferenceAnnonceIDE("testbid101", etablissement1);
			addReferenceAnnonceIDE("testbid102", etablissement1);

			final Etablissement etablissement2 = addEtablissement(etablissement2Id);

			final Etablissement etablissement3 = addEtablissement(etablissement3Id);
			addReferenceAnnonceIDE("testbid301", etablissement3);

			final Etablissement etablissement4 = addEtablissement(etablissement4Id);
			addReferenceAnnonceIDE("testbid401", etablissement4);
			addReferenceAnnonceIDE("testbid402", etablissement4);
			addReferenceAnnonceIDE("testbid403", etablissement4);
			return null;
		});

		final List<ReferenceAnnonceIDE> refAnnonceEtablissement1 = dao.getReferencesAnnonceIDE(etablissement1Id);
		Assert.assertNotNull(refAnnonceEtablissement1);
		Assert.assertEquals(2, refAnnonceEtablissement1.size());
		Collections.sort(refAnnonceEtablissement1, (o1, o2) -> Long.compare(o1.getId(), o2.getId()));
		{
			final ReferenceAnnonceIDE refAnnonce = refAnnonceEtablissement1.get(0);
			Assert.assertEquals("testbid101", refAnnonce.getMsgBusinessId());
			Assert.assertEquals(etablissement1Id, refAnnonce.getEtablissement().getNumero());
		}
		{
			final ReferenceAnnonceIDE refAnnonce = refAnnonceEtablissement1.get(1);
			Assert.assertEquals("testbid102", refAnnonce.getMsgBusinessId());
			Assert.assertEquals(etablissement1Id, refAnnonce.getEtablissement().getNumero());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	public void testLastReferenceAnnonceIDEPourEtablissement() throws Exception {

		final Long etablissement1Id = 3000001L;
		final Long etablissement2Id = 3000002L;
		final Long etablissement3Id = 3000003L;
		final Long etablissement4Id = 3000004L;

		doInNewTransaction(status -> {
			final Etablissement etablissement1 = addEtablissement(etablissement1Id);
			addReferenceAnnonceIDE("testbid101", etablissement1);
			addReferenceAnnonceIDE("testbid102", etablissement1);

			final Etablissement etablissement2 = addEtablissement(etablissement2Id);

			final Etablissement etablissement3 = addEtablissement(etablissement3Id);
			addReferenceAnnonceIDE("testbid301", etablissement3);

			final Etablissement etablissement4 = addEtablissement(etablissement4Id);
			addReferenceAnnonceIDE("testbid401", etablissement4);
			addReferenceAnnonceIDE("testbid402", etablissement4);
			addReferenceAnnonceIDE("testbid403", etablissement4);
			return null;
		});

		final ReferenceAnnonceIDE refAnnonceEtablissement = dao.getLastReferenceAnnonceIDE(etablissement4Id);
		Assert.assertNotNull(refAnnonceEtablissement);
		Assert.assertEquals("testbid403", refAnnonceEtablissement.getMsgBusinessId());
		Assert.assertEquals(etablissement4Id, refAnnonceEtablissement.getEtablissement().getNumero());
	}
}