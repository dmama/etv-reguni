package ch.vd.unireg.evenement.reqdes.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.reqdes.EtatTraitement;
import ch.vd.unireg.reqdes.EvenementReqDes;
import ch.vd.unireg.reqdes.InformationsActeur;
import ch.vd.unireg.reqdes.UniteTraitement;

public class EvenementReqDesRetryProcessorTest extends AbstractEvenementReqDesProcessingTest {

	private EvenementReqDesRetryProcessorImpl processor;
	private MyCollectingProcessor mainProcessor;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		mainProcessor = new MyCollectingProcessor();
		mainProcessor.setHibernateTemplate(hibernateTemplate);
		mainProcessor.setInfraService(serviceInfra);
		mainProcessor.setTransactionManager(transactionManager);
		mainProcessor.setUniteTraitementDAO(uniteTraitementDAO);
		mainProcessor.setTiersService(tiersService);
		mainProcessor.setAdresseService(getBean(AdresseService.class, "adresseService"));
		mainProcessor.setAssujettissementService(getBean(AssujettissementService.class, "assujettissementService"));
		mainProcessor.afterPropertiesSet();

		processor = new EvenementReqDesRetryProcessorImpl();
		processor.setHibernateTemplate(hibernateTemplate);
		processor.setTransactionManager(transactionManager);
		processor.setMainProcessor(mainProcessor);
	}

	@Override
	public void onTearDown() throws Exception {
		if (mainProcessor != null) {
			mainProcessor.destroy();
			mainProcessor = null;
		}
		super.onTearDown();
	}

	private static class MyCollectingProcessor extends EvenementReqDesProcessorImpl {

		private final List<Long> collectedIds = Collections.synchronizedList(new LinkedList<>());

		@Override
		public void postUniteTraitement(long id) {
			collectedIds.add(id);
			super.postUniteTraitement(id);
		}

		@Override
		public void postUnitesTraitement(Collection<Long> ids) {
			collectedIds.addAll(ids);
			super.postUnitesTraitement(ids);
		}
	}

	@Test(timeout = 10000)
	public void testRelanceLesBons() throws Exception {

		final class Ids {
			long idForce;
			long idATraite;
			long idEnErreur;
			long idTraite;
		}

		// mise en place
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("zianotaire", "Mourlin", "Stéphane"), null, date(2014, 5, 26), "16478432567");
				final UniteTraitement force = addUniteTraitement(evt, EtatTraitement.FORCE, DateHelper.getDateTime(2014, 6, 22, 10, 54, 12));
				final UniteTraitement atraiter = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final UniteTraitement enErreur = addUniteTraitement(evt, EtatTraitement.EN_ERREUR, DateHelper.getDateTime(2014, 6, 22, 10, 54, 12));
				final UniteTraitement traite = addUniteTraitement(evt, EtatTraitement.TRAITE, DateHelper.getDateTime(2014, 6, 22, 10, 54, 12));

				final Ids ids = new Ids();
				ids.idForce = force.getId();
				ids.idATraite = atraiter.getId();
				ids.idEnErreur = enErreur.getId();
				ids.idTraite = traite.getId();
				return ids;
			}
		});

		// lancement de l'analyse
		processor.relancerEvenementsReqDesNonTraites(null);

		// vérification que les bons (et ceux-là seulement) ont été relancés
		final List<Long> collected = new ArrayList<>(mainProcessor.collectedIds);
		Collections.sort(collected);
		Assert.assertEquals(2, collected.size());
		Assert.assertEquals((Long) ids.idATraite, collected.get(0));
		Assert.assertEquals((Long) ids.idEnErreur, collected.get(1));
	}
}
