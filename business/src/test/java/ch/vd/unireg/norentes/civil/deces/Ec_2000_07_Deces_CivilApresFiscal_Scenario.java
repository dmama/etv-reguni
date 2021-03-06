package ch.vd.unireg.norentes.civil.deces;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.metier.MetierService;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivil;

/**
 * Scénario de décès d'un habitant: d'abord dans le fiscal, ensuite dans le civil.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_2000_07_Deces_CivilApresFiscal_Scenario extends EvenementCivilScenario {

	private MetierService metierService;

	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	public static final String NAME = "2000_07_Deces";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.DECES;
	}

	@Override
	public String getDescription() {
		return "Décès d'un habitant: d'abord dans le fiscal, ensuite dans le civil";
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
	private final RegDate dateDeces = RegDate.get(2006, 8, 1);

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockIndividuConnector() {

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
		assertEquals(MockCommune.Lausanne.getNomOfficiel(), serviceCivilService.getAdresses(noIndGuillaume, dateArriveeVD, false).principale
				.getLocalite(), "l'adresse principale n'est pas à Lausanne");

		assertBlocageRemboursementAutomatique(false);
	}

	@Etape(id=2, descr="Décès fiscal")
	public void step2() throws Exception {
		metierService.deces((PersonnePhysique) tiersDAO.get(noHabGuillaume), dateDeces, null, null);
	}

	@Check(id=2, descr="Vérification que le for est bien fermé sur Lausanne après le décès, et que les remboursements automatiques sont bien bloqués")
	public void check2() {

		final PersonnePhysique hab = (PersonnePhysique)tiersDAO.get(noHabGuillaume);
		final List<ForFiscal> list = hab.getForsFiscauxSorted();
		assertEquals(1, list.size(), "Plusieurs for?: " + list.size());

		// For fermé sur Lausanne pour cause de décès
		final ForFiscalPrincipal ffpFerme = (ForFiscalPrincipal)list.get(0);
		assertEquals(dateDeces, ffpFerme.getDateFin(), "Le for sur Lausanne n'est pas fermé à la bonne date");
		assertEquals(MotifFor.VEUVAGE_DECES, ffpFerme.getMotifFermeture(), "Le for sur Lausanne n'est pas fermé pour cause de décès");

		assertBlocageRemboursementAutomatique(true);
	}

	@Etape(id=3, descr="Envoi de l'événement de décès")
	public void step3() throws Exception {

		doModificationIndividu(noIndGuillaume, individu -> individu.setDateDeces(dateDeces));

		long id = addEvenementCivil(TypeEvenementCivil.DECES, noIndGuillaume, dateDeces, MockCommune.Lausanne.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=3, descr="Vérification que l'événement a été bien traité et tout doit être comme après l'étape 2")
	public void check3() {

		// vérification que le
		final EvenementCivilRegPP evt = getEvenementCivilRegoupeForHabitant(noHabGuillaume);
		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "L'événement n'a pas été bien traité");

		// le reste pareil qu'après l'étape 2
		check2();
	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttendu) {
		assertBlocageRemboursementAutomatique(blocageAttendu, tiersDAO.get(noHabGuillaume));
	}
}
