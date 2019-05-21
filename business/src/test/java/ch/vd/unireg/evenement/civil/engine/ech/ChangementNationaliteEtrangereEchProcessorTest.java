package ch.vd.unireg.evenement.civil.engine.ech;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockIndividuConnector;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockNationalite;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeEvenementCivilEch;

public class ChangementNationaliteEtrangereEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Test(timeout = 10000L)
	public void testChangementNationaliteEtrangere() throws Exception {

		final long noIndividu = 546782151L;
		final RegDate dateNaissance = date(1956, 12, 12);
		final RegDate dateArrivee = date(2000, 1, 1);
		final RegDate dateChangement = RegDate.get().addMonths(-1);

		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Barbar", "Konan", true);
				addNationalite(ind, MockPays.Liechtenstein, dateNaissance, null);
			}
		});

		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, null, null, MockCommune.Renens, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
			return pp.getNumero();
		});

		doModificationIndividu(noIndividu, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setNationalites(Collections.singletonList((Nationalite) new MockNationalite(dateChangement, null, MockPays.Albanie)));
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(135566L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateChangement);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.CHGT_NATIONALITE_ETRANGERE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement synchrone de l'événement
		traiterEvenements(noIndividu);

		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

			final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			Assert.assertNotNull(pp);
			Assert.assertEquals((Long) ppId, pp.getNumero());

			final ForFiscalPrincipalPP ffp = pp.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(dateArrivee, ffp.getDateDebut());
			Assert.assertEquals(MotifFor.ARRIVEE_HS, ffp.getMotifOuverture());
			Assert.assertEquals(ModeImposition.SOURCE, ffp.getModeImposition());
			return null;
		});
	}
}
