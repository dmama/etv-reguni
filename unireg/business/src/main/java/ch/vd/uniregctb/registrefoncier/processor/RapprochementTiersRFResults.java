package ch.vd.uniregctb.registrefoncier.processor;

import java.util.LinkedList;
import java.util.List;

import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.registrefoncier.RapprochementRF;
import ch.vd.uniregctb.registrefoncier.TiersRF;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeRapprochementRF;

public class RapprochementTiersRFResults extends JobResults<Long, RapprochementTiersRFResults> {

	private static final String ERREUR_TIERS_IDENTIFIE_NON_CONTRIBUABLE = "Le tiers identifié n'est pas un contribuable.";
	private static final String ERREUR_TIERS_RF_DEJA_RAPPROCHE_A_DATE_DE_TRAITEMENT = "Le tiers RF possède déjà un rapprochement valide à la date de traitement.";

	private boolean interrompu = false;
	private final List<NouveauRapprochement> nouveauxRapprochements = new LinkedList<>();
	private final List<ErreurRapprochement> erreurs = new LinkedList<>();
	private final List<NonIdentification> nonIdentifications = new LinkedList<>();

	public static final class NouveauRapprochement {
		public final long idTiersRF;
		public final long idContribuable;
		public final TypeRapprochementRF type;

		public NouveauRapprochement(RapprochementRF rapprochement) {
			this.idTiersRF = rapprochement.getTiersRF().getId();
			this.idContribuable = rapprochement.getContribuable().getNumero();
			this.type = rapprochement.getTypeRapprochement();
		}

		@Override
		public String toString() {
			return "NouveauRapprochement{" +
					"idTiersRF=" + idTiersRF +
					", idContribuable=" + idContribuable +
					", type=" + type +
					'}';
		}
	}

	public static final class ErreurRapprochement {
		public final long idTiersRF;
		public final Long idContribuable;
		public final String message;

		public ErreurRapprochement(long idTiersRF, Long idContribuable, String message) {
			this.idTiersRF = idTiersRF;
			this.idContribuable = idContribuable;
			this.message = message;
		}

		@Override
		public String toString() {
			return "ErreurRapprochement{" +
					"idTiersRF=" + idTiersRF +
					", idContribuable=" + idContribuable +
					", message='" + message + '\'' +
					'}';
		}
	}

	public static final class NonIdentification {
		public final long idTiersRF;
		public final List<Long> candidats;

		public NonIdentification(long idTiersRF, List<Long> candidats) {
			this.idTiersRF = idTiersRF;
			this.candidats = candidats;
		}

		@Override
		public String toString() {
			return "NonIdentification{" +
					"idTiersRF=" + idTiersRF +
					", candidats=" + candidats +
					'}';
		}
	}

	public RapprochementTiersRFResults(TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
	}

	public List<NouveauRapprochement> getNouveauxRapprochements() {
		return nouveauxRapprochements;
	}

	public List<ErreurRapprochement> getErreurs() {
		return erreurs;
	}

	public List<NonIdentification> getNonIdentifications() {
		return nonIdentifications;
	}

	public int getNbDossiersInspectes() {
		return getNbIdentifications() + getNbNonIdentifications() + getNbErreurs();
	}

	public int getNbIdentifications() {
		return nouveauxRapprochements.size();
	}

	public int getNbNonIdentifications() {
		return nonIdentifications.size();
	}

	public int getNbErreurs() {
		return erreurs.size();
	}

	@Override
	public void addAll(RapprochementTiersRFResults right) {
		nouveauxRapprochements.addAll(right.nouveauxRapprochements);
		erreurs.addAll(right.erreurs);
		nonIdentifications.addAll(right.nonIdentifications);
	}

	public boolean isInterrompu() {
		return interrompu;
	}

	public void setInterrompu(boolean interrompu) {
		this.interrompu = interrompu;
	}

	@Override
	public void addErrorException(Long idTiersRF, Exception e) {
		erreurs.add(new ErreurRapprochement(idTiersRF, null, String.format("Exception levée : %s\n%s", e.getMessage(), ExceptionUtils.extractCallStack(e))));
	}

	public void addTiersNonIdentifie(TiersRF tiersRF, List<Long> candidats) {
		nonIdentifications.add(new NonIdentification(tiersRF.getId(), candidats));
	}

	public void addErrorTiersIdentifiePasContribuable(TiersRF tiersRF, long idTiersUnireg) {
		erreurs.add(new ErreurRapprochement(tiersRF.getId(), idTiersUnireg, ERREUR_TIERS_IDENTIFIE_NON_CONTRIBUABLE));
	}

	public void addErrorRapprochementDejaPresentADateTraitement(TiersRF tiersRF) {
		erreurs.add(new ErreurRapprochement(tiersRF.getId(), null, ERREUR_TIERS_RF_DEJA_RAPPROCHE_A_DATE_DE_TRAITEMENT));
	}

	public void addNouveauRapprochement(RapprochementRF rapprochement) {
		nouveauxRapprochements.add(new NouveauRapprochement(rapprochement));
	}
}
