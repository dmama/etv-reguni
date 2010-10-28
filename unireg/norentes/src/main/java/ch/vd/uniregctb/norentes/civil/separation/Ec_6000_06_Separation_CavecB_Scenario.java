package ch.vd.uniregctb.norentes.civil.separation;

import annotation.Check;
import annotation.Etape;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Scénario d'un événement séparation de deux étrangers: l'un avec permis C, l'autre avec permis autre B.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_6000_06_Separation_CavecB_Scenario extends EvenementCivilScenario {

	public static final String NAME = "6000_06_Separation";

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

	private final long noIndEsad = 6544; // Esad
	private final long noIndAida = 7981; // Aida

	private MockIndividu indEsad;
	private MockIndividu indAida;

	private long noHabEsad;
	private long noHabAida;
	private long noMenage;

	private final RegDate dateNaissanceEsad = RegDate.get(1975, 5, 5);
	private final RegDate dateNaissanceAida = RegDate.get(1986, 9, 6);
	private final RegDate dateMariage = RegDate.get(2006, 1, 9);
	private final RegDate dateSeparation = RegDate.get(2008, 2, 1);

	private final RegDate dateDebutCouple = RegDate.get(1998, 9, 3);
	private final RegDate dateDebutForCouple = dateDebutCouple.addMonths(1);

	private final Commune commune = MockCommune.Renens;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {

			@Override
			protected void init() {

				indEsad = addIndividu(noIndEsad, dateNaissanceEsad, "Shabani", "Esad", true);
				indAida = addIndividu(noIndAida, dateNaissanceAida, "Shabani", "Aida", false);

				marieIndividus(indEsad, indAida, dateMariage);
				separeIndividus(indEsad, indAida, dateSeparation);

				addOrigine(indEsad, MockPays.Albanie, null, dateNaissanceEsad);
				addNationalite(indEsad, MockPays.Albanie, dateNaissanceEsad, null, 0);
				addPermis(indEsad, EnumTypePermis.ETABLLISSEMENT, RegDate.get(2005, 1, 19), null, 0, false);
				addAdresse(indEsad, TypeAdresseCivil.PRINCIPALE, MockRue.Renens.QuatorzeAvril, null, RegDate.get(2005, 1, 19), null);

				addOrigine(indAida, MockPays.Albanie, null, dateNaissanceAida);
				addNationalite(indAida, MockPays.Albanie, dateNaissanceAida, null, 0);
				addPermis(indAida, EnumTypePermis.ANNUEL, RegDate.get(2007, 7, 7), null, 0, false);
				addAdresse(indAida, TypeAdresseCivil.PRINCIPALE, MockRue.Renens.QuatorzeAvril, null, RegDate.get(2007, 7, 7), null);
			}

		});
	}

	@Etape(id=1, descr="Chargement des habitants et du ménage commun")
	public void etape1() {
		// esad
		final PersonnePhysique esad = addHabitant(noIndEsad);
		{
			noHabEsad = esad.getNumero();
		}

		// aida
		final PersonnePhysique aida = addHabitant(noIndAida);
		{
			noHabAida = aida.getNumero();
		}

		// ménage
		{
			MenageCommun menage = new MenageCommun();
			menage = (MenageCommun) tiersDAO.save(menage);
			noMenage = menage.getNumero();
			tiersService.addTiersToCouple(menage, esad, dateDebutCouple, null);
			tiersService.addTiersToCouple(menage, aida, dateDebutCouple, null);
			final ForFiscalPrincipal f = addForFiscalPrincipal(menage, commune, dateDebutForCouple, null, MotifFor.DEMENAGEMENT_VD, null);
			f.setModeImposition(ModeImposition.ORDINAIRE);

			menage.setBlocageRemboursementAutomatique(false);
		}
	}

	@Check(id=1, descr="Vérifie que les habitants n'ont pas de For ouvert et le For du ménage existe")
	public void check1() {
		{
			final PersonnePhysique esad = (PersonnePhysique) tiersDAO.get(noHabEsad);
			final ForFiscalPrincipal ffp = esad.getDernierForFiscalPrincipal();
			assertNull(ffp, "For principal de l'Habitant " + esad.getNumero() + " non null");
		}

		{
			final PersonnePhysique aida = (PersonnePhysique)tiersDAO.get(noHabAida);
			final ForFiscalPrincipal ffp = aida.getDernierForFiscalPrincipal();
			assertNull(ffp, "For principal de l'Habitant " + aida.getNumero() + " non null");
		}

		{
			final MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateDebutForCouple, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(commune.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(),
					"Le dernier for n'est pas sur " + commune.getNomMinuscule());
		}

		assertBlocageRemboursementAutomatique(true, true, false);
	}

	@Etape(id=2, descr="Envoi de l'événement de séparation")
	public void etape2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.SEPARATION, noIndAida, dateSeparation, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérification des fors commun et individuels")
	public void check2() {
		{
			final MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertEquals(dateDebutForCouple, ffp.getDateDebut(), "Le for sur Lausanne n'est pas ouvert à la bonne date");
			assertNotNull(ffp.getDateFin(), "Le for sur Lausanne est ouvert");
			assertEquals(ffp.getMotifFermeture(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
					"Le motif de fermeture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
		}

		{
			final PersonnePhysique esad = (PersonnePhysique) tiersDAO.get(noHabEsad);
			final ForFiscalPrincipal ffp = esad.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + esad.getNumero() + " null");
			assertNull(ffp.getDateFin(), "Le for de l'habitant " + esad.getNumero() + " est fermé");
			// Esad doit passer au mode ordinaire
			ModeImposition expected = ModeImposition.ORDINAIRE;
			assertEquals(expected, ffp.getModeImposition(), "Le mode d'imposition n'est pas " + expected);
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifOuverture(), "Le motif de fermeture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
		}

		{
			final PersonnePhysique aida = (PersonnePhysique) tiersDAO.get(noHabAida);
			final ForFiscalPrincipal ffp = aida.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + aida.getNumero() + " null");
			assertNull(ffp.getDateFin(), "Le for de l'habitant " + aida.getNumero() + " est fermé");
			// aida doit passer au mode mixte 137 al.1
			ModeImposition expected = ModeImposition.MIXTE_137_1;
			assertEquals(expected, ffp.getModeImposition(), "Le mode d'imposition n'est pas " + expected);
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifOuverture(), "Le motif de fermeture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
		}

		assertBlocageRemboursementAutomatique(false, false, true);
	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttenduEsad, boolean blocageAttenduAida, boolean blocageAttenduMenage) {
		assertBlocageRemboursementAutomatique(blocageAttenduEsad, tiersDAO.get(noHabEsad));
		assertBlocageRemboursementAutomatique(blocageAttenduAida, tiersDAO.get(noHabAida));
		assertBlocageRemboursementAutomatique(blocageAttenduMenage, tiersDAO.get(noMenage));
	}
}
