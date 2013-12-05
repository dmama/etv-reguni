package ch.vd.uniregctb.pm;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.tiers.Entreprise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class EntrepriseEventHandlerTest extends BusinessTest {

	private EntrepriseEventHandler handler;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		handler = new EntrepriseEventHandler();
		handler.setHibernateTemplate(hibernateTemplate);
		handler.setIndexer(globalTiersIndexer);
		handler.setDataEventService(getBean(DataEventService.class, "dataEventService"));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCreationCoquillePM() throws Exception {

		final long id = 332244L;

		// on vérifie que la PM n'existe pas dans la base
		assertNull(hibernateTemplate.get(Entreprise.class, id));

		// on simule l'arrivée d'un événement PM sur la PM
		handler.onEvtEntreprise(id);

		// on vérifie que la PM existe dorénavant dans la base
		final Entreprise entreprise = hibernateTemplate.get(Entreprise.class, id);
		assertNotNull(entreprise);
		assertEquals(Long.valueOf(id), entreprise.getNumero());
	}
}
