package ch.vd.uniregctb.registrefoncier;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.BatimentRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.tiers.Contribuable;

public class RegistreFoncierServiceImpl implements RegistreFoncierService {

	private ImmeubleRFDAO immeubleRFDAO;
	private BatimentRFDAO batimentRFDAO;
	private AyantDroitRFDAO ayantDroitRFDAO;
	private ServiceInfrastructureService infraService;

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
		return ctb.getRapprochementsRF().stream()
				.filter(Annulable::isNotAnnule)                         // on ignore les rapprochements annulés
				.flatMap(RegistreFoncierServiceImpl::getDroitsValides)  // on demande les droits valides pour le rapprochement
				.sorted(DroitRF::compareTo)                             // on trie les droits pour garder un ordre constant entre chaque appel
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
	 * @return les liste des droits valides pour le contribuable
	 */
	private static Stream<DroitRF> getDroitsValides(RapprochementRF rapprochement) {
		return rapprochement.getTiersRF().getDroits().stream()
				.filter(Annulable::isNotAnnule)
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
		return ayantDroitRFDAO.getCommunauteMembreInfo(communauteId);
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
				.filter(Annulable::isNotAnnule)
				.max(SituationRF::compareTo)
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
		final String urlPattern = infraService.getUrlVers(ApplicationFiscale.CAPITASTRA, 0L);
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
}
