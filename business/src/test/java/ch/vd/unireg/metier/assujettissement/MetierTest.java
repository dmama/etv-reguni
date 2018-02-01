package ch.vd.unireg.metier.assujettissement;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper.Range;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseRetour;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
	protected PersonnePhysique createDiplomateSuisse(RegDate dateNomination) throws Exception {
		// on crée un non-habitant par commodité, mais cela devrait être un habitant normalement
		PersonnePhysique paul = createContribuableSansFor();
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.MAJORITE, dateNomination.getOneDayBefore(), MotifFor.DEPART_HS, MockCommune.Lausanne);
		// le for principal d'un diplomate suisse reste en suisse (= sa commune d'origine) malgré le fait qu'il soit basé à l'étranger
		addForPrincipal(paul, dateNomination, MotifFor.DEPART_HS, MockCommune.Lausanne, MotifRattachement.DIPLOMATE_SUISSE);
		return paul;
	}


	protected PersonnePhysique createDiplomateAvecImmeuble(@Nullable Long noTiers, RegDate dateNomination, RegDate achatImmeuble) throws Exception {
		// on crée un non-habitant par commodité, mais cela devrait être un habitant normalement
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.MAJORITE, dateNomination.getOneDayBefore(), MotifFor.DEPART_HS, MockCommune.Lausanne);
		// le for principal d'un diplomate suisse reste en suisse (= sa commune d'origine) malgré le fait qu'il soit basé à l'étranger
		addForPrincipal(paul, dateNomination, MotifFor.DEPART_HS, MockCommune.Lausanne, MotifRattachement.DIPLOMATE_SUISSE);
		addForSecondaire(paul, achatImmeuble, MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected PersonnePhysique createContribuableSansFor() throws Exception {
		return createContribuableSansFor(null);
	}

	protected PersonnePhysique createContribuableSansFor(@Nullable Long noTiers) throws Exception {
		return addNonHabitant(noTiers, "Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
	}

	protected PersonnePhysique createUnForSimple() throws Exception {
		return createUnForSimple(null);
	}

	protected PersonnePhysique createUnForSimple(@Nullable Long noTiers) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		return paul;
	}

	protected EnsembleTiersCouple createMenageSansFor(RegDate dateMariage, @Nullable RegDate datePremiereSeparation) {
		return createMenageSansFor(null, dateMariage, datePremiereSeparation);
	}

	protected EnsembleTiersCouple createMenageSansFor(@Nullable Long noTiers, RegDate dateMariage, @Nullable RegDate datePremiereSeparation) {
		return createMenageSansFor(noTiers, dateMariage, datePremiereSeparation, null, null);
	}

	protected EnsembleTiersCouple createMenageSansFor(RegDate dateMariage, @Nullable RegDate datePremiereSeparation, @Nullable RegDate dateReconciliation, @Nullable RegDate dateSecondeSeparation) {
		return createMenageSansFor(null, dateMariage, datePremiereSeparation, dateReconciliation, dateSecondeSeparation);
	}

	protected EnsembleTiersCouple createMenageSansFor(@Nullable Long noTiers, RegDate dateMariage, @Nullable RegDate datePremiereSeparation, @Nullable RegDate dateReconciliation,
	                                                  @Nullable RegDate dateSecondeSeparation) {

		final PersonnePhysique principal = addNonHabitant(noTiers == null ? null : noTiers + 1000, "Jean", "Moulin", date(1934, 1, 1), Sexe.MASCULIN);
		final PersonnePhysique conjoint = addNonHabitant(noTiers == null ? null : noTiers + 1001, "Jeanne", "Moulin", date(1934, 1, 1), Sexe.FEMININ);

		// mariage puis séparation
		final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(noTiers, principal, conjoint, dateMariage, datePremiereSeparation);
		final MenageCommun ctb = ensemble.getMenage();

		if (dateReconciliation != null) {
			// réconciliation pour re-séparation
			addAppartenanceMenage(ctb, principal, dateReconciliation, dateSecondeSeparation, false);
			addAppartenanceMenage(ctb, conjoint, dateReconciliation, dateSecondeSeparation, false);
		}

		return ensemble;
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
		addForSecondaire(paul, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
		                 MockCommune.Fraction.LAbbaye.getNoOFS(),
		                 MotifRattachement.IMMEUBLE_PRIVE);

		final PersonnePhysique marie = addNonHabitant(noConjoint, "Marie", "Duchêne", date(1970, 6, 3), Sexe.FEMININ);
		addForPrincipal(marie, date(1988, 6, 3), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

		final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(noMenage, paul, marie, dateMariage, null);
		addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
		addForSecondaire(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Fraction.LAbbaye.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		return ensemble;
	}

	protected EnsembleTiersCouple createMenageCommunDivorceVDImmeuble(Long noPrincipal, Long noConjoint, Long noMenage, RegDate dateMariage, RegDate dateDivorce) {

		final PersonnePhysique paul = addNonHabitant(noPrincipal, "Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);
		addForSecondaire(paul, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Fraction.LAbbaye.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		final PersonnePhysique marie = addNonHabitant(noConjoint, "Marie", "Duchêne", date(1970, 6, 3), Sexe.FEMININ);
		addForPrincipal(marie, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);

		final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(noMenage, paul, marie, dateMariage, dateDivorce.getOneDayBefore());
		addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDivorce.getOneDayBefore(),
				MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);
		addForSecondaire(ensemble.getMenage(), dateMariage, MotifFor.ACHAT_IMMOBILIER, dateDivorce.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
				MockCommune.Fraction.LAbbaye.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		return ensemble;
	}
	
	protected EnsembleTiersCouple createMenageCommunMariageHCImmeuble(Long noPrincipal, Long noConjoint, Long noMenage, RegDate dateMariage) {

		final PersonnePhysique paul = addNonHabitant(noPrincipal, "Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, date(1981, 4, 13), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Neuchatel);
		addForSecondaire(paul, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Fraction.LAbbaye.getNoOFS(),
				MotifRattachement.IMMEUBLE_PRIVE);

		final PersonnePhysique marie = addNonHabitant(noConjoint, "Marie", "Duchêne", date(1970, 6, 3), Sexe.FEMININ);
		addForPrincipal(marie, date(1988, 6, 3), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Neuchatel);

		final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(noMenage, paul, marie, dateMariage, null);
		addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Neuchatel);
		addForSecondaire(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Fraction.LAbbaye.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		return ensemble;
	}

	protected EnsembleTiersCouple createMenageCommunDivorceHCImmeuble(Long noPrincipal, Long noConjoint, Long noMenage, RegDate dateMariage, RegDate dateDivorce) {

		final PersonnePhysique paul = addNonHabitant(noPrincipal, "Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Neuchatel);
		addForSecondaire(paul, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Fraction.LAbbaye.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		final PersonnePhysique marie = addNonHabitant(noConjoint, "Marie", "Duchêne", date(1970, 6, 3), Sexe.FEMININ);
		addForPrincipal(marie, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Neuchatel);

		final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(noMenage, paul, marie, dateMariage, dateDivorce.getOneDayBefore());
		addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDivorce.getOneDayBefore(),
				MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Neuchatel);
		addForSecondaire(ensemble.getMenage(), dateMariage, MotifFor.ACHAT_IMMOBILIER, dateDivorce.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
				MockCommune.Fraction.LAbbaye.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		return ensemble;
	}

	protected EnsembleTiersCouple createMenageCommunMariageHSImmeuble(Long noPrincipal, Long noConjoint, Long noMenage, RegDate dateMariage) {

		final PersonnePhysique paul = addNonHabitant(noPrincipal, "Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, date(1981, 4, 13), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockPays.Allemagne);
		addForSecondaire(paul, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Fraction.LAbbaye.getNoOFS(),
				MotifRattachement.IMMEUBLE_PRIVE);

		final PersonnePhysique marie = addNonHabitant(noConjoint, "Marie", "Duchêne", date(1970, 6, 3), Sexe.FEMININ);
		addForPrincipal(marie, date(1988, 6, 3), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockPays.Allemagne);

		final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(noMenage, paul, marie, dateMariage, null);
		addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockPays.Allemagne);
		addForSecondaire(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Fraction.LAbbaye.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		return ensemble;
	}

	protected EnsembleTiersCouple createMenageCommunDivorceHSImmeuble(Long noPrincipal, Long noConjoint, Long noMenage, RegDate dateMariage, RegDate dateDivorce) {

		final PersonnePhysique paul = addNonHabitant(noPrincipal, "Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockPays.Allemagne);
		addForSecondaire(paul, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Fraction.LAbbaye.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		final PersonnePhysique marie = addNonHabitant(noConjoint, "Marie", "Duchêne", date(1970, 6, 3), Sexe.FEMININ);
		addForPrincipal(marie, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockPays.Allemagne);

		final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(noMenage, paul, marie, dateMariage, dateDivorce.getOneDayBefore());
		addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDivorce.getOneDayBefore(),
				MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockPays.Allemagne);
		addForSecondaire(ensemble.getMenage(), dateMariage, MotifFor.ACHAT_IMMOBILIER, dateDivorce.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
				MockCommune.Fraction.LAbbaye.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		return ensemble;
	}

	protected PersonnePhysique createDepartHorsCanton(Long noTiers, RegDate dateDepart) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HC, MockCommune.Lausanne);
		return paul;
	}

	protected PersonnePhysique createDepartHorsCantonAvecImmeuble(Long noTiers, RegDate dateDepart) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HC, MockCommune.Lausanne);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Neuchatel);
		addForSecondaire(paul, date(2000, 7, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(),
		                 MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected PersonnePhysique createDepartHorsCantonEtVenteImmeuble(Long noTiers, RegDate dateDepart, RegDate dateVente) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HC, MockCommune.Lausanne);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HC, dateVente, null, MockCommune.Neuchatel);
		addForSecondaire(paul, date(2000, 7, 1), MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected PersonnePhysique createDepartHorsCantonSourcierPur(RegDate dateDepart) throws Exception {
		return createDepartHorsCantonSourcierPur(null, dateDepart);
	}

	protected PersonnePhysique createDepartHorsCantonSourcierPur(@Nullable Long noTiers, RegDate dateDepart) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HC, MockCommune.Lausanne, ModeImposition.SOURCE);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Neuchatel, ModeImposition.SOURCE);
		return paul;
	}

	protected PersonnePhysique createDepartHorsCantonSourcierMixte137Al1_Invalide(RegDate dateDepart) throws Exception {
		return createDepartHorsCantonSourcierMixte137Al1_Invalide(null, dateDepart);
	}

	protected PersonnePhysique createDepartHorsCantonSourcierMixte137Al1_Invalide(@Nullable final Long noTiers, final RegDate dateDepart) throws Exception {
		return doWithoutValidation(
				new ExecuteCallback<PersonnePhysique>() { // [SIFISC-57] désactivation de la validation pour pouvoir construire un cas invalide, mais qui existe des fois tel quel en base de données
					@Override
					public PersonnePhysique execute() throws Exception {
						PersonnePhysique paul = createContribuableSansFor(noTiers);
						addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HC, MockCommune.Lausanne, ModeImposition.MIXTE_137_1);
						addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Neuchatel, ModeImposition.MIXTE_137_1);
						addForSecondaire(paul, date(2002, 7, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.LesClees.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
						return paul;
					}
				});
	}

	protected PersonnePhysique createDepartHorsCantonSourcierMixte137Al1(RegDate dateDepart) throws Exception {
		return createDepartHorsCantonSourcierMixte137Al1(null, dateDepart);
	}

	protected PersonnePhysique createDepartHorsCantonSourcierMixte137Al1(@Nullable Long noTiers, RegDate dateDepart) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HC, MockCommune.Lausanne, ModeImposition.MIXTE_137_1);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Neuchatel, ModeImposition.SOURCE);   // un hors-canton ne peut pas être mixte 137 al. 2
		return paul;
	}

	protected PersonnePhysique createDepartHorsCantonSourcierMixte137Al2(RegDate dateDepart) throws Exception {
		return createDepartHorsCantonSourcierMixte137Al2(null, dateDepart);
	}

	protected PersonnePhysique createDepartHorsCantonSourcierMixte137Al2(@Nullable Long noTiers, RegDate dateDepart) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HC, MockCommune.Lausanne, ModeImposition.MIXTE_137_2);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Neuchatel, ModeImposition.SOURCE);  // un hors-canton ne peut pas être mixte 137 al. 2
		return paul;
	}

	protected PersonnePhysique createDepartHorsSuisse(RegDate dateDepart) throws Exception {
		return createDepartHorsSuisse(null, dateDepart);
	}

	protected PersonnePhysique createDepartHorsSuisse(@Nullable Long noTiers, RegDate dateDepart) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.PaysInconnu);
		return paul;
	}

	protected PersonnePhysique createDepartHorsSuisseAvecImmeuble(RegDate dateDepart) throws Exception {
		return createDepartHorsSuisseAvecImmeuble(null, dateDepart);
	}

	protected PersonnePhysique createDepartHorsSuisseAvecImmeuble(@Nullable Long noTiers, RegDate dateDepart) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.Espagne);
		addForSecondaire(paul, date(2000, 7, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected PersonnePhysique createDepartHorsSuisseDepuisHorsCantonAvecImmeuble(RegDate dateDepart) throws Exception {
		return createDepartHorsSuisseDepuisHorsCantonAvecImmeuble(null, dateDepart);
	}

	protected PersonnePhysique createDepartHorsSuisseDepuisHorsCantonAvecImmeuble(@Nullable Long noTiers, RegDate dateDepart) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), null, dateDepart, MotifFor.DEPART_HS, MockCommune.Neuchatel);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.Espagne);
		addForSecondaire(paul, date(2000, 7, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected PersonnePhysique createDepartHorsSuisseEtArriveeDeHorsCanton(Long noTiers, RegDate dateDepart, RegDate dateArrivee) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1968, 4, 13), MotifFor.MAJORITE, dateDepart, MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockPays.PaysInconnu);
		addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		return paul;
	}

	protected PersonnePhysique createDepartHorsSuisseDepuisHorsCantonAvecActiviteIndependante(RegDate dateDepart) throws Exception {
		return createDepartHorsSuisseDepuisHorsCantonAvecActiviteIndependante(null, dateDepart);
	}

	protected PersonnePhysique createDepartHorsSuisseDepuisHorsCantonAvecActiviteIndependante(@Nullable Long noTiers, RegDate dateDepart) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), null, dateDepart, MotifFor.DEPART_HS, MockCommune.Neuchatel);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.Espagne);
		addForSecondaire(paul, date(2000, 7, 1), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		return paul;
	}

	protected PersonnePhysique createHorsSuisseAvecAchatEtVenteImmeuble(RegDate dateAchat, RegDate dateVente) throws Exception {
		return createHorsSuisseAvecAchatEtVenteImmeuble(null, dateAchat, dateVente);
	}

	protected PersonnePhysique createHorsSuisseAvecAchatEtVenteImmeuble(@Nullable Long noTiers, RegDate dateAchat, RegDate dateVente) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
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
	protected PersonnePhysique createHorsSuisseAvecAchatsEtVentesImmeubles(DateRange... periodes) throws Exception {
		return createHorsSuisseAvecAchatsEtVentesImmeubles(null, periodes);
	}

	protected PersonnePhysique createHorsSuisseAvecAchatsEtVentesImmeubles(@Nullable Long noTiers, DateRange... periodes) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1980, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockPays.Espagne);
		for (DateRange periode : periodes) {
			addForSecondaire(paul, periode.getDateDebut(), MotifFor.ACHAT_IMMOBILIER, periode.getDateFin(), MotifFor.VENTE_IMMOBILIER, MockCommune.Lausanne.getNoOFS(),
					MotifRattachement.IMMEUBLE_PRIVE);
		}
		return paul;
	}

	protected PersonnePhysique createHorsSuisseSourcierAvecAchatEtVenteImmeuble_Invalide(final Long noTiers, final RegDate dateAchat, final RegDate dateVente) throws Exception {
		// [SIFISC-57] désactivation de la validation pour pouvoir construire un cas invalide, mais qui existe des fois tel quel en base de données
		return doWithoutValidation(new ExecuteCallback<PersonnePhysique>() {
			@Override
			public PersonnePhysique execute() throws Exception {
				PersonnePhysique paul = createContribuableSansFor(noTiers);

				addForPrincipal(paul, date(2000, 1, 1), null, dateAchat.getOneDayBefore(), MotifFor.ACHAT_IMMOBILIER, MockPays.Espagne, ModeImposition.SOURCE);
				addForPrincipal(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockPays.Espagne, ModeImposition.MIXTE_137_1);    // TODO (msi) corriger le mode d'imposition en mixte 146 quand la spéc sera validée + ajout les fors AutreElementImposable.
				addForPrincipal(paul, dateVente.getOneDayAfter(), MotifFor.VENTE_IMMOBILIER, MockPays.Espagne, ModeImposition.SOURCE);

				addForSecondaire(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return paul;
			}
		});
	}

	protected PersonnePhysique createHorsSuisseVenteImmeubleEtFermetureFFPSansMotif(final RegDate dateVente) throws Exception {
		return createHorsSuisseVenteImmeubleEtFermetureFFPSansMotif(null, dateVente);
	}

	protected PersonnePhysique createHorsSuisseVenteImmeubleEtFermetureFFPSansMotif(@Nullable Long noTiers, final RegDate dateVente) throws Exception {
		PersonnePhysique ctb = createContribuableSansFor(noTiers);
		addForPrincipal(ctb, date(1976, 1, 7), MotifFor.ARRIVEE_HS, date(1997, 3, 7), MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(ctb, date(1997, 3, 8), MotifFor.DEPART_HS, dateVente, null, MockPays.France);
		addForSecondaire(ctb, date(2007, 3, 9), MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return ctb;
	}

	protected PersonnePhysique createArriveeHorsSuisseAvecImmeuble(RegDate dateArrivee) throws Exception {
		return createArriveeHorsSuisseAvecImmeuble(null, dateArrivee);
	}

	protected PersonnePhysique createArriveeHorsSuisseAvecImmeuble(@Nullable Long noTiers, RegDate dateArrivee) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Danemark);
		addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
		addForSecondaire(paul, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected PersonnePhysique createArriveeHorsSuisseEtDepartHCSourcier(long ctbId, RegDate dateArriveeHS, RegDate dateDepartHC) throws Exception {
		PersonnePhysique ctb = createContribuableSansFor(ctbId);
		addForPrincipal(ctb, date(2000, 1, 1), MotifFor.INDETERMINE, dateArriveeHS.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Danemark).setModeImposition(ModeImposition.SOURCE);
		addForPrincipal(ctb, dateArriveeHS, MotifFor.ARRIVEE_HS, dateDepartHC, MotifFor.DEPART_HC, MockCommune.Lausanne).setModeImposition(ModeImposition.SOURCE);
		addForPrincipal(ctb, dateDepartHC.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Zurich).setModeImposition(ModeImposition.SOURCE);
		return ctb;
	}

	protected PersonnePhysique createArriveeHorsSuisseEtDepartHCAvecImmeuble(long ctbId, RegDate dateArriveeHS, RegDate dateDepartHC) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(ctbId);
		addForPrincipal(paul, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateArriveeHS.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Danemark);
		addForPrincipal(paul, dateArriveeHS, MotifFor.ARRIVEE_HS, dateDepartHC, MotifFor.DEPART_HC, MockCommune.Lausanne);
		addForPrincipal(paul, dateDepartHC.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Zurich);
		addForSecondaire(paul, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected PersonnePhysique createArriveeHorsCantonEtDepartHSAvecImmeuble(long ctbId, RegDate dateArriveeHC, RegDate dateDepartHS) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(ctbId);
		addForPrincipal(paul, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateArriveeHC.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Zurich);
		addForPrincipal(paul, dateArriveeHC, MotifFor.ARRIVEE_HC, dateDepartHS, MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, dateDepartHS.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.Danemark);
		addForSecondaire(paul, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected PersonnePhysique createDepartHorsSuisseEtArriveeHorsSuisseDansLAnneeMaisAvecMotifArriveeHorsCantonSourcierPur(long ctbId, RegDate dateDepartHS, RegDate dateArriveeHS) throws Exception {
		PersonnePhysique ctb = createContribuableSansFor(ctbId);
		addForPrincipal(ctb, date(2000, 1, 1), MotifFor.INDETERMINE, dateDepartHS, MotifFor.DEPART_HS, MockCommune.Lausanne).setModeImposition(ModeImposition.SOURCE);
		addForPrincipal(ctb, dateDepartHS.getOneDayAfter(), MotifFor.DEPART_HS, dateArriveeHS.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockPays.Danemark).setModeImposition(ModeImposition.SOURCE);
		addForPrincipal(ctb, dateArriveeHS, MotifFor.ARRIVEE_HC, MockCommune.Lausanne).setModeImposition(ModeImposition.SOURCE);
		return ctb;
	}

	protected PersonnePhysique createArriveeHorsSuisseEtDepartHorsSuisseDansLAnneeMaisAvecMotifDepartHorsCantonSourcierPur(long ctbId, RegDate dateArriveeHS, RegDate dateDepartHS) throws Exception {
		PersonnePhysique ctb = createContribuableSansFor(ctbId);
		addForPrincipal(ctb, date(2000, 1, 1), MotifFor.INDETERMINE, dateArriveeHS.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Danemark).setModeImposition(ModeImposition.SOURCE);
		addForPrincipal(ctb, dateArriveeHS, MotifFor.ARRIVEE_HS, dateDepartHS, MotifFor.DEPART_HC, MockCommune.Lausanne).setModeImposition(ModeImposition.SOURCE);
		addForPrincipal(ctb, dateDepartHS.getOneDayAfter(), MotifFor.DEPART_HC, MockPays.Danemark).setModeImposition(ModeImposition.SOURCE);
		return ctb;
	}

	@SuppressWarnings({"deprecation"})
	protected PersonnePhysique createArriveeHorsSuisseAvecImmeubleEtMotifDemanagement(Long noTiers, RegDate dateAchat, RegDate dateArrivee) throws Exception {
		PersonnePhysique ctb = createContribuableSansFor(noTiers);
		addForPrincipal(ctb, dateAchat, MotifFor.INDETERMINE, dateArrivee.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockPays.PaysInconnu);
		addForPrincipal(ctb, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Leysin);
		addForSecondaire(ctb, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return ctb;
	}

	protected PersonnePhysique createArriveeHorsSuisseEtVenteImmeuble(RegDate dateArrivee, RegDate dateVente) throws Exception {
		return createArriveeHorsSuisseEtVenteImmeuble(null, dateArrivee, dateVente);
	}

	protected PersonnePhysique createArriveeHorsSuisseEtVenteImmeuble(@Nullable Long noTiers, RegDate dateArrivee, RegDate dateVente) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Danemark);
		addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
		addForSecondaire(paul, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected PersonnePhysique createArriveHorsSuisseEtDemenagementVaudoisDansLAnnee(@Nullable Long noTiers, RegDate dateArrivee, RegDate dateDemenagement) {
		PersonnePhysique ctb = addNonHabitant(noTiers, "Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);
		addForPrincipal(ctb, dateArrivee, MotifFor.ARRIVEE_HS, dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
		addForPrincipal(ctb, dateDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Bex);
		return ctb;
	}

	protected PersonnePhysique createArriveeHSAchatImmeubleEtDepartHS(RegDate dateArrivee, RegDate dateAchat, RegDate dateDepart) throws Exception {
		return createArriveeHSAchatImmeubleEtDepartHS(null, dateArrivee, dateAchat, dateDepart);
	}

	protected PersonnePhysique createArriveeHSAchatImmeubleEtDepartHS(@Nullable Long noTiers, RegDate dateArrivee, RegDate dateAchat, RegDate dateDepart) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HS, dateDepart, MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.France);
		addForSecondaire(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected PersonnePhysique createArriveeHSDepartHSPuisAchatImmeuble(RegDate dateArrivee, RegDate dateDepart, RegDate dateAchat) throws Exception {
		return createArriveeHSDepartHSPuisAchatImmeuble(null, dateArrivee, dateDepart, dateAchat);
	}

	protected PersonnePhysique createArriveeHSDepartHSPuisAchatImmeuble(@Nullable Long noTiers, RegDate dateArrivee, RegDate dateDepart, RegDate dateAchat) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HS, dateDepart, MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.France);
		addForSecondaire(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected PersonnePhysique createArriveeHorsCantonSourcierPur(RegDate dateArrivee) throws Exception {
		return createArriveeHorsCantonSourcierPur(null, dateArrivee);
	}

	protected PersonnePhysique createArriveeHorsCantonSourcierPur(@Nullable Long noTiers, RegDate dateArrivee) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), null, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Neuchatel, ModeImposition.SOURCE);
		addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HC, MockCommune.Lausanne, ModeImposition.SOURCE);
		return paul;
	}

	protected PersonnePhysique createArriveeHorsCanton(@Nullable Long noTiers, RegDate dateArrivee) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(2002, 7, 1), MotifFor.ACHAT_IMMOBILIER, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Neuchatel);
		addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		return paul;
	}

	protected PersonnePhysique createArriveeHorsCantonAvecImmeuble(Long noTiers, RegDate dateArrivee) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(2000, 7, 1), MotifFor.ACHAT_IMMOBILIER, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Neuchatel);
		addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		addForSecondaire(paul, date(2000, 7, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected PersonnePhysique createArriveeHorsCantonSourcierMixte137Al1(RegDate dateArrivee) throws Exception {
		return createArriveeHorsCantonSourcierMixte137Al1(null, dateArrivee);
	}

	protected PersonnePhysique createArriveeHorsCantonSourcierMixte137Al1(@Nullable final Long noTiers, final RegDate dateArrivee) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(2002, 7, 1), MotifFor.ARRIVEE_HS, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Neuchatel, ModeImposition.SOURCE);
		addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HC, MockCommune.Lausanne, ModeImposition.MIXTE_137_1);
		return paul;
	}

	protected PersonnePhysique createArriveeHorsCantonSourcierMixte137Al1_Invalide(RegDate dateArrivee) throws Exception {
		return createArriveeHorsCantonSourcierMixte137Al1_Invalide(null, dateArrivee);
	}

	protected PersonnePhysique createArriveeHorsCantonSourcierMixte137Al1_Invalide(@Nullable final Long noTiers, final RegDate dateArrivee) throws Exception {
		return doWithoutValidation(
				new ExecuteCallback<PersonnePhysique>() { // [SIFISC-57] désactivation de la validation pour pouvoir construire un cas invalide, mais qui existe des fois tel quel en base de données
					@Override
					public PersonnePhysique execute() throws Exception {
						PersonnePhysique paul = createContribuableSansFor(noTiers);
						addForPrincipal(paul, date(2002, 7, 1), MotifFor.ACHAT_IMMOBILIER, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Neuchatel, ModeImposition.MIXTE_137_1);
						addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HC, MockCommune.Lausanne, ModeImposition.MIXTE_137_1);
						addForSecondaire(paul, date(2002, 7, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.LesClees.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
						return paul;
					}
				});
	}

	protected PersonnePhysique createArriveeHorsCantonSourcierMixte137Al2(RegDate dateArrivee) throws Exception {
		return createArriveeHorsCantonSourcierMixte137Al2(null, dateArrivee);
	}

	protected PersonnePhysique createArriveeHorsCantonSourcierMixte137Al2(@Nullable Long noTiers, RegDate dateArrivee) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), null, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Neuchatel, ModeImposition.SOURCE);
		addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HC, MockCommune.Lausanne, ModeImposition.MIXTE_137_2);
		return paul;
	}

	protected PersonnePhysique createSourcierPur() throws Exception {
		PersonnePhysique paul = createContribuableSansFor();
		addForPrincipal(paul, date(2001, 1, 1), MotifFor.ARRIVEE_HS, date(2002, 2, 21), MotifFor.DEMENAGEMENT_VD, MockCommune.Renens).setModeImposition(ModeImposition.SOURCE);
		addForPrincipal(paul, date(2002, 2, 22), MotifFor.DEMENAGEMENT_VD, date(2002, 9, 26), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Vevey, ModeImposition.SOURCE);
		addForPrincipal(paul, date(2003, 10, 24), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, date(2008, 6, 30), MotifFor.DEMENAGEMENT_VD, MockCommune.Vevey, ModeImposition.SOURCE);
		addForPrincipal(paul, date(2008, 7, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne).setModeImposition(ModeImposition.SOURCE);
		return paul;
	}

	protected PersonnePhysique createSourcierPureHorsSuisse(Long noTiers) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1993, 5, 1), MotifFor.DEBUT_EXPLOITATION, MockPays.Albanie, ModeImposition.SOURCE);
		return paul;
	}

	protected PersonnePhysique createSourcierPureHorsCanton(Long noTiers) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1993, 5, 1), MotifFor.DEBUT_EXPLOITATION, MockCommune.Neuchatel, ModeImposition.SOURCE);
		return paul;
	}

	protected PersonnePhysique createSourcierMixte137Al1HorsCanton_Invalide(final Long noTiers, final RegDate achatImmeuble) throws Exception {
		// [SIFISC-57] désactivation de la validation pour pouvoir construire un cas invalide, mais qui existe des fois tel quel en base de données
		return doWithoutValidation(new ExecuteCallback<PersonnePhysique>() {
			@Override
			public PersonnePhysique execute() throws Exception {
				PersonnePhysique paul = createContribuableSansFor(noTiers);
				addForPrincipal(paul, date(1993, 5, 1), null, achatImmeuble.getOneDayBefore(), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Neuchatel, ModeImposition.SOURCE);
				addForPrincipal(paul, achatImmeuble, MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Neuchatel, ModeImposition.MIXTE_137_1);
				addForSecondaire(paul, achatImmeuble, MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return paul;
			}
		});
	}

	protected PersonnePhysique createSourcierMixte137Al1HorsSuisse_Invalide(final Long noTiers, final RegDate dateAchat) throws Exception {
		// [SIFISC-57] désactivation de la validation pour pouvoir construire un cas invalide, mais qui existe des fois tel quel en base de données
		return doWithoutValidation(new ExecuteCallback<PersonnePhysique>() {
			@Override
			public PersonnePhysique execute() throws Exception {
				PersonnePhysique paul = createContribuableSansFor(noTiers);
				addForPrincipal(paul, date(1993, 5, 1), null, date(2007, 6, 30), MotifFor.CHGT_MODE_IMPOSITION, MockPays.France, ModeImposition.SOURCE);
				addForPrincipal(paul, dateAchat, MotifFor.CHGT_MODE_IMPOSITION, MockPays.France, ModeImposition.MIXTE_137_1);
				addForSecondaire(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return paul;
			}
		});
	}

	protected PersonnePhysique createSourcierPassageMixte137Al2(Long noTiers, RegDate datePassage) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, datePassage.getOneDayBefore(), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne, ModeImposition.SOURCE);
		addForPrincipal(paul, datePassage, MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne, ModeImposition.MIXTE_137_2);
		return paul;
	}

	/**
	 * Sourcier pure vaudois qui passe en mode ordinaire en 2008
	 */
	protected PersonnePhysique createPassageSourceOrdinaire(Long noTiers, RegDate dateChangement, MotifFor motifPassage) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1993, 5, 1), MotifFor.ARRIVEE_HC, dateChangement.getOneDayBefore(), motifPassage, MockCommune.Lausanne, ModeImposition.SOURCE);
		addForPrincipal(paul, dateChangement, motifPassage, MockCommune.Lausanne, ModeImposition.ORDINAIRE);
		return paul;
	}

	/**
	 * Contribuable avec un for principal qui s'ouvre avec un motif 'obtention permis C' sans for fiscal précédent
	 */
	protected PersonnePhysique createPassageSourceOrdinaireImplicite(Long noTiers, RegDate dateChangement) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, dateChangement, MotifFor.PERMIS_C_SUISSE, MockCommune.Lausanne);
		return paul;
	}

	/**
	 * Sourcier mixte 2 vaudois qui passe en mode ordinaire en 2008
	 */
	protected PersonnePhysique createPassageMixteOrdinaire(Long noTiers, RegDate dateChangement, MotifFor motifPassage) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1993, 5, 1), MotifFor.ARRIVEE_HC, dateChangement.getOneDayBefore(), motifPassage, MockCommune.Lausanne, ModeImposition.MIXTE_137_2);
		addForPrincipal(paul, dateChangement, motifPassage, MockCommune.Lausanne, ModeImposition.ORDINAIRE);
		return paul;
	}

	/**
	 * Hors-canton avec immeuble qui décède à la date spécifiée.
	 */
	protected PersonnePhysique createDecesHorsCantonAvecImmeuble(RegDate dateAchat, RegDate dateDeces) throws Exception {
		return createDecesHorsCantonAvecImmeuble(null, dateAchat, dateDeces);
	}

	protected PersonnePhysique createDecesHorsCantonAvecImmeuble(@Nullable Long noTiers, RegDate dateAchat, RegDate dateDeces) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Neuchatel);
		addForSecondaire(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	/**
	 * Hors-Suisse avec immeuble qui décède à la date spécifiée.
	 */
	protected PersonnePhysique createDecesHorsSuisseAvecImmeuble(RegDate dateAchat, RegDate dateDeces) throws Exception {
		PersonnePhysique paul = createContribuableSansFor();
		addForPrincipal(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateDeces, MotifFor.VEUVAGE_DECES, MockPays.Danemark);
		addForSecondaire(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	/**
	 * Hors-canton avec activité indépendante qui décède à la date spécifiée.
	 */
	protected PersonnePhysique createDecesHorsCantonActiviteIndependante(RegDate debutExploitation, RegDate dateDeces) throws Exception {
		return createDecesHorsCantonActiviteIndependante(null, debutExploitation, dateDeces);
	}

	protected PersonnePhysique createDecesHorsCantonActiviteIndependante(@Nullable Long noTiers, RegDate debutExploitation, RegDate dateDeces) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, debutExploitation, MotifFor.DEBUT_EXPLOITATION, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Neuchatel);
		addForSecondaire(paul, debutExploitation, MotifFor.DEBUT_EXPLOITATION, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Leysin.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		return paul;
	}

	protected PersonnePhysique createDecesVaudoisOrdinaire(RegDate dateMajorite, RegDate dateDeces) throws Exception {
		PersonnePhysique paul = createContribuableSansFor();
		addForPrincipal(paul, dateMajorite, MotifFor.MAJORITE, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
		return paul;
	}

	protected PersonnePhysique createDecesVaudoisDepense(RegDate dateArrivee, RegDate dateDeces) throws Exception {
		PersonnePhysique paul = createContribuableSansFor();
		addForPrincipal(paul, dateArrivee, MotifFor.ARRIVEE_HS, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne, ModeImposition.DEPENSE);
		return paul;
	}

	protected PersonnePhysique createHorsCantonAvecImmeuble(RegDate dateAchat) throws Exception {
		return createHorsCantonAvecImmeuble(null, dateAchat);
	}

	protected PersonnePhysique createHorsCantonAvecImmeuble(@Nullable Long noTiers, RegDate dateAchat) throws Exception {
		PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Neuchatel);
		addForSecondaire(paul, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return paul;
	}

	protected PersonnePhysique createVenteImmeubleHorsCanton(RegDate dateVente) throws Exception {
		return createVenteImmeubleHorsCanton(null, dateVente);
	}

	protected PersonnePhysique createVenteImmeubleHorsCanton(@Nullable Long noTiers, RegDate dateVente) throws Exception {
		PersonnePhysique ctb = createContribuableSansFor(noTiers);
		addForPrincipal(ctb, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Neuchatel);
		addForSecondaire(ctb, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Aubonne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return ctb;
	}

	protected PersonnePhysique createFinActiviteHorsCanton(RegDate dateFin) throws Exception {
		return createFinActiviteHorsCanton(null, dateFin);
	}

	protected PersonnePhysique createFinActiviteHorsCanton(@Nullable Long noTiers, RegDate dateFin) throws Exception {
		PersonnePhysique ctb = createContribuableSansFor(noTiers);
		addForPrincipal(ctb, date(2000, 1, 1), MotifFor.DEBUT_EXPLOITATION, MockCommune.Neuchatel);
		addForSecondaire(ctb, date(2000, 1, 1), MotifFor.DEBUT_EXPLOITATION, dateFin, MotifFor.FIN_EXPLOITATION, MockCommune.Aubonne.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		return ctb;
	}

	protected PersonnePhysique createOrdinairePuisSourcierCasLimite(@Nullable Long noTiers) throws Exception {
		final PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2006, 1, 15), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne);
		addForPrincipal(paul, date(2006, 1, 16), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne, ModeImposition.SOURCE);
		return paul;
	}

	protected PersonnePhysique createSourcierPuisOrdinaireCasLimite(Long noTiers) throws Exception {
		final PersonnePhysique paul = createContribuableSansFor(noTiers);
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2006, 12, 16), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne, ModeImposition.SOURCE);
		addForPrincipal(paul, date(2006, 12, 17), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne);
		return paul;
	}

	protected PersonnePhysique createIndigentAvecDIs(int periodePrecedente, TypeDocument typeDIsPrecedentes) {

		final PeriodeFiscale periode = addPeriodeFiscale(periodePrecedente);
		final ModeleDocument declarationVaudTax = addModeleDocument(typeDIsPrecedentes, periode);

		final PersonnePhysique ctb = addNonHabitant("Marc", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
		ctb.setOfficeImpotId(MockOfficeImpot.OID_LAUSANNE_VILLE.getNoColAdm());
		addForPrincipal(ctb, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne, ModeImposition.INDIGENT);

		final DeclarationImpotOrdinaire decl = addDeclarationImpot(ctb, periode, date(periodePrecedente, 1, 1), date(periodePrecedente, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, declarationVaudTax);
		addEtatDeclarationEmise(decl, date(periodePrecedente + 1, 1, 15));
		addEtatDeclarationRetournee(decl, date(periodePrecedente + 1, 1, 15));
		return ctb;
	}

	protected void assertOrdinaire(RegDate debut, @Nullable RegDate fin, @Nullable MotifAssujettissement motifFractDebut, @Nullable MotifAssujettissement motifFractFin, Assujettissement assujettissement) {
		assertNotNull(assujettissement);
		assertInstanceOf(VaudoisOrdinaire.class, assujettissement);
		assertEquals(debut, assujettissement.getDateDebut());
		assertEquals(fin, assujettissement.getDateFin());
		assertEquals(motifFractDebut, assujettissement.getMotifFractDebut());
		assertEquals(motifFractFin, assujettissement.getMotifFractFin());
	}

	protected void assertDepense(RegDate debut, @Nullable RegDate fin, @Nullable MotifAssujettissement motifFractDebut, @Nullable MotifAssujettissement motifFractFin, Assujettissement assujettissement) {
		assertNotNull(assujettissement);
		assertInstanceOf(VaudoisDepense.class, assujettissement);
		assertEquals(debut, assujettissement.getDateDebut());
		assertEquals(fin, assujettissement.getDateFin());
		assertEquals(motifFractDebut, assujettissement.getMotifFractDebut());
		assertEquals(motifFractFin, assujettissement.getMotifFractFin());
	}

	protected void assertDiplomateSuisse(RegDate debut, RegDate fin, @Nullable MotifAssujettissement motifFractDebut, @Nullable MotifAssujettissement motifFractFin, Assujettissement assujettissement) {
		assertNotNull(assujettissement);
		assertInstanceOf(DiplomateSuisse.class, assujettissement);
		assertEquals(debut, assujettissement.getDateDebut());
		assertEquals(fin, assujettissement.getDateFin());
		assertEquals(motifFractDebut, assujettissement.getMotifFractDebut());
		assertEquals(motifFractFin, assujettissement.getMotifFractFin());
	}

	protected void assertHorsCanton(RegDate debut, @Nullable RegDate fin, @Nullable MotifAssujettissement motifFractDebut, @Nullable MotifAssujettissement motifFractFin, Assujettissement assujettissement) {
		assertNotNull(assujettissement);
		assertInstanceOf(HorsCanton.class, assujettissement);
		assertEquals(debut, assujettissement.getDateDebut());
		assertEquals(fin, assujettissement.getDateFin());
		assertEquals(motifFractDebut, assujettissement.getMotifFractDebut());
		assertEquals(motifFractFin, assujettissement.getMotifFractFin());
	}

	protected void assertHorsSuisse(RegDate debut, @Nullable RegDate fin, @Nullable MotifAssujettissement motifFractDebut, @Nullable MotifAssujettissement motifFractFin, Assujettissement assujettissement) {
		assertNotNull(assujettissement);
		assertInstanceOf(HorsSuisse.class, assujettissement);
		assertEquals(debut, assujettissement.getDateDebut());
		assertEquals(fin, assujettissement.getDateFin());
		assertEquals(motifFractDebut, assujettissement.getMotifFractDebut());
		assertEquals(motifFractFin, assujettissement.getMotifFractFin());
	}

	protected void assertSourcierPur(RegDate debut, @Nullable RegDate fin, @Nullable MotifAssujettissement motifFractDebut, @Nullable MotifAssujettissement motifFractFin, TypeAutoriteFiscale typeAutorite, Assujettissement assujettissement) {
		assertNotNull(assujettissement);
		assertInstanceOf(SourcierPur.class, assujettissement);
		final SourcierPur a = (SourcierPur) assujettissement;
		assertEquals(debut, assujettissement.getDateDebut());
		assertEquals(fin, assujettissement.getDateFin());
		assertEquals(motifFractDebut, assujettissement.getMotifFractDebut());
		assertEquals(motifFractFin, assujettissement.getMotifFractFin());
		assertEquals(typeAutorite, a.getTypeAutoriteFiscalePrincipale());
	}

	protected void assertSourcierMixteArt137Al1(RegDate debut, @Nullable RegDate fin, @Nullable MotifAssujettissement motifFractDebut, @Nullable MotifAssujettissement motifFractFin, TypeAutoriteFiscale typeAutorite, Assujettissement assujettissement) {
		assertNotNull(assujettissement);
		assertInstanceOf(SourcierMixteArt137Al1.class, assujettissement);
		final SourcierMixteArt137Al1 a = (SourcierMixteArt137Al1) assujettissement;
		assertEquals(debut, assujettissement.getDateDebut());
		assertEquals(fin, assujettissement.getDateFin());
		assertEquals(motifFractDebut, assujettissement.getMotifFractDebut());
		assertEquals(motifFractFin, assujettissement.getMotifFractFin());
		assertEquals(typeAutorite, a.getTypeAutoriteFiscalePrincipale());
	}

	protected void assertSourcierMixteArt137Al2(RegDate debut, @Nullable MotifAssujettissement motifFractDebut, TypeAutoriteFiscale typeAutorite, Assujettissement assujettissement) {
		assertNotNull(assujettissement);
		assertInstanceOf(SourcierMixteArt137Al2.class, assujettissement);
		final SourcierMixteArt137Al2 a = (SourcierMixteArt137Al2) assujettissement;
		assertEquals(debut, assujettissement.getDateDebut());
		assertNull(assujettissement.getDateFin());
		assertEquals(motifFractDebut, assujettissement.getMotifFractDebut());
		assertNull(assujettissement.getMotifFractFin());
		assertEquals(typeAutorite, a.getTypeAutoriteFiscalePrincipale());
	}

	protected void assertSourcierMixteArt137Al2(RegDate debut, @Nullable RegDate fin, @Nullable MotifAssujettissement motifFractDebut, @Nullable MotifAssujettissement motifFractFin, TypeAutoriteFiscale typeAutorite, Assujettissement assujettissement) {
		assertNotNull(assujettissement);
		assertInstanceOf(SourcierMixteArt137Al2.class, assujettissement);
		final SourcierMixteArt137Al2 a = (SourcierMixteArt137Al2) assujettissement;
		assertEquals(debut, assujettissement.getDateDebut());
		assertEquals(fin, assujettissement.getDateFin());
		assertEquals(motifFractDebut, assujettissement.getMotifFractDebut());
		assertEquals(motifFractFin, assujettissement.getMotifFractFin());
		assertEquals(typeAutorite, a.getTypeAutoriteFiscalePrincipale());
	}

	protected void assertPeriodeImpositionPersonnesPhysiques(RegDate debut, RegDate fin, CategorieEnvoiDIPP categorie, @Nullable TypeAdresseRetour adresseRetour, boolean optionnelle,
	                                                         boolean remplaceParNote, boolean fermetureCauseDeces, boolean fermetureCauseFinAssujettissementHS, PeriodeImposition periode) {

		assertNotNull(periode);
		assertInstanceOf(PeriodeImpositionPersonnesPhysiques.class, periode);

		final PeriodeImpositionPersonnesPhysiques pipp = (PeriodeImpositionPersonnesPhysiques) periode;

		assertEquals(debut, pipp.getDateDebut());
		assertEquals(fin, pipp.getDateFin());
		assertEquals(categorie, pipp.getCategorieEnvoiDI());
		assertEquals(adresseRetour, pipp.getAdresseRetour());
		assertEquals(optionnelle, pipp.isDeclarationOptionnelle());
		assertEquals(remplaceParNote, pipp.isDeclarationRemplaceeParNote());
		assertEquals(fermetureCauseDeces, pipp.getCauseFermeture() == PeriodeImposition.CauseFermeture.VEUVAGE_DECES);
		assertEquals(fermetureCauseFinAssujettissementHS, pipp.getCauseFermeture() == PeriodeImposition.CauseFermeture.FIN_ASSUJETTISSEMENT_HS);
	}

	protected void assertPeriodeImpositionPersonnesMorales(RegDate debut, RegDate fin, boolean optionnelle, TypeContribuable typeContribuable,
	                                                       boolean remplaceParNote, boolean fermetureCauseDeces, boolean fermetureCauseFinAssujettissementHS, PeriodeImposition periode) {

		assertNotNull(periode);
		assertInstanceOf(PeriodeImpositionPersonnesMorales.class, periode);

		final PeriodeImpositionPersonnesMorales pipm = (PeriodeImpositionPersonnesMorales) periode;

		assertEquals(debut, pipm.getDateDebut());
		assertEquals(fin, pipm.getDateFin());
		assertEquals(optionnelle, pipm.isDeclarationOptionnelle());
		assertEquals(typeContribuable, pipm.getTypeContribuable());
		assertEquals(remplaceParNote, pipm.isDeclarationRemplaceeParNote());
		assertEquals(fermetureCauseDeces, pipm.getCauseFermeture() == PeriodeImposition.CauseFermeture.VEUVAGE_DECES);
		assertEquals(fermetureCauseFinAssujettissementHS, pipm.getCauseFermeture() == PeriodeImposition.CauseFermeture.FIN_ASSUJETTISSEMENT_HS);
	}

	protected PersonnePhysique createSourcierPurePuisMixteSurDepartHSPuisArriveeHSDansLAnneeAvecImmeubleEtMotifsGrandguignolesques_Invalide() throws Exception {
		// [SIFISC-57] désactivation de la validation pour pouvoir construire un cas invalide, mais qui existe des fois tel quel en base de données
		return doWithoutValidation(new ExecuteCallback<PersonnePhysique>() {
			@Override
			public PersonnePhysique execute() throws Exception {
				PersonnePhysique paul = createContribuableSansFor(10003348L);
				addForPrincipal(paul, date(2003, 1, 1), MotifFor.ARRIVEE_HS, date(2003, 5, 27), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne, ModeImposition.SOURCE);
				addForPrincipal(paul, date(2003, 5, 28), MotifFor.INDETERMINE, date(2003, 8, 30), MotifFor.DEMENAGEMENT_VD, MockPays.France, ModeImposition.MIXTE_137_1);
				addForPrincipal(paul, date(2003, 8, 31), MotifFor.ARRIVEE_HS, MockCommune.Lausanne, ModeImposition.MIXTE_137_2);
				addForSecondaire(paul, date(2003, 5, 28), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return paul;
			}
		});
	}

	protected PersonnePhysique createFauxDemenagementVD_Invalide() throws Exception {
		// [SIFISC-57] désactivation de la validation pour pouvoir construire un cas invalide, mais qui existe des fois tel quel en base de données
		return doWithoutValidation(new ExecuteCallback<PersonnePhysique>() {
			@Override
			public PersonnePhysique execute() throws Exception {
				PersonnePhysique paul = createContribuableSansFor(10002080L);

				// le motif de fermeture est incorrect, il devrait être ARRIVEE_HC
				addForPrincipal(paul, date(2003, 9, 9), MotifFor.INDETERMINE, date(2004, 7, 26), MotifFor.DEMENAGEMENT_VD, MockCommune.Neuchatel, ModeImposition.MIXTE_137_1);
				addForPrincipal(paul, date(2004, 7, 27), MotifFor.ARRIVEE_HC, MockCommune.Lausanne, ModeImposition.MIXTE_137_2);
				addForSecondaire(paul, date(2003, 9, 9), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return paul;
			}
		});
	}

	protected PersonnePhysique createSourcierMixteHorsSuisseAvecImmeuble_Invalide() throws Exception {
		// [SIFISC-57] désactivation de la validation pour pouvoir construire un cas invalide, mais qui existe des fois tel quel en base de données
		return doWithoutValidation(new ExecuteCallback<PersonnePhysique>() {
			@Override
			public PersonnePhysique execute() throws Exception {
				PersonnePhysique paul = createContribuableSansFor(10015452L);
				addForPrincipal(paul, date(2004, 5, 6), MotifFor.INDETERMINE, date(2006, 7, 31), MotifFor.DEMENAGEMENT_VD, MockPays.Espagne, ModeImposition.MIXTE_137_1);
				addForPrincipal(paul, date(2006, 8, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne, ModeImposition.MIXTE_137_2);
				addForSecondaire(paul, date(2004, 5, 6), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return paul;
			}
		});
	}
}
