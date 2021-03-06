package ch.vd.unireg.norentes.civil.mariage;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.type.TypePermis;

/**
 * Scénario de mariage d'un couple, dont seul le conjoint est 
 * assujetti et la date de mariage antérieure à celle de son for.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_4000_09_Mariage_JIRA1481_Scenario extends EvenementCivilScenario {

	public static final String NAME = "4000_09_Mariage";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.MARIAGE;
	}

	@Override
	public String getDescription() {
		return "Mariage d'un couple, dont seul le conjoint est assujetti et la date de mariage antérieure à celle de son for.";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndAdrian = 957666; // Adrian
	private static final long noIndAngela = 957667; // Angela

	private MockIndividu indAdrian;
	private MockIndividu indAngela;

	private long noCtbAdrian;
	private long noCtbAngela;
	
	private final RegDate dateNaissanceAdrian = date(1963, 3, 18);
	private final RegDate dateNaissanceAngela = date(1964, 9, 14);
	private final RegDate dateArriveeAngela = date(2009, 5, 15);
	private final RegDate dateMariage = date(1996, 3, 11);
	
	private final MockCommune commune = MockCommune.Lausanne;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockIndividuConnector() {

			@Override
			protected void init() {

				indAdrian = addIndividu(noIndAdrian, dateNaissanceAdrian, "Fratila", "Adrian", true);

				addNationalite(indAdrian, MockPays.Albanie, dateNaissanceAdrian, null);
				addPermis(indAdrian, TypePermis.ETABLISSEMENT, date(2000, 6, 1), null, false);
				addAdresse(indAdrian, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.RouteGrangeNeuve, null, dateArriveeAngela, null);
				
				indAngela = addIndividu(noIndAngela, dateNaissanceAngela, "Fratila", "Angela", false);

				addNationalite(indAngela, MockPays.Albanie, dateNaissanceAngela, null);
				addPermis(indAngela, TypePermis.ETABLISSEMENT, date(2000, 9, 15), null, false);
				addAdresse(indAngela, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.RouteGrangeNeuve, null, dateArriveeAngela, null);
				
				marieIndividus(indAdrian, indAngela, dateMariage);
			}
		});
	}

	@Etape(id=1, descr="Chargement des habitants")
	public void etape1() throws Exception {
		
		// Adrian
		final PersonnePhysique adrian = addHabitant(noIndAdrian);
		{
			noCtbAdrian = adrian.getNumero();
		}
		
		final PersonnePhysique angela = addHabitant(noIndAngela);
		{
			noCtbAngela = angela.getNumero();
			
			addForFiscalPrincipal(angela, commune, dateArriveeAngela, null, MotifFor.ARRIVEE_HC, null);
		}
	}

	@Check(id=1, descr="Vérifie que seulement la femme est assujetti")
	public void check1() throws Exception {
		// Adrian
		final PersonnePhysique adrian = (PersonnePhysique) tiersDAO.get(noCtbAdrian);
		assertNotNull(adrian, "L'habitant Adrian n'a pas été créé");
		
		final PersonnePhysique angela = (PersonnePhysique) tiersDAO.get(noCtbAngela);
		assertNotNull(angela, "L'habitant Angela n'a pas été créé");
		assertExistenceForPrincipal(angela);
		final ForFiscalPrincipal ffpAngela = angela.getForFiscalPrincipalAt(null);
		assertEquals(commune.getNoOFS(), ffpAngela.getNumeroOfsAutoriteFiscale(), "Le for n'est pas assujetti à la bonne commune");
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffpAngela.getTypeAutoriteFiscale(), "Le for n'est pas assujetti à la bonne commune");
		
	}

	private void assertExistenceForPrincipal(Contribuable contribuable) {
		final ForFiscalPrincipal ffp = contribuable.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "Le contribuable n° " + FormatNumeroHelper.numeroCTBToDisplay(contribuable.getNumero()) + " devrait avoir un for principal");
	}

	@Etape(id=2, descr="Envoi de l'événement de mariage")
	public void etape2() throws Exception {
		final long id = addEvenementCivil(TypeEvenementCivil.MARIAGE, noIndAngela, dateMariage, commune.getNoOFS());
		
		commitAndStartTransaction();
		traiteEvenements(id);
	}
	
	@Check(id=2, descr="Vérifie que l'evenement est au statut traité et que le ménage est assujetti sur Lausanne")
	public void check2() {
		
		final List<EvenementCivilRegPP> list = evtExterneDAO.getAll();
		for (EvenementCivilRegPP evt : list) {
			if (noIndAngela == evt.getNumeroIndividuPrincipal()) {
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "L'événement n'a pas été traité");
			}
		}

		// Adrian
		final PersonnePhysique adrian = (PersonnePhysique) tiersDAO.get(noCtbAdrian);
		{
			ForFiscalPrincipal ffp = adrian.getForFiscalPrincipalAt(null);
			assertNull(ffp, "Angela devrait plus être assujetti");
		}
		
		// Angela
		final PersonnePhysique angela = (PersonnePhysique) tiersDAO.get(noCtbAngela);
		{
			ForFiscalPrincipal ffp = angela.getForFiscalPrincipalAt(null);
			assertNull(ffp, "Angela devrait plus être assujetti");
		}
		
		EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple((PersonnePhysique) tiersDAO.get(noCtbAngela), null);
		assertNotNull(couple, "Aucun ménage trouvé");
		
		final MenageCommun menage = couple.getMenage();
		assertNotNull(menage, "Aucun ménage trouvé");
		
		assertExistenceForPrincipal(menage);
		final ForFiscalPrincipal ffpMenage = menage.getForFiscalPrincipalAt(null);
		assertEquals(commune.getNoOFS(), ffpMenage.getNumeroOfsAutoriteFiscale(), "Le for du ménage n'est pas assujetti à la bonne commune");
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffpMenage.getTypeAutoriteFiscale(), "Le for du ménage n'est pas assujetti à la bonne commune");
	}
}
