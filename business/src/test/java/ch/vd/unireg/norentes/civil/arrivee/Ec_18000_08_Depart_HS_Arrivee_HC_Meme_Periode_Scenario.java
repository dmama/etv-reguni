package ch.vd.unireg.norentes.civil.arrivee;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tache;
import ch.vd.unireg.tiers.TacheControleDossier;
import ch.vd.unireg.tiers.TacheCriteria;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.type.TypeEvenementCivil;

public class Ec_18000_08_Depart_HS_Arrivee_HC_Meme_Periode_Scenario extends EvenementCivilScenario {

	public static final String NAME = "Ec_18000_08_Arrivee_HC";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC;
	}

	@Override
	public String getDescription() {
		return "Arrivée HC d'un contribuable parti HS plus tôt dans l'année et pour lequel une DI a déjà été émise au moment du départ";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndAlain = 123456L;

	private MockIndividu indAlain;

	private final RegDate dateNaissanceAlain = RegDate.get(1950, 12, 1);

	private final RegDate dateMajorite = dateNaissanceAlain.addYears(18);

	private final RegDate dateDepartHS = RegDate.get(2007, 3, 15);

	private final RegDate dateRetourHC = RegDate.get(2007, 10, 1);

	/**
	 * @see ch.vd.unireg.norentes.common.EvenementCivilScenario#initServiceCivil()
	 */
	@Override
	protected void initServiceCivil() {

		serviceCivilService.setUp(new MockIndividuConnector() {

			@Override
			protected void init() {
				indAlain = addIndividu(noIndAlain, dateNaissanceAlain, "Martin", "Alain", true);
				addNationalite(indAlain, MockPays.Suisse, dateNaissanceAlain, null);
				addAdresse(indAlain, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateNaissanceAlain, dateDepartHS.getOneDayBefore());
			}

		});

	}

	@Etape(id=1, descr="Création de l'individu parti HS")
	public void etape1() throws Exception {
		final PersonnePhysique alain = addHabitant(noIndAlain);
		addForFiscalPrincipal(alain, MockCommune.Lausanne, dateMajorite, dateDepartHS.getOneDayBefore(), MotifFor.MAJORITE, MotifFor.DEPART_HS);
		tiersService.openForFiscalPrincipal(alain, dateDepartHS, MotifRattachement.DOMICILE, MockPays.PaysInconnu.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, ModeImposition.ORDINAIRE, MotifFor.DEPART_HS);
		tiersService.updateHabitantFlag(alain, noIndAlain, null);
		addDeclarationImpot(alain, RegDate.get(dateDepartHS.year(),1,1), dateDepartHS.getOneDayBefore(), dateDepartHS, 60);
	}

	@Check(id=1, descr="Vérification que l'individu est bien parti HS, qu'une DI a bien été émise")
	public void check1() throws Exception {

		final PersonnePhysique alain = tiersDAO.getPPByNumeroIndividu(noIndAlain);
		assertFalse(alain.isHabitantVD(),"Alain aurait dû devenir non-habitant suite à son départ HS");

		final ForFiscalPrincipal f = alain.getDernierForFiscalPrincipal();
		assertNotNull(f, "For fiscal principal null");
		assertEquals(MotifFor.DEPART_HS, f.getMotifOuverture(), "Dernier for ouvert pour mauvais motif");

		final List<Declaration> declarations = alain.getDeclarationsDansPeriode(Declaration.class, dateDepartHS.year(), false);
		assertNotNull(declarations, "Liste des DI nulle");
		assertEquals(1, declarations.size(), "Mauvais nombre de déclarations");
		final Declaration di = declarations.get(0);
		assertEquals(TypeEtatDocumentFiscal.EMIS, di.getDernierEtatDeclaration().getEtat(), "Déclaration dans le mauvais état");

		// vérification qu'aucune tâche n'est actuellement en instance
		final TacheCriteria tacheCriteria = new TacheCriteria();
		tacheCriteria.setContribuable(alain);
		tacheCriteria.setEtatTache(TypeEtatTache.EN_INSTANCE);
		final List<Tache> taches = tacheDAO.find(tacheCriteria);
		assertNotNull(taches, "Liste des tâches en instance nulle");
		assertEquals(0, taches.size(), "Il y a déjà des tâches en instance");
	}

	@Etape(id=2, descr="Envoi de l'événement d'arrivée HC")
	public void etape2() throws Exception {

		// le retour, changement d'adresse
		doModificationIndividu(noIndAlain, individu -> individu.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Bex.CheminDeLaForet, null, dateRetourHC, null)));

		final long id = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC, noIndAlain, dateRetourHC, MockCommune.Bex.getNoOFS());
		commitAndStartTransaction();

		// On traite le nouvel événement
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérification que le for principal d'Alain est sur le canton et qu'une tâche de contrôle de dossier a été générée")
	public void check2() throws Exception {

		final PersonnePhysique alain = tiersDAO.getPPByNumeroIndividu(noIndAlain);
		assertTrue(alain.isHabitantVD(),"Alain aurait dû redevenir habitant suite à son retour");

		final ForFiscalPrincipal f = alain.getDernierForFiscalPrincipal();
		assertNotNull(f, "For fiscal principal null");
		assertEquals(MotifFor.ARRIVEE_HC, f.getMotifOuverture(), "Dernier for ouvert pour mauvais motif");

		final TacheCriteria tacheCriteria = new TacheCriteria();
		tacheCriteria.setContribuable(alain);
		tacheCriteria.setEtatTache(TypeEtatTache.EN_INSTANCE);
		final List<Tache> taches = tacheDAO.find(tacheCriteria);
		assertNotNull(taches, "Liste des tâches en instance nulle");

		Tache tacheControle = null;
		for (Tache tache : taches) {
			if (tache instanceof TacheControleDossier) {
				assertNull(tacheControle, "On n'attendait qu'une seule tâche de contrôle de dossier");
				tacheControle = tache;
			}
		}
		assertNotNull(tacheControle, "Aucune tâche de contrôle de dossier n'a été trouvée");
	}
}
