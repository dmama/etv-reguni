package ch.vd.unireg.common;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HibernateEntityIteratorTest extends CoreDAOTest {

	private HibernateTemplate hibernateTemplate;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		hibernateTemplate = getBean(HibernateTemplate.class, "hibernateTemplate");

		loadDatabase("HibernateEntityIteratorTest.xml");
	}

	/**
	 * La méthode 'find' retourne une liste d'objets présents en mémoire : aucun proxy n'est créé dans ce cas.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHibernateTemplateFind() {

		final List<?> list = hibernateTemplate.find("from Tiers", null);
		assertNotNull(list);
		assertEquals(4, list.size());

		for (Object o : list) {
			Tiers tiers = (Tiers) o;
			assertNotNull(tiers);

			checkTiers(tiers);
		}
	}

	/**
	 * Met-en-évidence le problème de l'itérateur Hibernate : il retourne des proxys qui dérivent de Tiers. Il est donc impossible de les
	 * caster en Habitant, NonHabitant ou autres sous-classes réelles.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHibernateTemplateIterate() {

		Iterator<?> iter = hibernateTemplate.iterate("from Tiers", null);
		assertNotNull(iter);

		while (iter.hasNext()) {
			Tiers tiers = (Tiers) iter.next();
			assertNotNull(tiers);

			switch (tiers.getNumero().intValue()) {
			case 12600004:
				assertFalse(tiers instanceof MenageCommun);
				break;
			case 12600001:
			case 12900001:
				//non habitant
				assertFalse(tiers instanceof PersonnePhysique);
				break;
			case 43308102:
				//habitant
				assertFalse(tiers instanceof PersonnePhysique);
				break;
			}
		}
	}

	/**
	 * L'itérateur 'HibernateEntityIterator' permet de contourner les proxys retournés par Hibernate dans certaines situation et d'attaquer
	 * directement les entités réelles
	 */
	@SuppressWarnings("unchecked")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHibernateEntityIterator() {

		final Iterator<Tiers> iter = new HibernateEntityIterator<>(hibernateTemplate.<Tiers>iterate("from Tiers", null));
		assertNotNull(iter);

		while (iter.hasNext()) {
			Tiers tiers = iter.next();
			assertNotNull(tiers);

			checkTiers(tiers);
		}
	}

	/**
	 * Vérifie la valeur du tiers par rapport au fichier DBUnit.
	 */
	private void checkTiers(Tiers tiers) {
		switch (tiers.getNumero().intValue()) {
			case 12600004:
				assertTrue(tiers instanceof MenageCommun);
				break;
			case 12600001: {
				assertTrue(tiers instanceof PersonnePhysique);
				PersonnePhysique nh = (PersonnePhysique) tiers;
				assertEquals("Isidor", nh.getPrenomUsuel());
				assertEquals("Pirez", nh.getNom());
				break;
			}
			case 12900001: {
				assertTrue(tiers instanceof PersonnePhysique);
				PersonnePhysique nh = (PersonnePhysique) tiers;
				assertEquals("Michel", nh.getPrenomUsuel());
				assertEquals("Lederet", nh.getNom());
				break;
			}
			case 43308102: {
				assertTrue(tiers instanceof PersonnePhysique);
				PersonnePhysique h = (PersonnePhysique) tiers;
				assertEquals(Long.valueOf(320073), h.getNumeroIndividu());
				break;
			}
		}
	}

}
