package ch.vd.uniregctb.norentes.civil.obtention.permis;


import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

public class Ec_16000_01_ObtentionPermis_MarieADeuxSansForPrincipal_Scenario extends EvenementCivilScenario {

	public static final String NAME = "16000_01_ObtentionPermis";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.CHGT_CATEGORIE_ETRANGER;
	}

	@Override
	public String getDescription() {
		return "Obtention d'un permis C pour l'un des membres d'un couple marié";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndMomo = 54321; // momo
	private static final long noIndBea = 23456; // bea

	private MockIndividu indMomo;
	private MockIndividu indBea;

	private long noHabMomo;
	private long noHabBea;
	private long noMenage;

	private final RegDate dateNaissanceBea = RegDate.get(1963, 8, 20);
	private final RegDate dateMariage = RegDate.get(2006, 4, 27);
	private final RegDate dateObtentionPermis =  dateMariage.addMonths(8);
	private final Commune communeMariage = MockCommune.VillarsSousYens;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				indMomo = addIndividu(noIndMomo, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);
				indBea = addIndividu(noIndBea, dateNaissanceBea, "Duval", "Béatrice", false);

				marieIndividus(indMomo, indBea, dateMariage);

				addOrigine(indMomo, MockPays.France.getNomMinuscule());
				addPermis(indMomo, TypePermis.ETABLISSEMENT, dateObtentionPermis, null, false);
				addNationalite(indMomo, MockPays.France, RegDate.get(1963, 8, 20), null);
				addAdresse(indMomo, TypeAdresseCivil.PRINCIPALE, MockRue.Zurich.GloriaStrasse, null, dateMariage, null);
				addAdresse(indMomo, TypeAdresseCivil.SECONDAIRE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateMariage, null);

				addOrigine(indBea, MockCommune.Lausanne);
				addNationalite(indBea, MockPays.Suisse, RegDate.get(1961, 3, 12), null);
				addAdresse(indBea, TypeAdresseCivil.PRINCIPALE, MockRue.Zurich.GloriaStrasse, null, dateMariage, null);
				addAdresse(indBea, TypeAdresseCivil.SECONDAIRE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateMariage, null);
			}
		});
	}

	@Etape(id=1, descr="Chargement d'un marié, de son conjoint et du ménage commun")
	public void etape1() {

		// momo
		final PersonnePhysique momo = addHabitant(noIndMomo);
		noHabMomo = momo.getNumero();

		// bea
		final PersonnePhysique bea = addHabitant(noIndBea);
		noHabBea = bea.getNumero();

		// ménage
		{
			MenageCommun menage = new MenageCommun();
			menage = (MenageCommun)tiersDAO.save(menage);
			noMenage = menage.getNumero();
			tiersService.addTiersToCouple(menage, momo, dateMariage, null);
			tiersService.addTiersToCouple(menage, bea, dateMariage, null);
		}
	}

	@Check(id=1, descr="")
	public void check1() {

		final PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noHabMomo);
		assertEquals(0, momo.getForsFiscauxValidAt(null).size(), "Momo a un for principal");

		final PersonnePhysique bea = (PersonnePhysique) tiersDAO.get(noHabBea);
		assertEquals(0, bea.getForsFiscauxValidAt(null).size(), "Béa a un for principal");

		final MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
		assertEquals(0, mc.getForsFiscauxValidAt(null).size(), "Le ménage a un for principal");
	}

	@Etape(id=2, descr="Obtention du permis C pour Momo")
	public void etape2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.CHGT_CATEGORIE_ETRANGER, noIndMomo, dateObtentionPermis, communeMariage.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie qu'aucun for n'a été ouvert")
	public void check2() {
		final EvenementCivilExterne evt = getEvenementCivilRegoupeForHabitant(noHabMomo);
		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "");

		check1();
	}
}
