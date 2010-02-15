package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.DateRange;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.interfaces.model.mock.MockOfficeImpot;
import ch.vd.uniregctb.type.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.DateRangeHelper.Range;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Classe de base pour les classes de test de l'assujettissement et des périodes d'imposition.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"JavaDoc"})
public abstract class MetierTest extends BusinessTest {

	protected static final Range RANGE_1999_2010 = new Range(date(1999, 1, 1), date(2010, 12, 31));
	protected static final Range RANGE_2002_2010 = new Range(date(2002, 1, 1), date(2010, 12, 31));
	protected static final Range RANGE_2000_2008 = new Range(date(2000, 1, 1), date(2008, 12, 31));
	protected static final Range RANGE_2006_2008 = new Range(date(2006, 1, 1), date(2008, 12, 31));

	/**
	 * Diplomate suisse ayant vécu à Lausanne jusqu'à sa nomination comme diplomate en Albanie le 1er janvier 2000.
	 */
	protected Contribuable createDiplomateSuisse(RegDate dateNomination) throws Exception {
		// on crée un non-habitant par commodité, mais cela devrait être un habitant normalement
		Contribuable paul = createContribuableSansFor();
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.MAJORITE, dateNomination.getOneDayBefore(), MotifFor.DEPART_HS, MockCommune.Lausanne);
		// le for principal d'un diplomate suisse reste en suisse (= sa commune d'origine) malgré le fait qu'il soit basé à l'étranger
		addForPrincipal(paul, dateNomination, MotifFor.DEPART_HS, null, null, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DIPLOMATE_SUISSE);
		return paul;
	}


	/**
	 * Diplomate suisse ayant vécu à Lausanne jusqu'à sa nomination comme diplomate en Albanie le 1er janvier 2000. Possède un immeuble à Cossonay depuis le 13 juin 2001.
	 */
	protected Contribuable createDiplomateAvecImmeuble(RegDate dateNomination, RegDate achatImmeuble) throws Exception {
		// on crée un non-habitant par commodité, mais cela devrait être un habitant normalement
		Contribuable paul = createContribuableSansFor();
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.MAJORITE, dateNomination.getOneDayBefore(), MotifFor.DEPART_HS, MockCommune.Lausanne);
		// le for principal d'un diplomate suisse reste en suisse (= sa commune d'origine) malgré le fait qu'il soit basé à l'étranger
		addForPrincipal(paul, dateNomination, MotifFor.DEPART_HS, null, null, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DIPLOMATE_SUISSE);
		addForSecondaire(paul, achatImmeuble, MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createContribuableSansFor() throws Exception {
		return addNonHabitant("Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
	}

	protected Contribuable createUnForSimple() throws Exception {
		Contribuable paul = createContribuableSansFor();
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		return paul;
	}

	protected EnsembleTiersCouple createMenageCommunMarieDansLAnnee() {
		PersonnePhysique paul = addNonHabitant("Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, date(1981, 4, 13), MotifFor.MAJORITE, date(2008, 6, 30),
				MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

		PersonnePhysique marie = addNonHabitant("Marie", "Duchêne", date(1970, 6, 3), Sexe.FEMININ);
		addForPrincipal(marie, date(1988, 6, 3), MotifFor.MAJORITE, date(2008, 6, 30),
				MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Vevey);

		EnsembleTiersCouple ensemble = addEnsembleTiersCouple(paul, marie, date(2008, 7, 1));
		addForPrincipal(ensemble.getMenage(), date(2008, 7, 1), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
				MockCommune.Lausanne);

		return ensemble;
	}

	protected Contribuable createDepartHorsCanton(RegDate dateDepart) throws Exception {
		Contribuable paul = createContribuableSansFor();
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HC, MockCommune.Lausanne);
		return paul;
	}

	protected Contribuable createDepartHorsCantonAvecImmeuble(RegDate dateDepart) throws Exception {
		Contribuable paul = createContribuableSansFor();
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HC, MockCommune.Lausanne);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Neuchatel);
		addForSecondaire(paul, date(2000, 7, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(),
				MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createDepartHorsCantonEtVenteImmeuble(RegDate dateDepart, RegDate dateVente) throws Exception {
		Contribuable paul = createContribuableSansFor();
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HC, MockCommune.Lausanne);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HC, dateVente, null, MockCommune.Neuchatel);
		addForSecondaire(paul, date(2000, 7, 1), MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createDepartHorsCantonSourcierPur(RegDate dateDepart) throws Exception {
		Contribuable paul = createContribuableSansFor();
		ForFiscalPrincipal ffp = addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HC, MockCommune.Lausanne);
		ffp.setModeImposition(ModeImposition.SOURCE);
		ffp = addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Neuchatel);
		ffp.setModeImposition(ModeImposition.SOURCE);
		return paul;
	}

	protected Contribuable createDepartHorsCantonSourcierMixte137Al1(RegDate dateDepart) throws Exception {
		Contribuable paul = createContribuableSansFor();
		ForFiscalPrincipal ffp = addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HC, MockCommune.Lausanne);
		ffp.setModeImposition(ModeImposition.MIXTE_137_1);
		ffp = addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Neuchatel);
		ffp.setModeImposition(ModeImposition.MIXTE_137_1);
		addForSecondaire(paul, date(2002, 7, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.LesClees.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createDepartHorsCantonSourcierMixte137Al2(RegDate dateDepart) throws Exception {
		Contribuable paul = createContribuableSansFor();
		ForFiscalPrincipal ffp = addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HC, MockCommune.Lausanne);
		ffp.setModeImposition(ModeImposition.MIXTE_137_2);
		ffp = addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Neuchatel);
		ffp.setModeImposition(ModeImposition.SOURCE);  // un hors-canton ne peut pas être mixte 137 al. 2
		return paul;
	}

	protected Contribuable createDepartHorsSuisse(RegDate dateDepart) throws Exception {
		Contribuable paul = createContribuableSansFor();
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.PaysInconnu);
		return paul;
	}

	protected Contribuable createDepartHorsSuisseAvecImmeuble(RegDate dateDepart) throws Exception {
		Contribuable paul = createContribuableSansFor();
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.Espagne);
		addForSecondaire(paul, date(2000, 7, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createDepartHorsSuisseDepuisHorsCantonAvecImmeuble(RegDate dateDepart) throws Exception {
		Contribuable paul = createContribuableSansFor();
		addForPrincipal(paul, date(1983, 4, 13), null, dateDepart, MotifFor.DEPART_HS, MockCommune.Neuchatel);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.Espagne);
		addForSecondaire(paul, date(2000, 7, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createDepartHorsSuisseDepuisHorsCantonAvecActiviteIndependante(RegDate dateDepart) throws Exception {
		Contribuable paul = createContribuableSansFor();
		addForPrincipal(paul, date(1983, 4, 13), null, dateDepart, MotifFor.DEPART_HS, MockCommune.Neuchatel);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.Espagne);
		addForSecondaire(paul, date(2000, 7, 1), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		return paul;
	}

	protected Contribuable createHorsSuisseAvecAchatEtVenteImmeuble(RegDate dateAchat, RegDate dateVente) throws Exception {
		Contribuable paul = createContribuableSansFor();
		addForPrincipal(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockPays.Espagne);
		addForSecondaire(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Lausanne.getNoOFS(),
				MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	/**
	 * Crée un contribuable hors-Suisse avec <i>n</i> immeubles dont les dates d'achat et de ventes correspondent aux périodes spécifiées.
	 *
	 * @param periodes les périodes d'achat et de vente des immeubles.
	 * @return un contribuable avec plusieurs immeubles
	 */
	protected Contribuable createHorsSuisseAvecAchatsEtVentesImmeubles(DateRange... periodes) throws Exception {
		Contribuable paul = createContribuableSansFor();
		addForPrincipal(paul, date(1980, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockPays.Espagne);
		for (DateRange periode : periodes) {
			addForSecondaire(paul, periode.getDateDebut(), MotifFor.ACHAT_IMMOBILIER, periode.getDateFin(), MotifFor.VENTE_IMMOBILIER, MockCommune.Lausanne.getNoOFS(),
					MotifRattachement.IMMEUBLE_PRIVE);
		}
		return paul;
	}

	/**
	 * Crée un contribuable hors-Suisse sourcier avec un immeuble acheté et vendu aux dates spécifiées.
	 */
	protected Contribuable createHorsSuisseSourcierAvecAchatEtVenteImmeuble(RegDate dateAchat, RegDate dateVente) throws Exception {
		Contribuable paul = createContribuableSansFor();

		ForFiscalPrincipal ffp = addForPrincipal(paul, date(2000, 1, 1), null, dateAchat.getOneDayBefore(), MotifFor.ACHAT_IMMOBILIER, MockPays.Espagne);
		ffp.setModeImposition(ModeImposition.SOURCE);

		ffp = addForPrincipal(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockPays.Espagne);
		ffp.setModeImposition(ModeImposition.MIXTE_137_1); // TODO (msi) corriger le mode d'imposition en mixte 146 quand la spéc sera validée + ajout les fors AutreElementImposable.

		ffp = addForPrincipal(paul, dateVente.getOneDayAfter(), MotifFor.VENTE_IMMOBILIER, MockPays.Espagne);
		ffp.setModeImposition(ModeImposition.SOURCE);
		
		addForSecondaire(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createHorsSuisseVenteImmeubleEtFermetureFFPSansMotif(final RegDate dateVente) throws Exception {
		Contribuable ctb = createContribuableSansFor();
		addForPrincipal(ctb, date(1976, 1, 7), MotifFor.ARRIVEE_HS, date(1997, 3, 7), MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(ctb, date(1997, 3, 8), MotifFor.DEPART_HS, dateVente, null, MockPays.France);
		addForSecondaire(ctb, date(2007, 3, 9), MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Lausanne
				.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return ctb;
	}

	protected Contribuable createArriveeHorsSuisseAvecImmeuble(RegDate dateArrivee) throws Exception {
		Contribuable paul = createContribuableSansFor();
		addForPrincipal(paul, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Danemark);
		addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
		addForSecondaire(paul, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	/**
	 * Cas réel du contribuable n°16109718. Il s'agit d'une arrivée de hors-Suisse normale, mais avec un motif de fermeture du for principal 'Déménagement'.
	 */
	protected Contribuable createArriveeHorsSuisseAvecImmeubleEtMotifDemanagement(RegDate dateAchat, RegDate dateArrivee) throws Exception {
		Contribuable ctb = createContribuableSansFor();
		addForPrincipal(ctb, dateAchat, MotifFor.INDETERMINE, dateArrivee.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockPays.PaysInconnu);
		addForPrincipal(ctb, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Leysin);
		addForSecondaire(ctb, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return ctb;
	}

	protected Contribuable createArriveeHorsSuisseEtVenteImmeuble(RegDate dateArrivee, RegDate dateVente) throws Exception {
		Contribuable paul = createContribuableSansFor();
		addForPrincipal(paul, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HS,
				MockPays.Danemark);
		addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
		addForSecondaire(paul, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Lausanne
				.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createArriveeHSAchatImmeubleEtDepartHS(RegDate dateArrivee, RegDate dateAchat, RegDate dateDepart)
			throws Exception {
		Contribuable paul = createContribuableSansFor();
		addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HS, dateDepart, MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.France);
		addForSecondaire(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createArriveeHSDepartHSPuisAchatImmeuble(RegDate dateArrivee, RegDate dateDepart, RegDate dateAchat)
			throws Exception {
		Contribuable paul = createContribuableSansFor();
		addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HS, dateDepart, MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.France);
		addForSecondaire(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createArriveeHorsCantonSourcierPur(RegDate dateArrivee) throws Exception {
		Contribuable paul = createContribuableSansFor();
		ForFiscalPrincipal ffp = addForPrincipal(paul, date(1983, 4, 13), null, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Neuchatel);
		ffp.setModeImposition(ModeImposition.SOURCE);
		ffp = addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		ffp.setModeImposition(ModeImposition.SOURCE);
		return paul;
	}

	protected Contribuable createArriveeHorsCantonSourcierMixte137Al1(RegDate dateArrivee) throws Exception {
		Contribuable paul = createContribuableSansFor();
		ForFiscalPrincipal ffp = addForPrincipal(paul, date(2002, 7, 1), MotifFor.ACHAT_IMMOBILIER, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Neuchatel);
		ffp.setModeImposition(ModeImposition.MIXTE_137_1);
		ffp = addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		ffp.setModeImposition(ModeImposition.MIXTE_137_1);
		addForSecondaire(paul, date(2002, 7, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.LesClees.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createArriveeHorsCantonSourcierMixte137Al2(RegDate dateArrivee) throws Exception {
		Contribuable paul = createContribuableSansFor();
		ForFiscalPrincipal ffp = addForPrincipal(paul, date(1983, 4, 13), null, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Neuchatel);
		ffp.setModeImposition(ModeImposition.SOURCE);  // un hors-canton ne peut pas être mixte 137 al. 2
		ffp = addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		ffp.setModeImposition(ModeImposition.MIXTE_137_2);
		return paul;
	}

	protected Contribuable createArriveeEtDepartHorsSuisse(RegDate dateArrivee, RegDate dateDepart) throws Exception {
		Contribuable paul = createContribuableSansFor();
		addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HS, dateDepart, MotifFor.DEPART_HS, MockCommune.Lausanne);
		return paul;
	}

	protected Contribuable createDepartEtArriveeHorsSuisseDansLAnnee(RegDate dateDepart, RegDate dateArrivee) throws Exception {
		Contribuable paul = createContribuableSansFor();
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.MAJORITE, dateDepart, MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
		return paul;
	}

	protected Contribuable createSourcierPur() throws Exception {
		Contribuable paul = createContribuableSansFor();
		addForPrincipal(paul, date(2001, 1, 1), MotifFor.ARRIVEE_HS, date(2002, 2, 21), MotifFor.DEMENAGEMENT_VD, MockCommune.Renens).setModeImposition(ModeImposition.SOURCE);
		addForPrincipal(paul, date(2002, 2, 22), MotifFor.DEMENAGEMENT_VD, date(2002, 9, 26), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Vevey).setModeImposition(ModeImposition.SOURCE);
		addForPrincipal(paul, date(2003, 10, 24), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, date(2008, 6, 30), MotifFor.DEMENAGEMENT_VD, MockCommune.Vevey).setModeImposition(ModeImposition.SOURCE);
		addForPrincipal(paul, date(2008, 7, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne).setModeImposition(ModeImposition.SOURCE);
		
		return paul;
	}

	protected Contribuable createSourcierPureHorsSuisse() throws Exception {
		Contribuable paul = createContribuableSansFor();
		ForFiscalPrincipal fp = addForPrincipal(paul, date(1993, 5, 1), MotifFor.DEBUT_EXPLOITATION, MockPays.Albanie);
		fp.setModeImposition(ModeImposition.SOURCE);
		return paul;
	}

	protected Contribuable createSourcierPureHorsCanton() throws Exception {
		Contribuable paul = createContribuableSansFor();
		ForFiscalPrincipal fp = addForPrincipal(paul, date(1993, 5, 1), MotifFor.DEBUT_EXPLOITATION, MockCommune.Neuchatel);
		fp.setModeImposition(ModeImposition.SOURCE);
		return paul;
	}

	/**
	 * Contribuable sourcier hors-canton avec un immeuble dans le canton => sourcier mixte art. 137 al. 1
	 */
	protected Contribuable createSourcierMixte137Al1HorsCanton() throws Exception {
		Contribuable paul = createContribuableSansFor();
		ForFiscalPrincipal fp = addForPrincipal(paul, date(1993, 5, 1), null, date(2007, 6, 30), MotifFor.CHGT_MODE_IMPOSITION,
				MockCommune.Neuchatel);
		fp.setModeImposition(ModeImposition.SOURCE);
		fp = addForPrincipal(paul, date(2007, 7, 1), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Neuchatel);
		fp.setModeImposition(ModeImposition.MIXTE_137_1);
		addForSecondaire(paul, date(2007, 7, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(),
				MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	/**
	 * TODO (msi) corriger cela (mixte 149) en fonction de la nouvelle spéc. de Thierry en cours de validation
	 * Contribuable sourcier hors-Suisse avec un immeuble dans le canton => sourcier mixte art. 137 al. 1
	 */
	protected Contribuable createSourcierMixte137Al1HorsSuisse(RegDate dateAchat) throws Exception {
		Contribuable paul = createContribuableSansFor();
		ForFiscalPrincipal fp = addForPrincipal(paul, date(1993, 5, 1), null, date(2007, 6, 30), MotifFor.CHGT_MODE_IMPOSITION, MockPays.France);
		fp.setModeImposition(ModeImposition.SOURCE);
		fp = addForPrincipal(paul, dateAchat, MotifFor.CHGT_MODE_IMPOSITION, MockPays.France);
		fp.setModeImposition(ModeImposition.MIXTE_137_1);
		addForSecondaire(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	/**
	 * Contribuable sourcier dans le canton avec un mode d'imposition mixte (gagnant plus de 120'000 francs) => sourcier mixte art. 137 al. 2
	 */
	protected Contribuable createSourcierMixte137Al2() throws Exception {
		Contribuable paul = createContribuableSansFor();
		ForFiscalPrincipal fp = addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2004, 12, 31),
				MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne);
		fp.setModeImposition(ModeImposition.SOURCE);
		fp = addForPrincipal(paul, date(2005, 1, 1), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne);
		fp.setModeImposition(ModeImposition.MIXTE_137_2);
		return paul;
	}

	/**
	 * Sourcier pure vaudois qui passe en mode ordinaire en 2008
	 */
	protected Contribuable createPassageRoleSourceAOrdinaire(RegDate dateChangement) throws Exception {
		Contribuable paul = createContribuableSansFor();
		ForFiscalPrincipal fp = addForPrincipal(paul, date(1993, 5, 1), MotifFor.ARRIVEE_HC, dateChangement.getOneDayBefore(), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne);
		fp.setModeImposition(ModeImposition.SOURCE);
		fp = addForPrincipal(paul, dateChangement, MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne);
		fp.setModeImposition(ModeImposition.ORDINAIRE);
		return paul;
	}

	/**
	 * Hors-canton avec immeuble qui décède à la date spécifiée.
	 */
	protected Contribuable createDecesHorsCantonAvecImmeuble(RegDate dateAchat, RegDate dateDeces) throws Exception {
		Contribuable paul = createContribuableSansFor();
		addForPrincipal(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Neuchatel);
		addForSecondaire(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Leysin.getNoOFS(),
				MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	/**
	 * Hors-Suisse avec immeuble qui décède à la date spécifiée.
	 */
	protected Contribuable createDecesHorsSuisseAvecImmeuble(RegDate dateAchat, RegDate dateDeces) throws Exception {
		Contribuable paul = createContribuableSansFor();
		addForPrincipal(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateDeces, MotifFor.VEUVAGE_DECES, MockPays.Danemark);
		addForSecondaire(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	/**
	 * Hors-canton avec activité indépendante qui décède à la date spécifiée.
	 */
	protected Contribuable createDecesHorsCantonActiviteIndependante(RegDate debutExploitation, RegDate dateDeces) throws Exception {
		Contribuable paul = createContribuableSansFor();
		addForPrincipal(paul, debutExploitation, MotifFor.DEBUT_EXPLOITATION, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Neuchatel);
		addForSecondaire(paul, debutExploitation, MotifFor.DEBUT_EXPLOITATION, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Leysin.getNoOFS(),
				MotifRattachement.ACTIVITE_INDEPENDANTE);
		return paul;
	}

	protected Contribuable createDecesVaudoisOrdinaire(RegDate dateMajorite, RegDate dateDeces) throws Exception {
		Contribuable paul = createContribuableSansFor();
		addForPrincipal(paul, dateMajorite, MotifFor.MAJORITE, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
		return paul;
	}

	protected Contribuable createDecesVaudoisDepense(RegDate dateArrivee, RegDate dateDeces) throws Exception {
		Contribuable paul = createContribuableSansFor();
		ForFiscalPrincipal ffp = addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HS, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
		ffp.setModeImposition(ModeImposition.DEPENSE);
		return paul;
	}

	protected Contribuable createHorsCantonAvecImmeuble(RegDate dateAchat) throws Exception {
		Contribuable paul = createContribuableSansFor();
		addForPrincipal(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Neuchatel);
		addForSecondaire(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createVenteImmeubleHorsCanton(RegDate dateVente) throws Exception {
		Contribuable ctb = createContribuableSansFor();
		addForPrincipal(ctb, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Neuchatel);
		addForSecondaire(ctb, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Aubonne.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
		return ctb;
	}

	protected Contribuable createFinActiviteHorsCanton(RegDate dateFin) throws Exception {
		Contribuable ctb = createContribuableSansFor();
		addForPrincipal(ctb, date(2000, 1, 1), MotifFor.DEBUT_EXPLOITATION, MockCommune.Neuchatel);
		addForSecondaire(ctb, date(2000, 1, 1), MotifFor.DEBUT_EXPLOITATION, dateFin, MotifFor.FIN_EXPLOITATION, MockCommune.Aubonne.getNoOFSEtendu(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		return ctb;
	}

	protected Contribuable createIndigentAvecDIs(int periodePrecedente, TypeDocument typeDIsPrecedentes) {

		final PeriodeFiscale periode = addPeriodeFiscale(periodePrecedente);
		final ModeleDocument declarationVaudTax = addModeleDocument(typeDIsPrecedentes, periode);

		final Contribuable ctb = addNonHabitant("Marc", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
		ctb.setOfficeImpotId(MockOfficeImpot.OID_LAUSANNE_VILLE.getNoColAdm());
		final ForFiscalPrincipal ffp = addForPrincipal(ctb, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
		ffp.setModeImposition(ModeImposition.INDIGENT);

		final DeclarationImpotOrdinaire decl =
				addDeclarationImpot(ctb, periode, date(periodePrecedente, 1, 1), date(periodePrecedente, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, declarationVaudTax);
		addEtatDeclaration(decl, date(periodePrecedente + 1, 1, 15), TypeEtatDeclaration.EMISE);
		addEtatDeclaration(decl, date(periodePrecedente + 1, 1, 15), TypeEtatDeclaration.RETOURNEE);
		return ctb;
	}

	protected void assertOrdinaire(RegDate debut, RegDate fin, MotifFor motifFractDebut, MotifFor motifFractFin,
			Assujettissement assujettissement) {
		assertNotNull(assujettissement);
		assertInstanceOf(VaudoisOrdinaire.class, assujettissement);
		assertEquals(debut, assujettissement.getDateDebut());
		assertEquals(fin, assujettissement.getDateFin());
		assertEquals(motifFractDebut, assujettissement.getMotifFractDebut());
		assertEquals(motifFractFin, assujettissement.getMotifFractFin());
	}

	protected void assertDiplomateSuisse(RegDate debut, RegDate fin, MotifFor motifFractDebut, MotifFor motifFractFin,
			Assujettissement assujettissement) {
		assertNotNull(assujettissement);
		assertInstanceOf(DiplomateSuisse.class, assujettissement);
		assertEquals(debut, assujettissement.getDateDebut());
		assertEquals(fin, assujettissement.getDateFin());
		assertEquals(motifFractDebut, assujettissement.getMotifFractDebut());
		assertEquals(motifFractFin, assujettissement.getMotifFractFin());
	}

	protected void assertHorsCanton(RegDate debut, RegDate fin, MotifFor motifFractDebut, MotifFor motifFractFin,
			Assujettissement assujettissement) {
		assertNotNull(assujettissement);
		assertInstanceOf(HorsCanton.class, assujettissement);
		assertEquals(debut, assujettissement.getDateDebut());
		assertEquals(fin, assujettissement.getDateFin());
		assertEquals(motifFractDebut, assujettissement.getMotifFractDebut());
		assertEquals(motifFractFin, assujettissement.getMotifFractFin());
	}

	protected void assertHorsSuisse(RegDate debut, RegDate fin, MotifFor motifFractDebut, MotifFor motifFractFin,
			Assujettissement assujettissement) {
		assertNotNull(assujettissement);
		assertInstanceOf(HorsSuisse.class, assujettissement);
		assertEquals(debut, assujettissement.getDateDebut());
		assertEquals(fin, assujettissement.getDateFin());
		assertEquals(motifFractDebut, assujettissement.getMotifFractDebut());
		assertEquals(motifFractFin, assujettissement.getMotifFractFin());
	}

	protected void assertSourcierPur(RegDate debut, RegDate fin, MotifFor motifFractDebut, MotifFor motifFractFin, TypeAutoriteFiscale typeAutorite, Assujettissement assujettissement) {
		assertNotNull(assujettissement);
		assertInstanceOf(SourcierPur.class, assujettissement);
		final SourcierPur a = (SourcierPur) assujettissement;
		assertEquals(debut, assujettissement.getDateDebut());
		assertEquals(fin, assujettissement.getDateFin());
		assertEquals(motifFractDebut, assujettissement.getMotifFractDebut());
		assertEquals(motifFractFin, assujettissement.getMotifFractFin());
		assertEquals(typeAutorite, a.getTypeAutoriteFiscale());
	}

	protected void assertSourcierMixte(RegDate debut, RegDate fin, MotifFor motifFractDebut, MotifFor motifFractFin, TypeAutoriteFiscale typeAutorite, Assujettissement assujettissement) {
		assertNotNull(assujettissement);
		assertInstanceOf(SourcierMixte.class, assujettissement);
		final SourcierMixte a = (SourcierMixte) assujettissement;
		assertEquals(debut, assujettissement.getDateDebut());
		assertEquals(fin, assujettissement.getDateFin());
		assertEquals(motifFractDebut, assujettissement.getMotifFractDebut());
		assertEquals(motifFractFin, assujettissement.getMotifFractFin());
		assertEquals(typeAutorite, a.getTypeAutoriteFiscale());
	}

	protected void assertPeriodeImposition(RegDate debut, RegDate fin, TypeContribuableDI type, TypeAdresseRetour adresseRetour, boolean optionnelle, boolean remplaceParNote,
	                                       PeriodeImposition periode) {
		assertNotNull(periode);
		assertEquals(debut, periode.getDateDebut());
		assertEquals(fin, periode.getDateFin());
		assertEquals(type, periode.getTypeContribuableDI());
		assertEquals(adresseRetour, periode.getAdresseRetour());
		assertEquals(optionnelle, periode.isOptionnelle());
		assertEquals(remplaceParNote, periode.isRemplaceeParNote());
	}
}
