package ch.vd.unireg.evenement.civil.engine.ech;


import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockIndividuConnector;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.SituationFamille;
import ch.vd.unireg.tiers.SituationFamilleMenageCommun;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatCivil;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TarifImpotSource;
import ch.vd.unireg.type.TypeEvenementCivilEch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

abstract public class AnnulationOuCessationSeparationEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	/**
	 * Test les effets de bord de l'annulation ou de la cessation de séparation sur les situations de famille unireg
	 * pour un menage avec une seul personne physique
	 * @throws Exception
	 */
	public void testSituationFamille(final ch.vd.unireg.type.EtatCivil etatCivilEnMenage,
	                                 final ch.vd.unireg.type.EtatCivil etatCivilSepare,
	                                 final ActionEvenementCivilEch typeAction,
	                                 final TypeEvenementCivilEch typeEvenement) throws Exception {

		final long noMonsieur = 411587L;
		final long noMonsieurDame = 411588L;
		final RegDate dateNaissance = date(1957, 3, 29);
		final RegDate dateMariage = date(2005, 5, 5);
		final RegDate dateSeparation = date(2008, 11, 23);
		final RegDate dateReconciliation = date(2009, 1, 15);

		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu monsieur = addIndividu(noMonsieur, dateNaissance, "Lambert", "Christophe", true);
				final MockIndividu monsieurDame = addIndividu(noMonsieurDame, dateNaissance, "Chapot", "Michel(le)", isPacs(etatCivilEnMenage));
				addNationalite(monsieur, MockPays.Suisse, dateNaissance, null);
				addNationalite(monsieur, MockPays.Suisse, dateNaissance, null);
				marieIndividus(monsieur, monsieurDame, dateMariage);
			}
		});

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique monsieur = addHabitant(noMonsieur);
			addForPrincipal(monsieur,
			                dateNaissance.addYears(18), MotifFor.MAJORITE,
			                dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
			                MockCommune.Echallens);
			addForPrincipal(monsieur,
			                dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
			                MockCommune.Echallens);

			final PersonnePhysique monsieurDame = addHabitant(noMonsieurDame);
			addForPrincipal(monsieurDame,
			                dateNaissance.addYears(18), MotifFor.MAJORITE,
			                dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
			                MockCommune.Lausanne);
			addForPrincipal(monsieurDame,
			                dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
			                MockCommune.Echallens);


			final MenageCommun menage = addEnsembleTiersCouple(monsieur, monsieurDame, dateMariage, dateSeparation.getOneDayBefore()).getMenage();
			addForPrincipal(menage,
			                dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
			                dateSeparation.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
			                MockCommune.Echallens);

			addSituation(monsieur, dateNaissance, dateMariage.getOneDayBefore(), 0, EtatCivil.CELIBATAIRE);
			addSituation(monsieurDame, dateNaissance, dateMariage.getOneDayBefore(), 0, EtatCivil.CELIBATAIRE);
			addSituation(menage, dateMariage, dateSeparation.getOneDayBefore(), 0, TarifImpotSource.NORMAL, etatCivilEnMenage);
			addSituation(monsieur, dateSeparation, null, 0, etatCivilSepare);
			addSituation(monsieurDame, dateSeparation, null, 0, etatCivilSepare);
			return null;
		});

		final RegDate dateEvenement = typeEvenement == TypeEvenementCivilEch.CESSATION_SEPARATION ? dateReconciliation : dateSeparation;

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long evtMonsieurId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(454563457L);
			evt.setAction(typeAction);
			evt.setDateEvenement(dateEvenement);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noMonsieur);
			evt.setType(typeEvenement);
			return hibernateTemplate.merge(evt).getId();
		});
		// traitement synchrone de l'événement
		traiterEvenements(noMonsieur);

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long evtMonsieurDameId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(454563458L);
			evt.setAction(typeAction);
			evt.setDateEvenement(dateEvenement);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noMonsieurDame);
			evt.setType(typeEvenement);
			return hibernateTemplate.merge(evt).getId();
		});
		// traitement synchrone de l'événement
		traiterEvenements(noMonsieurDame);

		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtMonsieurId);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
			final EvenementCivilEch evt2 = evtCivilDAO.get(evtMonsieurDameId);
			assertNotNull(evt2);
			assertEquals(EtatEvenementCivil.REDONDANT, evt2.getEtat());
			final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
			assertNotNull(monsieur);
			final SituationFamille situationDeFamillePersonnelle = monsieur.getSituationFamilleActive();
			assertNull(situationDeFamillePersonnelle);
			final PersonnePhysique monsieurDame = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieurDame);
			assertNotNull(monsieurDame);
			final SituationFamille situationDeFamillePersonnelle2 = monsieur.getSituationFamilleActive();
			assertNull(situationDeFamillePersonnelle2);
			final MenageCommun menage = tiersService.getEnsembleTiersCouple(monsieur, null).getMenage();
			assertNotNull(menage);
			final SituationFamilleMenageCommun situationDeFamilleMenage = (SituationFamilleMenageCommun) menage.getSituationFamilleActive();
			assertNotNull(situationDeFamilleMenage);
			assertNull(situationDeFamilleMenage.getDateFin());
			assertEquals(etatCivilEnMenage, situationDeFamilleMenage.getEtatCivil());
			return null;
		});
	}

	/**
	 * Test les effets de bord de l'annulation ou de la cessation de séparation sur les situations de famille unireg
	 * pour un menage avec une seul personne physique
	 * @throws Exception
	 */
	public void testSituationFamilleMarieSeul(final ch.vd.unireg.type.EtatCivil etatCivilEnMenage,
	                                          final ch.vd.unireg.type.EtatCivil etatCivilSepare,
	                                          final ActionEvenementCivilEch typeAction,
	                                          final TypeEvenementCivilEch typeEvenement) throws Exception {

		final long noMonsieur = 411587L;
		final RegDate dateNaissance = date(1957, 3, 29);
		final RegDate dateMariage = date(2005, 5, 5);
		final RegDate dateSeparation = date(2008, 11, 23);

		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu monsieur = addIndividu(noMonsieur, dateNaissance, "Lambert", "Christophe", true);
				addNationalite(monsieur, MockPays.Suisse, dateNaissance, null);
				if (isPacs(etatCivilEnMenage)) {
					pacseIndividu(monsieur, dateMariage);
				}
				else {
					marieIndividu(monsieur, dateMariage);
				}
			}
		});

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique monsieur = addHabitant(noMonsieur);
			final MenageCommun menage = addEnsembleTiersCouple(monsieur, null, dateMariage, dateSeparation.getOneDayBefore()).getMenage();
			addForPrincipal(monsieur,
			                dateNaissance.addYears(18), MotifFor.MAJORITE,
			                dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
			                MockCommune.Echallens);
			addForPrincipal(menage,
			                dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
			                dateSeparation.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
			                MockCommune.Echallens);
			addSituation(monsieur, dateNaissance, dateMariage.getOneDayBefore(), 0, EtatCivil.CELIBATAIRE);
			addSituation(menage, dateMariage, dateSeparation.getOneDayBefore(), 0, TarifImpotSource.NORMAL, etatCivilEnMenage);
			addSituation(monsieur, dateSeparation, null, 0, etatCivilSepare);
			return null;
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long annulationSeparationId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(454563457L);
			evt.setAction(typeAction);
			evt.setDateEvenement(dateSeparation);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noMonsieur);
			evt.setType(typeEvenement);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement synchrone de l'événement
		traiterEvenements(noMonsieur);

		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(annulationSeparationId);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
			final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
			assertNotNull(monsieur);
			final SituationFamille situationDeFamillePersonnelle = monsieur.getSituationFamilleActive();
			assertNull(situationDeFamillePersonnelle);
			final MenageCommun menage = tiersService.getEnsembleTiersCouple(monsieur, null).getMenage();
			assertNotNull(menage);
			final SituationFamilleMenageCommun situationDeFamilleMenage = (SituationFamilleMenageCommun) menage.getSituationFamilleActive();
			assertNotNull(situationDeFamilleMenage);
			assertNull(situationDeFamilleMenage.getDateFin());
			assertEquals(etatCivilEnMenage, situationDeFamilleMenage.getEtatCivil());
			return null;
		});
	}

	private boolean isPacs(EtatCivil etatCivilEnMenage) {
		final boolean pacs;
		if (etatCivilEnMenage == EtatCivil.MARIE) {
			pacs = false;
		} else if (etatCivilEnMenage == EtatCivil.LIE_PARTENARIAT_ENREGISTRE) {
			pacs = true;
		} else {
			throw new IllegalArgumentException("etatCivilEnMenage ne peut prendre que les valeurs " + EtatCivil.MARIE.name() + " ou " + EtatCivil.LIE_PARTENARIAT_ENREGISTRE.name() );
		}
		return pacs;
	}

}
