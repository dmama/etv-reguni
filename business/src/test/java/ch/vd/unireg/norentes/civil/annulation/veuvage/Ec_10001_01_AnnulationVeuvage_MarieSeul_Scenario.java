package ch.vd.unireg.norentes.civil.annulation.veuvage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.type.TypePermis;

/**
 * Scénario d'un événement annulation de veuvage d'une personne veuve seule.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_10001_01_AnnulationVeuvage_MarieSeul_Scenario extends EvenementCivilScenario {

	public static final String NAME = "10001_01_AnnulationVeuvage";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ANNUL_VEUVAGE;
	}

	@Override
	public String getDescription() {
		return "Scénario d'un événement annulation de veuvage d'une personne veuve seule.";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndAndre = 92647; // andré

	private MockIndividu indAndre;

	private long noHabAndre;
	private long noMenage;

	private final RegDate dateNaissance = RegDate.get(1956, 2, 25);
	private final RegDate dateMariage = RegDate.get(1982, 12, 4);
	private final RegDate dateAvantMariage = dateMariage.getOneDayBefore();
	private final RegDate dateVeuvage = RegDate.get(2008, 1, 1);
	private final Commune commune = MockCommune.Lausanne;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				indAndre = addIndividu(noIndAndre, dateNaissance, "Girard", "André", true);
				marieIndividu(indAndre, dateMariage);
				addNationalite(indAndre, MockPays.France, indAndre.getDateNaissance(), null);
				addPermis(indAndre, TypePermis.FRONTALIER, RegDate.get(1980, 3, 1), null, false);
			}
		});
	}

	@Etape(id=1, descr="Chargement de l'habitant marié seul")
	public void step1() {
		PersonnePhysique andre = addHabitant(noIndAndre);
		noHabAndre = andre.getNumero();

		addForFiscalPrincipal(andre, commune, RegDate.get(1980, 3, 1), dateAvantMariage, MotifFor.ARRIVEE_HC, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		addForFiscalPrincipal(andre, commune, dateVeuvage.getOneDayAfter(), null, MotifFor.VEUVAGE_DECES, null);

		MenageCommun menage = (MenageCommun) tiersDAO.save(new MenageCommun());
		noMenage = menage.getNumero();
		tiersService.addTiersToCouple(menage, andre, dateMariage, dateVeuvage);
		addForFiscalPrincipal(menage, commune, dateMariage, dateVeuvage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MotifFor.VEUVAGE_DECES);
	}

	@Check(id=1, descr="Vérifie que l'habitant André a un For ouvert et le For du ménage est fermé")
	public void check1() {
		{
			PersonnePhysique andre = (PersonnePhysique) tiersDAO.get(noHabAndre);
			ForFiscalPrincipal ffp = andre.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "Le For principal de l'habitant " + andre.getNumero() + " est null");
			assertEquals(dateVeuvage.getOneDayAfter(), ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
		}
		{
			MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			assertEquals(0, mc.getForsFiscauxValidAt(null).size(), "Le ménage a des Fors actifs");
			ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertEquals(dateVeuvage, ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(commune.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + commune.getNomOfficiel());
		}
	}

	@Etape(id=2, descr="Envoi de l'événement Annulation Veuvage")
	public void step2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.ANNUL_VEUVAGE, noIndAndre, dateVeuvage, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que le For du ménage commun précédent est retrouvé et les Fors d'André ont été annulés")
	public void check2() {
		{
			PersonnePhysique andre = (PersonnePhysique) tiersDAO.get(noHabAndre);
			// Vérification des fors fiscaux
			ForFiscalPrincipal ffp = andre.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'habitant " + andre.getNumero() + " null");
			assertNotNull(ffp.getDateFin(), "Le for de l'habitant " + andre.getNumero() + " est ouvert");
			for (ForFiscal forFiscal : andre.getForsFiscaux()) {
				if (forFiscal.getDateFin() == null && dateVeuvage.getOneDayAfter().equals(forFiscal.getDateDebut())) {
					assertEquals(true, forFiscal.isAnnule(), "Les fors fiscaux créés lors du veuvage doivent être annulés");
				}
			}
		}
		{
			MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertEquals(dateMariage, ffp.getDateDebut(), "Le for sur " + commune.getNomOfficiel() + " n'est pas ouvert à la bonne date");
			assertNull(ffp.getDateFin(), "Le for sur " + commune.getNomOfficiel() + " est fermé");
			assertNull(ffp.getMotifFermeture(), "Le motif de fermeture est faux");
		}
	}

}
