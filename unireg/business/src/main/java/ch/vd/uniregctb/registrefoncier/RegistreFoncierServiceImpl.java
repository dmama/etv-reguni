package ch.vd.uniregctb.registrefoncier;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.BatimentRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.DroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersService;

public class RegistreFoncierServiceImpl implements RegistreFoncierService {

	private DroitRFDAO droitRFDAO;
	private TiersService tiersService;
	private ImmeubleRFDAO immeubleRFDAO;
	private BatimentRFDAO batimentRFDAO;
	private AyantDroitRFDAO ayantDroitRFDAO;
	private ServiceInfrastructureService infraService;

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

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	@NotNull
	@Override
	public List<DroitRF> getDroitsForCtb(@NotNull Contribuable ctb) {
		return getDroitsForCtb(ctb, false);
	}

	@NotNull
	@Override
	public List<DroitRF> getDroitsForCtb(@NotNull Contribuable ctb, boolean prefetchSituationsImmeuble) {
		return ctb.getRapprochementsRF().stream()
				.filter(AnnulableHelper::nonAnnule)                     // on ignore les rapprochements annulés
				.flatMap(rapp -> getDroitsValides(rapp, prefetchSituationsImmeuble))  // on demande les droits valides pour le rapprochement
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
	 * @param rapprochement un rapprochement entre un contribuable et un tiers RF
	 * @param fetchSituations <code>true</code> s'il faut que les immeubles des droits retournés aient déjà leurs situations récupérées (optim)
	 * @return les liste des droits valides pour le contribuable
	 */
	private Stream<DroitRF> getDroitsValides(RapprochementRF rapprochement, boolean fetchSituations) {
		return droitRFDAO.findForAyantDroit(rapprochement.getTiersRF().getId(), fetchSituations).stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(d -> DateRangeHelper.intersect(d, rapprochement));
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

	@Nullable
	@Override
	public CommunauteRFMembreInfo getCommunauteMembreInfo(long communauteId) {
		final CommunauteRFMembreInfo info = ayantDroitRFDAO.getCommunauteMembreInfo(communauteId);
		// [SIFISC-23747] on trie la collection de tiers de telle manière que le leader de la communauté soit en première position
		if (info != null) {
			info.sortMembers(new CommunauteRFMembreComparator(tiersService));
		}
		return info;
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
				.map(SituationRF::getCommune)
				.map(CommuneRF::getNoOfs)
				.map(ofs -> infraService.getCommuneByNumeroOfs(ofs, dateReference))
				.filter(Objects::nonNull)           // au cas où le numéro de commune est inconnu de l'infrastructure (c'est possible), pour ne pas finir sur une NPE...
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
}
