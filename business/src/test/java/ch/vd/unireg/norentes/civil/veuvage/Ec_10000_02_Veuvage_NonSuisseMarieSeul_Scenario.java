package ch.vd.unireg.norentes.civil.veuvage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.civil.mock.MockEtatCivil;
import ch.vd.unireg.interfaces.civil.mock.MockEtatCivilList;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivil;

/**
 * Scénario de veuvage d'un individu non suisse marié seul.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_10000_02_Veuvage_NonSuisseMarieSeul_Scenario extends EvenementCivilScenario {

	public static final String NAME = "10000_02_Veuvage";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.VEUVAGE;
	}

	@Override
	public String getDescription() {
		return "Scénario de veuvage d'un individu non suisse marié seul";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndMikkel = 34897; // Mikkel

	private MockIndividu indMikkel;

	private long noHabMikkel;
	private long noMenage;

	private final RegDate avantDateMariage = RegDate.get(1986, 4, 27);
	private final RegDate dateMariage = avantDateMariage.getOneDayAfter();
	private final RegDate dateVeuvage = RegDate.get(2008, 1, 1);
	private final Commune communeMariage = MockCommune.Renens;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockIndividuConnector() {

			@Override
			protected void init() {

				indMikkel = addIndividu(noIndMikkel, RegDate.get(1956, 8, 3), "Hirst", "Mikkel", true);

				marieIndividu(indMikkel, RegDate.get(1986, 4, 8));

				addNationalite(indMikkel, MockPays.Danemark, RegDate.get(1961, 3, 12), null);
				addEtatCivil(indMikkel, dateVeuvage, TypeEtatCivil.VEUF);
				addAdresse(indMikkel, TypeAdresseCivil.PRINCIPALE, MockRue.VillarsSousYens.RuelleDuCarroz, null, RegDate.get(1974, 3, 3), avantDateMariage);
				addAdresse(indMikkel, TypeAdresseCivil.PRINCIPALE, MockRue.Renens.QuatorzeAvril, null, dateMariage, null);
			}
		});
	}

	@Etape(id=1, descr="Chargement de l'habitant Mikkel, danois marié seul et bientot veuf")
	public void step1() {

		// Pierre
		PersonnePhysique mikkel = addHabitant(noIndMikkel);
		noHabMikkel = mikkel.getNumero();
		addForFiscalPrincipal(mikkel, MockCommune.VillarsSousYens, RegDate.get(1974, 3, 3), avantDateMariage, MotifFor.DEMENAGEMENT_VD, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
		                      ModeImposition.SOURCE);

		// ménage
		MenageCommun menage = new MenageCommun();
		menage = (MenageCommun)tiersDAO.save(menage);
		noMenage = menage.getNumero();
		tiersService.addTiersToCouple(menage, mikkel, dateMariage, null);
		addForFiscalPrincipal(menage, communeMariage, dateMariage, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);

		menage.setBlocageRemboursementAutomatique(false);

	}

	@Check(id=1, descr="Vérifie que l'habitant Mikkel est marié seul et le For du menage existe")
	public void check1() throws Exception {

		PersonnePhysique mikkel = (PersonnePhysique) tiersDAO.get(noHabMikkel);
		{
			ForFiscalPrincipal ffp = mikkel.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + mikkel.getNumero() + " null");
			assertEquals(avantDateMariage, ffp.getDateFin(), "Date de fin du dernier for fausse");
		}

		{
			MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			EnsembleTiersCouple etc = tiersService.getEnsembleTiersCouple(mc, dateMariage.getOneDayAfter());
			assertNull(etc.getConjoint(mikkel), "Pierre n'est pas marié seul");
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(communeMariage.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + communeMariage.getNomOfficiel());
		}

		assertBlocageRemboursementAutomatique(true, false);
	}

	@Etape(id=2, descr="Envoi de l'événenent Veuvage")
	public void step2() throws Exception {

		doModificationIndividu(noIndMikkel, individu -> {
			final MockEtatCivilList etatsCivils = individu.getEtatsCivils();
			final MockEtatCivil etatCivil = new MockEtatCivil();
			etatCivil.setDateDebut(dateVeuvage);
			etatCivil.setTypeEtatCivil(TypeEtatCivil.VEUF);
			etatsCivils.add(etatCivil);
		});

		final long id = addEvenementCivil(TypeEvenementCivil.VEUVAGE, noIndMikkel, dateVeuvage, communeMariage.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que le menage commun a été fermé et le For principal de l'individu créé")
	public void check2() {

		{
			PersonnePhysique mikkel = (PersonnePhysique) tiersDAO.get(noHabMikkel);
			ForFiscalPrincipalPP ffp = mikkel.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + mikkel.getNumero() + " null");
			assertNull(ffp.getDateFin(), "Le for principal de l'habitant est fermé");
			ModeImposition expected = ModeImposition.MIXTE_137_1;
			assertEquals(expected, ffp.getModeImposition(), "L'habitant devrait être en mode " + expected.texte());
		}

		{
			MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " ouvert");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertEquals(dateVeuvage, ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(communeMariage.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + communeMariage.getNomOfficiel());
		}

		// le survivant ne doit pas voir ses remboursements automatiques bloqués
		assertBlocageRemboursementAutomatique(false, true);
}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttenduMikkel, boolean blocageAttenduMenage) {
		assertBlocageRemboursementAutomatique(blocageAttenduMikkel, tiersDAO.get(noHabMikkel));
		assertBlocageRemboursementAutomatique(blocageAttenduMenage, tiersDAO.get(noMenage));
	}
}
