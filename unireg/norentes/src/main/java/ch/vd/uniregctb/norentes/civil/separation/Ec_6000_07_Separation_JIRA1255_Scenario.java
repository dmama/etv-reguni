package ch.vd.uniregctb.norentes.civil.separation;

import annotation.Check;
import annotation.Etape;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.tiers.SituationFamilleMenageCommun;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TarifImpotSource;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Scénario de séparation correspondant à UNIREG-1255.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_6000_07_Separation_JIRA1255_Scenario extends EvenementCivilScenario {

	private MetierService metierService;

	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	public static final String NAME = "6000_07_Separation";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.SEPARATION;
	}

	@Override
	public String getDescription() {
		return "Séparation de deux étrangers: l'un avec permis C, l'autre avec permis B";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndPatrick = 366345; // Patrick
	private final long noIndSylvie = 367312; // Sylvie

	private MockIndividu indPatrick;
	private MockIndividu indSylvie;

	private long noHabPatrick;
	private long noHabSylvie;
	private long noMenage;

	private final RegDate dateNaissancePatrick = RegDate.get(1965, 8, 8);	// 08.08.1965
	private final RegDate dateNaissanceSylvie = RegDate.get(1973, 1, 25);	// 25.01.1973
	private final RegDate dateMariage = RegDate.get(1981, 1, 6);			// 06.01.1981
	private final RegDate dateSeparation = RegDate.get(2008, 1, 1);			// 01.01.2008

	private final Commune commune = MockCommune.VillarsSousYens;

	private boolean errorFound;
	private String errorMessage;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {

			@Override
			protected void init() {

				indPatrick = addIndividu(noIndPatrick, dateNaissancePatrick, "Fahrni", "Patrick", true);
				indSylvie = addIndividu(noIndSylvie, dateNaissanceSylvie, "Fahrni", "Sylvie", false);

				final RegDate dateMariageCivil = RegDate.get(1996, 7, 12);
				final RegDate dateSeparationCivil = RegDate.get(2008, 1, 1);

				marieIndividus(indPatrick, indSylvie, dateMariageCivil);
				separeIndividus(indPatrick, indSylvie, dateSeparationCivil);

				addOrigine(indPatrick, MockPays.Suisse, null, dateNaissancePatrick);
				addNationalite(indPatrick, MockPays.Suisse, dateNaissancePatrick, null);

				addOrigine(indSylvie, MockPays.Suisse, null, dateNaissanceSylvie);
				addNationalite(indSylvie, MockPays.Suisse, dateNaissanceSylvie, null);
			}

		});
	}

	@SuppressWarnings("deprecation")
	@Etape(id=1, descr="Chargement des habitants et du ménage commun")
	public void etape1() {
		// Patrick
		final PersonnePhysique patrick = addHabitant(noIndPatrick);
		{
			noHabPatrick = patrick.getNumero();
		}

		// Sylvie
		final PersonnePhysique sylvie = addHabitant(noIndSylvie);
		{
			noHabSylvie = sylvie.getNumero();
		}

		// ménage
		{
			MenageCommun menage = new MenageCommun();
			menage = (MenageCommun) tiersDAO.save(menage);
			noMenage = menage.getNumero();
			tiersService.addTiersToCouple(menage, patrick, dateMariage, RegDate.get(2008, 12, 31)).setAnnule(true);
			tiersService.addTiersToCouple(menage, patrick, dateMariage, null);
			tiersService.addTiersToCouple(menage, sylvie, dateMariage, RegDate.get(2008, 12, 31)).setAnnule(true);
			tiersService.addTiersToCouple(menage, sylvie, dateMariage, null);
			final ForFiscalPrincipal f = addForFiscalPrincipal(menage, commune, dateMariage, null, MotifFor.INDETERMINE, null);
			f.setModeImposition(ModeImposition.ORDINAIRE);

			SituationFamilleMenageCommun sf = new SituationFamilleMenageCommun();
			sf.setDateDebut(RegDate.get(2009, 1, 1));
			sf.setNombreEnfants(0);
			sf.setTarifApplicable(TarifImpotSource.NORMAL);
			sf.setEtatCivil(EtatCivil.MARIE);
			sf.setContribuablePrincipalId(patrick.getId());
			menage.addSituationFamille(sf);
		}
	}

	@Check(id=1, descr="Vérifie que les habitants n'ont pas de For ouvert et le For du ménage existe")
	public void check1() {
		{
			final PersonnePhysique patrick = (PersonnePhysique) tiersDAO.get(noHabPatrick);
			final ForFiscalPrincipal ffp = patrick.getDernierForFiscalPrincipal();
			assertNull(ffp, "For principal de l'Habitant " + patrick.getNumero() + " non null");
		}

		{
			final PersonnePhysique sylvie = (PersonnePhysique)tiersDAO.get(noHabSylvie);
			final ForFiscalPrincipal ffp = sylvie.getDernierForFiscalPrincipal();
			assertNull(ffp, "For principal de l'Habitant " + sylvie.getNumero() + " non null");
		}

		{
			final MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(commune.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(),
					"Le dernier for n'est pas sur " + commune.getNomMinuscule());

			{
				SituationFamille sf = mc.getSituationFamilleActive();
				assertNotNull(sf, "Aucune situation famille trouvée");
				assertEquals(sf.getDateDebut(), RegDate.get(2009, 1, 1), "Situation famille: Mauvaise date de début");
				assertNull(sf.getDateFin(), "Situation famille: Mauvaise date de fin");
			}
		}
	}

	@Etape(id=2, descr="Envoi de l'événement de séparation")
	public void etape2() throws Exception {
		try {
			metierService.separe((MenageCommun) tiersDAO.get(noMenage), dateSeparation, null, EtatCivil.SEPARE, true, null);
		}
		catch(MetierServiceException eche) {
			errorFound = true;
			errorMessage = eche.getMessage();
		}
	}

	@Check(id=2, descr="Vérifie qu'il y a eu bien une erreur")
	public void check2() {
		assertTrue(errorFound, "Le traitement aurait dû générer une erreur");
		assertEquals("Des situations famille actives existent après la date de séparation. Veuillez les annuler manuellement.", errorMessage, "L'erreur est pas la bonne");
	}
}
