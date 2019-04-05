package ch.vd.unireg.tiers;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.hibernate.cfg.NotYetImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.shared.validation.ValidationException;
import ch.vd.unireg.adresse.AdresseMandataire;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.common.DonneesCivilesException;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.linkedentity.LinkedEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;
import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.metier.assujettissement.Assujettissement;
import ch.vd.unireg.metier.bouclement.ExerciceCommercial;
import ch.vd.unireg.regimefiscal.FormeJuridiqueVersTypeRegimeFiscalMapping;
import ch.vd.unireg.tiers.rattrapage.flaghabitant.CorrectionFlagHabitantResults;
import ch.vd.unireg.type.CategorieEntreprise;
import ch.vd.unireg.type.CategorieEtranger;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.FormeJuridique;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeCommunication;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.PeriodeDecompte;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.StatutMenageCommun;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.type.TypeFlagEntreprise;
import ch.vd.unireg.type.TypeGenerationEtatEntreprise;
import ch.vd.unireg.type.TypeRapportEntreTiers;

public class MockTiersService implements TiersService {

	private Tiers tiers;

	private boolean isSuisse;

	private boolean isEtrangerSansPermisC;

	public MockTiersService() {
	}

	public MockTiersService(Tiers tiers) {
		this.tiers = tiers;
	}


	@Override
	public List<TiersIndexedData> search(TiersCriteria tiersCriteria) throws IndexerException {
		return null;
	}

	@Override
	public PersonnePhysique getPersonnePhysiqueByNumeroIndividu(long numeroIndividu) {
		return null;
	}

	@Override
	public Entreprise getEntrepriseByNoEntrepriseCivile(long numeroEntrepriseCivile) {
		return null;
	}

	@Override
	public Etablissement getEtablissementByNumeroEtablissementCivil(long numeroEtablissement) {
		return null;
	}

	@Override
	public List<DateRanged<Etablissement>> getEtablissementsPrincipauxEntreprise(Entreprise entreprise) {
		return null;
	}

	@Override
	public List<DateRanged<Etablissement>> getEtablissementsSecondairesEntreprise(Entreprise entreprise) {
		return null;
	}

	@Override
	public List<Etablissement> getEtablissementsSecondairesEntrepriseSansRange(Entreprise entreprise) {
		return null;
	}

	@Override
	public List<Etablissement> getEtablissementsSecondairesEntreprise(Entreprise entreprise, RegDate date) {
		return null;
	}

	@Override
	public Entreprise createEntreprisePourEvenement(EvenementEntreprise evt) throws TiersException {
		return null;
	}

	@Override
	public @NotNull Entreprise createEntreprise(long noEntrepriseCivile) {
		return null;
	}

	@Override
	public @NotNull Entreprise createEntreprise(String numeroIde, RegDate dateDebutValidite, RegDate dateDebutExerciceCommercial, RegDate dateFondation, FormeJuridiqueEntreprise formeJuridique, Long capitalLibere, String devise, String raisonSociale,
	                                            TypeAutoriteFiscale typeAutoriteFiscaleSiege, Integer numeroOfsSiege, boolean entrepriseInscriteRC, String personneContact, String complementNom, String numeroTelephonePrive,
	                                            String numeroTelephonePortable, String numeroTelephoneProfessionnel, String numeroTelecopie, String adresseCourrierElectronique, String iban, String adresseBicSwift, String titulaire) {
		return null;
	}

	@Override
	public @NotNull Etablissement createEtablissement(Long numeroEtablissementCivil) {
		return null;
	}

	@NotNull
	@Override
	public Etablissement createEtablissement(long noEntreprise, String raisonSociale, String nomEnseigne, RegDate dateDebutValidite, RegDate dateFinValidite, Integer noOfsCommuneDomicile, String numeroIDE, String personneContact,
	                                         String complementNom, String numeroTelephonePrive, String numeroTelephonePortable, String numeroTelephoneProfessionnel, String numeroTelecopie, String adresseCourrierElectronique, String iban,
	                                         String adresseBicSwift, String titulaireCompteBancaire) {
		return null;
	}

	@Override
	public List<DateRanged<Contribuable>> getEntitesJuridiquesEtablissement(Etablissement etablissement) {
		return null;
	}

	@Override
	public Tiers getTiers(long numeroTiers) {
		return null;
	}

	@Override
	public void setIdentifiantsPersonne(PersonnePhysique nonHabitant, String navs11, String numRce) {

	}

	@Override
	public void setIdentifiantEntreprise(Contribuable contribuable, String ide) {

	}

	@Override
	public void checkEditionCivileAutorisee(Contribuable contribuable) {

	}

	@Override
	public void changeNHenMenage(long numeroTiers) {

	}

	@Override
	public @NotNull PersonnePhysique createNonHabitantFromIndividu(long numeroIndividu) {
		return null;
	}

	@Override
	public @NotNull PersonnePhysique createNonHabitant(String nom, String nomNaissance, String prenomUsuel, String tousPrenoms, String numeroAssureSocial, Sexe sexe, RegDate dateNaissance, RegDate dateDeces, CategorieEtranger categorieEtranger,
	                                                   RegDate dateDebutValiditeAutorisation, Integer numeroOfsNationalite, Integer ofsCommuneOrigine, String libelleCommuneOrigine, String prenomsPere, String nomPere, String prenomsMere,
	                                                   String nomMere, String ancienNumAVS, String numRegistreEtranger, String personneContact, String complementNom, String numeroTelephonePrive, String numeroTelephonePortable,
	                                                   String numeroTelephoneProfessionnel, String numeroTelecopie, String adresseCourrierElectronique, String iban, String adresseBicSwift, String titulaireCompteBancaire) {
		return null;
	}

