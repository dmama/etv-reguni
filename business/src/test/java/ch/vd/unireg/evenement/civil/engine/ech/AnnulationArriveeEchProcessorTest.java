package ch.vd.unireg.evenement.civil.engine.ech;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.TypeEvenementCivilEch;

public class AnnulationArriveeEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {
	
	@Test(timeout = 10000L)
	public void testAnnulationArriveeDeMineur() throws Exception {

		final long noIndividu = 2378435L;
		final RegDate dateNaissance = RegDate.get().addYears(-15);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				addIndividu(noIndividu, dateNaissance, "Poucet", "Lepeti", true);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			return pp.getNumero();
		});

		// création de l'événement civil d'annulation d'arrivée
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(67485482L);
			evt.setDateEvenement(RegDate.get().getOneDayBefore());
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			evt.setAction(ActionEvenementCivilEch.ANNULATION);
			evt.setNumeroIndividu(noIndividu);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
			return null;
		});
	}
}
