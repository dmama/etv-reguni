package ch.vd.unireg.regimefiscal.changement;

import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AbstractJobResults;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;

public class ChangementRegimesFiscauxJobResults extends AbstractJobResults<Long, ChangementRegimesFiscauxJobResults> {

	public abstract static class Info {

		public final long entrepriseId;

		protected Info(long entrepriseId) {
			this.entrepriseId = entrepriseId;
		}
	}

	public static class TraiteInfo extends Info {

		public final String raisonSociale;
		public final RegDate dateDeCreation;

		public TraiteInfo(long entrepriseId, String raisonSociale, RegDate dateDeCreation) {
			super(entrepriseId);
			this.raisonSociale = raisonSociale;
			this.dateDeCreation = dateDeCreation;
		}
	}

	public static class ErreurInfo extends Info {

		public final String raisonSociale;
		public final RegDate dateDeCreation;
		public final String message;

		public ErreurInfo(long entrepriseId, String raisonSociale, RegDate dateDeCreation, String message) {
			super(entrepriseId);
			this.raisonSociale = raisonSociale;
			this.dateDeCreation = dateDeCreation;
			this.message = message;
		}
	}

	// Paramètres d'entrée
	private final TypeRegimeFiscal ancienType;
	private final TypeRegimeFiscal nouveauType;
	private final RegDate dateChangement;

	private int total;
	private final List<TraiteInfo> traites = new LinkedList<>();
	private final List<ErreurInfo> erreurs = new LinkedList<>();
	private boolean interrupted = false;

	public ChangementRegimesFiscauxJobResults(TypeRegimeFiscal ancienType, TypeRegimeFiscal nouveauType, RegDate dateChangement) {
		this.ancienType = ancienType;
		this.nouveauType = nouveauType;
		this.dateChangement = dateChangement;
	}

	public void addTraite(long entrepriseId, String raisonSociale, RegDate dateDeCreation) {
		traites.add(new TraiteInfo(entrepriseId, raisonSociale, dateDeCreation));
	}

	public void addErreur(long entrepriseId, String raisonSociale, RegDate dateDeCreation, @NotNull String message) {
		erreurs.add(new ErreurInfo(entrepriseId, raisonSociale, dateDeCreation, message));
	}

	@Override
	public void addErrorException(Long entrepriseId, Exception e) {
		erreurs.add(new ErreurInfo(entrepriseId, null, null, String.format("%s: %s", e.getClass().getName(), e.getMessage())));
	}

	@Override
	public void addAll(ChangementRegimesFiscauxJobResults right) {
		traites.addAll(right.traites);
		erreurs.addAll(right.erreurs);
	}

	public TypeRegimeFiscal getAncienType() {
		return ancienType;
	}

	public TypeRegimeFiscal getNouveauType() {
		return nouveauType;
	}

	public RegDate getDateChangement() {
		return dateChangement;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public boolean isInterrupted() {
		return interrupted;
	}

	public void setInterrupted(boolean interrupted) {
		this.interrupted = interrupted;
	}

	public List<TraiteInfo> getTraites() {
		return traites;
	}

	public List<ErreurInfo> getErreurs() {
		return erreurs;
	}
}
