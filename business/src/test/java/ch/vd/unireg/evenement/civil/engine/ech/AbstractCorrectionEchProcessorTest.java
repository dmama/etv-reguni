package ch.vd.unireg.evenement.civil.engine.ech;

import org.junit.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockIndividuConnector;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivilEch;

public abstract class AbstractCorrectionEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	protected void doTest(final TypeEvenementCivilEch type, final ActionEvenementCivilEch action) throws Exception {

		final long noIndividu = 126673246L;
		final RegDate dateEvt = date(2011, 10, 31);
		final RegDate dateNaissance = date(1956, 4, 23);
		serviceCivil.setUp(new DefaultMockIndividuConnector(false) {
			@Override
			protected void init() {
				MockIndividu gerard = addIndividu(noIndividu, dateNaissance, "Gérard", "Manfind", true);
				addAdresse(gerard, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, dateNaissance, null);
			}
		});

		// mise en place fiscale
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, MockCommune.Lausanne);
			return pp.getNumero();
		});

		// événement de Correction d'adresse
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(11824L);
			evt.setType(type);
			evt.setAction(action);
			evt.setDateEvenement(dateEvt);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
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
