package ch.vd.uniregctb.common;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.Tiers;

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
	public void testHibernateTemplateFind() {

		final List<?> list = hibernateTemplate.find("from Tiers");
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
	public void testHibernateTemplateIterate() {

		Iterator<?> iter = hibernateTemplate.iterate("from Tiers");
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
	public void testHibernateEntityIterator() {

		Iterator<Tiers> iter = new HibernateEntityIterator<Tiers>(hibernateTemplate.iterate("from Tiers"));
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
				assertEquals("Isidor", nh.getPrenom());
				assertEquals("Pirez", nh.getNom());
				break;
			}
			case 12900001: {
				assertTrue(tiers instanceof PersonnePhysique);
				PersonnePhysique nh = (PersonnePhysique) tiers;
				assertEquals("Michel", nh.getPrenom());
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
