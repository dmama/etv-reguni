package ch.vd.uniregctb.registrefoncier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Pair;
import ch.vd.unireg.common.NomPrenomDates;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.TiersNotFoundException;
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
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.FusionEntreprises;
import ch.vd.uniregctb.tiers.Heritage;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
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
	public List<DroitRF> getDroitsForCtb(@NotNull Contribuable ctb, boolean includeVirtualTransitive, boolean includeVirtualInheritance) {
		return getDroitsForCtb(ctb, false, includeVirtualTransitive, includeVirtualInheritance);
	}

	@Override
	public List<DroitRF> getDroitsForCtb(@NotNull Contribuable ctb, boolean prefetchSituationsImmeuble, boolean includeVirtualTransitive, boolean includeVirtualInheritance) {

		// on va chercher les droits du contribuable lui-même
		final List<DroitRF> droits = ctb.getRapprochementsRF().stream()
				.filter(AnnulableHelper::nonAnnule)                     // on ignore les rapprochements annulés
				.flatMap(rapp -> getDroitsValides(rapp,
				                                  prefetchSituationsImmeuble,
				                                  includeVirtualTransitive
				))  // on demande les droits valides pour le rapprochement
				.sorted()                             // on trie les droits pour garder un ordre constant entre chaque appel
				.collect(Collectors.toList());

		if (includeVirtualInheritance) {
			if (ctb instanceof PersonnePhysique) {
				// on détermine les liens d'héritage et on les regroupe par numéros de décédé
				final Map<Long, List<Heritage>> heritages = ctb.getRapportsSujet().stream()
						.filter(Heritage.class::isInstance)
						.map(Heritage.class::cast)
						.filter(AnnulableHelper::nonAnnule)
						.collect(Collectors.toMap(Heritage::getObjetId, Collections::singletonList, ListUtils::union));

				// on détermine les droits virtuels du tiers RF courant à partir des droits des tiers décédés dont il est l'héritier
				final List<DroitRF> droitsVirtuels = heritages.entrySet().stream()
						.map(this::resolveContribuable)
						.map(pair -> determineDroitsHeritageVirtuels(ctb, pair.getFirst(), pair.getSecond(), includeVirtualTransitive))
						.flatMap(Collection::stream)
						.collect(Collectors.toList());

				droits.addAll(droitsVirtuels);
			}
			else if (ctb instanceof Entreprise) {
				// on détermine les liens de fusion d'entreprises et on les regroupe par numéros d'entreprise absorbée
				final Map<Long, List<FusionEntreprises>> fusions = ctb.getRapportsObjet().stream()
						.filter(FusionEntreprises.class::isInstance)
						.map(FusionEntreprises.class::cast)
						.filter(AnnulableHelper::nonAnnule)
						.collect(Collectors.toMap(FusionEntreprises::getSujetId, Collections::singletonList, ListUtils::union));

				// on détermine les droits virtuels de l'entreprise RF courante à partir des droits des entreprises absorbées dont elle est le destinataire
				final List<DroitRF> droitsVirtuels = fusions.entrySet().stream()
						.map(this::resolveContribuable)
						.map(pair -> determineDroitsFusionVirtuels(ctb, pair.getFirst(), pair.getSecond(), includeVirtualTransitive))
						.flatMap(Collection::stream)
						.collect(Collectors.toList());

				droits.addAll(droitsVirtuels);
			}
		}

		return droits;
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
	 * @param rapprochement            un rapprochement entre un contribuable et un tiers RF
	 * @param fetchSituations          <code>true</code> s'il faut que les immeubles des droits retournés aient déjà leurs situations récupérées (optim)
	 * @param includeVirtualTransitive vrai s'il faut inclure les droits virtuels transitifs du contribuable
	 * @return les liste des droits valides pour le contribuable
	 */
	private Stream<DroitRF> getDroitsValides(RapprochementRF rapprochement, boolean fetchSituations, boolean includeVirtualTransitive) {
		final List<DroitRF> droits = getDroitsForTiersRF(rapprochement.getTiersRF(), fetchSituations, includeVirtualTransitive);
		return droits.stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(d -> DateRangeHelper.intersect(d, rapprochement));
	}

	List<DroitRF> getDroitsForTiersRF(AyantDroitRF ayantDroitRF, boolean prefetchSituationsImmeuble, boolean includeVirtualTransitive) {

		// on charge les droits réels
		final List<DroitRF> droits = droitRFDAO.findForAyantDroit(ayantDroitRF.getId(), prefetchSituationsImmeuble);

		if (includeVirtualTransitive) {
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

	@NotNull
	private <T extends RapportEntreTiers> Pair<Contribuable, List<T>> resolveContribuable(Map.Entry<Long, List<T>> entry) {
		final Contribuable decede = (Contribuable) tiersService.getTiers(entry.getKey());
		if (decede == null) {
			throw new TiersNotFoundException(entry.getKey());
		}
		return new Pair<>(decede, entry.getValue());
	}

	/**
	 * Détermine les droits d'héritage virtuels qui existent sur un tiers RF pour un décédé particulier.
	 *
	 * @param heritier                 le contribuable héritier Unireg
	 * @param decede                   le décédé
	 * @param heritages                les liens d'héritage entre le tiers et le décédé
	 * @param includeVirtualTransitive vrai s'il faut inclure les droits virtuels du décédé induits par des droits entre immeubles
	 * @return la liste des droits virtuels
	 */
	private List<DroitRF> determineDroitsHeritageVirtuels(@NotNull Contribuable heritier, Contribuable decede, List<Heritage> heritages, boolean includeVirtualTransitive) {

		final List<DroitRF> droitsDecede = getDroitsForCtb(decede, includeVirtualTransitive, false); // includeVirtualInheritance=false : on s'arrête au premier niveau d'héritage

		// [SIFISC-24999] les servitudes ne sont pas héritées, on les ignore
		final List<DroitRF> droitsProprieteDecede = droitsDecede.stream()
				.filter(d -> d instanceof DroitProprieteRF || d instanceof DroitProprieteVirtuelRF)
				.collect(Collectors.toList());

		final int nombreHeritiers = (int) decede.getRapportsObjet().stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(Heritage.class::isInstance)
				.count();

		return DroitRFHelper.extract(droitsProprieteDecede, heritages, (range, debut, fin) -> {
			final RegDate dateDebut = (debut == null ? range.getDateDebutMetier() : debut);
			final String motifDebut = (debut == null ? range.getMotifDebut() : "Succession");
			final RegDate dateFin = (fin == null ? range.getDateFinMetier() : fin);
			final String motifFin = (fin == null ? range.getMotifFin() : "Succession");
			final DroitVirtuelHeriteRF dv = new DroitVirtuelHeriteRF();
			dv.setDecedeId(decede.getNumero());
			dv.setHeritierId(heritier.getNumero());
			dv.setNombreHeritiers(nombreHeritiers);
			dv.setDateDebutMetier(dateDebut);
			dv.setDateFinMetier(dateFin);
			dv.setMotifDebut(motifDebut);
			dv.setMotifFin(motifFin);
			dv.setReference(range);
			return dv;
		});
	}

	/**
	 * Détermine les droits de fusion virtuels qui existent sur une entreprise RF pour une entreprise absorbée particulière.
	 *
	 * @param absorbante               l'entreprise absorbante Unireg
	 * @param absorbee                 l'entreprise absorbée
	 * @param fusions                  les liens de fusion entre les entreprises absorbées et l'entreprise absorbante
	 * @param includeVirtualTransitive vrai s'il faut inclure les droits virtuels du décédé induits par des droits entre immeubles
	 * @return la liste des droits virtuels
	 */
	private List<DroitRF> determineDroitsFusionVirtuels(@NotNull Contribuable absorbante, @NotNull Contribuable absorbee, @NotNull List<FusionEntreprises> fusions, boolean includeVirtualTransitive) {

		final List<DroitRF> droitsEntrepriseAbsorbee = getDroitsForCtb(absorbee, includeVirtualTransitive, false); // includeVirtualInheritance=false : on s'arrête au premier niveau de fusion

		final int nombreEntrepriseAbsorbantes = 1;  // par définition, dans une fusion d'entreprises, il n'y a qu'une entreprise absorbante.

		return DroitRFHelper.extract(droitsEntrepriseAbsorbee, fusions, (range, debut, fin) -> {
			final RegDate dateDebut = (debut == null ? range.getDateDebutMetier() : debut);
			final String motifDebut = (debut == null ? range.getMotifDebut() : "Fusion");
			final RegDate dateFin = (fin == null ? range.getDateFinMetier() : fin);
			final String motifFin = (fin == null ? range.getMotifFin() : "Fusion");
			final DroitVirtuelHeriteRF dv = new DroitVirtuelHeriteRF();
			dv.setDecedeId(absorbee.getNumero());
			dv.setHeritierId(absorbante.getNumero());
			dv.setNombreHeritiers(nombreEntrepriseAbsorbantes);
			dv.setDateDebutMetier(dateDebut);
			dv.setDateFinMetier(dateFin);
			dv.setMotifDebut(motifDebut);
			dv.setMotifFin(motifFin);
			dv.setReference(range);
			return dv;
		});
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

		// on va chercher les infos de la communauté
		final CommunauteRFMembreInfo info = communaute.buildMembreInfoNonTries();

		// [SIFISC-24595] on détermine l'historique des principaux
		final List<CommunauteRFPrincipalInfo> principaux = buildPrincipalHisto(communaute);
		info.setPrincipaux(principaux);

		// [SIFISC-23747] on trie la collection de tiers de telle manière que le leader de la communauté soit en première position
		final Long principalCtbId = principaux.stream()
				.max(new DateRangeComparator<>())
				.map(CommunauteRFPrincipalInfo::getCtbId)
				.orElse(null);
		info.sortMembers(new CommunauteRFMembreComparator(tiersService, principalCtbId));

		return info;
	}

	@Override
	@Nullable
	public Long getCommunauteCurrentPrincipalId(@NotNull CommunauteRF communaute) {

		// on va chercher les infos de la communauté
		final CommunauteRFMembreInfo info = communaute.buildMembreInfoNonTries();
		final Long principalCtbId = Optional.ofNullable(communaute.getPrincipalCommunauteDesigne())
				.filter(TiersRF.class::isInstance)
				.map(TiersRF.class::cast)
				.map(TiersRF::getCtbRapproche)
				.map(Tiers::getId)
				.orElse(null);

		if (principalCtbId != null) {
			// on a trouvé l'id
			return principalCtbId;
		}

		// [SIFISC-23747] on trie la collection de tiers de telle manière que le leader de la communauté soit en première position
		info.sortMembers(new CommunauteRFMembreComparator(tiersService, null));

		// on retourne l'id du principal par défaut
		final List<Long> ctbIds = info.getCtbIds();
		return ctbIds.isEmpty() ? null : ctbIds.get(0);
	}

	/**
	 * Construit la vue historique des principaux (par défaut + explicites) pour une communauté.
	 *
	 * @param communaute une modèle de communauté
	 * @return l'historique des principaux
	 */
	@NotNull
	public List<CommunauteRFPrincipalInfo> buildPrincipalHisto(@NotNull CommunauteRF communaute) {

		// on calcule l'historique regroupement par regroupement et on additionne bout-à-bout les périodes
		final List<CommunauteRFPrincipalInfo> histo = communaute.getRegroupements().stream()
				.filter(AnnulableHelper::nonAnnule)
				.sorted(new DateRangeComparator<>())
				.map(this::buildPrincipalHisto)
				.flatMap(Collection::stream)
				.collect(Collectors.toList());

		// on fusionne les périodes qui peuvent l'être
		return DateRangeHelper.collate(histo);
	}

	/**
	 * Construit la vue historique des principaux (par défaut + explicites) pour un regroupement de communauté.
	 *
	 * @param regroupement un regroupement de communauté
	 * @return l'historique des principaux
	 */
	@NotNull
	public List<CommunauteRFPrincipalInfo> buildPrincipalHisto(@NotNull RegroupementCommunauteRF regroupement) {

		// on calcule l'histo du modèle de communauté
		final List<CommunauteRFPrincipalInfo> histo = buildPrincipalHisto(regroupement.getModele());

		// on le réduit à la durée de validité du regroupement
		return DateRangeHelper.extract(histo, regroupement.getDateDebut(), regroupement.getDateFin(), CommunauteRFPrincipalInfo::adapter);
	}

	@Override
	@NotNull
	public List<CommunauteRFPrincipalInfo> buildPrincipalHisto(@NotNull ModeleCommunauteRF modeleCommunaute) {

		// on détermine le principal par défaut
		final Long defaultPrincipal = modeleCommunaute.getMembres().stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(TiersRF.class::isInstance)
				.map(TiersRF.class::cast)
				.map(TiersRF::getCtbRapproche)
				.filter(Objects::nonNull)
				.map(Tiers::getNumero)
				.sorted(new CommunauteRFMembreComparator(tiersService, null))
				.findFirst()
				.orElse(null);

		// on crée l'historique (une seule valeur en fait) des principaux par défaut
		final List<CommunauteRFPrincipalInfo> defaultHisto = new ArrayList<>();
		if (defaultPrincipal != null) {
			defaultHisto.add(new CommunauteRFPrincipalInfo(null, null, null, null, defaultPrincipal, true));
		}

		// on crée l'historique des principaux explicitement désignés
		final Set<PrincipalCommunauteRF> principaux = modeleCommunaute.getPrincipaux();
		final List<CommunauteRFPrincipalInfo> principauxInfo = (principaux == null ? Collections.emptyList() : principaux.stream()
				.filter(AnnulableHelper::nonAnnule)
				.map(CommunauteRFPrincipalInfo::get)
				.filter(Objects::nonNull)
				.sorted(new DateRangeComparator<>())
				.collect(Collectors.toList()));

		// on les combine ensemble
		final List<CommunauteRFPrincipalInfo> histo = DateRangeHelper.override(defaultHisto, principauxInfo, CommunauteRFPrincipalInfo::adapter);

		// on fusionne les périodes qui peuvent l'être
		return DateRangeHelper.collate(histo);
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

	@Override
	public void addPrincipalToModeleCommunaute(@NotNull TiersRF membre, @NotNull ModeleCommunauteRF modele, @NotNull RegDate dateDebut) {

		if (modele.getMembres().stream()
				.noneMatch(m -> m.getId().equals(membre.getId()))) {
			throw new IllegalArgumentException("L'ayant-droit id=[" + membre.getId() + "] ne fait pas partie des membres du modèle de communauté id=[" + modele.getId() + "]");
		}

		if (modele.getPrincipaux().stream()
				.filter(AnnulableHelper::nonAnnule)
				.anyMatch(m -> m.getDateDebut() == dateDebut)) {
			throw new IllegalArgumentException("La date [] est déjà utilisée comme date de début d'un principal du modèle de communauté id=[" + modele.getId() + "]");
		}

		// on ajoute le principal
		final PrincipalCommunauteRF principal = new PrincipalCommunauteRF();
		principal.setPrincipal(membre);
		principal.setDateDebut(dateDebut);
		principal.setModeleCommunaute(modele);
		modele.addPrincipal(principal);

		// on recalcule les dates de fin
		recalculeDatesFins(modele);

		// on publie un événement fiscal sur toutes les communautés impactées
		modele.getRegroupements().stream()
				.filter(AnnulableHelper::nonAnnule)
				.map(RegroupementCommunauteRF::getCommunaute)
				.forEach(communaute -> evenementFiscalService.publierModificationPrincipalCommunaute(dateDebut, communaute));
	}

	@Override
	public void cancelPrincipalCommunaute(@NotNull PrincipalCommunauteRF principal) {

		// on annule le principal
		principal.setAnnule(true);

		// on recalcule les dates de fin
		final ModeleCommunauteRF modele = principal.getModeleCommunaute();
		recalculeDatesFins(modele);

		// on publie un événement fiscal sur toutes les communautés impactées
		modele.getRegroupements().stream()
				.filter(AnnulableHelper::nonAnnule)
				.map(RegroupementCommunauteRF::getCommunaute)
				.forEach(communaute -> evenementFiscalService.publierModificationPrincipalCommunaute(null, communaute));
	}

	@Override
	public @Nullable AyantDroitRF getAyantDroit(long ayantDroitId) {
		return ayantDroitRFDAO.get(ayantDroitId);
	}

	@Override
	public @NotNull NomPrenomDates getDecompositionNomPrenomDateNaissanceRF(@NotNull TiersRF tiers) {
		if (tiers instanceof PersonnePhysiqueRF) {
			final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) tiers;
			return new NomPrenomDates(pp.getNom(), pp.getPrenom(), pp.getDateNaissance(), null);
		}
		else if (tiers instanceof PersonneMoraleRF) {
			final PersonneMoraleRF pm = (PersonneMoraleRF) tiers;
			return new NomPrenomDates(pm.getRaisonSociale(), null, null, null);
		}
		else if (tiers instanceof CollectivitePubliqueRF) {
			final CollectivitePubliqueRF coll = (CollectivitePubliqueRF) tiers;
			return new NomPrenomDates(coll.getRaisonSociale(), null, null, null);
		}
		else {
			throw new IllegalArgumentException("Type de tiers RF inconnu = [" + tiers.getClass() + "]");
		}
	}

	private static void recalculeDatesFins(@NotNull ModeleCommunauteRF modele) {
		final List<PrincipalCommunauteRF> principaux = modele.getPrincipaux().stream()
				.filter(AnnulableHelper::nonAnnule)
				.sorted(Comparator.comparing(HibernateDateRangeEntity::getDateDebut, NullDateBehavior.EARLIEST::compare))
				.collect(Collectors.toList());
		PrincipalCommunauteRF previous = null;
		for (PrincipalCommunauteRF current : principaux) {
			current.setDateFin(null);
			if (previous != null) {
				previous.setDateFin(current.getDateDebut().getOneDayBefore());
			}
			previous = current;
		}
	}
}