	@Override
	public @NotNull DebiteurPrestationImposable createDebiteur(long noCtbContactIS, CategorieImpotSource categorieImpotSource, ModeCommunication modeCommunication, PeriodiciteDecompte periodiciteDecompte, PeriodeDecompte periodeDecompte,
	                                                           String personneContact, String complementNom, String numeroTelephonePrive, String numeroTelephonePortable, String numeroTelephoneProfessionnel, String numeroTelecopie,
	                                                           String adresseCourrierElectronique, String iban, String adresseBicSwift, String titulaireCompteBancaire) {
		return null;
	}

	@Override
	public @NotNull AutreCommunaute createAutreCommunaute(String nom, String ide, FormeJuridique formeJuridique, String personneContact, String complementNom, String numeroTelephonePrive, String numeroTelephonePortable,
	                                                      String numeroTelephoneProfessionnel, String numeroTelecopie, String adresseCourrierElectronique, String iban, String adresseBicSwift, String titulaireCompteBancaire) {
		return null;
	}

	@Override
	public @NotNull List<DateRange> getPeriodesDeResidence(PersonnePhysique pp, boolean residencePrincipaleSeulement) throws DonneesCivilesException {
		return null;
	}

	@Override
	public StatutMenageCommun getStatutMenageCommun(MenageCommun menageCommun) {
		return null;
	}

	@Override
	public List<FormeLegaleHisto> getFormesLegales(@NotNull Entreprise entreprise, boolean aussiAnnule) {
		return null;
	}

	@Override
	public List<RaisonSocialeHisto> getRaisonsSociales(@NotNull Entreprise entreprise, boolean avecAnnulees) {
		return null;
	}

	@Override
	public List<DomicileHisto> getSieges(@NotNull Entreprise entreprise, boolean aussiAnnules) {
		return null;
	}

	@Override
	public List<DomicileHisto> getDomiciles(@NotNull Etablissement etablissement, boolean aussiAnnules) {
		return null;
	}

	@Override
	public List<DomicileHisto> getDomicilesEnActiviteSourceReelle(@NotNull Etablissement etablissement, boolean aussiAnnules) {
		return null;
	}

	@Override
	public boolean isInscriteRC(@NotNull Entreprise entreprise, RegDate dateReference) {
		return false;
	}

	@Override
	public boolean hasInscriptionActiveRC(@NotNull Entreprise entreprise, RegDate dateReference) {
		return false;
	}

	@Override
	public List<TypeEtatEntreprise> getTransitionsEtatEntrepriseDisponibles(Entreprise entreprise, RegDate date, TypeGenerationEtatEntreprise generation) {
		return null;
	}

	@Override
	public EtatEntreprise changeEtatEntreprise(TypeEtatEntreprise type, Entreprise entreprise, RegDate date, TypeGenerationEtatEntreprise generation) {
		return null;
	}

	@Override
	public void annuleEtatEntreprise(EtatEntreprise etatEntreprise) {

	}

	@Override
	public Entreprise getEntreprise(Etablissement etablissement, RegDate date) {
		return null;
	}

	@Override
	public Etablissement getEtablissementPrincipal(Entreprise entreprise, RegDate date) {
		return null;
	}

	@Override
	public void apparier(Entreprise entreprise, EntrepriseCivile entrepriseCivile, boolean fermerSurcharges) {

	}

	@Override
	public void apparier(Etablissement etablissement, EtablissementCivil etablissementCivil) {

	}

	@Override
	public void fermeSurchargesCiviles(Entreprise entreprise, RegDate finFiscale) {

	}

	@Override
	public void fermeSurchargesCiviles(Etablissement etablissement, RegDate finFiscale) {

	}

	@Override
	public DegreAssociationRegistreCivil determineDegreAssociationCivil(Entreprise entreprise, RegDate date) {
		return null;
	}

	@Override
	public DegreAssociationRegistreCivil determineDegreAssociationCivil(Etablissement etablissement, RegDate date) {
		return null;
	}

	@Override
	public String getLocalisationAsString(LocalizedDateRange localisation) throws ServiceInfrastructureException, ObjectNotFoundException {
		return null;
	}

	@Override
	public boolean existRapportEntreTiers(TypeRapportEntreTiers typeRapport, Contribuable tiersObjet, Contribuable tiersSujet, RegDate dateDebutLien) {
		return false;
	}

	@Override
	public UpdateHabitantFlagResultat updateHabitantFlag(@NotNull PersonnePhysique pp, long noInd, @Nullable Long numeroEvenement) throws TiersException {
		return null;
	}

	@Override
	public UpdateHabitantFlagResultat updateHabitantStatus(@NotNull PersonnePhysique pp, long noInd, @Nullable RegDate date, @Nullable Long numeroEvenement) throws TiersException {
		return null;
	}

	@Override
	public Boolean isHabitantResidencePrincipale(@NotNull PersonnePhysique pp, RegDate date) {
		return null;
	}

	@Override
	public List<PersonnePhysique> getEnfantsForDeclaration(Contribuable ctb, RegDate finPeriodeImposition) {
		return null;
	}

	@Override
	public ContribuableImpositionPersonnesPhysiques getAutoriteParentaleDe(PersonnePhysique contribuableEnfant, RegDate dateValidite) {
		return null;
	}

	@Override
	public CollectiviteAdministrative getOfficeImpot(int noTechnique) {
		return null;
	}

	@Override
	public CollectiviteAdministrative getCollectiviteAdministrative(int noTechnique) {
		return null;
	}

	@Override
	public CollectiviteAdministrative getCollectiviteAdministrative(int noTechnique, boolean doNotAutoFlush) {
		return null;
	}

	@Override
	public CollectiviteAdministrative getOrCreateCollectiviteAdministrative(int noTechnique) {
		return null;
	}

