package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.model.mock.MockAdresse;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class ContactEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Override
	protected void runOnSetUp() throws Exception {
		setWantIndexation(true);
		super.runOnSetUp();
	}
	
	private void doTest(final ActionEvenementCivilEch action) throws Exception {
		final long noIndividu = 24789565865237L;
		final RegDate dateNaissance = date(1980, 4, 23);
		final RegDate dateMajorite = dateNaissance.addYears(18);
		final RegDate dateEvt = RegDate.get().addMonths(-1);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Téléfon", "Gaston", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateNaissance, null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateMajorite, MotifFor.MAJORITE, MockCommune.Echallens);
				return pp.getNumero();
			}
		});

		globalTiersIndexer.sync();

		// recherche sur lausanne
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setLocaliteOuPays(MockLocalite.Lausanne.getNomCompletMinuscule());
		final List<TiersIndexedData> res = globalTiersSearcher.search(criteria);
		Assert.assertEquals(0, res.size());

		// modification du civil pour ajout de l'adresse de contact
		doModificationIndividu(noIndividu, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				final MockAdresse adr = new MockAdresse();
				adr.setTypeAdresse(TypeAdresseCivil.COURRIER);
				adr.setCommuneAdresse(MockCommune.Lausanne);
				adr.setDateDebutValidite(dateEvt);
				adr.setLocalite(MockLocalite.Lausanne.getNomCompletMinuscule());
				adr.setNoOfsPays(MockPays.Suisse.getNoOFS());
				adr.setNpa(Integer.toString(MockLocalite.Lausanne.getNPA()));
				adr.setNumeroRue(MockRue.Lausanne.AvenueDeBeaulieu.getNoRue());
				individu.getAdresses().add(adr);
			}
		});

		// création de l'événement civil
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(11824L);
				evt.setType(TypeEvenementCivilEch.CONTACT);
				evt.setAction(action);
				evt.setDateEvenement(dateEvt);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				return null;
			}
		});

		// attente de fin d'indexation
		globalTiersIndexer.sync();

		// recherche sur lausanne -> maintenant, il est là!
		final List<TiersIndexedData> resApres = globalTiersSearcher.search(criteria);
		Assert.assertEquals(1, resApres.size());

		final TiersIndexedData data = resApres.get(0);
		Assert.assertNotNull(data);
		Assert.assertEquals((Long) ppId, data.getNumero());
	}

	@Test(timeout = 10000L)
	public void testAnnonceContact() throws Exception {
		doTest(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
	}
	
	@Test(timeout = 10000L)
	public void testCorrectionContact() throws Exception {
		doTest(ActionEvenementCivilEch.CORRECTION);
	}

	@Test(timeout = 10000L)
	public void testAnnulationContact() throws Exception {
		doTest(ActionEvenementCivilEch.ANNULATION);
	}
}
