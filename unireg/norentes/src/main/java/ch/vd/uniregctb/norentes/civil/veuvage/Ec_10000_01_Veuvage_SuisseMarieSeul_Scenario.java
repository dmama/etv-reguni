package ch.vd.uniregctb.norentes.civil.veuvage;

import annotation.Check;
import annotation.Etape;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypeEtatCivil;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Scénario de veuvage d'un individu suisse marié seul.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_10000_01_Veuvage_SuisseMarieSeul_Scenario extends EvenementCivilScenario {

	public static final String NAME = "10000_01_Veuvage";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.VEUVAGE;
	}

	@Override
	public String getDescription() {
		return "Scénario de veuvage d'un individu suisse marié seul.";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndPierre = 12345; // Pierre

	private MockIndividu indPierre;

	private long noHabPierre;
	private long noMenage;

	private final RegDate avantDateMariage = RegDate.get(1986, 4, 27);
	private final RegDate dateMariage = avantDateMariage.getOneDayAfter();
	private final RegDate dateVeuvage = RegDate.get(2008, 1, 1);
	private final Commune communeMariage = MockCommune.Lausanne;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {

			@Override
			protected void init() {

				indPierre = addIndividu(12345, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				marieIndividu(indPierre, RegDate.get(1986, 4, 8));

				addOrigine(indPierre, MockPays.Suisse, null, RegDate.get(1953, 11, 2));
				addNationalite(indPierre, MockPays.Suisse, RegDate.get(1953, 11, 2), null, 0);
				addEtatCivil(indPierre, dateVeuvage, EnumTypeEtatCivil.VEUF);
			}
		});
	}

	@Etape(id=1, descr="Chargement de l'habitant Pierre, suisse marié seul et bientot veuf")
	public void step1() {

		// Pierre
		PersonnePhysique pierre = addHabitant(noIndPierre);
		noHabPierre = pierre.getNumero();
		ForFiscalPrincipal f = addForFiscalPrincipal(pierre, MockCommune.VillarsSousYens, RegDate.get(1974, 3, 3), avantDateMariage, MotifFor.DEMENAGEMENT_VD, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		f.setModeImposition(ModeImposition.ORDINAIRE);

		// ménage
		MenageCommun menage = new MenageCommun();
		menage = (MenageCommun)tiersDAO.save(menage);
		noMenage = menage.getNumero();
		tiersService.addTiersToCouple(menage, pierre, dateMariage, null);
		f = addForFiscalPrincipal(menage, communeMariage, dateMariage, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
		f.setModeImposition(ModeImposition.ORDINAIRE);
	}

	@Check(id=1, descr="Vérifie que l'habitant Pierre est marié seul et le For du menage existe")
	public void check1() throws Exception {

		PersonnePhysique pierre = (PersonnePhysique) tiersDAO.get(noHabPierre);
		{
			ForFiscalPrincipal ffp = pierre.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + pierre.getNumero() + " null");
			assertEquals(avantDateMariage, ffp.getDateFin(), "Date de fin du dernier for fausse");
		}

		{
			MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			EnsembleTiersCouple etc = tiersService.getEnsembleTiersCouple(mc, dateMariage.getOneDayAfter());
			assertNull(etc.getConjoint(pierre), "Pierre n'est pas marié seul");
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(communeMariage.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + communeMariage.getNomMinuscule());
		}

		// PBM 29.07.2009: UNIREG-1266 -> Blocage des remboursements automatiques sur tous les nouveaux tiers
		assertBlocageRemboursementAutomatique(true);
	}

	@Etape(id=2, descr="Envoi de l'événement de Veuvage")
	public void etape2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.VEUVAGE, noIndPierre, dateVeuvage, communeMariage.getNoOFS());
		commitAndStartTransaction();
		regroupeEtTraiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que le menage commun a été fermé et le For principal de l'individu créé")
	public void check2() {

		{
			PersonnePhysique pierre = (PersonnePhysique) tiersDAO.get(noHabPierre);
			ForFiscalPrincipal ffp = pierre.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + pierre.getNumero() + " null");
			assertNull(ffp.getDateFin(), "Le for principal de l'habitant est fermé");
			ModeImposition expected = ModeImposition.ORDINAIRE;
			assertEquals(expected, ffp.getModeImposition(), "L'habitant devrait être en mode " + expected.texte());
		}

		{
			MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " ouvert");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertEquals(dateVeuvage, ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(communeMariage.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + communeMariage.getNomMinuscule());
		}

		// le survivant ne doit pas voir ses remboursements automatiques bloqués
		// PBM 29.07.2009: UNIREG-1266 -> Blocage des remboursements automatiques sur tous les nouveaux tiers
		assertBlocageRemboursementAutomatique(true);
	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttendu) {
		assertBlocageRemboursementAutomatique(blocageAttendu, tiersDAO.get(noHabPierre));
	}
}
