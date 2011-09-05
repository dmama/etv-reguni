package ch.vd.uniregctb.norentes.civil.mariage;

import java.util.List;

import annotation.Check;
import annotation.Etape;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.norentes.common.NorentesException;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Scénario de mariage d'un non habitant avec un habitant vaudois dont le premier est le contribuable principal.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_4000_08_Mariage_HabitantAvecNonHabitant_Scenario extends EvenementCivilScenario {

	private MetierService metierService;

	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	public static final String NAME = "4000_08_Mariage";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.MARIAGE;
	}

	@Override
	public String getDescription() {
		return "Mariage d'un non habitant avec un habitant vaudois dont le premier est le contribuable principal.";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndMaria = 3913649; // maria

	private MockIndividu indMaria;

	private long noCtbRafa;
	private long noCtbMaria;
	
	private final RegDate dateNaissanceMaria = RegDate.get(1975, 7, 31);
	private final RegDate dateArriveeMaria = RegDate.get(2008, 9, 1);
	private final RegDate dateNaissanceRafa = RegDate.get(1974, 6, 25);
	private final RegDate dateMajoriteRafa = dateNaissanceRafa.addYears(18);
	private final RegDate dateDepartRafa = RegDate.get(2008, 2, 20);
	private final RegDate dateMariage = RegDate.get(2008, 6, 21);
	
	private final MockCommune communeRafa = MockCommune.Zurich;
	private final MockCommune communeMaria = MockCommune.Lausanne;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {

			@Override
			protected void init() {

				indMaria = addIndividu(noIndMaria, dateNaissanceMaria, "Nadalino", "Maria", false);

				addOrigine(indMaria, MockPays.Espagne, null, dateNaissanceMaria);
				addNationalite(indMaria, MockPays.Espagne, dateNaissanceMaria, null);
				addPermis(indMaria, TypePermis.ETABLISSEMENT, RegDate.get(2006, 5, 3), null, false);
				addAdresse(indMaria, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.RouteMaisonNeuve, null, dateArriveeMaria, null);
			}
		});
	}

	@Etape(id=1, descr="Chargement du non habitant")
	public void etape1() throws Exception {
		
		// rafa
		final PersonnePhysique rafa = addNonHabitant("Nadalino", "Rafa", dateNaissanceRafa, Sexe.MASCULIN);
		{
			noCtbRafa = rafa.getNumero();
			
			addForFiscalPrincipal(rafa, MockCommune.Lausanne, dateMajoriteRafa, dateDepartRafa.getOneDayBefore(), MotifFor.MAJORITE, MotifFor.DEPART_HC);
			addForFiscalPrincipal(rafa, communeRafa, dateDepartRafa, null, MotifFor.DEPART_HC, null);
			addForFiscalSecondaire(rafa, MockCommune.Lausanne.getNoOFS(), dateMajoriteRafa.addYears(5), null);
		}
	}

	@Check(id=1, descr="Vérifie que le non habitant a bien été créé")
	public void check1() throws Exception {
		// rafa
		final PersonnePhysique rafa = (PersonnePhysique) tiersDAO.get(noCtbRafa);
		assertNotNull(rafa, "Le non habitant Rafa n'a pas été créé");
		assertExistenceForPrincipal(rafa);
	}

	private void assertExistenceForPrincipal(Contribuable contribuable) {
		final ForFiscalPrincipal ffp = contribuable.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "Le contribuable n° " + FormatNumeroHelper.numeroCTBToDisplay(contribuable.getNumero()) + " devrait avoir un for principal");
	}

	@Etape(id=2, descr="Envoi de l'événement d'arrivée de la femme")
	public void etape2() throws Exception {
		final long id = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC, noIndMaria, dateArriveeMaria, communeMaria.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}
	
	@Check(id=2, descr="Vérifie que l'evenement d'arrivée est au statut traité et qu'un habitant a été créé")
	public void check2() {
		
		List<EvenementCivilExterne> list = evtExterneDAO.getAll();
		for (EvenementCivilExterne evt : list) {
			if (noIndMaria == evt.getNumeroIndividuPrincipal()) {
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "");
			}
		}
		
		// maria
		final PersonnePhysique maria = tiersDAO.getHabitantByNumeroIndividu(noIndMaria);
		{
			assertNotNull(maria, "Aucun habitant n'a été créé pour l'arrivant");
			noCtbMaria = maria.getNumero();
			
			assertExistenceForPrincipal(maria);
		}
		
	}

	@Etape(id=3, descr="Création du mariage entre le nouvel habitant et le non habitant à une date anterieur à l'arrive")
	public void etape3() throws MetierServiceException {
		
		final PersonnePhysique rafa = (PersonnePhysique) tiersDAO.get(noCtbRafa);
		final PersonnePhysique maria = (PersonnePhysique) tiersDAO.get(noCtbMaria);
		
		ValidationResults results = metierService.validateMariage(dateMariage, rafa, maria);
		if (!results.hasErrors()) {
			metierService.marie(dateMariage, rafa, maria, null, EtatCivil.MARIE, true, null);
		}
		else {
			throw new NorentesException(results.getErrors().toString());
		}
	}
	
	@Check(id=3, descr="Vérifie que le for du ménage est assujetti sur Lausanne")
	public void check3() throws Exception {

		EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple((PersonnePhysique) tiersDAO.get(noCtbMaria), null);
		assertNotNull(couple, "Aucun ménage trouvé");
		
		final MenageCommun menage = couple.getMenage();
		assertNotNull(menage, "Aucun ménage trouvé");
		
		assertExistenceForPrincipal(menage);
		final ForFiscalPrincipal ffpMenage = menage.getForFiscalPrincipalAt(null);
		assertEquals(communeMaria.getNoOFS(), ffpMenage.getNumeroOfsAutoriteFiscale(), "Le for du ménage n'est pas assujetti à la bonne commune");
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffpMenage.getTypeAutoriteFiscale(), "Le for du ménage n'est pas assujetti à la bonne commune");
		
		SituationFamille sf = menage.getSituationFamilleActive();
		assertNotNull(sf, "Aucune situation famille trouvée");
		assertEquals(EtatCivil.MARIE, sf.getEtatCivil(), "La situation famille n'a pas le bon état civil");
		assertEquals(dateArriveeMaria, sf.getDateDebut(), "La date de début de la situation famille n'est pas la bonne");
		assertNull(sf.getDateFin(), "La date de fin de la situation famille n'est pas la bonne");
	}
}
