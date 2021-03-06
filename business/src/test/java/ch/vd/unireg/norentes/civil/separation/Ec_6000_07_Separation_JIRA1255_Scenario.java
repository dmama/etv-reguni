package ch.vd.unireg.norentes.civil.separation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.metier.MetierService;
import ch.vd.unireg.metier.MetierServiceException;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.SituationFamille;
import ch.vd.unireg.tiers.SituationFamilleMenageCommun;
import ch.vd.unireg.type.EtatCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TarifImpotSource;
import ch.vd.unireg.type.TypeEvenementCivil;

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

	private static final long noIndPatrick = 366345; // Patrick
	private static final long noIndSylvie = 367312; // Sylvie

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
		serviceCivilService.setUp(new MockIndividuConnector() {

			@Override
			protected void init() {

				indPatrick = addIndividu(noIndPatrick, dateNaissancePatrick, "Fahrni", "Patrick", true);
				indSylvie = addIndividu(noIndSylvie, dateNaissanceSylvie, "Fahrni", "Sylvie", false);

				final RegDate dateMariageCivil = RegDate.get(1996, 7, 12);
				final RegDate dateSeparationCivil = RegDate.get(2008, 1, 1);

				marieIndividus(indPatrick, indSylvie, dateMariageCivil);
				separeIndividus(indPatrick, indSylvie, dateSeparationCivil);

				addOrigine(indPatrick, MockCommune.Echallens);
				addNationalite(indPatrick, MockPays.Suisse, dateNaissancePatrick, null);

				addOrigine(indSylvie, MockCommune.Geneve);
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
			addForFiscalPrincipal(menage, commune, dateMariage, null, MotifFor.INDETERMINE, null);

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
					"Le dernier for n'est pas sur " + commune.getNomOfficiel());

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
			metierService.separe((MenageCommun) tiersDAO.get(noMenage), dateSeparation, null, EtatCivil.SEPARE, null);
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
