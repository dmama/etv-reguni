package ch.vd.unireg.evenement.civil.engine.ech;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivilEch;

public class ContactEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Override
	public void onSetUp() throws Exception {
		setWantIndexationTiers(true);
		super.onSetUp();
	}
	
	private void doTest(final ActionEvenementCivilEch action) throws Exception {
		final long noIndividu = 24789565865237L;
		final RegDate dateNaissance = date(1980, 4, 23);
		final RegDate dateMajorite = dateNaissance.addYears(18);
		final RegDate dateEvt = RegDate.get().addMonths(-1);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Téléfon", "Gaston", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateNaissance, null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateMajorite, MotifFor.MAJORITE, MockCommune.Echallens);
			return pp.getNumero();
		});

		globalTiersIndexer.sync();

		// recherche sur Prilly
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setLocaliteOuPays(MockLocalite.Prilly.getNom());
		final List<TiersIndexedData> res = globalTiersSearcher.search(criteria);
		Assert.assertEquals(0, res.size());

		// modification du civil pour ajout de l'adresse de contact
		doModificationIndividu(noIndividu, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				final MockAdresse adr = new MockAdresse();
				adr.setTypeAdresse(TypeAdresseCivil.COURRIER);
				adr.setCommuneAdresse(MockCommune.Prilly);
				adr.setDateDebutValidite(dateEvt);
				adr.setLocalite(MockLocalite.Prilly.getNom());
				adr.setNoOfsPays(MockPays.Suisse.getNoOFS());
				adr.setNpa(Integer.toString(MockLocalite.Prilly.getNPA()));
				adr.setNumeroRue(MockRue.Prilly.CheminDeLaPossession.getNoRue());
				individu.addAdresse(adr);
			}
		});

		// création de l'événement civil
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(11824L);
			evt.setType(TypeEvenementCivilEch.CONTACT);
			evt.setAction(action);
			evt.setDateEvenement(dateEvt);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
			return null;
		});

		// attente de fin d'indexation
		globalTiersIndexer.sync();

		// recherche sur Prilly -> maintenant, il est là!
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