	@Override
	public CollectiviteAdministrative getOrCreateCollectiviteAdministrative(int noTechnique, boolean doNotAutoFlush) {
		return null;
	}

	@Override
	public Individu getIndividu(@NotNull PersonnePhysique personne) {
		return null;
	}

	@Override
	public Individu getIndividu(PersonnePhysique personne, RegDate date, AttributeIndividu... attributes) {
		return null;
	}

	@Override
	public boolean isSuisse(PersonnePhysique pp, RegDate date) throws TiersException {
		return this.isSuisse;
	}

	public void setIsSuisse(boolean bool) {
		this.isSuisse = bool;
	}

	@Override
	public boolean isSuisseOuPermisC(PersonnePhysique pp, RegDate dateEvenement) throws TiersException {
		return false;
	}

	@Override
	public boolean isSuisseOuPermisC(long numeroIndividu, RegDate date) throws TiersException {
		return false;
	}

	@Override
	public boolean isSuisse(Individu individu, @Nullable RegDate date) throws TiersException {
		return this.isSuisse;
	}

	@Override
	public boolean isAvecPermisC(Individu individu) {
		return false;
	}

	@Override
	public boolean isAvecPermisC(Individu individu, RegDate date) {
		return false;
	}

	@Override
	public boolean isHabitantEtrangerAvecPermisC(PersonnePhysique habitant, RegDate date) throws TiersException {
		return false;
	}

	@Override
	public boolean isEtrangerSansPermisC(PersonnePhysique pp, @Nullable RegDate date) throws TiersException {
		return this.isEtrangerSansPermisC;
	}

	public void setEtrangerSansPermisC(boolean bool) {
		this.isEtrangerSansPermisC = bool;
	}

	@Override
	public PersonnePhysique getPrincipal(@Nullable PersonnePhysique tiers1, @Nullable PersonnePhysique tiers2) {
		return null;
	}

	@Override
	public PersonnePhysique getPrincipal(MenageCommun menageCommun) {
		return null;
	}

	@Override
	public MenageCommun findMenageCommun(PersonnePhysique personne, @Nullable RegDate date) {
		return null;
	}

	@Override
	public MenageCommun findDernierMenageCommun(PersonnePhysique personne) {
		return null;
	}

	@Override
	public boolean isInMenageCommun(PersonnePhysique personne, @Nullable RegDate date) {
		return false;
	}

	@Override
	public EnsembleTiersCouple getEnsembleTiersCouple(MenageCommun menageCommun, @Nullable RegDate date) {
		return null;
	}

	@Override
	public EnsembleTiersCouple getEnsembleTiersCouple(MenageCommun menageCommun, int anneePeriode) {
		return null;
	}

	@Override
	public EnsembleTiersCouple getEnsembleTiersCouple(PersonnePhysique personne, @Nullable RegDate date) {
		return null;
	}

	@Override
	public List<EnsembleTiersCouple> getEnsembleTiersCouple(PersonnePhysique personne, int anneePeriode) {
		return null;
	}

	@Override
	public RapportEntreTiers addTiersToCouple(MenageCommun menage, PersonnePhysique tiers, RegDate dateDebut, @Nullable RegDate dateFin) {
		return null;
	}

	@Override
	public void closeAppartenanceMenage(PersonnePhysique pp, MenageCommun menage, RegDate dateFermeture) throws RapportEntreTiersException {

	}

	@Override
	public void closeAllRapports(PersonnePhysique pp, RegDate dateFermeture, Predicate<RapportEntreTiers> sauf) {

	}

	@Override
	public RapportPrestationImposable addRapportPrestationImposable(PersonnePhysique sourcier, DebiteurPrestationImposable debiteur, RegDate dateDebut, RegDate dateFin) {
		return null;
	}

	@Override
	public RapportEntreTiers addContactImpotSource(DebiteurPrestationImposable debiteur, Contribuable contribuable) {
		return null;
	}

	@Override
	public RapportEntreTiers addContactImpotSource(DebiteurPrestationImposable debiteur, Contribuable contribuable, RegDate dateDebut) {
		return null;
	}

	@Override
	public RapportEntreTiers addActiviteEconomique(Etablissement etablissement, Contribuable contribuable, RegDate dateDebut, boolean principal) {
		return null;
	}

	@Override
	public EnsembleTiersCouple createEnsembleTiersCouple(PersonnePhysique tiers1, @Nullable PersonnePhysique tiers2, RegDate dateDebut, @Nullable RegDate dateFin) {
		return null;
	}

	@Override
	public RapportEntreTiers addRapport(RapportEntreTiers rapport, Tiers sujet, Tiers objet) {
		return null;
	}

	@Override
	public Sexe getSexe(PersonnePhysique pp) {
		return null;
	}

	@Override
	public boolean isMemeSexe(PersonnePhysique pp1, PersonnePhysique pp2) {
		return false;
	}

	@Override
	public ForFiscalPrincipalPP openForFiscalPrincipal(ContribuableImpositionPersonnesPhysiques contribuable, RegDate dateOuverture, MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale,
	                                                   ModeImposition modeImposition, MotifFor motifOuverture) {
		return null;
	}

	@Override
	public ForFiscalPrincipalPM openForFiscalPrincipal(ContribuableImpositionPersonnesMorales contribuable, RegDate dateOuverture, MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale,
	                                                   MotifFor motifOuverture, GenreImpot genreImpot) {
		return null;
	}

	@Override
	public ForFiscalSecondaire openForFiscalSecondaire(Contribuable contribuable, RegDate dateOuverture, MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale, MotifFor motifOuverture,
	                                                   GenreImpot genreImpot) {
		return null;
	}

