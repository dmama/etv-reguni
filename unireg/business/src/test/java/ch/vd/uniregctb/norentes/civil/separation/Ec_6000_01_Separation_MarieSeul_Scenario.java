package ch.vd.uniregctb.norentes.civil.separation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Scenario d'un événement divorce d'une personne mariée seule.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_6000_01_Separation_MarieSeul_Scenario extends EvenementCivilScenario {

	public static final String NAME = "6000_01_Separation";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.SEPARATION;
	}

	@Override
	public String getDescription() {
		return "Séparation d'une personne mariée seule";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndJulie = 6789; // julie

	private MockIndividu indJulie;

	private long noHabJulie;
	private long noMenage;

	private final RegDate dateNaissance = RegDate.get(1977, 4, 19);
	private final RegDate dateMajorite = dateNaissance.addYears(18);
	private final RegDate dateMariage = dateMajorite.addYears(3);
	private final RegDate dateAvantMariage = dateMariage.addDays(-1);
	private final RegDate dateSeparation = RegDate.get(2008, 4, 18);
	private final RegDate dateDivorce = dateSeparation.addMonths(6);
	private final Commune communeMariage = MockCommune.Lausanne;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {

			@Override
			protected void init() {

				indJulie = addIndividu(noIndJulie, dateNaissance, "Goux", "Julie", false);

				marieIndividu(indJulie, dateMariage);
				separeIndividu(indJulie, dateSeparation);
				divorceIndividu(indJulie, dateDivorce);

				addOrigine(indJulie, MockCommune.Lausanne);
				addNationalite(indJulie, MockPays.Suisse, RegDate.get(1961, 3, 12), null);
				addAdresse(indJulie, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.CheminPrazBerthoud, null, RegDate.get(1961, 3, 12), null);
			}

		});
	}

	@Etape(id=1, descr="Chargement de l'habitant marié seul")
	public void etape1() {

		final PersonnePhysique julie = addHabitant(noIndJulie);
		{
			noHabJulie = julie.getNumero();
			addForFiscalPrincipal(julie, MockCommune.Lausanne, dateMajorite, dateAvantMariage, MotifFor.MAJORITE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		}

		// ménage
		{
			MenageCommun menage = new MenageCommun();
			menage = (MenageCommun)tiersDAO.save(menage);
			noMenage = menage.getNumero();
			tiersService.addTiersToCouple(menage, julie, dateMariage, null);
			final ForFiscalPrincipal f = addForFiscalPrincipal(menage, communeMariage, dateMariage, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
			f.setModeImposition(ModeImposition.DEPENSE);

			menage.setBlocageRemboursementAutomatique(false);
		}
	}

	@Check(id=1, descr="Vérifie que l'habitant Julie n'a pas de For ouvert et le For du ménage existe")
	public void check1() {

		{
			final PersonnePhysique julie = (PersonnePhysique) tiersDAO.get(noHabJulie);
			final ForFiscalPrincipal ffp = julie.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + julie.getNumero() + " null");
			assertEquals(dateAvantMariage, ffp.getDateFin(), "Date de fin du dernier for fausse");
		}

		{
			final MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(communeMariage.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(),
					"Le dernier for n'est pas sur " + communeMariage.getNomMajuscule());
		}

		assertBlocageRemboursementAutomatique(true, false);
	}

	@Etape(id=2, descr="Envoi de l'événement de Séparation")
	public void etape2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.SEPARATION, noIndJulie, dateSeparation, communeMariage.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que le menage commun a été fermé et le For principal de Julie créé")
	public void check2() {

		{
			final MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertEquals(dateMariage, ffp.getDateDebut(), "Le for sur Lausanne n'est pas ouvert à la bonne date");
			assertNotNull(ffp.getDateFin(), "Le for sur Lausanne est ouvert");
			assertEquals(ffp.getMotifFermeture(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
					"Le motif de fermeture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
		}

		{
			final PersonnePhysique julie = (PersonnePhysique) tiersDAO.get(noHabJulie);
			final ForFiscalPrincipal ffp = julie.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + julie.getNumero() + " null");
			assertNull(ffp.getDateFin(), "Le for de l'habitant " + julie.getNumero() + " est fermé");
			// julie doit passer au mode dépense
			final ModeImposition expected = ModeImposition.DEPENSE;
			assertEquals(ffp.getModeImposition(), expected, "Le mode d'imposition n'est pas " + expected.texte());
			assertEquals(ffp.getMotifOuverture(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
					"Le motif de fermeture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
		}

		assertBlocageRemboursementAutomatique(false, true);
	}

	@Etape(id=3, descr="Envoi de l'événement de Divorce")
	public void etape3() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.DIVORCE, noIndJulie, dateDivorce, communeMariage.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=3, descr="Vérifie que rien s'est passé depuis l'étape 2")
	public void check3() {
		check2();
	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttenduJulie, boolean blocageAttenduMenage) {
		assertBlocageRemboursementAutomatique(blocageAttenduJulie, tiersDAO.get(noHabJulie));
		assertBlocageRemboursementAutomatique(blocageAttenduMenage, tiersDAO.get(noMenage));
	}
}
