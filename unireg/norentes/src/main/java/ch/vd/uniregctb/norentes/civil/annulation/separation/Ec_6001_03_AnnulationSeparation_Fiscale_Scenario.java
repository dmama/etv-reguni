package ch.vd.uniregctb.norentes.civil.annulation.separation;

import annotation.Check;
import annotation.Etape;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;

/**
 * Scénario d'annulation de séparation d'un couple dans le registre fiscal.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_6001_03_AnnulationSeparation_Fiscale_Scenario extends AbstractAnnulationSeparationScenario {

	private MetierService metierService;

	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	public static final String NAME = "6001_03_AnnulationSeparation";

	@Override
	public String getDescription() {
		return "Scénario d'un événement annulation de séparation pour un couple.";
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {

			@Override
			protected void init() {

				MockIndividu andrea = addIndividu(noIndAndrea, dateNaissanceAndrea, "Polari", "Andrea", true);
				addOrigine(andrea, MockPays.Suisse.getNomMinuscule());
				addNationalite(andrea, MockPays.Suisse, dateNaissanceAndrea, null);

				MockIndividu liliana = addIndividu(noIndLiliana, dateNaissanceLiliana, "Polari", "Liliana", false);
				addOrigine(liliana, MockPays.Suisse.getNomMinuscule());
				addNationalite(liliana, MockPays.Suisse, dateNaissanceLiliana, null);

				marieIndividus(andrea, liliana , dateMariage);
				separeIndividus(andrea, liliana, dateSeparation);
			}
		});
	}

	private final MockCommune commune = MockCommune.Lausanne;
	private final RegDate dateNaissanceAndrea = RegDate.get(1972, 9, 23); 	// 23.09.1972
	private final RegDate dateNaissanceLiliana = RegDate.get(1973, 8, 25); 	// 25.08.1973
	private final RegDate dateMariage = RegDate.get(2002, 1, 6); 			// 06.01.2002
	private final RegDate dateSeparation = RegDate.get(2008, 5, 5); 		// 05.05.2008

	private final long noIndAndrea = 650499; // Andrea
	private final long noIndLiliana = 671500; // Liliana

	private long noHabAndrea;
	private long noHabLiliana;
	private long noMenage;

	@Etape(id=1, descr="Chargement du couple Andrea-Liliana")
	public void step1() {
		// Andrea
		PersonnePhysique andrea = addHabitant(noIndAndrea);
		noHabAndrea = andrea.getNumero();
		{
			ForFiscalPrincipal ffp = addForFiscalPrincipal(andrea, commune, dateSeparation, null, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null);
			ffp.setModeImposition(ModeImposition.ORDINAIRE);
		}

		// Liliana
		PersonnePhysique liliana = addHabitant(noIndLiliana);
		noHabLiliana = liliana.getNumero();
		{
			ForFiscalPrincipal ffp = addForFiscalPrincipal(liliana, commune, dateSeparation, null, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null);
			ffp.setModeImposition(ModeImposition.ORDINAIRE);
		}

		// Ménage commun
		MenageCommun menage = (MenageCommun) tiersDAO.save(new MenageCommun());
		noMenage = menage.getNumero();
		tiersService.addTiersToCouple(menage, andrea, dateMariage, dateSeparation.getOneDayBefore());
		tiersService.addTiersToCouple(menage, liliana, dateMariage, dateSeparation.getOneDayBefore());
		{
			ForFiscalPrincipal ffp = addForFiscalPrincipal(menage, commune, dateMariage, dateSeparation.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
			ffp.setModeImposition(ModeImposition.ORDINAIRE);
		}
	}

	@Check(id=1, descr="Vérifie que les contribuables ont leur For principal ouvert et celui du ménage est fermé")
	public void check1() {
		{
			PersonnePhysique andrea = (PersonnePhysique) tiersDAO.get(noHabAndrea);
			ForFiscalPrincipal ffp = andrea.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + andrea.getNumero() + " n'a pas pu être trouvé");
			assertEquals(dateSeparation, ffp.getDateDebut(), "Le dernier for n'est pas le bon");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifOuverture(), "Le motif d'ouverture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Le mode d'imposition n'est pas ORDINAIRE");
		}
		{
			PersonnePhysique liliana = (PersonnePhysique) tiersDAO.get(noHabLiliana);
			ForFiscalPrincipal ffp = liliana.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + liliana.getNumero() + " n'a pas pu être trouvé");
			assertEquals(dateSeparation, ffp.getDateDebut(), "Le dernier for n'est pas le bon");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifOuverture(), "Le motif d'ouverture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Le mode d'imposition n'est pas ORDINAIRE");
		}
		{
			MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage doit avoir un for fiscal");
			ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " n'a pas pu être trouvé");
			assertEquals(dateMariage, ffp.getDateDebut(), "Le dernier for n'est pas le bon");
			assertEquals(dateSeparation.getOneDayBefore(), ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifOuverture(), "Le motif d'ouverture n'est pas MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION");
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifFermeture(), "Le motif d'ouverture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Le mode d'imposition n'est pas ORDINAIRE");
		}
	}

	@Etape(id=2, descr="Annulation de la séparation dans le register fiscal")
	public void step2() throws Exception {
		metierService.annuleSeparation((MenageCommun) tiersDAO.get(noMenage), dateSeparation, null);
	}

	@Check(id=2, descr="Vérifie que les fors individuels ont été fermés, celui du ménage commun rouvert, et que la situation de famille existe car différente du civil")
	public void check2() {
		MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
		{
			ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "Le for du ménage Pierre doit être fermé");
			assertEquals(dateMariage, ffp.getDateDebut(), "Le dernier for trouvé n'est pas le bon");
			assertNull(ffp.getDateFin(), "Le dernier for trouvé n'est pas le bon");
			for (ForFiscal forFiscal : mc.getForsFiscaux()) {
				// recherche des fors fermés avec date de fin égal à celle de la séparation
				// ces fors doivent être annulés
				if (forFiscal.getDateFin() != null && dateSeparation.equals(forFiscal.getDateFin())) {
					assertEquals(true, forFiscal.isAnnule(), "Les fors fiscaux créés lors de la séparation doivent être annulés");
				}
			}
		}
		checkHabitantApresAnnulation((PersonnePhysique) tiersDAO.get(noHabAndrea), dateSeparation);
		checkHabitantApresAnnulation((PersonnePhysique) tiersDAO.get(noHabLiliana), dateSeparation);

		SituationFamille sf = mc.getSituationFamilleActive();
		assertNotNull(sf, "La situation de famille devrait exister");
		EtatCivil etatCivilFamilleAttendu = EtatCivil.MARIE;
		assertEquals(etatCivilFamilleAttendu, sf.getEtatCivil(), "La situation de famille devrait être " + etatCivilFamilleAttendu.format());
		assertEquals(dateSeparation, sf.getDateDebut(), "La surcharge de l'état civil séparé devrait commencer le jour de la séparation");
	}
}
