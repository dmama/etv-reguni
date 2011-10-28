package ch.vd.uniregctb.norentes.civil.annulation.mariage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.validation.ValidationService;

/**
 * Scénario de l'annulation de mariage du cas JIRA UNIREG-1086.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_4001_07_AnnulationMariage_JIRA1086_Scenario extends EvenementCivilScenario {

	private MetierService metierService;
	private ValidationService validationService;

	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}

	public static final String NAME = "4001_07_AnnulationMariage";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ANNUL_MARIAGE;
	}

	@Override
	public String getDescription() {
		return "Scénario de l'annulation de mariage d'un couple (UNIREG-1086).";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final class DefaultMockServiceCivil extends MockServiceCivil {

		@Override
		protected void init() {
			MockIndividu deborah = addIndividu(noIndDeborah, dateNaissanceDeborah, "Lopez", "Deborah", false);
			addOrigine(deborah, MockPays.Suisse.getNomMinuscule());
			addNationalite(deborah, MockPays.Suisse, dateNaissanceDeborah, null);

			addAdresse(deborah, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArriveeDeborah, null);
			addAdresse(deborah, TypeAdresseCivil.COURRIER, MockRue.Lausanne.RouteMaisonNeuve, null, dateArriveeDeborah, null);

			MockIndividu jaime = addIndividu(noIndJaime, dateNaissanceJaime, "Lopez", "Jaime", true);
			addOrigine(jaime, MockPays.Suisse.getNomMinuscule());
			addNationalite(jaime, MockPays.Suisse, dateNaissanceJaime, null);

			addAdresse(jaime, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArriveeJaime, null);
			addAdresse(jaime, TypeAdresseCivil.COURRIER, MockRue.Lausanne.RouteMaisonNeuve, null, dateArriveeJaime, null);

			marieIndividus(jaime, deborah, dateMariage);
		}

		public void annuleMariage() {
			// pour Jaime
			MockIndividu jaime = getIndividu(noIndJaime);
			ch.vd.uniregctb.interfaces.model.EtatCivil etatCivilAlexandre = jaime.getEtatCivilCourant();
			jaime.getEtatsCivils().remove(etatCivilAlexandre);
			jaime.setConjoint(null);
			// pour Deborah
			MockIndividu deborah = getIndividu(noIndDeborah);
			ch.vd.uniregctb.interfaces.model.EtatCivil etatCivilSylvie = deborah.getEtatCivilCourant();
			deborah.getEtatsCivils().remove(etatCivilSylvie);
			deborah.setConjoint(null);
		}

	}

	private DefaultMockServiceCivil serviceCivil;

	@Override
	protected void initServiceCivil() {
		serviceCivil = new DefaultMockServiceCivil();
		serviceCivilService.setUp(serviceCivil);
	}

	private final long noIndJaime = 2000465; // Jaime
	private final long noIndDeborah = 2000457; // Deborah

	private long noHabJaime;
	private long noHabDeborah;
	private long noMenage;

	private final RegDate dateNaissanceDeborah = RegDate.get(1971, 11, 6);
	private final RegDate dateArriveeDeborah = RegDate.get(1980, 3, 1);
	private final RegDate dateNaissanceJaime = RegDate.get(1971, 8, 23);
	private final RegDate dateArriveeJaime = RegDate.get(1985, 2, 1);	// 01.02.2009
	private final RegDate dateMariage = RegDate.get(1997, 7, 19);		// 19.07.1997
	private final RegDate dateDemenagement = RegDate.get(2003, 11, 30); // 30.11.2003

	private String errorMessage = null;

	@SuppressWarnings("deprecation")
	@Etape(id=1, descr="Chargement de madame et ses fors")
	public void step1() {
		// Deborah
		final PersonnePhysique deborah = addHabitant(noIndDeborah);
		noHabDeborah = deborah.getNumero();

		// Jaime
		final PersonnePhysique jaime = addHabitant(noIndJaime);
		noHabJaime = jaime.getNumero();

		// Ménage commun
		MenageCommun menage = (MenageCommun) tiersDAO.save(new MenageCommun());
		noMenage = menage.getNumero();
		// membres
		tiersService.addTiersToCouple(menage, jaime, dateMariage, null);
		tiersService.addTiersToCouple(menage, deborah, dateMariage, null);

		// Fors ménage
		addForFiscalPrincipal(menage, MockCommune.Renens, dateMariage, dateDemenagement, MotifFor.INDETERMINE, MotifFor.DEMENAGEMENT_VD);
		addForFiscalPrincipal(menage, MockCommune.Lausanne, dateDemenagement.getOneDayAfter(), null, MotifFor.DEMENAGEMENT_VD, null);
	}

	@Check(id=1, descr="Vérifie que les habitants existent et le ménage commun a un for ouvert")
	public void check1() {
		{
			final PersonnePhysique deborah = (PersonnePhysique) tiersDAO.get(noHabDeborah);
			assertNotNull(deborah, "L'individu correspondant à madame n'a pas été créé");
			final ForFiscalPrincipal ffp = deborah.getDernierForFiscalPrincipal();
			assertNull(ffp, "For principal de l'Habitant " + deborah.getNumero() + " non null");
		}

		{
			final PersonnePhysique jaime = (PersonnePhysique) tiersDAO.get(noHabJaime);
			assertNotNull(jaime, "L'individu correspondant à monsieur n'a pas été créé");
			final ForFiscalPrincipal ffp = jaime.getDernierForFiscalPrincipal();
			assertNull(ffp, "For principal de l'Habitant " + jaime.getNumero() + " non null");
		}
	}

	@Etape(id=2, descr="Annulation fiscale de mariage")
	public void step3() {
		try {
			metierService.annuleMariage(tiersDAO.getHabitantByNumeroIndividu(noIndJaime), tiersDAO.getHabitantByNumeroIndividu(noIndDeborah), dateMariage, null);
		}
		catch (MetierServiceException ece) {
			this.errorMessage = ece.getMessage();
		}

		final MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
		assertFalse(mc.isAnnule(), "L'annulation de mariage ne devrait pas annuler le ménage commun");
		final ValidationResults vr = validationService.validate(mc);
		assertEquals(0, vr.getErrors().size(), "Le ménage ne devrait avoir aucune erreur");
		assertEquals(0, vr.getWarnings().size(), "Le ménage ne devrait avoir aucun warning");
	}

	@Check(id=2, descr="Vérifie que le traitement est en erreur")
	public void check3() {
		assertNotNull(errorMessage, "Il aurait dû se produire une erreur");
		assertEquals("Il y a eu d'autres opérations après le mariage/réconciliation", errorMessage, "L'erreur est pas la bonne");
	}
}
