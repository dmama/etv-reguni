package ch.vd.unireg.evenement.civil.engine.ech;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.tiers.AppartenanceMenage;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementCivilEch;
import ch.vd.unireg.type.TypeRapportEntreTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DissolutionPartenariatEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Test(timeout = 10000L)
	public void testDissolutionPartenariat() throws Exception {

		final long noPrincipal = 78215611L;
		final long noConjoint = 46215611L;
		final RegDate dateEnregistrement = date(2005, 5, 5);
		final RegDate dateDissolution = date(2008, 11, 23);

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu principal = addIndividu(noPrincipal, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				addNationalite(principal, MockPays.Suisse, date(1923, 2, 12), null);
				final MockIndividu conjoint = addIndividu(noConjoint, date(1974, 8, 1), "David", "Bouton", true);
				addNationalite(conjoint, MockPays.France, date(1974, 8, 1), null);
				marieIndividus(principal, conjoint, dateEnregistrement);
				divorceIndividus(principal, conjoint, dateDissolution);
			}
		});

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique principal = addHabitant(noPrincipal);
			final PersonnePhysique conjoint = addHabitant(noConjoint);
			final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(principal, conjoint, dateEnregistrement, null);
			addForPrincipal(ensemble.getMenage(), dateEnregistrement, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
			return null;
		});

		// événement civil
		final long eventId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(454563456L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateDissolution);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noPrincipal);
			evt.setType(TypeEvenementCivilEch.DISSOLUTION_PARTENARIAT);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement synchrone de l'événement
		traiterEvenements(noPrincipal);

		// on vérifie que le ménage-commun a bien été dissolu dans le fiscal
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(eventId);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

			final PersonnePhysique principal = tiersService.getPersonnePhysiqueByNumeroIndividu(noPrincipal);
			assertNotNull(principal);

			final AppartenanceMenage appartenancePrincipal = (AppartenanceMenage) principal.getRapportSujetValidAt(dateDissolution.getOneDayBefore(), TypeRapportEntreTiers.APPARTENANCE_MENAGE);
			assertNotNull(appartenancePrincipal);
			assertEquals(dateEnregistrement, appartenancePrincipal.getDateDebut());
			assertEquals(dateDissolution.getOneDayBefore(), appartenancePrincipal.getDateFin());

			final PersonnePhysique conjoint = tiersService.getPersonnePhysiqueByNumeroIndividu(noConjoint);
			assertNotNull(conjoint);

			final AppartenanceMenage appartenanceConjoint = (AppartenanceMenage) conjoint.getRapportSujetValidAt(dateDissolution.getOneDayBefore(), TypeRapportEntreTiers.APPARTENANCE_MENAGE);
			assertNotNull(appartenanceConjoint);
			assertEquals(dateEnregistrement, appartenanceConjoint.getDateDebut());
			assertEquals(dateDissolution.getOneDayBefore(), appartenanceConjoint.getDateFin());

			assertNull(tiersService.getEnsembleTiersCouple(conjoint, dateDissolution));
			return null;
		});
	}

	/**
	 * Problème soulevé par SIFISC-6624: l'état civil des individus concerné par une dissolution de
	 * partenariat par annulation sont dans interprété dans unireg comme étant non marié
	 * on devrait sans doute traité leur cas comme une annulation de mariage et non comme un
	 * divorce
	 *
	 * @throws Exception
	 */
	@Test(timeout = 10000L)
	public void testDissolutionPartenariatParAnnulation() throws Exception {

		final long noPrincipal = 78215611L;
		final long noConjoint = 46215611L;
		final long eventId = 454563456L;
		final RegDate dateEnregistrement = date(2005, 5, 5);
		final RegDate dateDissolution = date(2008, 11, 23);

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu principal = addIndividu(noPrincipal, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				addNationalite(principal, MockPays.Suisse, date(1923, 2, 12), null);
				final MockIndividu conjoint = addIndividu(noConjoint, date(1974, 8, 1), "David", "Bouton", true);
				addNationalite(conjoint, MockPays.France, date(1974, 8, 1), null);
				marieIndividus(principal, conjoint, dateEnregistrement);
				dissouePartenartiatParAnnulation(principal, conjoint, dateDissolution);
				addIndividuAfterEvent(eventId, principal, dateDissolution, TypeEvenementCivilEch.DISSOLUTION_PARTENARIAT);
			}
		});

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique principal = addHabitant(noPrincipal);
			final PersonnePhysique conjoint = addHabitant(noConjoint);
			final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(principal, conjoint, dateEnregistrement, null);
			addForPrincipal(ensemble.getMenage(), dateEnregistrement, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
			return null;
		});


		// événement civil
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(eventId);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateDissolution);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noPrincipal);
			evt.setType(TypeEvenementCivilEch.DISSOLUTION_PARTENARIAT);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement synchrone de l'événement
		traiterEvenements(noPrincipal);

		// on vérifie que le ménage-commun a bien été dissolu dans le fiscal
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(eventId);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			assertTrue(evt.getErreurs().iterator().hasNext());
			assertContains("dissolution de partenariat pour motif annulation", evt.getErreurs().iterator().next().getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDissolutionPartenariatAvecDecision() throws Exception {

		final long noPrincipal = 78215611L;
		final long noConjoint = 46215611L;
		final long eventId = 454563456L;
		final RegDate dateEnregistrement = date(2005, 5, 5);
		final RegDate dateDissolution = date(2008, 11, 23);

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu principal = addIndividu(noPrincipal, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				addNationalite(principal, MockPays.Suisse, date(1923, 2, 12), null);
				final MockIndividu conjoint = addIndividu(noConjoint, date(1974, 8, 1), "David", "Bouton", true);
				addNationalite(conjoint, MockPays.France, date(1974, 8, 1), null);
				marieIndividus(principal, conjoint, dateEnregistrement);
				divorceIndividus(principal, conjoint, dateDissolution);
			}
		});

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique principal = addHabitant(noPrincipal);
			final PersonnePhysique conjoint = addHabitant(noConjoint);
			final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(principal, conjoint, dateEnregistrement, null);
			addForPrincipal(ensemble.getMenage(), dateEnregistrement, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
			addDecisionAci(principal, dateEnregistrement, null, MockCommune.Aigle.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null);
			return null;
		});


		// événement civil
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(eventId);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateDissolution);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noPrincipal);
			evt.setType(TypeEvenementCivilEch.DISSOLUTION_PARTENARIAT);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement synchrone de l'événement
		traiterEvenements(noPrincipal);

		// on vérifie que le ménage-commun a bien été dissolu dans le fiscal
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(eventId);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noPrincipal);
			Assert.assertNotNull(monsieur);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());
			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			String message = String.format("Le contribuable trouvé (%s) est sous l'influence d'une décision ACI",
			                               FormatNumeroHelper.numeroCTBToDisplay(monsieur.getNumero()));
			Assert.assertEquals(message, erreur.getMessage());
			return null;
		});
	}
}
