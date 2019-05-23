package ch.vd.unireg.evenement.civil.engine.ech;

import java.util.Collections;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockIndividuConnector;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockNationalite;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivilEch;

public class NaturalisationEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Test(timeout = 10000L)
	public void testNaturalisationAvant2014() throws Exception {

		final long noIndividu = 54678215611L;
		final RegDate dateNaissance = date(1956, 12, 12);
		final RegDate dateNaturalisation = date(2013, 11, 4);

		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Barbar", "Konan", true);
				addNationalite(ind, MockPays.Liechtenstein, dateNaissance, null);
			}
		});

		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, date(2000, 1, 1), MotifFor.ARRIVEE_HS, null, null, MockCommune.Renens, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
			return pp.getNumero();
		});

		doModificationIndividu(noIndividu, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setNationalites(Collections.singletonList((Nationalite) new MockNationalite(dateNaturalisation, null, MockPays.Suisse)));
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(135566L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateNaturalisation);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.NATURALISATION);
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
			Assert.assertEquals(dateNaturalisation.getOneDayAfter(), ffp.getDateDebut());
			Assert.assertEquals(MotifFor.PERMIS_C_SUISSE, ffp.getMotifOuverture());
			Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testNaturalisationApres2014() throws Exception {

		final long noIndividu = 54678215611L;
		final RegDate dateNaissance = date(1956, 12, 12);
		final RegDate dateNaturalisation = date(2014, 1, 6);

		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Barbar", "Konan", true);
				addNationalite(ind, MockPays.Liechtenstein, dateNaissance, null);
			}
		});

		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, date(2000, 1, 1), MotifFor.ARRIVEE_HS, null, null, MockCommune.Renens, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
			return pp.getNumero();
		});

		doModificationIndividu(noIndividu, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setNationalites(Collections.singletonList((Nationalite) new MockNationalite(dateNaturalisation, null, MockPays.Suisse)));
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(135566L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateNaturalisation);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.NATURALISATION);
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
			Assert.assertEquals(dateNaturalisation, ffp.getDateDebut());
			Assert.assertEquals(MotifFor.PERMIS_C_SUISSE, ffp.getMotifOuverture());
			Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
			return null;
		});
	}

	/**
	 * C'est le cas du SIFISC-24702 : un for principal était ouvert sur la commune de résidence secondaire vaudoise
	 */
	@Test
	public void testNaturalisationEtrangerHorsCantonEnSecondaireDansLeCanton() throws Exception {

		final long noIndividu = 14781548L;
		final RegDate dateNaissance = date(1956, 12, 12);
		final RegDate dateArriveeSecondaire = date(2010, 10, 4);
		final RegDate dateNaturalisation = date(2016, 1, 6);

		// mise en place civile -> étranger résident HC inscrit en secondaire sur VD
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Barbar", "Konan", true);
				addNationalite(ind, MockPays.Liechtenstein, dateNaissance, null);
				addAdresse(ind, TypeAdresseCivil.COURRIER, MockRue.Aubonne.CheminDesClos, null, dateArriveeSecondaire, null);
				final MockAdresse sec = addAdresse(ind, TypeAdresseCivil.SECONDAIRE, MockRue.Aubonne.CheminDesClos, null, dateArriveeSecondaire, null);
				sec.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Bern.getNoOFS(), null));
			}
		});

		// mise en place fiscale... aucun for pour le moment (= résidence en secondaire seulement !)
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
			return pp.getNumero();
		});

		doModificationIndividu(noIndividu, individu -> individu.setNationalites(Collections.singletonList((Nationalite) new MockNationalite(dateNaturalisation, null, MockPays.Suisse))));

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(135566L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateNaturalisation);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.NATURALISATION);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement synchrone de l'événement
		traiterEvenements(noIndividu);

		// vérification des résultats
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

			// aucun for sur l'individu (un for sur Aubonne était créé par erreur...)
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			Assert.assertNotNull(pp);

			final Set<ForFiscal> fors = pp.getForsFiscaux();
			Assert.assertNotNull(fors);
			Assert.assertEquals(Collections.emptySet(), fors);
			return null;
		});
	}
}
