package ch.vd.unireg.indexer.messageidentification;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.evenement.identification.contribuable.CriteresPersonne;
import ch.vd.unireg.evenement.identification.contribuable.Demande;
import ch.vd.unireg.evenement.identification.contribuable.EsbHeader;
import ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuableCriteria;
import ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuableEtatFilter;
import ch.vd.unireg.evenement.identification.contribuable.TypeDemande;
import ch.vd.unireg.indexer.GlobalIndexInterface;
import ch.vd.unireg.tiers.TypeTiers;

public class MessageIdentificationIndexerHibernateInterceptorTest extends BusinessTest {

	private GlobalIndexInterface index;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.index = getBean(GlobalIndexInterface.class, "globalMessageIdentificationIndex");
	}

	public MessageIdentificationIndexerHibernateInterceptorTest() {
		setWantIndexationMessagesIdentification(true);
	}

	protected IdentificationContribuable addIdentificationContribuable(CriteresPersonne criteres) {
		final EsbHeader header = new EsbHeader();
		header.setBusinessId("123456");
		header.setBusinessUser("Test");
		header.setReplyTo("Test");

		final Demande demande = new Demande();
		demande.setEmetteurId("empaciTao");
		demande.setMessageId("2222");
		demande.setPrioriteEmetteur(Demande.PrioriteEmetteur.NON_PRIORITAIRE);
		demande.setModeIdentification(Demande.ModeIdentificationType.MANUEL_AVEC_ACK);
		demande.setTypeMessage("LISTE_IS");
		demande.setDate(DateHelper.getCurrentDate());
		demande.setPeriodeFiscale(2010);
		demande.setPersonne(criteres);
		demande.setTypeDemande(TypeDemande.IMPOT_SOURCE);
		demande.setTypeContribuableRecherche(TypeTiers.PERSONNE_PHYSIQUE);

		final IdentificationContribuable message = new IdentificationContribuable();
		message.setHeader(header);
		message.setDemande(demande);
		message.setEtat(IdentificationContribuable.Etat.RECU);

		return hibernateTemplate.merge(message);
	}

	@Test
	public void testIndexationOnCreate() throws Exception {

		final int countAvant = index.getExactDocCount();
		Assert.assertEquals(0, countAvant);

		// on le crée
		final Long id = doInNewTransactionAndSession(status -> {
			final CriteresPersonne criteres = new CriteresPersonne();
			criteres.setDateNaissance(date(1971, 10));
			criteres.setNom("Dumoulin");
			criteres.setPrenoms("Alexandre");
			final IdentificationContribuable identCtb = addIdentificationContribuable(criteres);
			return identCtb.getId();
		});

		final int countApres = index.getExactDocCount();
		Assert.assertEquals(1, countApres);

		{
			// et si on le recherche par date de naissance, on devrait maintenant le trouver
			final IdentificationContribuableCriteria criteria = new IdentificationContribuableCriteria();
			criteria.setDateNaissance(date(1971, 10, 31));

			final List<MessageIdentificationIndexedData> result = globalMessageIdentificationSearcher.search(criteria, null, IdentificationContribuableEtatFilter.TOUS, null);
			Assert.assertNotNull(result);
			Assert.assertEquals(1, result.size());
			Assert.assertEquals(id, result.get(0).getId());
		}
		{
			// mais avec une date de naissance qui n'a rien à voir, on ne devrait plus rien trouver
			final IdentificationContribuableCriteria criteria = new IdentificationContribuableCriteria();
			criteria.setDateNaissance(date(1972, 12, 9));

			final List<MessageIdentificationIndexedData> result = globalMessageIdentificationSearcher.search(criteria, null, IdentificationContribuableEtatFilter.TOUS, null);
			Assert.assertNotNull(result);
			Assert.assertEquals(0, result.size());
		}
	}

	@Test
	public void testIndexationOnUpdate() throws Exception {

		final int countAvant = index.getExactDocCount();
		Assert.assertEquals(0, countAvant);

		// on le crée
		final Long id = doInNewTransactionAndSession(status -> {
			final CriteresPersonne criteres = new CriteresPersonne();
			criteres.setDateNaissance(date(1971, 10));
			criteres.setNom("Dumoulin");
			criteres.setPrenoms("Alexandre");
			final IdentificationContribuable identCtb = addIdentificationContribuable(criteres);
			return identCtb.getId();
		});

		final int countApresCreation = index.getExactDocCount();
		Assert.assertEquals(1, countApresCreation);

		// si on recherche par état, l'état RECU doit trouver quelque chose ...
		{
			final IdentificationContribuableCriteria criteria = new IdentificationContribuableCriteria();
			criteria.setEtatMessage(IdentificationContribuable.Etat.RECU);

			final List<MessageIdentificationIndexedData> result = globalMessageIdentificationSearcher.search(criteria, null, IdentificationContribuableEtatFilter.TOUS, null);
			Assert.assertNotNull(result);
			Assert.assertEquals(1, result.size());
			Assert.assertEquals(id, result.get(0).getId());
		}
		// ... mais pas avec l'état A_TRAITER_MANUELLEMENT
		{
			final IdentificationContribuableCriteria criteria = new IdentificationContribuableCriteria();
			criteria.setEtatMessage(IdentificationContribuable.Etat.A_TRAITER_MANUELLEMENT);

			final List<MessageIdentificationIndexedData> result = globalMessageIdentificationSearcher.search(criteria, null, IdentificationContribuableEtatFilter.TOUS, null);
			Assert.assertNotNull(result);
			Assert.assertEquals(0, result.size());
		}

		// si maintenant on change l'état en base, il doit être ré-indexé
		doInNewTransactionAndSession(status -> {
			final IdentificationContribuable ident = hibernateTemplate.get(IdentificationContribuable.class, id);
			Assert.assertNotNull(ident);
			ident.setEtat(IdentificationContribuable.Etat.A_TRAITER_MANUELLEMENT);
			return null;
		});

		// toujours un seul document dans l'indexeur, hein ?
		final int countApresModif = index.getExactDocCount();
		Assert.assertEquals(1, countApresModif);

		// si on recherche par état, l'état RECU ne doit plus rien trouver...
		{
			final IdentificationContribuableCriteria criteria = new IdentificationContribuableCriteria();
			criteria.setEtatMessage(IdentificationContribuable.Etat.RECU);

			final List<MessageIdentificationIndexedData> result = globalMessageIdentificationSearcher.search(criteria, null, IdentificationContribuableEtatFilter.TOUS, null);
			Assert.assertNotNull(result);
			Assert.assertEquals(0, result.size());
		}
		// ... mais l'état A_TRAITER_MANUELLEMENT, si
		{
			final IdentificationContribuableCriteria criteria = new IdentificationContribuableCriteria();
			criteria.setEtatMessage(IdentificationContribuable.Etat.A_TRAITER_MANUELLEMENT);

			final List<MessageIdentificationIndexedData> result = globalMessageIdentificationSearcher.search(criteria, null, IdentificationContribuableEtatFilter.TOUS, null);
			Assert.assertNotNull(result);
			Assert.assertEquals(1, result.size());
			Assert.assertEquals(id, result.get(0).getId());
		}
	}
}
