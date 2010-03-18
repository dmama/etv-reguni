package ch.vd.uniregctb.norentes.civil.reconciliation;

import annotation.Check;
import annotation.Etape;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Scénario d'un événement réconciliation d'un individu marié seul.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_7000_01_Reconciliation_MarieSeul_Scenario extends EvenementCivilScenario {

	public static final String NAME = "7000_01_Reconciliation";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.RECONCILIATION;
	}

	@Override
	public String getDescription() {
		return "Scénario de réconciliation d'une personne mariée seule";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndPierre = 12345; // Pierre

	private MockIndividu indPierre;

	private long noHabPierre;
	private long noMenage;

	private final RegDate dateMariage = RegDate.get(1986, 4, 8);
	private final RegDate dateSeparation = RegDate.get(2007, 3, 24);
	private final RegDate dateReconciliation = RegDate.get(2008, 12, 1);
	private final Commune communeMariage = MockCommune.Lausanne;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {

			@Override
			protected void init() {

				indPierre = addIndividu(noIndPierre, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				marieIndividu(indPierre, dateMariage);
				separeIndividu(indPierre, null, dateSeparation);
				marieIndividu(indPierre, dateReconciliation);

				addOrigine(indPierre, MockPays.Suisse, null, RegDate.get(1953, 11, 2));
				addNationalite(indPierre, MockPays.Suisse, RegDate.get(1953, 11, 2), null, 0);
			}
		});
	}

	@Etape(id=1, descr="Chargement de l'habitant marié seul et son ménage")
	public void step1() {
		PersonnePhysique pierre = addHabitant(noIndPierre);
		noHabPierre = pierre.getNumero();
		addForFiscalPrincipal(pierre, MockCommune.Lausanne.getNoOFS(), dateSeparation, null,
				MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null);

		// ménage
		MenageCommun menage = new MenageCommun();
		menage = (MenageCommun) tiersDAO.save(menage);
		noMenage = menage.getNumero();
		tiersService.addTiersToCouple(menage, pierre, dateMariage, dateSeparation.getOneDayBefore());
		addForFiscalPrincipal(menage, communeMariage.getNoOFS(), dateMariage, dateSeparation.getOneDayBefore(),
				MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
	}

	@Check(id=1, descr="Vérifie que l'habitant Pierre a un For fiscal principal ouvert et que le For du ménage est fermé")
	public void check1() {

		{
			PersonnePhysique pierre = (PersonnePhysique) tiersDAO.get(noHabPierre);
			ForFiscalPrincipal ffp = pierre.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + pierre.getNumero() + " null");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
		}

		{
			MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNotNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(communeMariage.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(),
					"Le dernier for n'est pas sur " + communeMariage.getNomMajuscule());

		}

	}

	@Etape(id=2, descr="Envoi de l'événement de Réconciliation")
	public void step2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.RECONCILIATION, noIndPierre, dateReconciliation, communeMariage.getNoOFS());
		commitAndStartTransaction();
		regroupeEtTraiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que le For principal du ménage a été rouvert et celui de Pierre fermé")
	public void check2() {

		{
			MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertEquals(dateReconciliation, ffp.getDateDebut(), "Le for sur Lausanne n'est pas ouvert à la bonne date");
			assertNull(ffp.getDateFin(), "Le for sur Lausanne est fermé");
			assertEquals(ffp.getMotifOuverture(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
					"Le motif d'ouverture n'est pas MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION");
		}

		{
			PersonnePhysique pierre = (PersonnePhysique) tiersDAO.get(noHabPierre);
			ForFiscalPrincipal ffp = pierre.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + pierre.getNumero() + " null");
			assertNotNull(ffp.getDateFin(), "Le for de l'habitant " + pierre.getNumero() + " est ouvert");
			assertEquals(ffp.getMotifOuverture(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
					"Le motif de fermeture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
			assertEquals(ffp.getMotifFermeture(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
					"Le motif de fermeture n'est pas MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION");
		}

	}
}
