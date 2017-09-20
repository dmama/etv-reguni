package ch.vd.uniregctb.registrefoncier.rattrapage;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleBeneficiaireRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.RapprochementRF;
import ch.vd.uniregctb.registrefoncier.TiersRF;

public class RattrapageModelesCommunautesRFProcessorResults extends JobResults<Long, RattrapageModelesCommunautesRFProcessorResults> {

	private final int nbThreads;

	private boolean interrompu = false;

	private int processed = 0;
	private final List<Updated> updated = new LinkedList<>();
	private final List<Erreur> erreurs = new LinkedList<>();

	public final class Updated {

		private final long communauteId;
		private final List<String> membres;
		private final String egrid;
		private final String idRF;

		public Updated(@NotNull CommunauteRF communaute) {
			this.communauteId = communaute.getId();
			this.membres = communaute.getMembres().stream()
					.map(DroitProprieteRF::getAyantDroit)
					.map(RattrapageModelesCommunautesRFProcessorResults.this::buildLogName)
					.collect(Collectors.toList());
			final Optional<ImmeubleRF> immeuble = communaute.getDroitsPropriete().stream()
					.findAny()
					.map(DroitProprieteRF::getImmeuble);
			this.egrid = immeuble.map(ImmeubleRF::getEgrid).orElse(null);
			this.idRF = immeuble.map(ImmeubleRF::getIdRF).orElse(null);
		}

		public long getCommunauteId() {
			return communauteId;
		}

		public List<String> getMembres() {
			return membres;
		}

		public String getEgrid() {
			return egrid;
		}

		public String getIdRF() {
			return idRF;
		}
	}

	/**
	 * Construit le nom de logging de l'ayant-droit spécifié.
	 *
	 * @param ayantDroit un ayant-droit
	 * @return un nom suffisamment précis pour le logging
	 */
	private String buildLogName(AyantDroitRF ayantDroit) {
		if (ayantDroit instanceof CommunauteRF) {
			final CommunauteRF communaute = (CommunauteRF) ayantDroit;
			return "Communauté n°" + communaute.getId();
		}
		else if (ayantDroit instanceof ImmeubleBeneficiaireRF) {
			final ImmeubleBeneficiaireRF immeuble = (ImmeubleBeneficiaireRF) ayantDroit;
			final String egrid = immeuble.getImmeuble().getEgrid();
			return "Immeuble " + (egrid == null ? immeuble.getIdRF() : egrid);
		}
		else if (ayantDroit instanceof TiersRF) {
			final TiersRF tiers = (TiersRF) ayantDroit;
			final RapprochementRF rapprochement = tiers.getRapprochements().stream()
					.filter(r -> r.isValidAt(null))
					.findFirst()
					.orElse(null);
			if (rapprochement == null) {
				return "Tiers non-rapproché n°" + tiers.getId();
			}
			else {
				return "Contribuable n°" + rapprochement.getContribuable().getId();
			}
		}
		else {
			throw new IllegalArgumentException("Type d'ayant-droit inconnu = [" + ayantDroit.getClass().getName() + "]");
		}
	}


	public static final class Erreur {
		private final long communauteId;
		private final String message;

		public Erreur(long communauteId, String message) {
			this.communauteId = communauteId;
			this.message = message;
		}

		public Erreur(long communauteId, Exception e) {
			this(communauteId, String.format("%s: %s", e.getClass().getName(), e.getMessage()));
		}

		public long getCommunauteId() {
			return communauteId;
		}

		public String getMessage() {
			return message;
		}
	}

	public RattrapageModelesCommunautesRFProcessorResults(int nbThreads) {
		super(null, null);
		this.nbThreads = nbThreads;
	}

	public int getNbThreads() {
		return nbThreads;
	}

	public boolean isInterrompu() {
		return interrompu;
	}

	public void setInterrompu(boolean interrompu) {
		this.interrompu = interrompu;
	}

	public int getProcessed() {
		return processed;
	}

	public List<Updated> getUpdated() {
		return updated;
	}

	public List<Erreur> getErreurs() {
		return erreurs;
	}

	@Override
	public void addErrorException(Long element, Exception e) {
		this.erreurs.add(new Erreur(element, e));
	}

	public void addProcessed() {
		this.processed++;
	}

	public void addUpdated(@NotNull CommunauteRF communaute) {
		this.updated.add(new Updated(communaute));
	}

	@Override
	public void addAll(RattrapageModelesCommunautesRFProcessorResults right) {
		this.processed += right.processed;
		this.updated.addAll(right.updated);
		this.erreurs.addAll(right.erreurs);
	}
}