	@Override
	public ForFiscalAutreElementImposable openForFiscalAutreElementImposable(Contribuable contribuable, GenreImpot genreImpot, RegDate dateOuverture, MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale, MotifFor motifOuverture) {
		return null;
	}

	@Override
	public ForFiscalAutreElementImposable openForFiscalAutreElementImposable(Contribuable contribuable, RegDate dateOuverture, MotifFor motifOuverture, @Nullable RegDate dateFermeture, @Nullable MotifFor motifFermeture,
	                                                                         MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale) {
		return null;
	}

	@Override
	public ForFiscalAutreImpot openForFiscalAutreImpot(Contribuable contribuable, GenreImpot genreImpot, RegDate dateImpot, int numeroOfsAutoriteFiscale) {
		return null;
	}

	@Override
	public ForDebiteurPrestationImposable openForDebiteurPrestationImposable(DebiteurPrestationImposable debiteur, RegDate dateOuverture, MotifFor motifOuverture, int numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale) {
		return null;
	}

	@Override
	public void reopenRapportsPrestation(DebiteurPrestationImposable debiteur, RegDate dateDesactivation, RegDate dateReactivation) {

	}

	@Override
	public void reopenRapportsEntreTiers(Tiers tiers, RegDate dateFermeture, Set<TypeRapportEntreTiers> typesRapportsSujet, Set<TypeRapportEntreTiers> typesRapportsObjet) {

	}

	@Override
	public ForFiscalPrincipal closeForFiscalPrincipal(Contribuable contribuable, RegDate dateFermeture, MotifFor motifFermeture) {
		return null;
	}

	@Override
	public <F extends ForFiscalPrincipal> F closeForFiscalPrincipal(F forFiscalPrincipal, RegDate dateFermeture, MotifFor motifFermeture) {
		return null;
	}

	@Override
	public DecisionAci closeDecisionAci(DecisionAci decision, RegDate dateFin) {
		return null;
	}

	@Override
	public ForFiscalSecondaire closeForFiscalSecondaire(Contribuable contribuable, ForFiscalSecondaire forFiscalSecondaire, RegDate dateFermeture, MotifFor motifFermeture) {
		return null;
	}

	@Override
	public ForFiscalAutreElementImposable closeForFiscalAutreElementImposable(Contribuable contribuable, ForFiscalAutreElementImposable forFiscalAutreElementImposable, RegDate dateFermeture, MotifFor motifFermeture) {
		return null;
	}

	@Override
	public ForDebiteurPrestationImposable closeForDebiteurPrestationImposable(DebiteurPrestationImposable debiteur, ForDebiteurPrestationImposable forDebiteurPrestationImposable, RegDate dateFermeture, MotifFor motifFermeture,
	                                                                          boolean fermerRapportsPrestation) {
		return null;
	}

	@Override
	public void closeForAutreImpot(ForFiscalAutreImpot autre, RegDate dateFermeture) {

	}

	@Override
	public void closeAllForsFiscaux(Contribuable contribuable, RegDate dateFermeture, MotifFor motifFermeture) {

	}

	@Override
	public ForFiscalPrincipalPP changeModeImposition(ContribuableImpositionPersonnesPhysiques contribuable, RegDate dateChangementModeImposition, ModeImposition modeImposition, MotifFor motifFor) {
		return null;
	}

	@Override
	public ForFiscalPrincipalPP addForPrincipal(ContribuableImpositionPersonnesPhysiques contribuable, RegDate dateDebut, MotifFor motifOuverture, @Nullable RegDate dateFin, @Nullable MotifFor motifFermeture, MotifRattachement motifRattachement,
	                                            int autoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale, ModeImposition modeImposition) {
		return null;
	}

	@Override
	public ForFiscalPrincipalPM addForPrincipal(ContribuableImpositionPersonnesMorales contribuable, RegDate dateDebut, MotifFor motifOuverture, @Nullable RegDate dateFin, @Nullable MotifFor motifFermeture, MotifRattachement motifRattachement,
	                                            int autoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale, GenreImpot genreImpot) {
		return null;
	}

	@Override
	public @Nullable ForFiscalSecondaire updateForSecondaire(ForFiscalSecondaire ffs, RegDate dateOuverture, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture, int noOfsAutoriteFiscale) {
		return null;
	}

	@Override
	public @Nullable ForFiscalPrincipal updateForPrincipal(ForFiscalPrincipal ffp, RegDate dateFermeture, MotifFor motifFermeture, int noOfsAutoriteFiscale) {
		return null;
	}

	@Override
	public @Nullable ForFiscalAutreElementImposable updateForAutreElementImposable(ForFiscalAutreElementImposable ffaei, RegDate dateFermeture, MotifFor motifFermeture, Integer noOfsAutoriteFiscale) {
		return null;
	}

	@Override
	public @Nullable ForDebiteurPrestationImposable updateForDebiteur(ForDebiteurPrestationImposable fdpi, RegDate dateFermeture, MotifFor motifFermeture) {
		return null;
	}

	@Override
	public void annuleForsOuvertsAu(Contribuable contribuable, RegDate dateOuverture, MotifFor motifOuverture) {

	}

	@Override
	public Periodicite addPeriodicite(DebiteurPrestationImposable debiteur, PeriodiciteDecompte periodiciteDecompte, @Nullable PeriodeDecompte periodeDecompte, RegDate dateDebut, @Nullable RegDate dateFin) {
		return null;
	}

	@Override
	public ForFiscalSecondaire addForSecondaire(Contribuable contribuable, RegDate dateOuverture, RegDate dateFermeture, MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale,
	                                            MotifFor motifOuverture, MotifFor motifFermeture, GenreImpot genreImpot) {
		return null;
	}

