package ch.vd.unireg.norentes.civil.annulationpermis;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
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
 * Scénario d'un événement annulation de permis d'une personne mariée seule.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_16001_02_AnnulationPermis_MarieSeul_Scenario extends AnnulationPermisNorentesScenario {

	public static final String NAME = "16001_02_AnnulationPermis";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ANNUL_CATEGORIE_ETRANGER;
	}

	@Override
	public String getDescription() {
		return "Annulation du Permis C d'un habitant marié seul";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndJulie = 6789; // julie

	private MockIndividu indJulie;

	private long noHabJulie;
	private long noMenage;

	private final RegDate dateNaissance = RegDate.get(1977, 4, 19);
	private final RegDate dateMajorite = dateNaissance.addYears(18);
	private final RegDate dateMariage = dateMajorite.addYears(3);
	private final RegDate dateAvantMariage = dateMariage.addDays(-1);
	private final RegDate dateObtentionPermis = RegDate.get(2007, 12, 6);
	private final RegDate dateAnnulationPermis = RegDate.get(2007, 12, 21);
	private final Commune communeMariage = MockCommune.Lausanne;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				indJulie = addIndividu(noIndJulie, dateNaissance, "Goux", "Julie", false);
				marieIndividu(indJulie, dateMariage);
				addNationalite(indJulie, MockPays.Espagne, RegDate.get(1961, 3, 12), null);
				addPermis(indJulie, TypePermis.ETABLISSEMENT, dateObtentionPermis, null, false);
			}
		});
	}

	@Etape(id=1, descr="Chargement de l'habitant marié seul")
	public void etape1() {
		PersonnePhysique julie = addHabitant(noIndJulie);
		noHabJulie = julie.getNumero();
		addForFiscalPrincipal(julie, MockCommune.Lausanne, dateMajorite, dateAvantMariage, MotifFor.MAJORITE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);

		// ménage
		MenageCommun menage = new MenageCommun();
		menage = (MenageCommun)tiersDAO.save(menage);
		noMenage = menage.getNumero();
		tiersService.addTiersToCouple(menage, julie, dateMariage, null);
		addForFiscalPrincipal(menage, communeMariage, dateMariage, dateObtentionPermis.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MotifFor.PERMIS_C_SUISSE, ModeImposition.DEPENSE);
		addForFiscalPrincipal(menage, communeMariage, dateObtentionPermis, null, MotifFor.PERMIS_C_SUISSE, null);
	}

	@Check(id=1, descr="Vérifie que l'habitant Julie n'a pas de For ouvert et le For du ménage existe")
	public void check1() {
		{
			PersonnePhysique julie = (PersonnePhysique) tiersDAO.get(noHabJulie);
			ForFiscalPrincipal ffp = julie.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'habitant " + julie.getNumero() + " null");
			assertEquals(dateAvantMariage, ffp.getDateFin(), "Date de fin du dernier for fausse");
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

	@Etape(id=2, descr="Envoi de l'événement Annulation Permis")
	public void etape2() throws Exception {

		{
			// annulation du permis
			searchPermis(noIndJulie, TypePermis.ETABLISSEMENT, dateAnnulationPermis).setDateAnnulation(dateAnnulationPermis);
		}

		long id = addEvenementCivil(TypeEvenementCivil.ANNUL_CATEGORIE_ETRANGER, noIndJulie, dateObtentionPermis, communeMariage.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que le for ménage commun précédent est retrouvé")
	public void check2() {
		{
			MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			ForFiscalPrincipalPP ffp = mc.getDernierForFiscalPrincipal();
			assertEquals(dateMariage, ffp.getDateDebut(),
					"Le for sur " + communeMariage.getNomOfficiel() + " n'est pas ouvert à la bonne date");
			assertNull(ffp.getDateFin(), "Le for sur " + communeMariage.getNomOfficiel() + " est fermé");
			assertNull(ffp.getMotifFermeture(), "Le motif de fermeture est faux");
			// le ménage doit revenir au mode dépense
			assertEquals(ffp.getModeImposition(), ModeImposition.DEPENSE, "Le mode d'imposition n'est pas DEPENSE");
			assertEquals(ffp.getMotifOuverture(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
					"Le motif d'ouverture n'est pas MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION");
		}
		{
			PersonnePhysique julie = (PersonnePhysique) tiersDAO.get(noHabJulie);
			ForFiscalPrincipal ffp = julie.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'habitant " + julie.getNumero() + " null");
			assertNotNull(ffp.getDateFin(), "Le for de l'habitant " + julie.getNumero() + " est ouvert");
		}
	}
}
