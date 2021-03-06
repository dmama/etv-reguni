package ch.vd.unireg.norentes.civil.annulationpermis;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.type.TypePermis;

/**
 * Scénario d'un événement annulation de permis d'une personne mariée.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_16001_03_AnnulationPermis_MarieADeux_Scenario extends AnnulationPermisNorentesScenario {

	public static final String NAME = "16001_03_AnnulationPermis";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ANNUL_CATEGORIE_ETRANGER;
	}

	@Override
	public String getDescription() {
		return "Annulation du permis C d'un habitant marié";
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
	private final RegDate dateMajorite = dateNaissanceBea.addYears(18);
	private final RegDate dateArriveeVillars = RegDate.get(1974, 3, 3);
	private final RegDate dateMariage = RegDate.get(2006, 4, 27);
	private final RegDate dateAvantMariage = dateMariage.getOneDayBefore();
	private final RegDate dateObtentionPermis =  dateMariage.addMonths(8);
	private final RegDate dateAnnulationPermis = dateObtentionPermis.addDays(12);
	private final Commune communeMariage = MockCommune.VillarsSousYens;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {

				indMomo = addIndividu(noIndMomo, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);
				indBea = addIndividu(noIndBea, dateNaissanceBea, "Duval", "Béatrice", false);

				marieIndividus(indMomo, indBea, dateMariage);

				addNationalite(indMomo, MockPays.France, RegDate.get(1963, 8, 20), null);
				addPermis(indMomo, TypePermis.ETABLISSEMENT, dateObtentionPermis, null, false);

				addOrigine(indBea, MockCommune.Lausanne);
				addNationalite(indBea, MockPays.Suisse, RegDate.get(1961, 3, 12), null);

			}
		});
	}

	@Etape(id=1, descr="Chargement d'un habitant marié, de son conjoint et du ménage commun")
	public void etape1() {
		// momo
		PersonnePhysique momo = addHabitant(noIndMomo);
		noHabMomo = momo.getNumero();
		addForFiscalPrincipal(momo, MockCommune.VillarsSousYens, dateArriveeVillars, dateAvantMariage, MotifFor.ARRIVEE_HS, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ModeImposition.SOURCE);
		// bea
		PersonnePhysique bea = addHabitant(noIndBea);
		noHabBea = bea.getNumero();
		addForFiscalPrincipal(bea, MockCommune.Lausanne, dateMajorite, dateAvantMariage, MotifFor.MAJORITE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		// ménage
		MenageCommun menage = new MenageCommun();
		menage = (MenageCommun)tiersDAO.save(menage);
		noMenage = menage.getNumero();
		tiersService.addTiersToCouple(menage, momo, dateMariage, null);
		tiersService.addTiersToCouple(menage, bea, dateMariage, null);
		addForFiscalPrincipal(menage, communeMariage, dateMariage, dateObtentionPermis.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MotifFor.PERMIS_C_SUISSE);
		addForFiscalPrincipal(menage, communeMariage, dateObtentionPermis, null, MotifFor.PERMIS_C_SUISSE, null);
	}

	@Check(id=1, descr="")
	public void check1() {
		{
			PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noHabMomo);
			ForFiscalPrincipal ffp = momo.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'habitant " + momo.getNumero() + " null");
			assertEquals(dateAvantMariage, ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertNotNull(ffp.getMotifFermeture(), "Le for principal de Maurice est encore ouvert");
		}
		{
			PersonnePhysique bea = (PersonnePhysique) tiersDAO.get(noHabBea);
			ForFiscalPrincipal ffp = bea.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'habitant " + bea.getNumero() + " null");
			assertEquals(dateAvantMariage, ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertNotNull(ffp.getMotifFermeture(), "Le for principal de Béatrice est encore ouvert");
		}
		{
			MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			assertEquals(1, mc.getForsFiscauxValidAt(null).size(), "Le ménage a plus d'un for principal");
			ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateObtentionPermis, ffp.getDateDebut(),
					"Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(communeMariage.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(),
					"Le dernier for n'est pas sur " + communeMariage.getNomOfficiel());
		}
	}

	@Etape(id=2, descr="Envoi de l'événement Annulation de Permis")
	public void etape2() throws Exception {

		{
			// annulation du permis
			searchPermis(noIndMomo, TypePermis.ETABLISSEMENT, dateAnnulationPermis).setDateAnnulation(dateAnnulationPermis);
		}

		long id = addEvenementCivil(TypeEvenementCivil.ANNUL_CATEGORIE_ETRANGER, noIndMomo, dateObtentionPermis, communeMariage.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que le ménage commun actif est le précédent et son mode d'imposition")
	public void check2() {
		{
			MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			ForFiscalPrincipalPP ffp = mc.getDernierForFiscalPrincipal();
			assertEquals(dateMariage, ffp.getDateDebut(), "Le for sur Lausanne n'est pas ouvert à la bonne date");
			assertNull(ffp.getDateFin(), "Le for sur " + communeMariage.getNomOfficiel() + " est fermé");
			assertNull(ffp.getMotifFermeture(), "Le motif de fermeture est faux");
			// le ménage doit revenir a son ancient mode d'imposition
			assertEquals(ffp.getModeImposition(), ModeImposition.ORDINAIRE, "Le mode d'imposition n'est pas ORDINAIRE");
			assertEquals(ffp.getMotifOuverture(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
					"Le motif d'ouverture n'est pas MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION");
		}
		{
			PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noHabMomo);
			ForFiscalPrincipalPP ffp = momo.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + momo.getNumero() + " null");
			assertNotNull(ffp.getDateFin(), "Le for de l'habitant " + momo.getNumero() + " est ouvert");
			assertEquals(ffp.getMotifOuverture(), MotifFor.ARRIVEE_HS,
					"Le motif de fermeture n'est pas ARRIVEE_HS");
		}
		{
			PersonnePhysique bea = (PersonnePhysique) tiersDAO.get(noHabBea);
			ForFiscalPrincipalPP ffp = bea.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + bea.getNumero() + " null");
			assertNotNull(ffp.getDateFin(), "Le for de l'habitant " + bea.getNumero() + " est ouvert");
			// bea doit passer au mode ordinaire
			assertEquals(ffp.getModeImposition(), ModeImposition.ORDINAIRE, "Le mode d'imposition n'est pas ORDINAIRE");
			assertEquals(ffp.getMotifOuverture(), MotifFor.MAJORITE,
					"Le motif de fermeture n'est pas MAJORITE");
		}
	}
}