	@Override
	public ForFiscalAutreElementImposable addForAutreElementImposable(Contribuable contribuable, RegDate dateDebut, MotifFor motifOuverture, RegDate dateFin, MotifFor motifFermeture, MotifRattachement motifRattachement, int autoriteFiscale) {
		return null;
	}

	@Override
	public ForDebiteurPrestationImposable addForDebiteur(DebiteurPrestationImposable debiteur, RegDate dateDebut, MotifFor motifOuverture, RegDate dateFin, MotifFor motifFermeture, TypeAutoriteFiscale typeAutoriteFiscale, int autoriteFiscale) {
		return null;
	}

	@Override
	public String getNomRaisonSociale(Tiers tiers) {
		return null;
	}

	@Override
	public NomPrenom getDecompositionNomPrenom(Individu individu) {
		return null;
	}

	@Override
	public String getNomPrenom(Individu individu) {
		return null;
	}

	@Override
	public @NotNull NomPrenom getDecompositionNomPrenom(PersonnePhysique pp, boolean tousPrenoms) {
		return null;
	}

	@Override
	public String getNomPrenom(PersonnePhysique personne) {
		return null;
	}

	@Override
	public RegDate getDateNaissance(PersonnePhysique pp) {
		return null;
	}

	@Override
	public boolean isMineur(PersonnePhysique pp, RegDate date) {
		return false;
	}

	@Override
	public RegDate getDateDebutVeuvage(PersonnePhysique pp, RegDate date) {
		return null;
	}

	@Override
	public RegDate getDateDeces(@Nullable PersonnePhysique pp) {
		return null;
	}

	@Override
	public RegDate getDateDecesDepuisDernierForPrincipal(PersonnePhysique pp) {
		return null;
	}

	@Override
	public boolean isDecede(PersonnePhysique pp) {
		return false;
	}

	@Override
	public String getNumeroAssureSocial(PersonnePhysique pp) {
		return null;
	}

	@Override
	public String getAncienNumeroAssureSocial(PersonnePhysique pp) {
		return null;
	}

	@Override
	public Integer getOfficeImpotId(Tiers tiers) {
		return null;
	}

	@Override
	public Integer getOfficeImpotIdAt(Tiers tiers, RegDate date) {
		return null;
	}

	@Override
	public Integer getOfficeImpotId(ForGestion forGestion) {
		return null;
	}

	@Override
	public Integer getOfficeImpotId(int noOfsCommune) {
		return null;
	}

	@Override
	public CollectiviteAdministrative getOfficeImpotAt(Tiers tiers, @Nullable RegDate date) {
		return null;
	}

	@Override
	public CollectiviteAdministrative getOfficeImpotRegionAt(Tiers tiers, @Nullable RegDate date) {
		return null;
	}

	@Override
	public Integer calculateCurrentOfficeID(Tiers tiers) {
		return null;
	}

	@Override
	public void reopenFor(ForFiscal ff, Tiers tiers) {

	}

	@Override
	public void reopenForsClosedAt(RegDate date, MotifFor motifFermeture, Tiers tiers) {

	}

	@Override
	public ForFiscal annuleForFiscal(ForFiscal forFiscal) throws ValidationException {
		return null;
	}

	@Override
	public void annuleTiers(Tiers tiers) {

	}

	@Override
	public ForGestion getForGestionActif(Tiers tiers, @Nullable RegDate date) {
		return null;
	}

	@Override
	public List<ForGestion> getForsGestionHisto(Tiers tiers) {
		return null;
	}

	@Override
	public ForGestion getDernierForGestionConnu(Tiers tiers, @Nullable RegDate date) {
		return null;
	}

	@Override
	public @NotNull List<ForFiscalPrincipal> getForsFiscauxVirtuels(@NotNull Tiers tiers, boolean doNotAutoflush) {
		return null;
	}

	@Override
	public List<AdresseTiers> fermeAdresseTiersTemporaire(Tiers tiers, RegDate date) {
		return null;
	}

	@Override
	public String getRoleAssujettissement(Tiers tiers, @Nullable RegDate date) {
		return null;
	}

	@Override
	public Assujettissement getAssujettissement(Contribuable contribuable, @Nullable RegDate date) {
		return null;
	}

	@NotNull
	@Override
	public List<ExerciceCommercial> getExercicesCommerciaux(Entreprise entreprise) {
		return null;
	}

	@Nullable
	@Override
	public ExerciceCommercial getExerciceCommercialAt(Entreprise entreprise, RegDate date) {
		return null;
	}

	@Override
	public ExclureContribuablesEnvoiResults setDateLimiteExclusion(List<Long> ctbIds, RegDate dateLimite, StatusManager s) {
		return null;
	}

	@Override
	public ForFiscalPrincipalPP openAndCloseForFiscalPrincipal(ContribuableImpositionPersonnesPhysiques contribuable, RegDate dateOuverture, MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale,
	                                                           TypeAutoriteFiscale typeAutoriteFiscale, ModeImposition modeImposition, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture) {
		return null;
	}

	@Override
	public ForFiscalPrincipalPM openAndCloseForFiscalPrincipal(ContribuableImpositionPersonnesMorales contribuable, RegDate dateOuverture, MotifRattachement motifRattachement, int numeroOfsAutoriteFiscale,
	                                                           TypeAutoriteFiscale typeAutoriteFiscale, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture, GenreImpot genreImpot) {
		return null;
	}

	@Override
	public ForDebiteurPrestationImposable openAndCloseForDebiteurPrestationImposable(DebiteurPrestationImposable debiteur, RegDate dateOuverture, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture, int numeroOfsAutoriteFiscale,
	                                                                                 TypeAutoriteFiscale typeAutoriteFiscale) {
		return null;
	}

