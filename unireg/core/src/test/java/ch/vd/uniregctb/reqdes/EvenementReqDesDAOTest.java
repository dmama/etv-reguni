package ch.vd.uniregctb.reqdes;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

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
		final Ids ids = doInNewTransaction(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final EvenementReqDes one = addEvenementReqDes(today, 421L, "gouzigouzi", "Lenotaire", "Clothaire");
				final EvenementReqDes two = addEvenementReqDes(today.getOneDayBefore(), 124L, "gazougazou", "Pêtimplon", "Je");
				two.setOperateur(new InformationsActeur("xsxdsewqa", "Dugenou", "Pimprelette"));

				final Ids ids = new Ids();
				ids.one = one.getId();
				ids.two = two.getId();
				return ids;
			}
		});

		// et maintenant on essaie de les récupérer
		doInNewTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				{
					final EvenementReqDes evt = dao.findByNumeroMinute(421L);
					Assert.assertNotNull(evt);
					Assert.assertEquals(today, evt.getDateActe());
					Assert.assertEquals((Long) 421L, evt.getNumeroMinute());
					Assert.assertNotNull(evt.getNotaire());
					Assert.assertEquals("gouzigouzi", evt.getNotaire().getVisa());
					Assert.assertEquals("Lenotaire", evt.getNotaire().getNom());
					Assert.assertEquals("Clothaire", evt.getNotaire().getPrenom());
					Assert.assertNull(evt.getOperateur());
				}
				{
					final EvenementReqDes evt = dao.findByNumeroMinute(124L);
					Assert.assertNotNull(evt);
					Assert.assertEquals(today.getOneDayBefore(), evt.getDateActe());
					Assert.assertEquals((Long) 124L, evt.getNumeroMinute());
					Assert.assertNotNull(evt.getNotaire());
					Assert.assertEquals("gazougazou", evt.getNotaire().getVisa());
					Assert.assertEquals("Pêtimplon", evt.getNotaire().getNom());
					Assert.assertEquals("Je", evt.getNotaire().getPrenom());
					Assert.assertNotNull(evt.getOperateur());
					Assert.assertEquals("xsxdsewqa", evt.getOperateur().getVisa());
					Assert.assertEquals("Dugenou", evt.getOperateur().getNom());
					Assert.assertEquals("Pimprelette", evt.getOperateur().getPrenom());
				}
				{
					final EvenementReqDes evt = dao.findByNumeroMinute(42L);
					Assert.assertNull(evt);
				}
			}
		});
	}
}
