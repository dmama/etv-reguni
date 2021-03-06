package ch.vd.unireg.evenement.civil.engine.ech;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockIndividuConnector;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeEvenementCivilEch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class EnregistrementPartenariatEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Test(timeout = 10000L)
	public void testEnregistrementPartenariat() throws Exception {

		final long noPrincipal = 78215611L;
		final long noConjoint = 46215611L;
		final RegDate dateEnregistrement = RegDate.get().addMonths(-1);

		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				MockIndividu principal = addIndividu(noPrincipal, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				MockIndividu conjoint = addIndividu(noConjoint, date(1974, 8, 1), "David", "Bouton", true);
				marieIndividus(principal, conjoint, dateEnregistrement);
			}
		});

		doInNewTransactionAndSession(status -> {
			PersonnePhysique monsieur = addHabitant(noPrincipal);
			addForPrincipal(monsieur, date(1943, 2, 12), MotifFor.MAJORITE, MockCommune.Echallens);
			PersonnePhysique madame = addHabitant(noConjoint);
			addForPrincipal(madame, date(1992, 8, 1), MotifFor.MAJORITE, MockCommune.Chamblon);
			return null;
		});

		// événement civil
		final long eventId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(1235563456L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateEnregistrement);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noPrincipal);
			evt.setType(TypeEvenementCivilEch.ENREGISTREMENT_PARTENARIAT);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement synchrone de l'événement
		traiterEvenements(noPrincipal);

		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(eventId);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

			final PersonnePhysique principal = tiersService.getPersonnePhysiqueByNumeroIndividu(noPrincipal);
			assertNotNull(principal);

			final PersonnePhysique conjoint = tiersService.getPersonnePhysiqueByNumeroIndividu(noConjoint);
			assertNotNull(conjoint);

			final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(conjoint, dateEnregistrement);
			assertNotNull(ensemble);
			assertSame(principal, ensemble.getPrincipal());
			assertSame(conjoint, ensemble.getConjoint());
			return null;
		});
	}
}