	@Override
	public CorrectionFlagHabitantResults corrigeFlagHabitantSurPersonnesPhysiques(int nbThreads, StatusManager statusManager) {
		return null;
	}

	@Override
	public boolean isSourcierGris(Contribuable pp, @Nullable RegDate date) {
		return false;
	}

	@Override
	public Set<DebiteurPrestationImposable> getDebiteursPrestationImposable(Contribuable contribuable) {
		return null;
	}

	@Override
	public RegDate getDateDebutNouvellePeriodicite(DebiteurPrestationImposable debiteur, PeriodiciteDecompte nouvelle) {
		return null;
	}

	@Override
	public @NotNull Set<PersonnePhysique> getPersonnesPhysiques(MenageCommun menage) {
		return null;
	}

	@Override
	public @NotNull Map<PersonnePhysique, RapportEntreTiers> getToutesPersonnesPhysiquesImpliquees(MenageCommun menage) {
		return null;
	}

	@Override
	public Contribuable getContribuable(DebiteurPrestationImposable debiteur) {
		return null;
	}

	@Override
	public List<String> getRaisonSociale(DebiteurPrestationImposable debiteur) {
		return null;
	}

	@Override
	public String getDerniereRaisonSociale(Entreprise pm) {
		return null;
	}

	@Override
	public String getDerniereRaisonSociale(Etablissement etablissement) {
		return null;
	}

	@Override
	public Set<PersonnePhysique> getComposantsMenage(MenageCommun menageCommun, RegDate date) {
		return null;
	}

	@Override
	public Set<PersonnePhysique> getComposantsMenage(MenageCommun menageCommun, int anneePeriode) {
		return null;
	}

	@Override
	public EvenementsCivilsNonTraites getIndividusAvecEvenementsCivilsNonTraites(Tiers tiers) {
		return null;
	}

	@Override
	public boolean isVeuvageMarieSeul(PersonnePhysique tiers) {
		return false;
	}

	@Override
	public Long extractNumeroIndividuPrincipal(Tiers tiers) {
		return null;
	}

	@NotNull
	@Override
	public <T extends HibernateEntity> Set<T> getLinkedEntities(@NotNull LinkedEntity entity, @NotNull Class<T> clazz, LinkedEntityContext context, boolean includeAnnuled) {
		return null;
	}

	@NotNull
	@Override
	public Set<HibernateEntity> getLinkedEntities(@NotNull LinkedEntity entity, @NotNull Set<Class<?>> classes, LinkedEntityContext context, boolean includeAnnuled) {
		return null;
	}

	@Override
	public void adaptPremierePeriodicite(DebiteurPrestationImposable debiteurPrestationImposable, RegDate dateDebut) {

	}

	@Override
	public String getNomCollectiviteAdministrative(int collId) {
		return null;
	}

	@Override
	public String getNomCollectiviteAdministrative(CollectiviteAdministrative collectiviteAdministrative) {
		return null;
	}

	@Override
	public boolean isHorsCanton(Contribuable contribuable, RegDate date) {
		return false;
	}

	@Override
	public boolean isDernierForFiscalPrincipalFermePourSeparation(ContribuableImpositionPersonnesPhysiques ctb) {
		return false;
	}

	@Override
	public boolean isMenageActif(MenageCommun menage, @Nullable RegDate date) {
		return false;
	}

	@Override
	public @NotNull List<Parente> getParents(PersonnePhysique enfant, boolean yComprisRelationsAnnulees) {
		return null;
	}

	@Override
	public @NotNull List<PersonnePhysique> getParents(PersonnePhysique enfant, RegDate dateValidite) {
		return null;
	}

	@Override
	public @NotNull List<Parente> getEnfants(PersonnePhysique parent, boolean yComprisRelationsAnnulees) {
		return null;
	}

	@Override
	public ParenteUpdateResult refreshParentesDepuisNumeroIndividu(long noIndividu) {
		return null;
	}

	@Override
	public void markParentesDirtyDepuisNumeroIndividu(long noIndividu) {

	}

	@Override
	public ParenteUpdateResult refreshParentesSurPersonnePhysique(PersonnePhysique pp, boolean enfantsAussi) {
		return null;
	}

	@Override
	public ParenteUpdateResult initParentesDepuisFiliationsCiviles(PersonnePhysique pp) {
		return null;
	}

	@Override
	public Set<Long> getNumerosIndividusLiesParParente(long noIndividuSource) {
		return null;
	}

	@Override
	public void reouvrirForDebiteur(@NotNull ForDebiteurPrestationImposable forDebiteur) {

	}

	@Override
	public NumerosOfficesImpot getOfficesImpot(int noOfs, @Nullable RegDate date) {
		return null;
	}

	@Override
	public List<RapportPrestationImposable> getAllRapportPrestationImposable(DebiteurPrestationImposable dpi, PersonnePhysique sourcier, boolean nonAnnuleOnly, boolean doNotAutoFlush) {
		return null;
	}

	@Override
	public DecisionAci addDecisionAci(Contribuable ctb, TypeAutoriteFiscale typeAutoriteFiscale, int numeroAutoriteFiscale, RegDate dateDebut, RegDate dateFin, String Remarque) {
		return null;
	}

	@Override
	public DecisionAci updateDecisionAci(DecisionAci decisionAci, RegDate dateFin, String remarque, Integer numeroAutoriteFiscale) {
		return null;
	}

	@Override
	public boolean hasDecisionAciEnCours(long idTiers) {
		return false;
	}

	@Override
	public boolean hasDecisionAciValidAt(long idTiers, RegDate date) {
		return false;
	}

	@Override
	public List<MenageCommun> getAllMenagesCommuns(PersonnePhysique pp) {
		return null;
	}

