package ch.vd.unireg.registrefoncier.dataimport;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.common.BusinessItTestingConstants;
import ch.vd.unireg.common.BusinessTestingConstants;
import ch.vd.unireg.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFImport;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.unireg.evenement.registrefoncier.TypeImportRF;
import ch.vd.unireg.registrefoncier.BatimentRF;
import ch.vd.unireg.registrefoncier.BeneficeServitudeRF;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.ChargeServitudeRF;
import ch.vd.unireg.registrefoncier.CollectivitePubliqueRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.CommuneRF;
import ch.vd.unireg.registrefoncier.DescriptionBatimentRF;
import ch.vd.unireg.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.unireg.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.unireg.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.EstimationRF;
import ch.vd.unireg.registrefoncier.Fraction;
import ch.vd.unireg.registrefoncier.GenrePropriete;
import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;
import ch.vd.unireg.registrefoncier.IdentifiantDroitRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.ImplantationRF;
import ch.vd.unireg.registrefoncier.PartCoproprieteRF;
import ch.vd.unireg.registrefoncier.PersonneMoraleRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.ProprieteParEtageRF;
import ch.vd.unireg.registrefoncier.QuotePartRF;
import ch.vd.unireg.registrefoncier.RaisonAcquisitionRF;
import ch.vd.unireg.registrefoncier.SituationRF;
import ch.vd.unireg.registrefoncier.SurfaceAuSolRF;
import ch.vd.unireg.registrefoncier.SurfaceTotaleRF;
import ch.vd.unireg.registrefoncier.TypeCommunaute;
import ch.vd.unireg.registrefoncier.UsufruitRF;
import ch.vd.unireg.scheduler.JobDefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@ContextConfiguration(locations = {
		BusinessTestingConstants.UNIREG_BUSINESS_JOBS,
		BusinessTestingConstants.UNIREG_BUSINESS_UT_REGISTREFONCIER_IMPORT,
		BusinessItTestingConstants.UNIREG_BUSINESSIT_JMS
})
public abstract class ImportRFTestClass extends BusinessItTest {

	public static final Logger LOGGER = LoggerFactory.getLogger(ImportRFTestClass.class);

