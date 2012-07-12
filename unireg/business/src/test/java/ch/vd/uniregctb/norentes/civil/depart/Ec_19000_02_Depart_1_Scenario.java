package ch.vd.uniregctb.norentes.civil.depart;

import java.util.Collection;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class Ec_19000_02_Depart_1_Scenario extends EvenementCivilScenario {

	public static final String NAME = "19000_02_Depart";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {

		return TypeEvenementCivil.DEPART_COMMUNE;
	}

	@Override
	public String getDescription() {

		return "Départ d'un individu seul Pour une destination inconnue avec residence principal à Bex";
	}

	@Override
	public String getName() {

		return NAME;
	}

	private static final long noIndAlain = 122456L;

	private MockIndividu indAlain;

	private long noHabAlain;




	private final int communeDepartBex = MockCommune.Bex.getNoOFS();
	private final RegDate dateArriveeBex = RegDate.get(1982, 7, 5);
	private final RegDate dateDepartBex = RegDate.get(2006, 4, 11);

	private final int NumOfsPaysInconnue = MockPays.PaysInconnu.getNoOFS();





	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				indAlain = addIndividu(noIndAlain, RegDate.get(1952, 2, 21), "Gregoire", "Alain", true);

				addAdresse(indAlain, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, dateArriveeBex, null);


			}
		});
	}


	@Etape(id=1, descr="Chargement d'un habitant à Bex")
	public void etape1() throws Exception {

		final PersonnePhysique alain = addHabitant(noIndAlain);
		noHabAlain = alain.getNumero();
		addForFiscalPrincipal(alain, MockCommune.Bex, dateArriveeBex, null, MotifFor.ARRIVEE_HC, null);
		alain.setBlocageRemboursementAutomatique(false);
	}

	@Check(id=1, descr="Vérifie que l'habitant Alain a son adresse à Bex et son For à Bex")
	public void check1() throws Exception {

		{
			final PersonnePhysique alain = (PersonnePhysique)tiersDAO.get(noHabAlain);
			final ForFiscalPrincipal ffp = alain.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant "+alain.getNumero()+" null");
			assertNull( ffp.getDateFin(), "Date de fin du dernier for d'Alain fausse");
		}


		// vérification que les adresses civiles sont a Bex
		assertEquals(MockCommune.Bex.getNomMinuscule(), serviceCivilService.getAdresses(noIndAlain, dateArriveeBex, false).principale.getLocalite(),
			"l'adresse principale n'est pas à Bex");

		assertBlocageRemboursementAutomatique(false);
	}

	@Etape(id=2, descr="Départ de Alain vers destination inconnue")
	public void etape2() throws Exception {
		FermerAdresse(indAlain);

	}

	@Check(id=2, descr="Vérifie que l'habitant Alain a toujours son For à Bex mais son adresse est inconnue")
	public void check2() throws Exception {

		{
			final PersonnePhysique alain = (PersonnePhysique)tiersDAO.get(noHabAlain);
			final ForFiscalPrincipal ffp = alain.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant "+alain.getNumero()+" null");
			assertNull( ffp.getDateFin(), "Date de fin du dernier for fausse");
		}

		// vérification que les adresses civiles sont inconnue
		assertNull(serviceCivilService.getAdresses(noIndAlain, dateDepartBex.addDays(1), false).principale,
			"l'adresse principale n'est pas fermée");

		assertBlocageRemboursementAutomatique(false);
	}


	@Etape(id=3, descr="Envoi de l'événement de départ de l'individu Alain")
	public void etape3() throws Exception {

		long id = addEvenementCivil(TypeEvenementCivil.DEPART_COMMUNE, noIndAlain, dateDepartBex, communeDepartBex);

		commitAndStartTransaction();

		// On traite les evenements
		traiteEvenements(id);
	}

	@Check(id=3, descr="Vérifie que l'habitant Alain n'a plus son for sur Bex mais sur Pays inconnue")
	public void check3() throws Exception {

		// On check que Alain  est parti
		{
			final EvenementCivilRegPP evt = getEvenementCivilRegoupeForHabitant(noHabAlain);
			assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "");

			final PersonnePhysique hab = (PersonnePhysique)tiersDAO.get(noHabAlain);
			final List<ForFiscal> list = hab.getForsFiscauxSorted();

			// For fermé sur Bex
			final ForFiscalPrincipal ffpFerme = (ForFiscalPrincipal)list.get(list.size()-2);
			assertEquals(dateDepartBex, ffpFerme.getDateFin(), "Le for sur Bex n'est pas fermé à la bonne date");

			// For ouvert sur PaysInconnu
			final ForFiscalPrincipal ffpOuvert = (ForFiscalPrincipal)list.get(list.size()-1);
			assertEquals(dateDepartBex.addDays(1), ffpOuvert.getDateDebut(), "Le for sur Le pay inconnue n'est pas ouvert à la bonne date");
			assertEquals(NumOfsPaysInconnue, ffpOuvert.getNumeroOfsAutoriteFiscale(), "Le for ouvert n'est pas sur Zurich");
			assertEquals(MotifRattachement.DOMICILE, ffpOuvert.getMotifRattachement(), "Le MotifRattachement du for est faux");
			assertEquals(GenreImpot.REVENU_FORTUNE, ffpOuvert.getGenreImpot(), "Le GenreImpot du for est faux");
			assertEquals(ModeImposition.ORDINAIRE, ffpOuvert.getModeImposition(), "Le ModeImposition du for est faux");
		}

		assertBlocageRemboursementAutomatique(true);
	}

	private void FermerAdresse(MockIndividu ind) {
		final Collection<Adresse> adrs = ind.getAdresses();
		MockAdresse last = null;
		for (Adresse a : adrs) {
			last = (MockAdresse)a;
		}
		last.setDateFinValidite(dateDepartBex);

	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttendu) {
		assertBlocageRemboursementAutomatique(blocageAttendu, tiersDAO.get(noHabAlain));
	}
}