	@Override
	public boolean isSousInfluenceDecisions(Contribuable ctb) {
		return false;
	}

	@Override
	public DomicileEtablissement addDomicileEtablissement(Etablissement etb, TypeAutoriteFiscale typeAutoriteFiscale, int numeroAutoriteFiscale, RegDate dateDebut, RegDate dateFin) {
		return null;
	}

	@Override
	public void closeDomicileEtablissement(DomicileEtablissement domicile, RegDate dateFin) {

	}

	@Override
	public void closeRapportEntreTiers(RapportEntreTiers rapport, RegDate dateFin) {

	}

	@Override
	public AllegementFiscalCanton addAllegementFiscalCantonal(Entreprise e, @Nullable BigDecimal pourcentageAllegement, AllegementFiscal.TypeImpot typeImpot, RegDate dateDebut, RegDate dateFin, AllegementFiscalCantonCommune.Type type) {
		return null;
	}

	@Override
	public AllegementFiscalCommune addAllegementFiscalCommunal(Entreprise e, @Nullable BigDecimal pourcentageAllegement, AllegementFiscal.TypeImpot typeImpot, RegDate dateDebut, RegDate dateFin, AllegementFiscalCantonCommune.Type type,
	                                                           Integer noOfsCommune) {
		return null;
	}

	@Override
	public AllegementFiscalConfederation addAllegementFiscalFederal(Entreprise e, @Nullable BigDecimal pourcentageAllegement, AllegementFiscal.TypeImpot typeImpot, RegDate dateDebut, RegDate dateFin, AllegementFiscalConfederation.Type type) {
		return null;
	}

	@Override
	public AllegementFiscalCanton openAllegementFiscalCantonal(Entreprise e, @Nullable BigDecimal pourcentageAllegement, AllegementFiscal.TypeImpot typeImpot, RegDate dateDebut, AllegementFiscalCantonCommune.Type type) {
		return null;
	}

	@Override
	public AllegementFiscalCanton openAndCloseAllegementFiscalCantonal(Entreprise e, @Nullable BigDecimal pourcentageAllegement, AllegementFiscal.TypeImpot typeImpot, RegDate dateDebut, RegDate dateFin, AllegementFiscalCantonCommune.Type type) {
		return null;
	}

	@Override
	public AllegementFiscalCommune openAllegementFiscalCommunal(Entreprise e, @Nullable BigDecimal pourcentageAllegement, AllegementFiscal.TypeImpot typeImpot, Integer noOfsCommune, RegDate dateDebut, AllegementFiscalCantonCommune.Type type) {
		return null;
	}

	@Override
	public AllegementFiscalCommune openAndCloseAllegementFiscalCommunal(Entreprise e, @Nullable BigDecimal pourcentageAllegement, AllegementFiscal.TypeImpot typeImpot, Integer noOfsCommune, RegDate dateDebut, RegDate dateFin,
	                                                                    AllegementFiscalCantonCommune.Type type) {
		return null;
	}

	@Override
	public AllegementFiscalConfederation openAllegementFiscalFederal(Entreprise e, @Nullable BigDecimal pourcentageAllegement, AllegementFiscal.TypeImpot typeImpot, RegDate dateDebut, AllegementFiscalConfederation.Type type) {
		return null;
	}

	@Override
	public AllegementFiscalConfederation openAndCloseAllegementFiscalFederal(Entreprise e, @Nullable BigDecimal pourcentageAllegement, AllegementFiscal.TypeImpot typeImpot, RegDate dateDebut, RegDate dateFin,
	                                                                         AllegementFiscalConfederation.Type type) {
		return null;
	}

	@Override
	public void closeAllegementFiscal(AllegementFiscal af, RegDate dateFin) {

	}

	@Override
	public void annuleAllegementFiscal(AllegementFiscal af) {

	}

	@Override
	public RaisonSocialeFiscaleEntreprise addRaisonSocialeFiscale(Entreprise e, String raisonSociale, RegDate dateDebut, RegDate dateFin) throws TiersException {
		return null;
	}

	@Override
	public void updateRaisonSocialeFiscale(RaisonSocialeFiscaleEntreprise rs, String raisonSociale) throws TiersException {

	}

	@Override
	public void closeRaisonSocialeFiscale(RaisonSocialeFiscaleEntreprise raisonSociale, RegDate dateFin) {

	}

	@Override
	public void annuleRaisonSocialeFiscale(RaisonSocialeFiscaleEntreprise raisonSociale) throws TiersException {

	}

	@Override
	public FormeJuridiqueFiscaleEntreprise addFormeJuridiqueFiscale(Entreprise e, FormeJuridiqueEntreprise formeJuridique, RegDate dateDebut, RegDate dateFin) throws TiersException {
		return null;
	}

	@Override
	public void updateFormeJuridiqueFiscale(FormeJuridiqueFiscaleEntreprise fj, FormeJuridiqueEntreprise formeJuridique) throws TiersException {

	}

	@Override
	public void closeFormeJuridiqueFiscale(FormeJuridiqueFiscaleEntreprise formeJuridique, RegDate dateFin) {

	}

	@Override
	public void annuleFormeJuridiqueFiscale(FormeJuridiqueFiscaleEntreprise formeJuridique) throws TiersException {

	}

	@Override
	public DomicileEtablissement addDomicileFiscal(Etablissement etablissement, TypeAutoriteFiscale typeAutorite, Integer noOfs, RegDate dateDebut, RegDate dateFin) throws TiersException {
		return null;
	}

	@Override
	public DomicileEtablissement updateDomicileFiscal(DomicileEtablissement domicile, TypeAutoriteFiscale typeAutorite, Integer noOfs) throws TiersException {
		return null;
	}

