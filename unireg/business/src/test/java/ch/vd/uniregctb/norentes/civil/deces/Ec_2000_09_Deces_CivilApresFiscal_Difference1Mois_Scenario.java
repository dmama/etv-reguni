package ch.vd.uniregctb.norentes.civil.deces;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Décès d'un habitant: d'abord dans le fiscal, ensuite dans
 * le civil avec un différence dans les dates d'un mois.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_2000_09_Deces_CivilApresFiscal_Difference1Mois_Scenario extends EvenementCivilScenario {

	private MetierService metierService;

	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	public static final String NAME = "2000_09_Deces";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.DECES;
	}

	@Override
	public String getDescription() {
		return "Décès d'un habitant: d'abord dans le fiscal, ensuite dans le civil avec un différence dans les dates d'un mois";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndGuillaume = 43252;

	private MockIndividu indGuillaume;

	private long noHabGuillaume;

	private final RegDate dateNaissanceGuillaume = RegDate.get(1952, 2, 21);
	private final RegDate dateArriveeVD = RegDate.get(2001, 9, 11);
	private final RegDate dateDecesFiscal = RegDate.get(2006, 8, 1);
	private final RegDate dateDecesCivil = RegDate.get(2006, 7, 1);

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {

			@Override
			protected void init() {
				indGuillaume = addIndividu(noIndGuillaume, dateNaissanceGuillaume, "Tell", "Guillaume", true);
				addAdresse(indGuillaume, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null,
						dateArriveeVD, null);
			}
		});
	}

	@Etape(id=1, descr="Chargement de l'habitant")
	public void step1() {
		final PersonnePhysique guillaume = addHabitant(noIndGuillaume);
		noHabGuillaume = guillaume.getNumero();
		addForFiscalPrincipal(guillaume, MockCommune.Lausanne, dateArriveeVD, null, MotifFor.ARRIVEE_HC, null);
		guillaume.setBlocageRemboursementAutomatique(false);
	}

	@Check(id=1, descr="Vérification que l'habitant Guillaume a son adresse et son for à Lausanne")
	public void check1() throws Exception {

		{
			final PersonnePhysique guillaume = (PersonnePhysique) tiersDAO.get(noHabGuillaume);
			final ForFiscalPrincipal ffp = guillaume.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + guillaume.getNumero() + " null");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for de Guillaume fausse");
		}

		// vérification que les adresses civiles sont a Bex
		assertEquals(MockCommune.Lausanne.getNomMinuscule(), serviceCivilService.getAdresses(noIndGuillaume, dateArriveeVD, false).principale
				.getLocalite(), "l'adresse principale n'est pas à Lausanne");

		assertBlocageRemboursementAutomatique(false);
	}

	@Etape(id=2, descr="Décès fiscal")
	public void step2() throws Exception {
		metierService.deces((PersonnePhysique) tiersDAO.get(noHabGuillaume), dateDecesFiscal, null, null);
	}

	@Check(id=2, descr="Vérification que le for est bien fermé sur Lausanne après le décès, et que les remboursements automatiques sont bien bloqués")
	public void check2() {

		final PersonnePhysique hab = (PersonnePhysique)tiersDAO.get(noHabGuillaume);
		final List<ForFiscal> list = hab.getForsFiscauxSorted();
		assertEquals(1, list.size(), "Plusieurs for?: " + list.size());

		// For fermé sur Lausanne pour cause de décès
		final ForFiscalPrincipal ffpFerme = (ForFiscalPrincipal)list.get(0);
		assertEquals(dateDecesFiscal, ffpFerme.getDateFin(), "Le for sur Lausanne n'est pas fermé à la bonne date");
		assertEquals(MotifFor.VEUVAGE_DECES, ffpFerme.getMotifFermeture(), "Le for sur Lausanne n'est pas fermé pour cause de décès");

		assertBlocageRemboursementAutomatique(true);
	}

	@Etape(id=3, descr="Envoi de l'événement de décès")
	public void step3() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.DECES, noIndGuillaume, dateDecesCivil, MockCommune.Lausanne.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=3, descr="Vérification que l'événement est bien en erreur et tout doit être comme après l'étape 2")
	public void check3() {

		// vérification que le
		final EvenementCivilRegPP evt = getEvenementCivilRegoupeForHabitant(noHabGuillaume);
		assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat(), "L'événement devrait être en ERREUR");

		// le reste pareil qu'après l'étape 2
		check2();
	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttendu) {
		assertBlocageRemboursementAutomatique(blocageAttendu, tiersDAO.get(noHabGuillaume));
	}
}
