package ch.vd.uniregctb.norentes.civil.veuvage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.EtatCivilList;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockEtatCivil;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

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
		serviceCivilService.setUp(new MockServiceCivil() {

			@Override
			protected void init() {

				indMikkel = addIndividu(noIndMikkel, RegDate.get(1956, 8, 3), "Hirst", "Mikkel", true);

				marieIndividu(indMikkel, RegDate.get(1986, 4, 8));

				addOrigine(indMikkel, MockPays.Danemark.getNomMinuscule());
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
		ForFiscalPrincipal f = addForFiscalPrincipal(mikkel, MockCommune.VillarsSousYens, RegDate.get(1974, 3, 3), avantDateMariage, MotifFor.DEMENAGEMENT_VD, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		f.setModeImposition(ModeImposition.SOURCE);

		// ménage
		MenageCommun menage = new MenageCommun();
		menage = (MenageCommun)tiersDAO.save(menage);
		noMenage = menage.getNumero();
		tiersService.addTiersToCouple(menage, mikkel, dateMariage, null);
		f = addForFiscalPrincipal(menage, communeMariage, dateMariage, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
		f.setModeImposition(ModeImposition.ORDINAIRE);

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
			assertEquals(communeMariage.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + communeMariage.getNomMinuscule());
		}

		assertBlocageRemboursementAutomatique(true, false);
	}

	@Etape(id=2, descr="Envoi de l'événenent Veuvage")
	public void step2() throws Exception {

		doModificationIndividu(noIndMikkel, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				final EtatCivilList etatsCivils = individu.getEtatsCivils();
				final MockEtatCivil etatCivil = new MockEtatCivil();
				etatCivil.setDateDebut(dateVeuvage);
				etatCivil.setTypeEtatCivil(TypeEtatCivil.VEUF);
				etatsCivils.add(etatCivil);
			}
		});

		final long id = addEvenementCivil(TypeEvenementCivil.VEUVAGE, noIndMikkel, dateVeuvage, communeMariage.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que le menage commun a été fermé et le For principal de l'individu créé")
	public void check2() {

		{
			PersonnePhysique mikkel = (PersonnePhysique) tiersDAO.get(noHabMikkel);
			ForFiscalPrincipal ffp = mikkel.getDernierForFiscalPrincipal();
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
			assertEquals(communeMariage.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + communeMariage.getNomMinuscule());
		}

		// le survivant ne doit pas voir ses remboursements automatiques bloqués
		assertBlocageRemboursementAutomatique(false, true);
}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttenduMikkel, boolean blocageAttenduMenage) {
		assertBlocageRemboursementAutomatique(blocageAttenduMikkel, tiersDAO.get(noHabMikkel));
		assertBlocageRemboursementAutomatique(blocageAttenduMenage, tiersDAO.get(noMenage));
	}
}
