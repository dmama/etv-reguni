package ch.vd.uniregctb.norentes.civil.annulation.deces;

import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPPErreur;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Scénario d'un événement d'annulation de décès d'une personne séparée.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class Ec_2001_05_AnnulationDeces_Separe_Scenario extends EvenementCivilScenario {

	private static final RegDate DATE_DECES = date(2009, 4, 27);
	private static final RegDate DATE_SEPARATION = date(2007, 6, 30);
	private static final RegDate LENDEMAIN_DATE_SEPARATION = DATE_SEPARATION.getOneDayAfter();
	private static final RegDate DATE_MARIAGE = date(2005, 10, 31);
	public static final String NAME = "2001_05_AnnulationDeces_Separe";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ANNUL_DECES;
	}

	@Override
	public String getDescription() {
		return "Scénario d'un événement d'annulation de décès d'une personne séparée.";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndGeorgette = 937931L;
	private static final long noIndJean = 312580L;

	private long noCtbJean;

	private long evenementId;

	/**
	 * @see ch.vd.uniregctb.norentes.common.EvenementCivilScenario#initServiceCivil()
	 */
	@Override
	protected void initServiceCivil() {

		serviceCivilService.setUp(new MockServiceCivil() {

			// crée un couple marié au 31/10/2005, séparé au 30/6/2007 et finalement divorcé au 9/2/2009.
			// de plus, l'homme est décédé au 27/4/2009

			@Override
			protected void init() {
				MockIndividu jean = addIndividu(noIndJean, date(1941, 6, 6), "Nzikou", "Jean", true);
				addPermis(jean, TypePermis.ETABLISSEMENT, date(2001, 7, 11), null, false);
				addAdresse(jean, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2004, 1,
						15), null);
				addEtatCivil(jean, date(2004, 1, 15), TypeEtatCivil.CELIBATAIRE);
				addEtatCivil(jean, DATE_MARIAGE, TypeEtatCivil.MARIE);
				addEtatCivil(jean, DATE_SEPARATION, TypeEtatCivil.SEPARE);
				jean.setDateDeces(DATE_DECES);

				MockIndividu georgette = addIndividu(noIndGeorgette, date(1951, 11, 3), "Matala Bambi", "Georgette", false);
				addPermis(georgette, TypePermis.ANNUEL, date(2008, 9, 8), date(2009, 6, 12), false);
				addPermis(georgette, TypePermis.ANNUEL, date(2009, 6, 13), null, false);
				addNationalite(georgette, MockPays.France, date(1951, 11, 3), null);
				addAdresse(georgette, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(
						2008, 8, 11), null);
				addEtatCivil(georgette, date(1951, 11, 3), TypeEtatCivil.CELIBATAIRE);
				addEtatCivil(georgette, DATE_MARIAGE, TypeEtatCivil.MARIE);
				addEtatCivil(georgette, DATE_SEPARATION, TypeEtatCivil.SEPARE);
			}

		});

	}

	@Etape(id = 1, descr = "Création du ménage commun Jean & Georgette dans le registre fiscal")
	public void etape1() throws Exception {

		final PersonnePhysique jean = addHabitant(noIndJean);
		addForFiscalPrincipal(jean, MockCommune.Lausanne, LENDEMAIN_DATE_SEPARATION, DATE_DECES, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MotifFor.VEUVAGE_DECES);

		final PersonnePhysique georgette = addHabitant(noIndGeorgette);
		addForFiscalPrincipal(georgette, MockCommune.Lausanne, LENDEMAIN_DATE_SEPARATION, null, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null);

		final EnsembleTiersCouple ensemble = tiersService.createEnsembleTiersCouple(jean, georgette, DATE_MARIAGE, DATE_SEPARATION);
		final MenageCommun menage = ensemble.getMenage();
		addForFiscalPrincipal(menage, MockCommune.Lausanne, DATE_MARIAGE, DATE_SEPARATION, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);

		noCtbJean = jean.getNumero();
	}

	@Check(id = 1, descr = "Vérification que tout est là")
	public void check1() throws Exception {
		// rien à vérifier
	}

	@Etape(id = 2, descr = "Envoi de l'événement d'arrivée HC de Georgette en tant que mariée seule")
	public void etape2() throws Exception {

		evenementId = addEvenementCivil(TypeEvenementCivil.ANNUL_DECES, noIndJean, DATE_DECES, MockCommune.Lausanne.getNoOFS());
		commitAndStartTransaction();

		// On traite le nouvel événement
		traiteEvenements(evenementId);
	}

	@Check(id = 2, descr = "Vérification que l'événement est bien traité et que le for fiscal principal de Jean a été réouvert")
	public void check2() throws Exception {

		final EvenementCivilRegPP evenement = evtExterneDAO.get(evenementId);
		assertEquals(EtatEvenementCivil.TRAITE, evenement.getEtat(), "L'événement civil devrait être traité.");

		final Set<EvenementCivilRegPPErreur> erreurs = evenement.getErreurs();
		assertEquals(0, erreurs.size(), "Il ne devrait pas y avoir d'erreur");

		final PersonnePhysique jean = (PersonnePhysique) tiersDAO.get(noCtbJean);
		final ForFiscalPrincipal ffp = jean.getForFiscalPrincipalAt(LENDEMAIN_DATE_SEPARATION);
		assertNull(ffp.getDateFin(), "Le for fiscal principal devrait être ouvert");
		assertNull(ffp.getMotifFermeture(), "Le motif de fermeture du for fiscal principal devrait être nul");
	}
}
