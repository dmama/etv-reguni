package ch.vd.uniregctb.norentes.civil.deces;

import java.util.List;

import annotation.Check;
import annotation.Etape;
import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupe;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class Ec_2000_01_Deces_Celibataire_Scenario extends EvenementCivilScenario {

	public static final String NAME = "2000_01_Deces_Celibataire";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.DECES;
	}

	@Override
	public String getDescription() {
		return "Décès d'un habitant";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndGuillaume = 43252;

	private MockIndividu indGuillaume;

	private long noHabGuillaume;

	private final RegDate dateNaissanceGuillaume = RegDate.get(1952, 2, 21);

	private final RegDate dateArriveeVD = RegDate.get(2001, 9, 11);

	private final RegDate dateDeces = RegDate.get(2006, 8, 1);

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {

			@Override
			protected void init() {
				indGuillaume = addIndividu(noIndGuillaume, dateNaissanceGuillaume, "Tell", "Guillaume", true);
				addAdresse(indGuillaume, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null,
						dateArriveeVD, null);
			}
		});
	}

	@Etape(id = 1, descr = "Chargement de l'habitant")
	public void etape1() {
		final PersonnePhysique guillaume = addHabitant(noIndGuillaume);
		noHabGuillaume = guillaume.getNumero();
		addForFiscalPrincipal(guillaume, MockCommune.Lausanne, dateArriveeVD, null, MotifFor.ARRIVEE_HC, null);
	}

	@Check(id = 1, descr = "Vérification que l'habitant Guillaume a son adresse et son for à Lausanne")
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

		// PBM 29.07.2009: UNIREG-1266 -> Blocage des remboursements automatiques sur tous les nouveaux tiers
		assertBlocageRemboursementAutomatique(true);
	}

	@Etape(id = 2, descr = "Déclaration de décès")
	public void etape2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.DECES, noIndGuillaume, dateDeces, MockCommune.Lausanne.getNoOFS());
		commitAndStartTransaction();
		regroupeEtTraiteEvenements(id);
	}

	@Check(id = 2, descr = "Vérification que le for est bien fermé sur Lausanne après le décès, et que les remboursements automatiques sont bien bloqués")
	public void check2() {

		final EvenementCivilRegroupe evt = getEvenementCivilRegoupeForHabitant(noHabGuillaume);
		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "");

		final PersonnePhysique hab = (PersonnePhysique)tiersDAO.get(noHabGuillaume);
		final List<ForFiscal> list = hab.getForsFiscauxSorted();
		assertEquals(1, list.size(), "Plusieurs for?: " + list.size());

		// For fermé sur Lausanne pour cause de décès
		final ForFiscalPrincipal ffpFerme = (ForFiscalPrincipal)list.get(0);
		assertEquals(dateDeces, ffpFerme.getDateFin(), "Le for sur Lausanne n'est pas fermé à la bonne date");
		assertEquals(MotifFor.VEUVAGE_DECES, ffpFerme.getMotifFermeture(), "Le for sur Lausanne n'est pas fermé pour cause de décès");

		assertBlocageRemboursementAutomatique(true);
	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttendu) {
		assertBlocageRemboursementAutomatique(blocageAttendu, tiersDAO.get(noHabGuillaume));
	}
}
