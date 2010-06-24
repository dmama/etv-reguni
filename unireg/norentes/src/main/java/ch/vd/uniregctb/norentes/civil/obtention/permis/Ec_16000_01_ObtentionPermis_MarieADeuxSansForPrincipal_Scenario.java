package ch.vd.uniregctb.norentes.civil.obtention.permis;

import annotation.Check;
import annotation.Etape;
import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

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

	private final long noIndMomo = 54321; // momo
	private final long noIndBea = 23456; // bea

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

				addOrigine(indMomo, MockPays.France, null, RegDate.get(1963, 8, 20));
				addPermis(indMomo, EnumTypePermis.ETABLLISSEMENT, dateObtentionPermis, null, 0, false);
				addNationalite(indMomo, MockPays.France, RegDate.get(1963, 8, 20), null, 0);
				addAdresse(indMomo, EnumTypeAdresse.PRINCIPALE, MockRue.Zurich.GloriaStrasse, null, dateMariage, null);
				addAdresse(indMomo, EnumTypeAdresse.SECONDAIRE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateMariage, null);

				addOrigine(indBea, MockPays.Suisse, MockCommune.Lausanne, RegDate.get(1961, 3, 12));
				addNationalite(indBea, MockPays.Suisse, RegDate.get(1961, 3, 12), null, 0);
				addAdresse(indBea, EnumTypeAdresse.PRINCIPALE, MockRue.Zurich.GloriaStrasse, null, dateMariage, null);
				addAdresse(indBea, EnumTypeAdresse.SECONDAIRE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateMariage, null);
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
		final EvenementCivilData evt = getEvenementCivilRegoupeForHabitant(noHabMomo);
		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "");

		check1();
	}
}
