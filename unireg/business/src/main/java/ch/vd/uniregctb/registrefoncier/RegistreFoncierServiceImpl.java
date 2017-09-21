package ch.vd.uniregctb.registrefoncier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.BatimentRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.DroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ModeleCommunauteRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.SituationRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.DroitRFHelper;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

public class RegistreFoncierServiceImpl implements RegistreFoncierService {

	private DroitRFDAO droitRFDAO;
	private TiersService tiersService;
	private ImmeubleRFDAO immeubleRFDAO;
	private BatimentRFDAO batimentRFDAO;
	private AyantDroitRFDAO ayantDroitRFDAO;
	private SituationRFDAO situationRFDAO;
	private ModeleCommunauteRFDAO modeleCommunauteRFDAO;
	private ServiceInfrastructureService infraService;
	private EvenementFiscalService evenementFiscalService;
	private PlatformTransactionManager transactionManager;

	public void setDroitRFDAO(DroitRFDAO droitRFDAO) {
		this.droitRFDAO = droitRFDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setImmeubleRFDAO(ImmeubleRFDAO immeubleRFDAO) {
		this.immeubleRFDAO = immeubleRFDAO;
	}

	public void setBatimentRFDAO(BatimentRFDAO batimentRFDAO) {
		this.batimentRFDAO = batimentRFDAO;
	}

	public void setAyantDroitRFDAO(AyantDroitRFDAO ayantDroitRFDAO) {
		this.ayantDroitRFDAO = ayantDroitRFDAO;
	}

	public void setSituationRFDAO(SituationRFDAO situationRFDAO) {
		this.situationRFDAO = situationRFDAO;
	}

	public void setModeleCommunauteRFDAO(ModeleCommunauteRFDAO modeleCommunauteRFDAO) {
		this.modeleCommunauteRFDAO = modeleCommunauteRFDAO;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	public List<DroitRF> getDroitsForCtb(@NotNull Contribuable ctb, boolean includeVirtual) {
		return getDroitsForCtb(ctb, false, includeVirtual);
	}

	@Override
	public List<DroitRF> getDroitsForCtb(@NotNull Contribuable ctb, boolean prefetchSituationsImmeuble, boolean includeVirtual) {
		return ctb.getRapprochementsRF().stream()
				.filter(AnnulableHelper::nonAnnule)                     // on ignore les rapprochements annulés
				.flatMap(rapp -> getDroitsValides(rapp, prefetchSituationsImmeuble, includeVirtual))  // on demande les droits valides pour le rapprochement
				.sorted()                             // on trie les droits pour garder un ordre constant entre chaque appel
				.collect(Collectors.toList());
	}

	/**
	 * Cette méthode calcule et retourne les droits valides pour un rapprochement déterminé.
	 * <p/>
	 * Selon la spécification SCU-Unireg-RFI-Mettre_a_disposition_immeuble.doc, version 0.4, chapitre 3.1.2,
	 * les droits valides pour un rapprochement donné sont les droits du tiers RF correspondant qui
	 * <b>intersectent</b> la période de validité du rapprochement:
	 * <p>
	 * <pre>
	 *  Contribuable              |---------------------------------------|
	 *  Rapprochement tiers RF 1  |-------------------|
	 *  Rapprochement tiers RF 2                      |-------------------|
	 *  Droits tiers RF 1            |--------A----------|
	 *                                                       |---B---|
	 *                                  |----C---|
	 *  Droits tiers RF 2                               |--------D-------|
	 *                            |--------E-------|
	 *                                  |-------F--------|
	 *
	 *  ----------------------------------------------------------------------
	 *
	 *  Droits contribuables
	 *                               |--------A----------|
	 *                                  |----C---|
	 *                                                  |--------D-------|
	 *                                  |-------F--------|
	 * </pre>
	 *
	 * @param rapprochement   un rapprochement entre un contribuable et un tiers RF
	 * @param fetchSituations <code>true</code> s'il faut que les immeubles des droits retournés aient déjà leurs situations récupérées (optim)
	 * @param includeVirtual  vrai s'il faut inclure les droits virtuels du contribuable
	 * @return les liste des droits valides pour le contribuable
	 */
	private Stream<DroitRF> getDroitsValides(RapprochementRF rapprochement, boolean fetchSituations, boolean includeVirtual) {
		final List<DroitRF> droits = getDroitsForTiersRF(rapprochement.getTiersRF(), fetchSituations, includeVirtual);
		return droits.stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(d -> DateRangeHelper.intersect(d, rapprochement));
	}

	@Override
	public List<DroitRF> getDroitsForTiersRF(AyantDroitRF ayantDroitRF, boolean prefetchSituationsImmeuble, boolean includeVirtual) {
		final List<DroitRF> droits = droitRFDAO.findForAyantDroit(ayantDroitRF.getId(), prefetchSituationsImmeuble);
		if (includeVirtual) {
			// on parcourt les droits entre immeubles pour déterminer la liste des droits virtuels
			final List<DroitRF> droitsProprieteVirtuels = droits.stream()
					.filter(AnnulableHelper::nonAnnule)
					.filter(DroitProprieteRF.class::isInstance)
					.map(DroitProprieteRF.class::cast)
					.map(this::determineDroitsProprieteVirtuels)
					.flatMap(Collection::stream)
					.collect(Collectors.toList());
			droits.addAll(droitsProprieteVirtuels);

			final List<DroitRF> servitudesVirtuelles = droits.stream()
					.filter(AnnulableHelper::nonAnnule)
					.filter(ServitudeRF.class::isInstance)
					.map(ServitudeRF.class::cast)
					.map(d -> determineServitudesVirtuelles(ayantDroitRF, d))
					.flatMap(Collection::stream)
					.collect(Collectors.toList());
			droits.addAll(servitudesVirtuelles);
		}
		return droits;
	}

	/**
	 * Détermine les <i>droits de propriété</i> virtuels.
	 */
	private List<DroitRF> determineDroitsProprieteVirtuels(@NotNull DroitProprieteRF droitReel) {

		final List<DroitRF> chemin = new ArrayList<>();
		chemin.add(droitReel);

		final ImmeubleRF immeuble = droitReel.getImmeuble();
		return determineDroitsVirtuels(droitReel.getAyantDroit(), droitReel, immeuble, chemin, RegistreFoncierServiceImpl::newDroitProprieteRFVirtuel);
	}

	/**
	 * Détermine les <i>servitudes</i> virtuelles.
	 */
	private List<DroitRF> determineServitudesVirtuelles(@NotNull AyantDroitRF ayantDroit, @NotNull ServitudeRF droitReel) {

		if (droitReel instanceof DroitHabitationRF) {
			// on ne virtualise pas les droits d'habitation, seulement les usufruits
			return Collections.emptyList();
		}

		final List<DroitRF> chemin = new ArrayList<>();
		chemin.add(droitReel);

		final Set<ImmeubleRF> immeubles = droitReel.getImmeubles();
		return immeubles.stream()
				.map(i -> determineDroitsVirtuels(ayantDroit, droitReel, i, chemin, RegistreFoncierServiceImpl::newUsufruitRFVirtuel))
				.flatMap(Collection::stream)
				.collect(Collectors.toList());
	}

	private interface DroitVirtuelSupplier {
		DroitRF apply(@NotNull AyantDroitRF ayantDroit, @NotNull ImmeubleRF immeuble, @NotNull DroitRFHelper.DroitIntersection intersection, @NotNull List<DroitRF> chemin);
	}

	private List<DroitRF> determineDroitsVirtuels(@NotNull AyantDroitRF ayantDroit,
	                                              @NotNull DroitRF parent,
	                                              @NotNull ImmeubleRF immeuble,
	                                              @NotNull List<DroitRF> chemin,
	                                              @NotNull DroitVirtuelSupplier supplier) {

		final ImmeubleBeneficiaireRF beneficiaire = immeuble.getEquivalentBeneficiaire();
		if (beneficiaire == null) {
			// l'immeuble n'est pas propriétaire d'autres immeubles, on sort
			return Collections.emptyList();
		}

		final List<DroitRF> virtuels = new ArrayList<>();

		final Set<DroitProprieteRF> sousDroits = beneficiaire.getDroitsPropriete();
		for (DroitProprieteRF sousDroit : sousDroits) {
			if (sousDroit.isAnnule() || chemin.contains(sousDroit)) {
				// le droit est annulé ou a déjà été traité, on l'ignore
				continue;
			}

			final DroitRFHelper.DroitIntersection intersection = DroitRFHelper.intersection(parent, sousDroit);
			if (intersection != null) { // le sous-droit intersecte avec le parent -> on le suit

				final List<DroitRF> sousChemin = new ArrayList<>(chemin);
				sousChemin.add(sousDroit);

				// on crée le droit virtuel
				final DroitRF droitVirtuel = supplier.apply(ayantDroit, sousDroit.getImmeuble(), intersection, sousChemin);
				virtuels.add(droitVirtuel);

				// on continue avec l'immeuble suivant
				final ImmeubleRF sousImmeuble = sousDroit.getImmeuble();
				virtuels.addAll(determineDroitsVirtuels(ayantDroit, droitVirtuel, sousImmeuble, sousChemin, supplier));
			}
		}
		return virtuels;
	}

	@NotNull
	private static DroitProprieteVirtuelRF newDroitProprieteRFVirtuel(@NotNull AyantDroitRF ayantDroit,
	                                                                  @NotNull ImmeubleRF immeuble,
	                                                                  @NotNull DroitRFHelper.DroitIntersection intersection,
	                                                                  @NotNull List<DroitRF> chemin) {
		final DroitProprieteVirtuelRF droitVirtuel = new DroitProprieteVirtuelRF();
		droitVirtuel.setAyantDroit(ayantDroit);
		droitVirtuel.setImmeuble(immeuble);
		droitVirtuel.setDateDebutMetier(intersection.getDateDebut());
		droitVirtuel.setDateFinMetier(intersection.getDateFin());
		droitVirtuel.setMotifDebut(intersection.getMotifDebut());
		droitVirtuel.setMotifFin(intersection.getMotifFin());
		droitVirtuel.setChemin(chemin.stream()
				                       .map(DroitProprieteRF.class::cast)
				                       .collect(Collectors.toList()));
		return droitVirtuel;
	}

	@NotNull
	private static UsufruitVirtuelRF newUsufruitRFVirtuel(@NotNull AyantDroitRF ayantDroit,
	                                                      @NotNull ImmeubleRF immeuble,
	                                                      @NotNull DroitRFHelper.DroitIntersection intersection,
	                                                      @NotNull List<DroitRF> chemin) {
		final UsufruitVirtuelRF droitVirtuel = new UsufruitVirtuelRF();
		droitVirtuel.setAyantDroit(ayantDroit);
		droitVirtuel.setImmeuble(immeuble);
		droitVirtuel.setDateDebutMetier(intersection.getDateDebut());
		droitVirtuel.setDateFinMetier(intersection.getDateFin());
		droitVirtuel.setMotifDebut(intersection.getMotifDebut());
		droitVirtuel.setMotifFin(intersection.getMotifFin());
		droitVirtuel.setChemin(new ArrayList<>(chemin));
		return droitVirtuel;
	}

	@Nullable
	@Override
	public ImmeubleRF getImmeuble(long immeubleId) {
		return immeubleRFDAO.get(immeubleId);
	}

	@Nullable
	@Override
	public BatimentRF getBatiment(long batimentId) {
		return batimentRFDAO.get(batimentId);
	}

	@Nullable
	@Override
	public CommunauteRF getCommunaute(long communauteId) {
		return (CommunauteRF) ayantDroitRFDAO.get(communauteId);
	}

	@Override
	@NotNull
	public CommunauteRFMembreInfo getCommunauteMembreInfo(@NotNull CommunauteRF communaute) {
		final CommunauteRFMembreInfo info = communaute.buildMembreInfoNonTries();
		final Long principalCtbId = Optional.ofNullable(communaute.getPrincipalCommunauteDesigne())
				.filter(TiersRF.class::isInstance)
				.map(TiersRF.class::cast)
				.map(TiersRF::getCtbRapproche)
				.map(Tiers::getId)
				.orElse(null);
		// [SIFISC-23747] on trie la collection de tiers de telle manière que le leader de la communauté soit en première position
		info.sortMembers(new CommunauteRFMembreComparator(tiersService, principalCtbId));
		return info;
	}

	@NotNull
	@Override
	public ModeleCommunauteRF findOrCreateModeleCommunaute(@NotNull Set<? extends AyantDroitRF> membres) {

		if (membres.isEmpty()) {
			throw new IllegalArgumentException("La liste des membres est vide");
		}

		// on va chercher la communauté
		ModeleCommunauteRF modele = modeleCommunauteRFDAO.findByMembers(membres);
		if (modele != null) {
			// le modèle existe déjà, tout va bien
			return modele;
		}

		// le modèle n'existe pas encore : on le créé (dans une nouvelle transaction !)
		createModeleCommunaute(membres);

		// il doit exister maintenant
		modele = modeleCommunauteRFDAO.findByMembers(membres);
		if (modele == null) {
			final Object[] ids = membres.stream()
					.map(AyantDroitRF::getId)
					.sorted(Comparator.naturalOrder())
					.toArray();
			throw new RuntimeException("Impossible de créer un modèle de communauté sur les membres = "  + Arrays.toString(ids));
		}

		return modele;
	}

	/**
	 * Creé un nouveau modèle de communauté dans une transaction séparée.
	 *
	 * @param membres les membres de la communauté
	 */
	private void createModeleCommunaute(@NotNull Set<? extends AyantDroitRF> membres) {
		synchronized (this) {
			final ModeleCommunauteRF m = modeleCommunauteRFDAO.findByMembers(membres);
			if (m != null) {
				// la communauté a été créée entre-temps, rien à faire
				return;
			}

			final Set<Long> membresIds = membres.stream()
					.map(AyantDroitRF::getId)
					.collect(Collectors.toSet());

			// on créé la communauté dans une nouvelle transaction pour qu'elle soit
			// immédiatement visible au sortir de la méthode (qui est synchronisée pour
			// éviter de créer plusieurs fois le même modèle).
			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			template.execute(status -> modeleCommunauteRFDAO.createWith(membresIds));
		}
	}

	@NotNull
	@Override
	public String getCapitastraURL(long immeubleId) throws ObjectNotFoundException {

		final ImmeubleRF immeuble = immeubleRFDAO.get(immeubleId);
		if (immeuble == null) {
			throw new ObjectNotFoundException("L'immeuble RF avec l'id=[" + immeubleId + "] est inconnu.");
		}

		// on va chercher la dernière situation
		final SituationRF situation = immeuble.getSituations().stream()
				.filter(AnnulableHelper::nonAnnule)
				.max(Comparator.naturalOrder())
				.orElseThrow(() -> new IllegalArgumentException("L'immeuble id=[" + immeubleId + "] ne possède pas de situation"));

		// on prépare les paramètres de l'URL
		final String noCommuneRF = String.valueOf(situation.getCommune().getNoRf());
		final String noParcelle = String.valueOf(situation.getNoParcelle());
		final String index1 = Optional.ofNullable(situation.getIndex1())
				.map(String::valueOf)
				.orElse(StringUtils.EMPTY);
		final String index2 = Optional.ofNullable(situation.getIndex2())
				.map(String::valueOf)
				.orElse(StringUtils.EMPTY);
		final String index3 = Optional.ofNullable(situation.getIndex3())
				.map(String::valueOf)
				.orElse(StringUtils.EMPTY);

		// on résout l'URL
		final String urlPattern = infraService.getUrlBrutte(ApplicationFiscale.CAPITASTRA);
		if (urlPattern == null) {
			throw new ObjectNotFoundException("L'url de connexion à CAPITASTRA n'est pas définie dans Fidor.");
		}
		return urlPattern.replaceAll("\\{noCommune\\}", noCommuneRF)
				.replaceAll("\\{noParcelle\\}", noParcelle)
				.replaceAll("\\{index1\\}", index1)
				.replaceAll("\\{index2\\}", index2)
				.replaceAll("\\{index3\\}", index3);
	}

	@Nullable
	@Override
	public Long getContribuableIdFor(@NotNull TiersRF tiersRF) {
		return ayantDroitRFDAO.getContribuableIdFor(tiersRF);
	}

	@Nullable
	@Override
	public Commune getCommune(ImmeubleRF immeuble, RegDate dateReference) {
		final SituationRF situation = getSituation(immeuble, dateReference);
		return Optional.ofNullable(situation)
				.map(SituationRF::getNoOfsCommune)
				.map(ofs -> infraService.getCommuneByNumeroOfs(ofs, dateReference))
				.orElse(null);
	}

	@Nullable
	@Override
	public EstimationRF getEstimationFiscale(ImmeubleRF immeuble, RegDate dateReference) {
		return immeuble.getEstimations().stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(est -> est.getRangeMetier().isValidAt(dateReference))
				.findFirst()
				.orElse(null);
	}

	@Nullable
	@Override
	public String getNumeroParcelleComplet(ImmeubleRF immeuble, RegDate dateReference) {
		final SituationRF situation = getSituation(immeuble, dateReference);
		if (situation != null) {
			return StringUtils.trimToNull(Stream.of(situation.getNoParcelle(), situation.getIndex1(), situation.getIndex2(), situation.getIndex3())
					                              .filter(Objects::nonNull)
					                              .map(String::valueOf)
					                              .collect(Collectors.joining("-")));
		}
		else {
			return null;
		}
	}

	@Nullable
	@Override
	public SituationRF getSituation(ImmeubleRF immeuble, RegDate dateReference) {
		final Optional<SituationRF> atOrNext = immeuble.getSituations().stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(s -> s.isValidAt(dateReference) || RegDateHelper.isBefore(dateReference, s.getDateDebut(), NullDateBehavior.EARLIEST))
				.min(Comparator.comparing(SituationRF::getDateDebut, Comparator.nullsFirst(Comparator.naturalOrder())));

		if (atOrNext.isPresent()) {
			return atOrNext.get();
		}

		// rien trouvé après, essayons avant...
		return immeuble.getSituations().stream()
				.filter(AnnulableHelper::nonAnnule)
				.max(Comparator.comparing(SituationRF::getDateFin, NullDateBehavior.LATEST::compare))
				.orElse(null);
	}

	@Override
	public void surchargerCommuneFiscaleSituation(long situationId, @Nullable Integer noOfsCommune) {

		final SituationRF situation = situationRFDAO.get(situationId);
		if (situation == null) {
			throw new ObjectNotFoundException("La situation avec l'id=[" + situationId + "] n'existe pas.");
		}

		// on met-à-jour la situation
		situation.setNoOfsCommuneSurchargee(noOfsCommune);

		// on publie un événement fiscal
		evenementFiscalService.publierModificationSituationImmeuble(situation.getDateDebut(), situation.getImmeuble());
	}
}
