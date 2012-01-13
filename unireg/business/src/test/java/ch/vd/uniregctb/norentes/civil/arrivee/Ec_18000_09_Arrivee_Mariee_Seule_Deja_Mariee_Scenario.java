package ch.vd.uniregctb.norentes.civil.arrivee;

import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Scénario qui teste l'arrivée l'une personne mariée seule, alors qu'elle existe déjà dans le registre comme composant d'un ménage actif
 * (cas réel observé en production, voir l'événement civil n°522)
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class Ec_18000_09_Arrivee_Mariee_Seule_Deja_Mariee_Scenario extends EvenementCivilScenario {

	public static final String NAME = "Ec_18000_09_Arrivee_Mariee_Seule_Deja_Mariee";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC;
	}

	@Override
	public String getDescription() {
		return "Arrivée d'une personne mariée seule, alors qu'elle existe déjà dans le registre comme composant d'un ménage actif (cas réel observé en production, voir l'événement civil n°522)";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndGeorgette = 937931L;
	private static final long noIndJean = 312580L;

	private long noCtbMenageCommun;
	private long noCtbJean;
	private long noCtbGeorgette;

	private long evenementId;

	/**
	 * @see ch.vd.uniregctb.norentes.common.EvenementCivilScenario#initServiceCivil()
	 */
	@Override
	protected void initServiceCivil() {

		serviceCivilService.setUp(new MockServiceCivil() {

			@Override
			protected void init() {
				MockIndividu jean = addIndividu(noIndJean, date(1941, 6, 6), "Nzikou", "Jean", true);
				setPermis(jean, TypePermis.ETABLISSEMENT, date(2001, 7, 11), null, false);
				addAdresse(jean, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2004, 1,
						15), null);
				addEtatCivil(jean, date(2004, 1, 15), TypeEtatCivil.DIVORCE);
				addEtatCivil(jean, date(2008, 7, 26), TypeEtatCivil.MARIE);

				MockIndividu georgette = addIndividu(noIndGeorgette, date(1951, 11, 3), "Matala Bambi", "Georgette", false);
				setPermis(georgette, TypePermis.ANNUEL, date(2008, 9, 8), date(2009, 6, 12), false);
				setPermis(georgette, TypePermis.ANNUEL, date(2009, 6, 13), null, false);
				addNationalite(georgette, MockPays.France, date(1951, 11, 3), null);
				addAdresse(georgette, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(
						2008, 8, 11), date(2008, 8, 14));
				addAdresse(georgette, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(
						2008, 8, 15), date(2009, 6, 12));
				addAdresse(georgette, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, date(
						2009, 6, 13), null);
				addEtatCivil(georgette, date(1951, 11, 3), TypeEtatCivil.CELIBATAIRE);
				addEtatCivil(georgette, date(2008, 7, 26), TypeEtatCivil.MARIE);
				addEtatCivil(georgette, date(2009, 6, 13), TypeEtatCivil.MARIE);
			}

		});

	}

	@Etape(id = 1, descr = "Création du ménage commun Jean & Georgette dans le registre fiscal")
	public void etape1() throws Exception {

		final PersonnePhysique jean = addHabitant(noIndJean);
		final PersonnePhysique georgette = addHabitant(noIndGeorgette);
		final EnsembleTiersCouple ensemble = tiersService.createEnsembleTiersCouple(jean, georgette, date(2008, 7, 26), null);

		final MenageCommun menage = ensemble.getMenage();
		addForFiscalPrincipal(menage, MockCommune.Lausanne, date(2008, 7, 26), null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);

		noCtbMenageCommun = menage.getNumero();
		noCtbJean = jean.getNumero();
		noCtbGeorgette = georgette.getNumero();
	}

	@Check(id = 1, descr = "Vérification que le ménage commun est bien ouvert")
	public void check1() throws Exception {

		final MenageCommun menage = (MenageCommun) tiersDAO.get(noCtbMenageCommun);
		assertNotNull(menage, "Le ménage n'existe pas et aurait dû être créé.");

		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(menage, RegDate.get());
		assertNotNull(ensemble.getPrincipal(), "Le ménage ne possède pas de tiers principal actif alors qu'il le devrait.");
		assertNotNull(ensemble.getConjoint(), "Le ménage ne possède pas de tiers conjoint actif alors qu'il le devrait.");
	}

	@Etape(id = 2, descr = "Envoi de l'événement d'arrivée HC de Georgette en tant que mariée seule")
	public void etape2() throws Exception {

		evenementId = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC, noIndGeorgette, date(2009, 6, 13), MockCommune.Lausanne.getNoOFS());
		commitAndStartTransaction();

		// On traite le nouvel événement
		traiteEvenements(evenementId);
	}

	@Check(id = 2, descr = "Vérification que l'événement est en erreur et qu'aucune modification n'a été apportée sur le ménage commun")
	public void check2() throws Exception {

		final EvenementCivilExterne evenement = evtExterneDAO.get(evenementId);
		assertEquals(EtatEvenementCivil.EN_ERREUR, evenement.getEtat(), "L'événement civil devrait être en erreur.");

		final Set<EvenementCivilExterneErreur> erreurs = evenement.getErreurs();
		assertEquals(1, erreurs.size(), "Il devrait y avoir exactement 1 erreur");

		final EvenementCivilExterneErreur erreur = erreurs.iterator().next();
		final String messageAttendu = String.format("L'individu principal [%d] est en ménage commun avec une personne [%d] dans le fiscal alors qu'il est marié seul dans le civil", noCtbGeorgette, noCtbJean);
		assertEquals(messageAttendu, erreur.getMessage(), "L'exception levée n'est pas la bonne.");

		check1();
	}
}