	private EvenementRFImportDAO evenementRFImportDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evenementRFImportDAO = getBean(EvenementRFImportDAO.class, "evenementRFImportDAO");
		evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
	}

	protected static CommuneRF newCommuneRF(int noRf, String nomRf, int noOFS) {
		CommuneRF commune = new CommuneRF();
		commune.setNoRf(noRf);
		commune.setNomRf(nomRf);
		commune.setNoOfs(noOFS);
		return commune;
	}

	protected static BatimentRF newBatimentRF(String masterIdRF) {
		final BatimentRF batiment = new BatimentRF();
		batiment.setMasterIdRF(masterIdRF);
		return batiment;
	}

	protected static DescriptionBatimentRF newDescriptionBatiment(String type, Integer surface, RegDate dateDebut, RegDate dateFin) {
		return new DescriptionBatimentRF(type, surface, dateDebut, dateFin);
	}

	protected static ImplantationRF newImplantationRF(ImmeubleRF immeuble, Integer surface, RegDate dateDebut, RegDate dateFin) {
		final ImplantationRF implantation = new ImplantationRF();
		implantation.setImmeuble(immeuble);
		implantation.setSurface(surface);
		implantation.setDateDebut(dateDebut);
		implantation.setDateFin(dateFin);
		return implantation;
	}

	protected static BienFondsRF newBienFondsRF(String idRF, String egrid, CommuneRF commune, int noParcelle,
	                                            Long montantEstimation, String referenceEstimation, Integer anneeReference, RegDate dateInscription,
	                                            RegDate dateDebutMetier, boolean enRevision, boolean cfa, RegDate dateValeur, int surface) {

		final SituationRF situation = new SituationRF();
		situation.setCommune(commune);
		situation.setNoParcelle(noParcelle);
		situation.setDateDebut(dateValeur);

		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(montantEstimation);
		estimation.setReference(referenceEstimation);
		estimation.setAnneeReference(anneeReference);
		estimation.setDateInscription(dateInscription);
		estimation.setDateDebutMetier(dateDebutMetier);
		estimation.setEnRevision(enRevision);
		estimation.setDateDebut(dateValeur);

		final SurfaceTotaleRF surfaceTotale = new SurfaceTotaleRF();
		surfaceTotale.setDateDebut(dateValeur);
		surfaceTotale.setSurface(surface);

		final BienFondsRF immeuble = new BienFondsRF();
		immeuble.setIdRF(idRF);
		immeuble.setCfa(cfa);
		immeuble.setEgrid(egrid);
		immeuble.addSituation(situation);
		immeuble.addEstimation(estimation);
		immeuble.addSurfaceTotale(surfaceTotale);

		return immeuble;
	}

	protected static DroitDistinctEtPermanentRF newDroitDistinctEtPermanentRF(String idRF, String egrid, CommuneRF commune, int noParcelle,
	                                                                          Long montantEstimation, String referenceEstimation, Integer anneeReference, RegDate dateInscription,
	                                                                          RegDate dateDebutMetier, boolean enRevision, RegDate dateValeur, int surface) {

		final SituationRF situation = new SituationRF();
		situation.setCommune(commune);
		situation.setNoParcelle(noParcelle);
		situation.setDateDebut(dateValeur);

		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(montantEstimation);
		estimation.setReference(referenceEstimation);
		estimation.setAnneeReference(anneeReference);
		estimation.setDateInscription(dateInscription);
		estimation.setDateDebutMetier(dateDebutMetier);
		estimation.setEnRevision(enRevision);
		estimation.setDateDebut(dateValeur);

		final SurfaceTotaleRF surfaceTotale = new SurfaceTotaleRF();
		surfaceTotale.setDateDebut(dateValeur);
		surfaceTotale.setSurface(surface);

		final DroitDistinctEtPermanentRF immeuble = new DroitDistinctEtPermanentRF();
		immeuble.setIdRF(idRF);
		immeuble.setEgrid(egrid);
		immeuble.addSituation(situation);
		immeuble.addEstimation(estimation);
		immeuble.addSurfaceTotale(surfaceTotale);

		return immeuble;
	}

	protected static ProprieteParEtageRF newProprieteParEtageRF(String idRF, String egrid, CommuneRF commune, int noParcelle, Integer index1,
	                                                            Long montantEstimation, String referenceEstimation, Integer anneeReference, RegDate dateInscription,
	                                                            RegDate dateDebutMetier, boolean enRevision, Fraction quotePart, RegDate dateValeur) {

		final SituationRF situation = new SituationRF();
		situation.setCommune(commune);
		situation.setNoParcelle(noParcelle);
		situation.setIndex1(index1);
		situation.setDateDebut(dateValeur);

		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(montantEstimation);
		estimation.setReference(referenceEstimation);
		estimation.setAnneeReference(anneeReference);
		estimation.setDateInscription(dateInscription);
		estimation.setEnRevision(enRevision);
		estimation.setDateDebut(dateValeur);
		estimation.setDateDebutMetier(dateDebutMetier);

		final ProprieteParEtageRF immeuble = new ProprieteParEtageRF();
		immeuble.setIdRF(idRF);
		immeuble.setEgrid(egrid);
		immeuble.addQuotePart(new QuotePartRF(dateValeur, null, quotePart));
		immeuble.addSituation(situation);
		immeuble.addEstimation(estimation);

		return immeuble;
	}

	protected static PartCoproprieteRF newPartCoproprieteRF(String idRF, String egrid, CommuneRF commune, int noParcelle, Integer index1, Integer index2,
	                                                        Long montantEstimation, String referenceEstimation, Integer anneeReference, RegDate dateInscription,
	                                                        RegDate dateDebutMetier, boolean enRevision, Fraction quotePart, RegDate dateValeur) {

		final SituationRF situation = new SituationRF();
		situation.setCommune(commune);
		situation.setNoParcelle(noParcelle);
		situation.setIndex1(index1);
		situation.setIndex2(index2);
		situation.setDateDebut(dateValeur);

		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(montantEstimation);
		estimation.setReference(referenceEstimation);
		estimation.setAnneeReference(anneeReference);
		estimation.setDateInscription(dateInscription);
		estimation.setEnRevision(enRevision);
		estimation.setDateDebut(dateValeur);
		estimation.setDateDebutMetier(dateDebutMetier);

		final PartCoproprieteRF immeuble = new PartCoproprieteRF();
		immeuble.setIdRF(idRF);
		immeuble.setEgrid(egrid);
		immeuble.addQuotePart(new QuotePartRF(dateValeur, null, quotePart));
		immeuble.addSituation(situation);
		immeuble.addEstimation(estimation);

		return immeuble;
	}

	@NotNull
	public static PersonnePhysiqueRF newPersonnePhysique(String idRF, long noRF, long noIrole, String nom, String prenom, RegDate dateNaissance) {
		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setIdRF(idRF);
		pp.setNoRF(noRF);
		pp.setNoContribuable(noIrole);
		pp.setNom(nom);
		pp.setPrenom(prenom);
		pp.setDateNaissance(dateNaissance);
		return pp;
	}

	@NotNull
	public static PersonneMoraleRF newPersonneMorale(String idRF, long noRF, long noACI, String name) {
		final PersonneMoraleRF pm = new PersonneMoraleRF();
		pm.setIdRF(idRF);
		pm.setNoRF(noRF);
		pm.setNoContribuable(noACI);
		pm.setRaisonSociale(name);
		return pm;
	}

	@NotNull
	public static CollectivitePubliqueRF newCollectivitePublique(String idRF, long noRF, long noACI, String raisonSociale) {
		final CollectivitePubliqueRF collectivite = new CollectivitePubliqueRF();
		collectivite.setIdRF(idRF);
		collectivite.setNoRF(noRF);
		collectivite.setNoContribuable(noACI);
		collectivite.setRaisonSociale(raisonSociale);
		return collectivite;
	}

	@NotNull
	public static CommunauteRF newCommunauté(String idRF, TypeCommunaute type) {
		final CommunauteRF com = new CommunauteRF();
		com.setIdRF(idRF);
		com.setType(type);
		return com;
	}

	public static UsufruitRF newUsufruitRF(String masterIdRF,
	                                       String versionIdRf, BienFondsRF immeuble,
	                                       PersonnePhysiqueRF personne,
	                                       RegDate dateDebut,
	                                       RegDate dateDebutMetier,
	                                       RegDate dateFin,
	                                       RegDate dateFinMetier,
	                                       IdentifiantDroitRF identifiantDroit,
	                                       IdentifiantAffaireRF numeroAffaire) {
		return newUsufruitRF(masterIdRF, versionIdRf, Collections.singletonList(immeuble), Collections.singletonList(personne), dateDebut, dateDebutMetier, dateFin, dateFinMetier, identifiantDroit, numeroAffaire);
	}

	public static UsufruitRF newUsufruitRF(String masterIdRF,
	                                       String versionIdRf,
	                                       List<ImmeubleRF> immeubles,
	                                       List<PersonnePhysiqueRF> personnes,
	                                       RegDate dateDebut,
	                                       RegDate dateDebutMetier,
	                                       RegDate dateFin,
	                                       RegDate dateFinMetier,
	                                       IdentifiantDroitRF identifiantDroit,
	                                       IdentifiantAffaireRF numeroAffaire) {
		final UsufruitRF usu = new UsufruitRF();
		immeubles.forEach(i -> usu.addCharge(new ChargeServitudeRF(dateDebutMetier, dateFinMetier, usu, i)));
		personnes.forEach(a -> usu.addBenefice(new BeneficeServitudeRF(dateDebutMetier, dateFinMetier, usu, a)));
		usu.setDateDebut(dateDebut);
		usu.setDateDebutMetier(dateDebutMetier);
		usu.setDateFin(dateFin);
		usu.setDateFinMetier(dateFinMetier);
		usu.setIdentifiantDroit(identifiantDroit);
		usu.setMasterIdRF(masterIdRF);
		usu.setVersionIdRF(versionIdRf);
		usu.setNumeroAffaire(numeroAffaire);
		return usu;
	}

	@NotNull
	protected static SurfaceAuSolRF newSurfaceAuSol(BienFondsRF immeuble, String type, int surface, RegDate dateDebut, RegDate dateFin) {
		final SurfaceAuSolRF sur = new SurfaceAuSolRF();
		sur.setImmeuble(immeuble);
		sur.setType(type);
		sur.setSurface(surface);
		sur.setDateDebut(dateDebut);
		sur.setDateFin(dateFin);
		return sur;
	}

	protected static DroitProprietePersonnePhysiqueRF newDroitPP(@NotNull String masterIdRf, String versionIdRf, @NotNull PersonnePhysiqueRF personnePhysique, @NotNull ImmeubleRF immeuble, @Nullable CommunauteRF communaute,
	                                                             @NotNull Fraction part,
	                                                             @NotNull GenrePropriete regime, @NotNull RegDate dateDebutMetier, @NotNull IdentifiantAffaireRF affaire, @NotNull RegDate dateDebut, @NotNull String motifDebut,
	                                                             @Nullable RegDate dateFin) {

		final DroitProprietePersonnePhysiqueRF droit = new DroitProprietePersonnePhysiqueRF();
		droit.setDateDebut(dateDebut);
		droit.setMotifDebut(motifDebut);
		droit.setDateFin(dateFin);
		droit.setImmeuble(immeuble);
		droit.setAyantDroit(personnePhysique);
		droit.setCommunaute(communaute);
		droit.setMasterIdRF(masterIdRf);
		droit.setVersionIdRF(versionIdRf);
		droit.setDateDebutMetier(dateDebutMetier);
		droit.setPart(part);
		droit.setRegime(regime);
		droit.addRaisonAcquisition(new RaisonAcquisitionRF(dateDebutMetier, motifDebut, affaire));
		return droit;
	}

	protected static DroitProprieteCommunauteRF newDroitColl(@NotNull String masterIdRf, String versionIdRf, @NotNull CommunauteRF communaute, @NotNull ImmeubleRF immeuble, @NotNull Fraction part, @NotNull GenrePropriete regime,
	                                                         @NotNull RegDate dateDebutMetier,
	                                                         @NotNull IdentifiantAffaireRF affaire, @NotNull RegDate dateDebut, @NotNull String motifDebut, @Nullable RegDate dateFin) {

		final DroitProprieteCommunauteRF droit = new DroitProprieteCommunauteRF();
		droit.setDateDebut(dateDebut);
		droit.setMotifDebut(motifDebut);
		droit.setDateFin(dateFin);
		droit.setImmeuble(immeuble);
		droit.setAyantDroit(communaute);
		droit.setMasterIdRF(masterIdRf);
		droit.setVersionIdRF(versionIdRf);
		droit.setDateDebutMetier(dateDebutMetier);
		droit.setPart(part);
		droit.setRegime(regime);
		droit.addRaisonAcquisition(new RaisonAcquisitionRF(dateDebutMetier, motifDebut, affaire));
		return droit;
	}

	protected static void waitForJobCompletion(JobDefinition job) throws InterruptedException {
		int count = 0;
		while (job.isRunning()) {
			Thread.sleep(5000);
			count++;
			if (count % 6 == 0) { // 1 minute
				LOGGER.debug("Attente de la fin du job " + job.getName());
			}
			if (count > 60) { // 5 minutes
				LOGGER.debug("Interruption du job " + job.getName() + "...");
				job.interrupt();
				fail("Le job " + job.getName() + " tournait depuis plus de cinq minutes et a été interrompu.");
			}
		}
	}

	protected void assertEtatMutations(final int count, final EtatEvenementRF etat) throws Exception {
		doInNewTransaction(status -> {
			final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
			assertEquals(count, mutations.size());
			Collections.sort(mutations, new MutationComparator());
			for (EvenementRFMutation mutation : mutations) {
				assertEquals(etat, mutation.getEtat());
			}
			return null;
		});
	}

	protected Long insertImport(final TypeImportRF type, final RegDate dateEvenement, final EtatEvenementRF etat, final String fileUrl) throws Exception {
		// on insère les données de l'import dans la base
		return doInNewTransaction(status -> {
			final EvenementRFImport importEvent = new EvenementRFImport();
			importEvent.setType(type);
			importEvent.setDateEvenement(dateEvenement);
			importEvent.setEtat(etat);
			importEvent.setFileUrl(fileUrl);
			return evenementRFImportDAO.save(importEvent).getId();
		});
	}
}