	@Override
	public DomicileEtablissement updateDomicileFiscal(DomicileEtablissement domicile, TypeAutoriteFiscale typeAutorite, Integer noOfs, RegDate dateFin) throws TiersException {
		return null;
	}

	@Override
	public void closeDomicileFiscal(DomicileEtablissement domicile, RegDate dateFin) throws TiersException {

	}

	@Override
	public void annuleDomicileFiscal(DomicileEtablissement domicile) throws TiersException {

	}

	@Override
	public CapitalFiscalEntreprise addCapitalFiscal(Entreprise e, Long montant, String monnaie, RegDate dateDebut, RegDate dateFin) throws TiersException {
		return null;
	}

	@Override
	public void updateCapitalFiscal(CapitalFiscalEntreprise cf, Long montant, String monnaie, RegDate dateFin) throws TiersException {

	}

	@Override
	public void closeCapitalFiscal(CapitalFiscalEntreprise capital, RegDate dateFin) {

	}

	@Override
	public void annuleCapitalFiscal(CapitalFiscalEntreprise capital) throws TiersException {

	}

	@Override
	public RegimeFiscal addRegimeFiscal(Entreprise e, RegimeFiscal.Portee portee, TypeRegimeFiscal type, RegDate dateDebut, RegDate dateFin) {
		return null;
	}

	@Override
	public RegimeFiscal replaceRegimeFiscal(RegimeFiscal oldValue, TypeRegimeFiscal type) {
		return null;
	}

	@Override
	public RegimeFiscal openRegimeFiscal(Entreprise e, RegimeFiscal.Portee portee, TypeRegimeFiscal type, RegDate dateDebut) {
		return null;
	}

	@Override
	public void openRegimesFiscauxParDefautCHVD(Entreprise entreprise, FormeJuridiqueEntreprise formeJuridique, RegDate dateDebut, @Nullable Consumer<FormeJuridiqueVersTypeRegimeFiscalMapping> onDetectedMapping) {

	}

	@Override
	public void changeRegimesFiscauxParDefautCHVD(Entreprise entreprise, FormeJuridiqueEntreprise formeJuridique, RegDate dateDebut, @Nullable Consumer<FormeJuridiqueVersTypeRegimeFiscalMapping> onDetectedMapping) {

	}

	@Override
	public RegimeFiscal openAndCloseRegimeFiscal(Entreprise e, RegimeFiscal.Portee portee, TypeRegimeFiscal type, RegDate dateDebut, RegDate dateFin) {
		return null;
	}

	@Override
	public void closeRegimeFiscal(RegimeFiscal rf, RegDate dateFin) {

	}

	@Override
	public void annuleRegimeFiscal(RegimeFiscal rf) {

	}

	@Override
	public FlagEntreprise addFlagEntreprise(Entreprise e, TypeFlagEntreprise type, RegDate dateDebut, @Nullable RegDate dateFin) {
		return null;
	}

	@Override
	public FlagEntreprise openFlagEntreprise(Entreprise e, TypeFlagEntreprise type, RegDate dateDebut) {
		return null;
	}

	@Override
	public FlagEntreprise openAndCloseFlagEntreprise(Entreprise e, TypeFlagEntreprise type, RegDate dateDebut, RegDate dateFin) {
		return null;
	}

	@Override
	public void closeFlagEntreprise(FlagEntreprise flag, RegDate dateFin) {

	}

	@Override
	public void annuleFlagEntreprise(FlagEntreprise flag) {

	}

	@Override
	public void addMandat(Contribuable mandant, Mandat mandat) {

	}

	@Override
	public void addMandat(Contribuable mandant, AdresseMandataire mandat) {

	}

	@Override
	public Set<Contribuable> getContribuablesLies(Contribuable ctb, Integer ageLiaison) {
		return null;
	}

	@Override
	public EntrepriseCivile getEntrepriseCivile(@NotNull Entreprise entreprise) {
		return null;
	}

	@Override
	public EntrepriseCivile getEntrepriseCivileByEtablissement(@NotNull Etablissement etablissement) {
		return null;
	}

	@Override
	public EtablissementCivil getEtablissementCivil(@NotNull Etablissement etablissement) {
		return null;
	}

	@Override
	public RegDate getDateCreation(@NotNull Entreprise entreprise) {
		return null;
	}

	@Override
	public @Nullable String getNumeroIDE(@NotNull Entreprise entreprise) {
		return null;
	}

	@Override
	public @Nullable String getNumeroIDE(@NotNull Etablissement etablissement) {
		return null;
	}

	@Override
	public List<CapitalHisto> getCapitaux(@NotNull Entreprise entreprise, boolean aussiAnnule) {
		return null;
	}

	@Override
	public @NotNull CategorieEntreprise getCategorieEntreprise(@NotNull Entreprise entreprise, @Nullable RegDate date) {
		return null;
	}

	@Override
	public void appliqueDonneesCivilesSurPeriode(Entreprise entreprise, DateRange range, RegDate dateValeur, boolean donneeMinimale) throws TiersException {

	}

	@Override
	public void appliqueDonneesCivilesSurPeriode(Etablissement etablissement, DateRange range, RegDate dateValeur, boolean donneeMinimale) throws TiersException {

	}

	@Override
	public @NotNull Map<Long, CommunauteHeritiers> getCommunautesHeritiers(@NotNull Collection<Long> tiersIds) {
		throw new NotImplementedException();
	}

	@Override
	public boolean isHabitant(Contribuable ctb) {
		throw new NotYetImplementedException("isHabitant");
	}

	@Override
	public Contribuable getContribuableAssujettissable(Contribuable ctb) {
		throw new NotYetImplementedException("getContribuableAssujettissable");
	}
}
