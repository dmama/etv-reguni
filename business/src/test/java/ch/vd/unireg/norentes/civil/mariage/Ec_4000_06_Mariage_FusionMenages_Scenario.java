package ch.vd.unireg.norentes.civil.mariage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.metier.MetierService;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.SituationFamille;
import ch.vd.unireg.type.EtatCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.type.TypePermis;

/**
 * Scénario de reconstitution d'un ménage commun à partir de deux ménages communs incomplets.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_4000_06_Mariage_FusionMenages_Scenario extends EvenementCivilScenario {

	private MetierService metierService;

	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	public static final String NAME = "4000_06_Mariage";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.MARIAGE;
	}

	@Override
	public String getDescription() {
		return "Reconstitution d'un ménage commun à partir de deux ménages communs incomplets.";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndAlfredo = 211605; // Alfredo
	private static final long noIndArmando = 363623; // Armando

	private MockIndividu indAlfredo;
	private MockIndividu indArmando;

	private long noHabAlfredo;
	private long noHabArmando;
	private long noMenageAlfredo;
	private long noMenageArmando;

	private final RegDate dateDebutAlfredo = RegDate.get(2003, 1, 1);
	private final RegDate dateDemenagementArmando = RegDate.get(2003, 1, 1);
	private final RegDate dateMariageAlfredo = RegDate.get(2003, 1, 6);
	private final RegDate dateMariageArmando = RegDate.get(2003, 7, 1);
	private final Commune commune = MockCommune.Lausanne;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockIndividuConnector() {

			@Override
			protected void init() {

				final RegDate dateNaissanceAlfredo = RegDate.get(1952, 4, 8);
				indAlfredo = addIndividu(noIndAlfredo, dateNaissanceAlfredo , "Abrantes Fidalgo", "Alfredo", true);

				final RegDate dateNaissanceArmando = RegDate.get(1960, 9, 9);
				indArmando = addIndividu(noIndArmando, dateNaissanceArmando, "Cerqueira Barbosa", "Armando", true);

				marieIndividu(indAlfredo, RegDate.get(1977, 4, 30));
				marieIndividu(indArmando, RegDate.get(1982, 4, 25));

				addNationalite(indAlfredo, MockPays.Albanie, dateNaissanceAlfredo, null);
				addPermis(indAlfredo, TypePermis.ETABLISSEMENT, null, null, false);
				addAdresse(indAlfredo, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateDebutAlfredo, null);
				addAdresse(indAlfredo, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateDebutAlfredo, null);

				addNationalite(indArmando, MockPays.Danemark, dateNaissanceArmando, null);
				addPermis(indArmando, TypePermis.ETABLISSEMENT, null, null, false);
				addAdresse(indArmando, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateDemenagementArmando, null);
				addAdresse(indArmando, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateDemenagementArmando, null);
			}

		});
	}

	@SuppressWarnings("deprecation")
	@Etape(id=1, descr="Chargement des deux habitants mariés seuls et leurs ménages communs")
	public void step1() {
		// Alfredo
		final PersonnePhysique alfredo = addHabitant(noIndAlfredo);
		noHabAlfredo = alfredo.getNumero();

		// ménage Alfredo
		{
			MenageCommun menage = new MenageCommun();
			menage = (MenageCommun) tiersDAO.save(menage);
			noMenageAlfredo = menage.getNumero();
			tiersService.addTiersToCouple(menage, alfredo, dateMariageAlfredo, null);

			addForFiscalPrincipal(menage, commune, dateMariageAlfredo, null, MotifFor.INDETERMINE, null);
			menage.setBlocageRemboursementAutomatique(false);
		}

		// Armando
		final PersonnePhysique armando = addHabitant(noIndArmando);
		noHabArmando = armando.getNumero();

		// ménage Armando
		{
			MenageCommun menage = new MenageCommun();
			menage = (MenageCommun) tiersDAO.save(menage);
			noMenageArmando = menage.getNumero();
			tiersService.addTiersToCouple(menage, armando, dateMariageArmando, null);

			addForFiscalPrincipal(menage, commune, dateMariageArmando, null, MotifFor.INDETERMINE, null);
			menage.setBlocageRemboursementAutomatique(false);
		}
	}

	@Check(id=1, descr="Vérifie que les deux sont mariés, ayant chacun son For ouvert")
	public void check1() {

		{
			final PersonnePhysique alfredo = (PersonnePhysique) tiersDAO.get(noHabAlfredo);
			final ForFiscalPrincipal ffp = alfredo.getForFiscalPrincipalAt(null);
			assertNull(ffp, "For principal de l'Habitant " + alfredo.getNumero() + " non null");
		}

		{
			final MenageCommun menageAlfredo = (MenageCommun) tiersDAO.get(noMenageAlfredo);
			assertEquals(1, menageAlfredo.getForsFiscaux().size(), "Le ménage d'Alfredo a plus d'un for principal");
			final ForFiscalPrincipal ffp = menageAlfredo.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du ménage d'Alfredo est null");
			assertEquals(dateMariageAlfredo, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(commune.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + commune.getNomOfficiel());
		}

		{
			final PersonnePhysique armando = (PersonnePhysique)tiersDAO.get(noHabArmando);
			final ForFiscalPrincipal ffp = armando.getForFiscalPrincipalAt(null);
			assertNull(ffp, "For principal de l'Habitant " + armando.getNumero() + " non null");
		}

		{
			final MenageCommun menageArmando = (MenageCommun) tiersDAO.get(noMenageArmando);
			assertEquals(1, menageArmando.getForsFiscaux().size(), "Le ménage d'Armando a plus d'un for principal");
			final ForFiscalPrincipal ffp = menageArmando.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du ménage d'Armando est null");
			assertEquals(dateMariageArmando, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(commune.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + commune.getNomOfficiel());
		}

		assertBlocageRemboursementAutomatique(true, true, false);
	}

	@Etape(id=2, descr="Reconstitution du ménage commun à partir des deux ménages communs incomplets")
	public void step2() throws Exception {
		//UNIREG-2771 On annule le for principal du ménage d'armando pour que la fusion fonctionne
		final MenageCommun menageArmando = (MenageCommun) tiersDAO.get(noMenageArmando);
		menageArmando.getDernierForFiscalPrincipal().setAnnule(true);
		metierService.fusionneMenages((MenageCommun) tiersDAO.get(noMenageAlfredo), (MenageCommun) tiersDAO.get(noMenageArmando), null, EtatCivil.LIE_PARTENARIAT_ENREGISTRE);
	}

	@Check(id=2, descr="Vérifie que la reconstitution du ménage commun a été bien effectuée")
	public void check2() {

		{
			final PersonnePhysique alfredo = (PersonnePhysique) tiersDAO.get(noHabAlfredo);
			final ForFiscalPrincipal ffp = alfredo.getForFiscalPrincipalAt(null);
			assertNull(ffp, "For principal de l'Habitant " + alfredo.getNumero() + " non null");
		}

		{
			final PersonnePhysique armando = (PersonnePhysique)tiersDAO.get(noHabArmando);
			final ForFiscalPrincipal ffp = armando.getForFiscalPrincipalAt(null);
			assertNull(ffp, "For principal de l'Habitant " + armando.getNumero() + " non null");
		}

		long noMenageChoisi = noMenageAlfredo;
		if (dateMariageArmando.isBefore(dateMariageAlfredo)) {
			noMenageChoisi = noMenageArmando;
		}
		{
			final MenageCommun menageChoisi = (MenageCommun) tiersDAO.get(noMenageChoisi);
			assertEquals(1, menageChoisi.getForsFiscaux().size(), "Le ménage a plus d'un for principal");

			final ForFiscalPrincipal ffp = menageChoisi.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du ménage est null");
			assertEquals(dateMariageAlfredo, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(commune.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + commune.getNomOfficiel());

			SituationFamille sf = menageChoisi.getSituationFamilleActive();
			assertNotNull(sf, "Aucune situation famille trouvée");
			assertEquals(EtatCivil.LIE_PARTENARIAT_ENREGISTRE, sf.getEtatCivil(), "Mauvaise situation famille");
		}

		long noMenageAnnule = noMenageArmando;
		if (noMenageChoisi == noMenageArmando) {
			noMenageAnnule = noMenageAlfredo;
			final MenageCommun menageAnnule = (MenageCommun) tiersDAO.get(noMenageAnnule);
			assertTrue(menageAnnule.isAnnule(), "Le ménage n'est pas annulé");
		}

		assertBlocageRemboursementAutomatique(true, true, false);
	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttenduAlfredo, boolean blocageAttenduArmando, boolean blocageAttenduMenage) {

		assertBlocageRemboursementAutomatique(blocageAttenduAlfredo, tiersDAO.get(noHabAlfredo));
		assertBlocageRemboursementAutomatique(blocageAttenduArmando, tiersDAO.get(noHabArmando));
		assertBlocageRemboursementAutomatique(blocageAttenduMenage, tiersDAO.get(noMenageAlfredo));
	}

}
