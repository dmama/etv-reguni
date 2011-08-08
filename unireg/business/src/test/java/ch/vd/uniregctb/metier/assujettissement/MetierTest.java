package ch.vd.uniregctb.metier.assujettissement;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper.Range;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockOfficeImpot;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Classe de base pour les classes de test de l'assujettissement et des périodes d'imposition.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"JavaDoc", "deprecation"})
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
		addForPrincipal(paul, dateNomination, MotifFor.DEPART_HS, MockCommune.Lausanne, MotifRattachement.DIPLOMATE_SUISSE);
		return paul;
	}


	protected Contribuable createDiplomateAvecImmeuble(@Nullable Long noTiers, RegDate dateNomination, RegDate achatImmeuble) throws Exception {
		// on crée un non-habitant par commodité, mais cela devrait être un habitant normalement
		Contribuable paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.MAJORITE, dateNomination.getOneDayBefore(), MotifFor.DEPART_HS, MockCommune.Lausanne);
		// le for principal d'un diplomate suisse reste en suisse (= sa commune d'origine) malgré le fait qu'il soit basé à l'étranger
		addForPrincipal(paul, dateNomination, MotifFor.DEPART_HS, MockCommune.Lausanne, MotifRattachement.DIPLOMATE_SUISSE);
		addForSecondaire(paul, achatImmeuble, MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createContribuableSansFor() throws Exception {
		return createContribuableSansFor(null);
	}

	protected Contribuable createContribuableSansFor(@Nullable Long noTiers) throws Exception {
		return addNonHabitant(noTiers, "Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
	}

	protected Contribuable createUnForSimple() throws Exception {
		return createUnForSimple(null);
	}

	protected Contribuable createUnForSimple(@Nullable Long noTiers) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		return paul;
	}

	protected EnsembleTiersCouple createMenageCommunMarie(Long noPrincipal, Long noConjoint, Long noMenage, RegDate dateMariage) {

		PersonnePhysique paul = addNonHabitant(noPrincipal, "Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, date(1981, 4, 13), MotifFor.MAJORITE, dateMariage.getOneDayBefore(),
				MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

		PersonnePhysique marie = addNonHabitant(noConjoint, "Marie", "Duchêne", date(1970, 6, 3), Sexe.FEMININ);
		addForPrincipal(marie, date(1988, 6, 3), MotifFor.MAJORITE, dateMariage.getOneDayBefore(),
				MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Vevey);

		EnsembleTiersCouple ensemble = addEnsembleTiersCouple(noMenage, paul, marie, dateMariage, null);
		addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
				MockCommune.Lausanne);

		return ensemble;
	}

	protected EnsembleTiersCouple createMenageCommunDivorce(Long noPrincipal, Long noConjoint, Long noMenage, RegDate dateMariage, RegDate dateDivorce) {

		PersonnePhysique paul = addNonHabitant(noPrincipal, "Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, date(1981, 4, 13), MotifFor.MAJORITE, dateMariage.getOneDayBefore(),
				MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
		addForPrincipal(paul, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);

		PersonnePhysique marie = addNonHabitant(noConjoint, "Marie", "Duchêne", date(1970, 6, 3), Sexe.FEMININ);
		addForPrincipal(marie, date(1988, 6, 3), MotifFor.MAJORITE, dateMariage.getOneDayBefore(),
				MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Vevey);
		addForPrincipal(marie, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Vevey);

		EnsembleTiersCouple ensemble = addEnsembleTiersCouple(noMenage, paul, marie, dateMariage, dateDivorce.getOneDayBefore());
		addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDivorce.getOneDayBefore(),
				MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);

		return ensemble;
	}

	protected EnsembleTiersCouple createMenageCommunMariageVDImmeuble(Long noPrincipal, Long noConjoint, Long noMenage, RegDate dateMariage) {

		final PersonnePhysique paul = addNonHabitant(noPrincipal, "Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, date(1981, 4, 13), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
		addForSecondaire(paul, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Fraction.LAbbaye.getNoOFSEtendu(),
				MotifRattachement.IMMEUBLE_PRIVE);

		final PersonnePhysique marie = addNonHabitant(noConjoint, "Marie", "Duchêne", date(1970, 6, 3), Sexe.FEMININ);
		addForPrincipal(marie, date(1988, 6, 3), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

		final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(noMenage, paul, marie, dateMariage, null);
		addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
		addForSecondaire(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Fraction.LAbbaye.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

		return ensemble;
	}

	protected EnsembleTiersCouple createMenageCommunDivorceVDImmeuble(Long noPrincipal, Long noConjoint, Long noMenage, RegDate dateMariage, RegDate dateDivorce) {

		final PersonnePhysique paul = addNonHabitant(noPrincipal, "Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);
		addForSecondaire(paul, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Fraction.LAbbaye.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

		final PersonnePhysique marie = addNonHabitant(noConjoint, "Marie", "Duchêne", date(1970, 6, 3), Sexe.FEMININ);
		addForPrincipal(marie, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);

		final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(noMenage, paul, marie, dateMariage, dateDivorce.getOneDayBefore());
		addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDivorce.getOneDayBefore(),
				MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);
		addForSecondaire(ensemble.getMenage(), dateMariage, MotifFor.ACHAT_IMMOBILIER, dateDivorce.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
				MockCommune.Fraction.LAbbaye.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

		return ensemble;
	}
	
	protected EnsembleTiersCouple createMenageCommunMariageHCImmeuble(Long noPrincipal, Long noConjoint, Long noMenage, RegDate dateMariage) {

		final PersonnePhysique paul = addNonHabitant(noPrincipal, "Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, date(1981, 4, 13), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Neuchatel);
		addForSecondaire(paul, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Fraction.LAbbaye.getNoOFSEtendu(), 
				MotifRattachement.IMMEUBLE_PRIVE);

		final PersonnePhysique marie = addNonHabitant(noConjoint, "Marie", "Duchêne", date(1970, 6, 3), Sexe.FEMININ);
		addForPrincipal(marie, date(1988, 6, 3), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Neuchatel);

		final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(noMenage, paul, marie, dateMariage, null);
		addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Neuchatel);
		addForSecondaire(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Fraction.LAbbaye.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

		return ensemble;
	}

	protected EnsembleTiersCouple createMenageCommunDivorceHCImmeuble(Long noPrincipal, Long noConjoint, Long noMenage, RegDate dateMariage, RegDate dateDivorce) {

		final PersonnePhysique paul = addNonHabitant(noPrincipal, "Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Neuchatel);
		addForSecondaire(paul, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Fraction.LAbbaye.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

		final PersonnePhysique marie = addNonHabitant(noConjoint, "Marie", "Duchêne", date(1970, 6, 3), Sexe.FEMININ);
		addForPrincipal(marie, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Neuchatel);

		final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(noMenage, paul, marie, dateMariage, dateDivorce.getOneDayBefore());
		addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDivorce.getOneDayBefore(),
				MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Neuchatel);
		addForSecondaire(ensemble.getMenage(), dateMariage, MotifFor.ACHAT_IMMOBILIER, dateDivorce.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
				MockCommune.Fraction.LAbbaye.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

		return ensemble;
	}

	protected EnsembleTiersCouple createMenageCommunMariageHSImmeuble(Long noPrincipal, Long noConjoint, Long noMenage, RegDate dateMariage) {

		final PersonnePhysique paul = addNonHabitant(noPrincipal, "Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, date(1981, 4, 13), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockPays.Allemagne);
		addForSecondaire(paul, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Fraction.LAbbaye.getNoOFSEtendu(),
				MotifRattachement.IMMEUBLE_PRIVE);

		final PersonnePhysique marie = addNonHabitant(noConjoint, "Marie", "Duchêne", date(1970, 6, 3), Sexe.FEMININ);
		addForPrincipal(marie, date(1988, 6, 3), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockPays.Allemagne);

		final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(noMenage, paul, marie, dateMariage, null);
		addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockPays.Allemagne);
		addForSecondaire(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Fraction.LAbbaye.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

		return ensemble;
	}

	protected EnsembleTiersCouple createMenageCommunDivorceHSImmeuble(Long noPrincipal, Long noConjoint, Long noMenage, RegDate dateMariage, RegDate dateDivorce) {

		final PersonnePhysique paul = addNonHabitant(noPrincipal, "Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockPays.Allemagne);
		addForSecondaire(paul, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Fraction.LAbbaye.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

		final PersonnePhysique marie = addNonHabitant(noConjoint, "Marie", "Duchêne", date(1970, 6, 3), Sexe.FEMININ);
		addForPrincipal(marie, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockPays.Allemagne);

		final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(noMenage, paul, marie, dateMariage, dateDivorce.getOneDayBefore());
		addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDivorce.getOneDayBefore(),
				MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockPays.Allemagne);
		addForSecondaire(ensemble.getMenage(), dateMariage, MotifFor.ACHAT_IMMOBILIER, dateDivorce.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
				MockCommune.Fraction.LAbbaye.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

		return ensemble;
	}

	protected Contribuable createDepartHorsCanton(Long noTiers, RegDate dateDepart) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HC, MockCommune.Lausanne);
		return paul;
	}

	protected Contribuable createDepartHorsCantonAvecImmeuble(Long noTiers, RegDate dateDepart) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HC, MockCommune.Lausanne);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Neuchatel);
		addForSecondaire(paul, date(2000, 7, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(),
				MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createDepartHorsCantonEtVenteImmeuble(Long noTiers, RegDate dateDepart, RegDate dateVente) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HC, MockCommune.Lausanne);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HC, dateVente, null, MockCommune.Neuchatel);
		addForSecondaire(paul, date(2000, 7, 1), MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createDepartHorsCantonSourcierPur(RegDate dateDepart) throws Exception {
		return createDepartHorsCantonSourcierPur(null, dateDepart);
	}

	protected Contribuable createDepartHorsCantonSourcierPur(@Nullable Long noTiers, RegDate dateDepart) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		ForFiscalPrincipal ffp = addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HC, MockCommune.Lausanne);
		ffp.setModeImposition(ModeImposition.SOURCE);
		ffp = addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Neuchatel);
		ffp.setModeImposition(ModeImposition.SOURCE);
		return paul;
	}

	protected Contribuable createDepartHorsCantonSourcierMixte137Al1(RegDate dateDepart) throws Exception {
		return createDepartHorsCantonSourcierMixte137Al1(null, dateDepart);
	}

	protected Contribuable createDepartHorsCantonSourcierMixte137Al1(@Nullable Long noTiers, RegDate dateDepart) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		ForFiscalPrincipal ffp = addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HC, MockCommune.Lausanne);
		ffp.setModeImposition(ModeImposition.MIXTE_137_1);
		ffp = addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Neuchatel);
		ffp.setModeImposition(ModeImposition.MIXTE_137_1);
		addForSecondaire(paul, date(2002, 7, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.LesClees.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createDepartHorsCantonSourcierMixte137Al2(RegDate dateDepart) throws Exception {
		return createDepartHorsCantonSourcierMixte137Al2(null, dateDepart);
	}

	protected Contribuable createDepartHorsCantonSourcierMixte137Al2(@Nullable Long noTiers, RegDate dateDepart) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		ForFiscalPrincipal ffp = addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HC, MockCommune.Lausanne);
		ffp.setModeImposition(ModeImposition.MIXTE_137_2);
		ffp = addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Neuchatel);
		ffp.setModeImposition(ModeImposition.SOURCE);  // un hors-canton ne peut pas être mixte 137 al. 2
		return paul;
	}

	protected Contribuable createDepartHorsSuisse(RegDate dateDepart) throws Exception {
		return createDepartHorsSuisse(null, dateDepart);
	}

	protected Contribuable createDepartHorsSuisse(@Nullable Long noTiers, RegDate dateDepart) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.PaysInconnu);
		return paul;
	}

	protected Contribuable createDepartHorsSuisseAvecImmeuble(RegDate dateDepart) throws Exception {
		return createDepartHorsSuisseAvecImmeuble(null, dateDepart);
	}

	protected Contribuable createDepartHorsSuisseAvecImmeuble(@Nullable Long noTiers, RegDate dateDepart) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.Espagne);
		addForSecondaire(paul, date(2000, 7, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createDepartHorsSuisseDepuisHorsCantonAvecImmeuble(RegDate dateDepart) throws Exception {
		return createDepartHorsSuisseDepuisHorsCantonAvecImmeuble(null, dateDepart);
	}

	protected Contribuable createDepartHorsSuisseDepuisHorsCantonAvecImmeuble(@Nullable Long noTiers, RegDate dateDepart) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), null, dateDepart, MotifFor.DEPART_HS, MockCommune.Neuchatel);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.Espagne);
		addForSecondaire(paul, date(2000, 7, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createDepartHorsSuisseEtArriveeDeHorsCanton(Long noTiers, RegDate dateDepart, RegDate dateArrivee) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1968, 4, 13), MotifFor.MAJORITE, dateDepart, MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockPays.PaysInconnu);
		addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		return paul;
	}

	protected Contribuable createDepartHorsSuisseDepuisHorsCantonAvecActiviteIndependante(RegDate dateDepart) throws Exception {
		return createDepartHorsSuisseDepuisHorsCantonAvecActiviteIndependante(null, dateDepart);
	}

	protected Contribuable createDepartHorsSuisseDepuisHorsCantonAvecActiviteIndependante(@Nullable Long noTiers, RegDate dateDepart) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), null, dateDepart, MotifFor.DEPART_HS, MockCommune.Neuchatel);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.Espagne);
		addForSecondaire(paul, date(2000, 7, 1), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		return paul;
	}

	protected Contribuable createHorsSuisseAvecAchatEtVenteImmeuble(RegDate dateAchat, RegDate dateVente) throws Exception {
		return createHorsSuisseAvecAchatEtVenteImmeuble(null, dateAchat, dateVente);
	}

	protected Contribuable createHorsSuisseAvecAchatEtVenteImmeuble(@Nullable Long noTiers, RegDate dateAchat, RegDate dateVente) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
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
		return createHorsSuisseAvecAchatsEtVentesImmeubles(null, periodes);
	}

	protected Contribuable createHorsSuisseAvecAchatsEtVentesImmeubles(@Nullable Long noTiers, DateRange... periodes) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1980, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockPays.Espagne);
		for (DateRange periode : periodes) {
			addForSecondaire(paul, periode.getDateDebut(), MotifFor.ACHAT_IMMOBILIER, periode.getDateFin(), MotifFor.VENTE_IMMOBILIER, MockCommune.Lausanne.getNoOFS(),
					MotifRattachement.IMMEUBLE_PRIVE);
		}
		return paul;
	}

	protected Contribuable createHorsSuisseSourcierAvecAchatEtVenteImmeuble(Long noTiers, RegDate dateAchat, RegDate dateVente) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);

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
		return createHorsSuisseVenteImmeubleEtFermetureFFPSansMotif(null, dateVente);
	}

	protected Contribuable createHorsSuisseVenteImmeubleEtFermetureFFPSansMotif(@Nullable Long noTiers, final RegDate dateVente) throws Exception {
		Contribuable ctb = createContribuableSansFor(noTiers);
		addForPrincipal(ctb, date(1976, 1, 7), MotifFor.ARRIVEE_HS, date(1997, 3, 7), MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(ctb, date(1997, 3, 8), MotifFor.DEPART_HS, dateVente, null, MockPays.France);
		addForSecondaire(ctb, date(2007, 3, 9), MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Lausanne
				.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return ctb;
	}

	protected Contribuable createArriveeHorsSuisseAvecImmeuble(RegDate dateArrivee) throws Exception {
		return createArriveeHorsSuisseAvecImmeuble(null, dateArrivee);
	}

	protected Contribuable createArriveeHorsSuisseAvecImmeuble(@Nullable Long noTiers, RegDate dateArrivee) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Danemark);
		addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
		addForSecondaire(paul, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createArriveeHorsSuisseEtDepartHCSourcier(long ctbId, RegDate dateArriveeHS, RegDate dateDepartHC) throws Exception {
		Contribuable ctb = createContribuableSansFor(ctbId);
		addForPrincipal(ctb, date(2000, 1, 1), MotifFor.INDETERMINE, dateArriveeHS.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Danemark).setModeImposition(ModeImposition.SOURCE);
		addForPrincipal(ctb, dateArriveeHS, MotifFor.ARRIVEE_HS, dateDepartHC, MotifFor.DEPART_HC, MockCommune.Lausanne).setModeImposition(ModeImposition.SOURCE);
		addForPrincipal(ctb, dateDepartHC.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Zurich).setModeImposition(ModeImposition.SOURCE);
		return ctb;
	}

	protected Contribuable createArriveeHorsSuisseEtDepartHCAvecImmeuble(long ctbId, RegDate dateArriveeHS, RegDate dateDepartHC) throws Exception {
		Contribuable paul = createContribuableSansFor(ctbId);
		addForPrincipal(paul, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateArriveeHS.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Danemark);
		addForPrincipal(paul, dateArriveeHS, MotifFor.ARRIVEE_HS, dateDepartHC, MotifFor.DEPART_HC, MockCommune.Lausanne);
		addForPrincipal(paul, dateDepartHC.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Zurich);
		addForSecondaire(paul, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createArriveeHorsCantonEtDepartHSAvecImmeuble(long ctbId, RegDate dateArriveeHC, RegDate dateDepartHS) throws Exception {
		Contribuable paul = createContribuableSansFor(ctbId);
		addForPrincipal(paul, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateArriveeHC.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Zurich);
		addForPrincipal(paul, dateArriveeHC, MotifFor.ARRIVEE_HC, dateDepartHS, MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, dateDepartHS.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.Danemark);
		addForSecondaire(paul, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createDepartHorsSuisseEtArriveeHorsSuisseDansLAnneeMaisAvecMotifArriveeHorsCantonSourcierPur(long ctbId, RegDate dateDepartHS, RegDate dateArriveeHS) throws Exception {
		Contribuable ctb = createContribuableSansFor(ctbId);
		addForPrincipal(ctb, date(2000, 1, 1), MotifFor.INDETERMINE, dateDepartHS, MotifFor.DEPART_HS, MockCommune.Lausanne).setModeImposition(ModeImposition.SOURCE);
		addForPrincipal(ctb, dateDepartHS.getOneDayAfter(), MotifFor.DEPART_HS, dateArriveeHS.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockPays.Danemark).setModeImposition(ModeImposition.SOURCE);
		addForPrincipal(ctb, dateArriveeHS, MotifFor.ARRIVEE_HC, MockCommune.Lausanne).setModeImposition(ModeImposition.SOURCE);
		return ctb;
	}

	protected Contribuable createArriveeHorsSuisseEtDepartHorsSuisseDansLAnneeMaisAvecMotifDepartHorsCantonSourcierPur(long ctbId, RegDate dateArriveeHS, RegDate dateDepartHS) throws Exception {
		Contribuable ctb = createContribuableSansFor(ctbId);
		addForPrincipal(ctb, date(2000, 1, 1), MotifFor.INDETERMINE, dateArriveeHS.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Danemark).setModeImposition(ModeImposition.SOURCE);
		addForPrincipal(ctb, dateArriveeHS, MotifFor.ARRIVEE_HS, dateDepartHS, MotifFor.DEPART_HC, MockCommune.Lausanne).setModeImposition(ModeImposition.SOURCE);
		addForPrincipal(ctb, dateDepartHS.getOneDayAfter(), MotifFor.DEPART_HC, MockPays.Danemark).setModeImposition(ModeImposition.SOURCE);
		return ctb;
	}

	@SuppressWarnings({"deprecation"})
	protected Contribuable createArriveeHorsSuisseAvecImmeubleEtMotifDemanagement(Long noTiers, RegDate dateAchat, RegDate dateArrivee) throws Exception {
		Contribuable ctb = createContribuableSansFor(noTiers);
		addForPrincipal(ctb, dateAchat, MotifFor.INDETERMINE, dateArrivee.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockPays.PaysInconnu);
		addForPrincipal(ctb, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Leysin);
		addForSecondaire(ctb, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return ctb;
	}

	protected Contribuable createArriveeHorsSuisseEtVenteImmeuble(RegDate dateArrivee, RegDate dateVente) throws Exception {
		return createArriveeHorsSuisseEtVenteImmeuble(null, dateArrivee, dateVente);
	}

	protected Contribuable createArriveeHorsSuisseEtVenteImmeuble(@Nullable Long noTiers, RegDate dateArrivee, RegDate dateVente) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HS,
				MockPays.Danemark);
		addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
		addForSecondaire(paul, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Lausanne
				.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createArriveHorsSuisseEtDemenagementVaudoisDansLAnnee(@Nullable Long noTiers, RegDate dateArrivee, RegDate dateDemenagement) {
		Contribuable ctb = addNonHabitant(noTiers, "Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);
		addForPrincipal(ctb, dateArrivee, MotifFor.ARRIVEE_HS, dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
		addForPrincipal(ctb, dateDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Bex);
		return ctb;
	}

	protected Contribuable createArriveeHSAchatImmeubleEtDepartHS(RegDate dateArrivee, RegDate dateAchat, RegDate dateDepart)
			throws Exception {
		return createArriveeHSAchatImmeubleEtDepartHS(null, dateArrivee, dateAchat, dateDepart);
	}

	protected Contribuable createArriveeHSAchatImmeubleEtDepartHS(@Nullable Long noTiers, RegDate dateArrivee, RegDate dateAchat, RegDate dateDepart)
			throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HS, dateDepart, MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.France);
		addForSecondaire(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createArriveeHSDepartHSPuisAchatImmeuble(RegDate dateArrivee, RegDate dateDepart, RegDate dateAchat)
			throws Exception {
		return createArriveeHSDepartHSPuisAchatImmeuble(null, dateArrivee, dateDepart, dateAchat);
	}

	protected Contribuable createArriveeHSDepartHSPuisAchatImmeuble(@Nullable Long noTiers, RegDate dateArrivee, RegDate dateDepart, RegDate dateAchat)
			throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HS, dateDepart, MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.France);
		addForSecondaire(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createArriveeHorsCantonSourcierPur(RegDate dateArrivee) throws Exception {
		return createArriveeHorsCantonSourcierPur(null, dateArrivee);
	}

	protected Contribuable createArriveeHorsCantonSourcierPur(@Nullable Long noTiers, RegDate dateArrivee) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		ForFiscalPrincipal ffp = addForPrincipal(paul, date(1983, 4, 13), null, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Neuchatel);
		ffp.setModeImposition(ModeImposition.SOURCE);
		ffp = addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		ffp.setModeImposition(ModeImposition.SOURCE);
		return paul;
	}

	protected Contribuable createArriveeHorsCanton(@Nullable Long noTiers, RegDate dateArrivee) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(2002, 7, 1), MotifFor.ACHAT_IMMOBILIER, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Neuchatel);
		addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		return paul;
	}

	protected Contribuable createArriveeHorsCantonAvecImmeuble(Long noTiers, RegDate dateArrivee) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(2000, 7, 1), MotifFor.ACHAT_IMMOBILIER, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Neuchatel);
		addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		addForSecondaire(paul, date(2000, 7, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createArriveeHorsCantonSourcierMixte137Al1(RegDate dateArrivee) throws Exception {
		return createArriveeHorsCantonSourcierMixte137Al1(null, dateArrivee);
	}

	protected Contribuable createArriveeHorsCantonSourcierMixte137Al1(@Nullable Long noTiers, RegDate dateArrivee) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		ForFiscalPrincipal ffp = addForPrincipal(paul, date(2002, 7, 1), MotifFor.ACHAT_IMMOBILIER, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Neuchatel);
		ffp.setModeImposition(ModeImposition.MIXTE_137_1);
		ffp = addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		ffp.setModeImposition(ModeImposition.MIXTE_137_1);
		addForSecondaire(paul, date(2002, 7, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.LesClees.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createArriveeHorsCantonSourcierMixte137Al2(RegDate dateArrivee) throws Exception {
		return createArriveeHorsCantonSourcierMixte137Al2(null, dateArrivee);
	}

	protected Contribuable createArriveeHorsCantonSourcierMixte137Al2(@Nullable Long noTiers, RegDate dateArrivee) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		ForFiscalPrincipal ffp = addForPrincipal(paul, date(1983, 4, 13), null, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Neuchatel);
		ffp.setModeImposition(ModeImposition.SOURCE);  // un hors-canton ne peut pas être mixte 137 al. 2
		ffp = addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		ffp.setModeImposition(ModeImposition.MIXTE_137_2);
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

	protected Contribuable createSourcierPureHorsSuisse(Long noTiers) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		ForFiscalPrincipal fp = addForPrincipal(paul, date(1993, 5, 1), MotifFor.DEBUT_EXPLOITATION, MockPays.Albanie);
		fp.setModeImposition(ModeImposition.SOURCE);
		return paul;
	}

	protected Contribuable createSourcierPureHorsCanton(Long noTiers) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		ForFiscalPrincipal fp = addForPrincipal(paul, date(1993, 5, 1), MotifFor.DEBUT_EXPLOITATION, MockCommune.Neuchatel);
		fp.setModeImposition(ModeImposition.SOURCE);
		return paul;
	}

	protected Contribuable createSourcierMixte137Al1HorsCanton(Long noTiers, RegDate achatImmeuble) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		ForFiscalPrincipal fp = addForPrincipal(paul, date(1993, 5, 1), null, achatImmeuble.getOneDayBefore(),
				MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Neuchatel);
		fp.setModeImposition(ModeImposition.SOURCE);

		fp = addForPrincipal(paul, achatImmeuble, MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Neuchatel);
		fp.setModeImposition(ModeImposition.MIXTE_137_1);
		addForSecondaire(paul, achatImmeuble, MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(),
				MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	/**
	 * TODO (msi) corriger cela (mixte 149) en fonction de la nouvelle spéc. de Thierry en cours de validation
	 */
	protected Contribuable createSourcierMixte137Al1HorsSuisse(Long noTiers, RegDate dateAchat) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		ForFiscalPrincipal fp = addForPrincipal(paul, date(1993, 5, 1), null, date(2007, 6, 30), MotifFor.CHGT_MODE_IMPOSITION, MockPays.France);
		fp.setModeImposition(ModeImposition.SOURCE);
		fp = addForPrincipal(paul, dateAchat, MotifFor.CHGT_MODE_IMPOSITION, MockPays.France);
		fp.setModeImposition(ModeImposition.MIXTE_137_1);
		addForSecondaire(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createSourcierPassageMixte137Al2(Long noTiers, RegDate datePassage) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		ForFiscalPrincipal fp = addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, datePassage.getOneDayBefore(),
				MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne);
		fp.setModeImposition(ModeImposition.SOURCE);
		fp = addForPrincipal(paul, datePassage, MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne);
		fp.setModeImposition(ModeImposition.MIXTE_137_2);
		return paul;
	}

	/**
	 * Sourcier pure vaudois qui passe en mode ordinaire en 2008
	 */
	protected Contribuable createPassageRoleSourceAOrdinaire(Long noTiers, RegDate dateChangement) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		ForFiscalPrincipal fp = addForPrincipal(paul, date(1993, 5, 1), MotifFor.ARRIVEE_HC, dateChangement.getOneDayBefore(), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne);
		fp.setModeImposition(ModeImposition.SOURCE);
		fp = addForPrincipal(paul, dateChangement, MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne);
		fp.setModeImposition(ModeImposition.ORDINAIRE);
		return paul;
	}

	/**
	 * Contribuable avec un for principal qui s'ouvre avec un motif 'obtention permis C' sans for fiscal précédent
	 */
	protected Contribuable createPassageRoleSourceAOrdinaireImplicite(Long noTiers, RegDate dateChangement) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, dateChangement, MotifFor.PERMIS_C_SUISSE, MockCommune.Lausanne);
		return paul;
	}

	/**
	 * Hors-canton avec immeuble qui décède à la date spécifiée.
	 */
	protected Contribuable createDecesHorsCantonAvecImmeuble(RegDate dateAchat, RegDate dateDeces) throws Exception {
		return createDecesHorsCantonAvecImmeuble(null, dateAchat, dateDeces);
	}

	protected Contribuable createDecesHorsCantonAvecImmeuble(@Nullable Long noTiers, RegDate dateAchat, RegDate dateDeces) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
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
		return createDecesHorsCantonActiviteIndependante(null, debutExploitation, dateDeces);
	}

	protected Contribuable createDecesHorsCantonActiviteIndependante(@Nullable Long noTiers, RegDate debutExploitation, RegDate dateDeces) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
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
		return createHorsCantonAvecImmeuble(null, dateAchat);
	}

	protected Contribuable createHorsCantonAvecImmeuble(@Nullable Long noTiers, RegDate dateAchat) throws Exception {
		Contribuable paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Neuchatel);
		addForSecondaire(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected Contribuable createVenteImmeubleHorsCanton(RegDate dateVente) throws Exception {
		return createVenteImmeubleHorsCanton(null, dateVente);
	}

	protected Contribuable createVenteImmeubleHorsCanton(@Nullable Long noTiers, RegDate dateVente) throws Exception {
		Contribuable ctb = createContribuableSansFor(noTiers);
		addForPrincipal(ctb, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Neuchatel);
		addForSecondaire(ctb, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Aubonne.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
		return ctb;
	}

	protected Contribuable createFinActiviteHorsCanton(RegDate dateFin) throws Exception {
		return createFinActiviteHorsCanton(null, dateFin);
	}

	protected Contribuable createFinActiviteHorsCanton(@Nullable Long noTiers, RegDate dateFin) throws Exception {
		Contribuable ctb = createContribuableSansFor(noTiers);
		addForPrincipal(ctb, date(2000, 1, 1), MotifFor.DEBUT_EXPLOITATION, MockCommune.Neuchatel);
		addForSecondaire(ctb, date(2000, 1, 1), MotifFor.DEBUT_EXPLOITATION, dateFin, MotifFor.FIN_EXPLOITATION, MockCommune.Aubonne.getNoOFSEtendu(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		return ctb;
	}

	protected Contribuable createOrdinairePuisSourcierCasLimite(@Nullable Long noTiers) throws Exception {
		final Contribuable paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2006, 1, 15), MotifFor.CHGT_MODE_IMPOSITION,
				MockCommune.Lausanne);
		ForFiscalPrincipal fp = addForPrincipal(paul, date(2006, 1, 16), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne);
		fp.setModeImposition(ModeImposition.SOURCE);
		return paul;
	}

	protected Contribuable createSourcierPuisOrdinaireCasLimite(Long noTiers) throws Exception {
		final Contribuable paul = createContribuableSansFor(noTiers);
		ForFiscalPrincipal fp = addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2006, 12, 16),
				MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne);
		fp.setModeImposition(ModeImposition.SOURCE);
		addForPrincipal(paul, date(2006, 12, 17), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne);
		return paul;
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
		addEtatDeclarationEmise(decl, date(periodePrecedente + 1, 1, 15));
		addEtatDeclarationRetournee(decl, date(periodePrecedente + 1, 1, 15));
		return ctb;
	}

	protected void assertOrdinaire(RegDate debut, @Nullable RegDate fin, @Nullable MotifFor motifFractDebut, @Nullable MotifFor motifFractFin,
			Assujettissement assujettissement) {
		assertNotNull(assujettissement);
		assertInstanceOf(VaudoisOrdinaire.class, assujettissement);
		assertEquals(debut, assujettissement.getDateDebut());
		assertEquals(fin, assujettissement.getDateFin());
		assertEquals(motifFractDebut, assujettissement.getMotifFractDebut());
		assertEquals(motifFractFin, assujettissement.getMotifFractFin());
	}

	protected void assertDiplomateSuisse(RegDate debut, RegDate fin, @Nullable MotifFor motifFractDebut, @Nullable MotifFor motifFractFin,
			Assujettissement assujettissement) {
		assertNotNull(assujettissement);
		assertInstanceOf(DiplomateSuisse.class, assujettissement);
		assertEquals(debut, assujettissement.getDateDebut());
		assertEquals(fin, assujettissement.getDateFin());
		assertEquals(motifFractDebut, assujettissement.getMotifFractDebut());
		assertEquals(motifFractFin, assujettissement.getMotifFractFin());
	}

	protected void assertHorsCanton(RegDate debut, @Nullable RegDate fin, @Nullable MotifFor motifFractDebut, @Nullable MotifFor motifFractFin,
			Assujettissement assujettissement) {
		assertNotNull(assujettissement);
		assertInstanceOf(HorsCanton.class, assujettissement);
		assertEquals(debut, assujettissement.getDateDebut());
		assertEquals(fin, assujettissement.getDateFin());
		assertEquals(motifFractDebut, assujettissement.getMotifFractDebut());
		assertEquals(motifFractFin, assujettissement.getMotifFractFin());
	}

	protected void assertHorsSuisse(RegDate debut, @Nullable RegDate fin, @Nullable MotifFor motifFractDebut, @Nullable MotifFor motifFractFin,
			Assujettissement assujettissement) {
		assertNotNull(assujettissement);
		assertInstanceOf(HorsSuisse.class, assujettissement);
		assertEquals(debut, assujettissement.getDateDebut());
		assertEquals(fin, assujettissement.getDateFin());
		assertEquals(motifFractDebut, assujettissement.getMotifFractDebut());
		assertEquals(motifFractFin, assujettissement.getMotifFractFin());
	}

	protected void assertSourcierPur(RegDate debut, RegDate fin, @Nullable MotifFor motifFractDebut, @Nullable MotifFor motifFractFin, TypeAutoriteFiscale typeAutorite, Assujettissement assujettissement) {
		assertNotNull(assujettissement);
		assertInstanceOf(SourcierPur.class, assujettissement);
		final SourcierPur a = (SourcierPur) assujettissement;
		assertEquals(debut, assujettissement.getDateDebut());
		assertEquals(fin, assujettissement.getDateFin());
		assertEquals(motifFractDebut, assujettissement.getMotifFractDebut());
		assertEquals(motifFractFin, assujettissement.getMotifFractFin());
		assertEquals(typeAutorite, a.getTypeAutoriteFiscale());
	}

	protected void assertSourcierMixteArt137Al1(RegDate debut, @Nullable RegDate fin, @Nullable MotifFor motifFractDebut, @Nullable MotifFor motifFractFin, TypeAutoriteFiscale typeAutorite, Assujettissement assujettissement) {
		assertNotNull(assujettissement);
		assertInstanceOf(SourcierMixteArt137Al1.class, assujettissement);
		final SourcierMixteArt137Al1 a = (SourcierMixteArt137Al1) assujettissement;
		assertEquals(debut, assujettissement.getDateDebut());
		assertEquals(fin, assujettissement.getDateFin());
		assertEquals(motifFractDebut, assujettissement.getMotifFractDebut());
		assertEquals(motifFractFin, assujettissement.getMotifFractFin());
		assertEquals(typeAutorite, a.getTypeAutoriteFiscale());
	}

	protected void assertSourcierMixteArt137Al2(RegDate debut, @Nullable RegDate fin, @Nullable MotifFor motifFractDebut, @Nullable MotifFor motifFractFin, TypeAutoriteFiscale typeAutorite, Assujettissement assujettissement) {
		assertNotNull(assujettissement);
		assertInstanceOf(SourcierMixteArt137Al2.class, assujettissement);
		final SourcierMixteArt137Al2 a = (SourcierMixteArt137Al2) assujettissement;
		assertEquals(debut, assujettissement.getDateDebut());
		assertEquals(fin, assujettissement.getDateFin());
		assertEquals(motifFractDebut, assujettissement.getMotifFractDebut());
		assertEquals(motifFractFin, assujettissement.getMotifFractFin());
		assertEquals(typeAutorite, a.getTypeAutoriteFiscale());
	}

	protected void assertPeriodeImposition(RegDate debut, RegDate fin, CategorieEnvoiDI categorie, @Nullable TypeAdresseRetour adresseRetour, boolean optionnelle, boolean remplaceParNote,
	                                       boolean fermetureCauseDeces, boolean fermetureCauseFinAssujettissementHS, PeriodeImposition periode) {
		assertNotNull(periode);
		assertEquals(debut, periode.getDateDebut());
		assertEquals(fin, periode.getDateFin());
		assertEquals(categorie, periode.getCategorieEnvoiDI());
		assertEquals(adresseRetour, periode.getAdresseRetour());
		assertEquals(optionnelle, periode.isOptionnelle());
		assertEquals(remplaceParNote, periode.isRemplaceeParNote());
		assertEquals(fermetureCauseDeces, periode.isFermetureCauseDeces());
		assertEquals(fermetureCauseFinAssujettissementHS, periode.isFermetureCauseFinAssujettissementHorsSuisse());
	}
}
