package ch.vd.unireg.reqdes;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.pagination.ParamPagination;

public class UniteTraitementDAOTest extends AbstractReqDesDAOTest {

	private UniteTraitementDAO dao;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		dao = getBean(UniteTraitementDAO.class, "reqdesUniteTraitementDAO");
	}

	@Test
	public void testFindCountSansPagination() throws Exception {

		final RegDate dateActe1 = date(1990, 6, 12);
		final RegDate dateActe2 = date(1998, 3, 31);
		final String noMinute1 = "484811";
		final String noMinute2 = "458415854";
		final String visaNotaire1 = "ufi6738v";
		final String visaNotaire2 = "ew89ghvb";
		final long noAffaire1 = 465515468L;
		final long noAffaire2 = 915198L;

		final class Ids {
			long ut1;
			long ut2;
			long ut3;
			long ut4;
		}

		// mise en place de quelques cas
		final Ids ids = doInNewTransaction(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final EvenementReqDes evt1 = addEvenementReqDes(dateActe1, noAffaire1, noMinute1, visaNotaire1, "Notaire", "Clothaire");
				final UniteTraitement ut1 = addUniteTraitement(evt1, EtatTraitement.A_TRAITER, null);
				final PartiePrenante pp1 = addPartiePrenante(ut1, "Wallbert", "Gaspard André");
				final PartiePrenante pp2 = addPartiePrenante(ut1, "Wallbert", "Albertine");
				pp1.setConjointPartiePrenante(pp2);
				pp2.setConjointPartiePrenante(pp1);

				final UniteTraitement ut2 = addUniteTraitement(evt1, EtatTraitement.EN_ERREUR, DateHelper.getDateTime(2006, 5, 12, 15, 53, 14));
				addPartiePrenante(ut2, "Petitbois", "Philippe");

				final EvenementReqDes evt2 = addEvenementReqDes(dateActe2, noAffaire2, noMinute2, visaNotaire2, "Notilde", "Clothilde");
				final UniteTraitement ut3 = addUniteTraitement(evt2, EtatTraitement.FORCE, DateHelper.getDateTime(2008, 6, 30, 18, 12, 14));
				final UniteTraitement ut4 = addUniteTraitement(evt2, EtatTraitement.TRAITE, DateHelper.getDateTime(2014, 6, 1, 12, 0, 0));

				final Ids ids = new Ids();
				ids.ut1 = ut1.getId();
				ids.ut2 = ut2.getId();
				ids.ut3 = ut3.getId();
				ids.ut4 = ut4.getId();
				return ids;
			}
		});

		// et quelques recherches...
		doInNewReadOnlyTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// tous
				{
					final UniteTraitementCriteria criteria = new UniteTraitementCriteria();
					final List<UniteTraitement> result = dao.find(criteria, null);
					Assert.assertNotNull(result);
					Assert.assertEquals(4, result.size());
					Assert.assertEquals((Long) ids.ut4, result.get(0).getId());
					Assert.assertEquals((Long) ids.ut3, result.get(1).getId());
					Assert.assertEquals((Long) ids.ut2, result.get(2).getId());
					Assert.assertEquals((Long) ids.ut1, result.get(3).getId());

					Assert.assertEquals(4, dao.getCount(criteria));
				}

				// par état
				{
					final UniteTraitementCriteria criteria = new UniteTraitementCriteria();
					criteria.setEtatTraitement(EtatTraitement.A_TRAITER);

					final List<UniteTraitement> result = dao.find(criteria, null);
					Assert.assertNotNull(result);
					Assert.assertEquals(1, result.size());
					Assert.assertEquals((Long) ids.ut1, result.get(0).getId());

					Assert.assertEquals(1, dao.getCount(criteria));
				}

				// par numéro de minute
				{
					final UniteTraitementCriteria criteria = new UniteTraitementCriteria();
					criteria.setNumeroMinute(noMinute1);

					final List<UniteTraitement> result = dao.find(criteria, null);
					Assert.assertNotNull(result);
					Assert.assertEquals(2, result.size());
					Assert.assertEquals((Long) ids.ut2, result.get(0).getId());
					Assert.assertEquals((Long) ids.ut1, result.get(1).getId());

					Assert.assertEquals(2, dao.getCount(criteria));
				}

				// par date d'acte (une seule date)
				{
					final UniteTraitementCriteria criteria = new UniteTraitementCriteria();
					criteria.setDateActeMax(dateActe1);
					criteria.setDateActeMin(dateActe1);

					final List<UniteTraitement> result = dao.find(criteria, null);
					Assert.assertNotNull(result);
					Assert.assertEquals(2, result.size());
					Assert.assertEquals((Long) ids.ut2, result.get(0).getId());
					Assert.assertEquals((Long) ids.ut1, result.get(1).getId());

					Assert.assertEquals(2, dao.getCount(criteria));
				}

				// par date d'acte (avant la date la plus ancienne)
				{
					final UniteTraitementCriteria criteria = new UniteTraitementCriteria();
					criteria.setDateActeMax(dateActe1);

					final List<UniteTraitement> result = dao.find(criteria, null);
					Assert.assertNotNull(result);
					Assert.assertEquals(2, result.size());
					Assert.assertEquals((Long) ids.ut2, result.get(0).getId());
					Assert.assertEquals((Long) ids.ut1, result.get(1).getId());

					Assert.assertEquals(2, dao.getCount(criteria));
				}

				// par date d'acte (après la date la plus récente)
				{
					final UniteTraitementCriteria criteria = new UniteTraitementCriteria();
					criteria.setDateActeMin(dateActe2);

					final List<UniteTraitement> result = dao.find(criteria, null);
					Assert.assertNotNull(result);
					Assert.assertEquals(2, result.size());
					Assert.assertEquals((Long) ids.ut4, result.get(0).getId());
					Assert.assertEquals((Long) ids.ut3, result.get(1).getId());

					Assert.assertEquals(2, dao.getCount(criteria));
				}

				// par visa de notaire (connu)
				{
					final UniteTraitementCriteria criteria = new UniteTraitementCriteria();
					criteria.setVisaNotaire(visaNotaire1);

					final List<UniteTraitement> result = dao.find(criteria, null);
					Assert.assertNotNull(result);
					Assert.assertEquals(2, result.size());
					Assert.assertEquals((Long) ids.ut2, result.get(0).getId());
					Assert.assertEquals((Long) ids.ut1, result.get(1).getId());

					Assert.assertEquals(2, dao.getCount(criteria));
				}

				// par visa de notaire (inconnu)
				{
					final UniteTraitementCriteria criteria = new UniteTraitementCriteria();
					criteria.setVisaNotaire(visaNotaire1 + visaNotaire2);

					final List<UniteTraitement> result = dao.find(criteria, null);
					Assert.assertNotNull(result);
					Assert.assertEquals(0, result.size());
					Assert.assertEquals(0, dao.getCount(criteria));
				}

				// par date de traitement
				{
					final UniteTraitementCriteria criteria = new UniteTraitementCriteria();
					criteria.setDateTraitementMin(RegDate.get(2005, 1, 5));
					criteria.setDateTraitementMax(RegDate.get(2009, 9, 12));

					final List<UniteTraitement> result = dao.find(criteria, null);
					Assert.assertNotNull(result);
					Assert.assertEquals(2, result.size());
					Assert.assertEquals((Long) ids.ut3, result.get(0).getId());
					Assert.assertEquals((Long) ids.ut2, result.get(1).getId());

					Assert.assertEquals(2, dao.getCount(criteria));
				}
			}
		});
	}

	@Test
	public void testFindAvecPagination() throws Exception {

		final RegDate dateActe1 = date(1990, 6, 12);
		final RegDate dateActe2 = date(1998, 3, 31);
		final String noMinute1 = "484811";
		final String noMinute2 = "458415854";
		final String visaNotaire1 = "ufi6738v";
		final String visaNotaire2 = "ew89ghvb";
		final long noAffaire1 = 465515468L;
		final long noAffaire2 = 915198L;

		final class Ids {
			long ut1;
			long ut2;
			long ut3;
			long ut4;
		}

		// mise en place de quelques cas
		final Ids ids = doInNewTransaction(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final EvenementReqDes evt1 = addEvenementReqDes(dateActe1, noAffaire1, noMinute1, visaNotaire1, "Notaire", "Clothaire");
				final UniteTraitement ut1 = addUniteTraitement(evt1, EtatTraitement.A_TRAITER, null);
				final UniteTraitement ut2 = addUniteTraitement(evt1, EtatTraitement.EN_ERREUR, DateHelper.getDateTime(2006, 5, 12, 15, 53, 14));

				final EvenementReqDes evt2 = addEvenementReqDes(dateActe2, noAffaire2, noMinute2, visaNotaire2, "Notilde", "Clothilde");
				final UniteTraitement ut3 = addUniteTraitement(evt2, EtatTraitement.FORCE, DateHelper.getDateTime(2008, 6, 30, 18, 12, 14));
				final UniteTraitement ut4 = addUniteTraitement(evt2, EtatTraitement.TRAITE, DateHelper.getDateTime(2014, 6, 1, 12, 0, 0));

				final Ids ids = new Ids();
				ids.ut1 = ut1.getId();
				ids.ut2 = ut2.getId();
				ids.ut3 = ut3.getId();
				ids.ut4 = ut4.getId();
				return ids;
			}
		});

		// recherches paginées
		doInNewReadOnlyTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// grandes pages, id croissant
				{
					final List<UniteTraitement> uts = dao.find(new UniteTraitementCriteria(), new ParamPagination(1, 25, "id", true));
					Assert.assertNotNull(uts);
					Assert.assertEquals(4, uts.size());
					Assert.assertEquals((Long) ids.ut1, uts.get(0).getId());
					Assert.assertEquals((Long) ids.ut2, uts.get(1).getId());
					Assert.assertEquals((Long) ids.ut3, uts.get(2).getId());
					Assert.assertEquals((Long) ids.ut4, uts.get(3).getId());
				}
				// grandes pages, id décroissant
				{
					final List<UniteTraitement> uts = dao.find(new UniteTraitementCriteria(), new ParamPagination(1, 25, "id", false));
					Assert.assertNotNull(uts);
					Assert.assertEquals(4, uts.size());
					Assert.assertEquals((Long) ids.ut4, uts.get(0).getId());
					Assert.assertEquals((Long) ids.ut3, uts.get(1).getId());
					Assert.assertEquals((Long) ids.ut2, uts.get(2).getId());
					Assert.assertEquals((Long) ids.ut1, uts.get(3).getId());
				}
				// pages plus petites -> première page, id croissant
				{
					final List<UniteTraitement> uts = dao.find(new UniteTraitementCriteria(), new ParamPagination(1, 3, "id", true));
					Assert.assertNotNull(uts);
					Assert.assertEquals(3, uts.size());
					Assert.assertEquals((Long) ids.ut1, uts.get(0).getId());
					Assert.assertEquals((Long) ids.ut2, uts.get(1).getId());
					Assert.assertEquals((Long) ids.ut3, uts.get(2).getId());
				}
				// pages plus petites -> deuxième page, id croissant
				{
					final List<UniteTraitement> uts = dao.find(new UniteTraitementCriteria(), new ParamPagination(2, 3, "id", true));
					Assert.assertNotNull(uts);
					Assert.assertEquals(1, uts.size());
					Assert.assertEquals((Long) ids.ut4, uts.get(0).getId());
				}
				// pages plus petites -> première page, id décroissant
				{
					final List<UniteTraitement> uts = dao.find(new UniteTraitementCriteria(), new ParamPagination(1, 3, "id", false));
					Assert.assertNotNull(uts);
					Assert.assertEquals(3, uts.size());
					Assert.assertEquals((Long) ids.ut4, uts.get(0).getId());
					Assert.assertEquals((Long) ids.ut3, uts.get(1).getId());
					Assert.assertEquals((Long) ids.ut2, uts.get(2).getId());
				}
				// pages plus petites -> deuxième page, id décroissant
				{
					final List<UniteTraitement> uts = dao.find(new UniteTraitementCriteria(), new ParamPagination(2, 3, "id", false));
					Assert.assertNotNull(uts);
					Assert.assertEquals(1, uts.size());
					Assert.assertEquals((Long) ids.ut1, uts.get(0).getId());
				}
			}
		});
	}
}
