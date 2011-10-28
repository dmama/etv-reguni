package ch.vd.uniregctb.norentes.civil.separation;

import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Scénario d'une séparation pour laquelle nous recevons deux événements civils (un par individu,
 * comme vu dans le cas Jira Unireg-953
 */
public class Ec_6000_04_Separation_SecondEvenementRecu_Scenario extends EvenementCivilScenario {

	public static final String NAME = "6000_04_Separation";

	/**
	 * @see ch.vd.uniregctb.norentes.common.NorentesScenario#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Séparation pour laquelle deux événements civils ont été reçus: le deuxième doit certes être traité, mais ne doit pas neutraliser l'état du premier";
	}

	/**
	 * @see ch.vd.uniregctb.norentes.common.NorentesScenario#getName()
	 */
	@Override
	public String getName() {
		return NAME;
	}

	/**
	 * @see ch.vd.uniregctb.norentes.common.NorentesScenario#geTypeEvenementCivil()
	 */
	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.SEPARATION;
	}

	private final long noIndMomo = 54321; // momo
	private final long noIndBea = 23456; // bea

	private MockIndividu indMomo;
	private MockIndividu indBea;

	private long noHabMomo;
	private long noHabBea;
	private long noMenage;

	private final RegDate dateNaissanceMomo = RegDate.get(1961, 3, 12);
	private final RegDate dateArriveeMomoVillars = RegDate.get(1974, 3, 3);
	private final RegDate dateNaissanceBea = RegDate.get(1963, 8, 20);
	private final RegDate dateMajoriteBea = dateNaissanceBea.addYears(18);
	private final RegDate dateMariage = RegDate.get(1986, 4, 27);
	private final RegDate dateSeparation = RegDate.get(2008, 1, 22);
	private final RegDate dateAvantMariage = dateMariage.addDays(-1);

	private final Commune communeMariage = MockCommune.Lausanne;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {

			@Override
			protected void init() {

				indMomo = addIndividu(noIndMomo, dateNaissanceMomo, "Durant", "Maurice", true);
				indBea = addIndividu(noIndBea, dateNaissanceBea, "Duval", "Béatrice", false);

				marieIndividus(indMomo, indBea, dateMariage);
				separeIndividus(indMomo, indBea, dateSeparation);

				addOrigine(indMomo, MockPays.France.getNomMinuscule());
				addNationalite(indMomo, MockPays.France, RegDate.get(1963, 8, 20), null);
				addPermis(indMomo, TypePermis.ETABLISSEMENT, RegDate.get(1963, 8, 20), null, false);
				addAdresse(indMomo, TypeAdresseCivil.PRINCIPALE, MockRue.VillarsSousYens.CheminDuCollege, null, dateArriveeMomoVillars, dateAvantMariage);
				addAdresse(indMomo, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.PlaceSaintFrancois, null, dateMariage, null);

				addOrigine(indBea, MockCommune.Lausanne);
				addNationalite(indBea, MockPays.Suisse, RegDate.get(1961, 3, 12), null);
				addAdresse(indBea, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.PlaceSaintFrancois, null, dateMariage, null);
			}
		});
	}

	@Etape(id=1, descr="Initialisation du couple marié avec for secondaire")
	public void etape1() {
		// momo
		final PersonnePhysique momo = addHabitant(noIndMomo);
		{
			noHabMomo = momo.getNumero();
			addForFiscalPrincipal(momo, MockCommune.VillarsSousYens, dateArriveeMomoVillars, dateAvantMariage, MotifFor.DEMENAGEMENT_VD, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		}

		// bea
		final PersonnePhysique bea = addHabitant(noIndBea);
		{
			noHabBea = bea.getNumero();
			addForFiscalPrincipal(bea, MockCommune.Lausanne, dateMajoriteBea, dateAvantMariage, MotifFor.MAJORITE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		}

		// ménage
		{
			MenageCommun menage = new MenageCommun();
			menage = (MenageCommun)tiersDAO.save(menage);
			noMenage = menage.getNumero();
			tiersService.addTiersToCouple(menage, momo, dateMariage, null);
			tiersService.addTiersToCouple(menage, bea, dateMariage, null);

			final ForFiscalPrincipal f = addForFiscalPrincipal(menage, communeMariage, dateMariage, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
			f.setModeImposition(ModeImposition.DEPENSE);

			final ForFiscalSecondaire fs = addForFiscalSecondaire(menage, communeMariage.getNoOFS(), dateMariage, null);
			fs.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);

			menage.setBlocageRemboursementAutomatique(false);
		}
	}

	@Check(id=1, descr="Vérifie que les habitants n'ont pas de For ouvert et le For du ménage existe")
	public void check1() {
		{
			final PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noHabMomo);
			final ForFiscalPrincipal ffp = momo.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + momo.getNumero() + " null");
			assertEquals(dateAvantMariage, ffp.getDateFin(), "Date de fin du dernier for fausse");
		}

		{
			final PersonnePhysique bea = (PersonnePhysique)tiersDAO.get(noHabBea);
			final ForFiscalPrincipal ffp = bea.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + bea.getNumero() + " null");
			assertEquals(dateAvantMariage, ffp.getDateFin(), "Date de fin du dernier for fausse");
		}

		{
			final MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);

			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(communeMariage.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(),
					"Le dernier for n'est pas sur " + communeMariage.getNomMinuscule());

			final Set<ForFiscal> forsFiscaux = mc.getForsFiscaux();
			assertEquals(2, forsFiscaux.size(), "Nombre total de fors fiscaux faux");
			int nbForFiscauxSecondaires = 0;
			for (ForFiscal ff : forsFiscaux) {
				if (ff != ffp) {
					assertTrue(ff instanceof ForFiscalSecondaire, "Second for n'est pas un for secondaire?");
					assertEquals(dateMariage, ff.getDateDebut(), "Date de début du for secondaire fausse");
					assertNull(ff.getDateFin(), "Date de fin du for secondaire fausse");
					assertEquals(communeMariage.getNoOFS(), ff.getNumeroOfsAutoriteFiscale(),
							"Le for secondaire n'est pas sur " + communeMariage.getNomMinuscule());
					++ nbForFiscauxSecondaires;
				}
			}
			assertEquals(1, nbForFiscauxSecondaires, "Nombre de fors fiscaux secondaires faux");
		}

		assertBlocageRemboursementAutomatique(true, true, false);
	}

	@Etape(id=2, descr="Envoi de l'événement de Séparation pour Béa")
	public void etape2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.SEPARATION, noIndBea, dateSeparation, communeMariage.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérification des for commun et principaux")
	public void check2() {

		// comme il y a fermeture d'un for secondaire, l'événement doit être dans l'état "A_VERIFIER"
		final EvenementCivilExterne evt = getEvenementCivilRegoupeForHabitant(noHabBea);
		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "");

		{
			final MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertEquals(dateMariage, ffp.getDateDebut(), "Le for sur Lausanne n'est pas ouvert à la bonne date");
			assertNotNull(ffp.getDateFin(), "Le for sur Lausanne est ouvert");
			assertEquals(ffp.getMotifFermeture(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
					"Le motif de fermeture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");

			final Set<ForFiscal> forsFiscaux = mc.getForsFiscaux();
			assertEquals(2, forsFiscaux.size(), "Nombre total de fors fiscaux faux");
			int nbForFiscauxSecondaires = 0;
			for (ForFiscal ff : forsFiscaux) {
				if (ff != ffp) {
					assertTrue(ff instanceof ForFiscalSecondaire, "Second for n'est pas un for secondaire?");
					assertEquals(dateMariage, ff.getDateDebut(), "Date de début du for secondaire fausse");
					assertEquals(dateSeparation.addDays(-1), ff.getDateFin(), "Date de fin du for secondaire fausse");
					assertEquals(ffp.getMotifFermeture(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
					"Le motif de fermeture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
					++ nbForFiscauxSecondaires;
				}
			}
			assertEquals(1, nbForFiscauxSecondaires, "Nombre de fors fiscaux secondaires faux");
		}

		{
			final PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noHabMomo);
			final ForFiscalPrincipal ffp = momo.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + momo.getNumero() + " null");
			assertNull(ffp.getDateFin(), "Le for de l'habitant " + momo.getNumero() + " est fermé");
			// momo doit passer au mode dépense
			assertEquals(ffp.getModeImposition(), ModeImposition.DEPENSE, "Le mode d'imposition n'est pas DEPENSE");
			assertEquals(ffp.getMotifOuverture(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
					"Le motif de fermeture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
		}

		{
			final PersonnePhysique bea = (PersonnePhysique) tiersDAO.get(noHabBea);
			final ForFiscalPrincipal ffp = bea.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + bea.getNumero() + " null");
			assertNull(ffp.getDateFin(), "Le for de l'habitant " + bea.getNumero() + " est fermé");
			// bea doit passer au mode dépense
			final ModeImposition expected = ModeImposition.DEPENSE;
			assertEquals(ffp.getModeImposition(), expected, "Le mode d'imposition n'est pas " + expected.texte());
			assertEquals(ffp.getMotifOuverture(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
					"Le motif de fermeture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
		}

		assertBlocageRemboursementAutomatique(false, false, true);
	}

	@Etape(id=3, descr="Envoi de l'événement de Séparation pour Momo")
	public void etape3() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.SEPARATION, noIndMomo, dateSeparation, communeMariage.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=3, descr="Vérification des états des événements reçus")
	public void check3() {

		// l'événement envoyé pour Béa est toujours dans l'état "à vérifier"
		{
			final EvenementCivilExterne evt = getEvenementCivilRegoupeForHabitant(noHabBea);
			assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "");
		}

		// l'événement envoyé pour Momo est traité (en fait, il n'avait rien à faire!)
		{
			final EvenementCivilExterne evt = getEvenementCivilRegoupeForHabitant(noHabMomo);
			assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "");
		}

		assertBlocageRemboursementAutomatique(false, false, true);
	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttenduMomo, boolean blocageAttenduBea, boolean blocageAttenduMenage) {
		assertBlocageRemboursementAutomatique(blocageAttenduMomo, tiersDAO.get(noHabMomo));
		assertBlocageRemboursementAutomatique(blocageAttenduBea, tiersDAO.get(noHabBea));
		assertBlocageRemboursementAutomatique(blocageAttenduMenage, tiersDAO.get(noMenage));
	}
}

