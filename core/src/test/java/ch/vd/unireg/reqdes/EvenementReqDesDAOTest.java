package ch.vd.unireg.reqdes;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;

public class EvenementReqDesDAOTest extends AbstractReqDesDAOTest {

	private EvenementReqDesDAO dao;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		dao = getBean(EvenementReqDesDAO.class, "reqdesEvenementDAO");
	}

	@Test
	public void testFindByNumeroMinute() throws Exception {

		final RegDate today = RegDate.get();

		final class Ids {
			long one;
			long two;
		}

		// on en sauvegarde quelques uns
		final Ids ids = doInNewTransaction(status -> {
			final EvenementReqDes one = addEvenementReqDes(today, null, "421", "gouzigouzi", "Lenotaire", "Clothaire");
			final EvenementReqDes two = addEvenementReqDes(today.getOneDayBefore(), null, "124B", "gazougazou", "Pêtimplon", "Je");
			two.setOperateur(new InformationsActeur("xsxdsewqa", "Dugenou", "Pimprelette"));

			final Ids ids1 = new Ids();
			ids1.one = one.getId();
			ids1.two = two.getId();
			return ids1;
		});

		// et maintenant on essaie de les récupérer
		doInNewTransaction(status -> {
			{
				final List<EvenementReqDes> evts = dao.findByNumeroMinute("421", "gouzigouzi");
				Assert.assertNotNull(evts);
				Assert.assertEquals(1, evts.size());

				final EvenementReqDes evt = evts.get(0);
				Assert.assertNotNull(evt);
				Assert.assertEquals((Long) ids.one, evt.getId());
				Assert.assertEquals(today, evt.getDateActe());
				Assert.assertEquals("421", evt.getNumeroMinute());
				Assert.assertNotNull(evt.getNotaire());
				Assert.assertEquals("gouzigouzi", evt.getNotaire().getVisa());
				Assert.assertEquals("Lenotaire", evt.getNotaire().getNom());
				Assert.assertEquals("Clothaire", evt.getNotaire().getPrenom());
				Assert.assertNull(evt.getOperateur());
				Assert.assertNull(evt.getNoAffaire());
			}
			{
				final List<EvenementReqDes> evts = dao.findByNumeroMinute("124B", "gazougazou");
				Assert.assertNotNull(evts);
				Assert.assertEquals(1, evts.size());

				final EvenementReqDes evt = evts.get(0);
				Assert.assertNotNull(evt);
				Assert.assertEquals((Long) ids.two, evt.getId());
				Assert.assertEquals(today.getOneDayBefore(), evt.getDateActe());
				Assert.assertEquals("124B", evt.getNumeroMinute());
				Assert.assertNotNull(evt.getNotaire());
				Assert.assertEquals("gazougazou", evt.getNotaire().getVisa());
				Assert.assertEquals("Pêtimplon", evt.getNotaire().getNom());
				Assert.assertEquals("Je", evt.getNotaire().getPrenom());
				Assert.assertNotNull(evt.getOperateur());
				Assert.assertEquals("xsxdsewqa", evt.getOperateur().getVisa());
				Assert.assertEquals("Dugenou", evt.getOperateur().getNom());
				Assert.assertEquals("Pimprelette", evt.getOperateur().getPrenom());
				Assert.assertNull(evt.getNoAffaire());
			}
			{
				final List<EvenementReqDes> evts = dao.findByNumeroMinute("42", "gouzigouzi");
				Assert.assertNotNull(evts);
				Assert.assertEquals(0, evts.size());
			}
			{
				final List<EvenementReqDes> evts = dao.findByNumeroMinute("421", "gazougazou");
				Assert.assertNotNull(evts);
				Assert.assertEquals(0, evts.size());
			}
			{
				final List<EvenementReqDes> evts = dao.findByNumeroMinute("124B", "toto");
				Assert.assertNotNull(evts);
				Assert.assertEquals(0, evts.size());
			}
			return null;
		});
	}

	@Test
	public void testFindByNumeroAffaire() throws Exception {

		final RegDate today = RegDate.get();

		final class Ids {
			long one;
			long two;
		}

		// on en sauvegarde quelques uns
		final Ids ids = doInNewTransaction(status -> {
			final EvenementReqDes one = addEvenementReqDes(today, 1541515L, "421", "gouzigouzi", "Lenotaire", "Clothaire");
			final EvenementReqDes two = addEvenementReqDes(today.getOneDayBefore(), 418496198L, "124B", "gazougazou", "Pêtimplon", "Je");
			two.setOperateur(new InformationsActeur("xsxdsewqa", "Dugenou", "Pimprelette"));

			final Ids ids1 = new Ids();
			ids1.one = one.getId();
			ids1.two = two.getId();
			return ids1;
		});

		// et maintenant on essaie de les récupérer
		doInNewTransaction(status -> {
			{
				final List<EvenementReqDes> evts = dao.findByNoAffaire(1541515L);
				Assert.assertNotNull(evts);
				Assert.assertEquals(1, evts.size());

				final EvenementReqDes evt = evts.get(0);
				Assert.assertNotNull(evt);
				Assert.assertEquals((Long) ids.one, evt.getId());
				Assert.assertEquals(today, evt.getDateActe());
				Assert.assertEquals("421", evt.getNumeroMinute());
				Assert.assertNotNull(evt.getNotaire());
				Assert.assertEquals("gouzigouzi", evt.getNotaire().getVisa());
				Assert.assertEquals("Lenotaire", evt.getNotaire().getNom());
				Assert.assertEquals("Clothaire", evt.getNotaire().getPrenom());
				Assert.assertNull(evt.getOperateur());
				Assert.assertEquals((Long) 1541515L, evt.getNoAffaire());
			}
			{
				final List<EvenementReqDes> evts = dao.findByNoAffaire(418496198L);
				Assert.assertNotNull(evts);
				Assert.assertEquals(1, evts.size());

				final EvenementReqDes evt = evts.get(0);
				Assert.assertNotNull(evt);
				Assert.assertEquals((Long) ids.two, evt.getId());
				Assert.assertEquals(today.getOneDayBefore(), evt.getDateActe());
				Assert.assertEquals("124B", evt.getNumeroMinute());
				Assert.assertNotNull(evt.getNotaire());
				Assert.assertEquals("gazougazou", evt.getNotaire().getVisa());
				Assert.assertEquals("Pêtimplon", evt.getNotaire().getNom());
				Assert.assertEquals("Je", evt.getNotaire().getPrenom());
				Assert.assertNotNull(evt.getOperateur());
				Assert.assertEquals("xsxdsewqa", evt.getOperateur().getVisa());
				Assert.assertEquals("Dugenou", evt.getOperateur().getNom());
				Assert.assertEquals("Pimprelette", evt.getOperateur().getPrenom());
				Assert.assertEquals((Long) 418496198L, evt.getNoAffaire());
			}
			{
				final List<EvenementReqDes> evts = dao.findByNoAffaire(42L);
				Assert.assertNotNull(evts);
				Assert.assertEquals(0, evts.size());
			}
			return null;
		});
	}
}
