package ch.vd.uniregctb.tiers.rattrapage.flaghabitant;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;

public class CorrectionFlagHabitantResults extends JobResults<Long, CorrectionFlagHabitantResults> {

	private boolean interrupted;

	public enum Message {
		PP_NOUVEL_HABITANT("Personne physique changée en habitant en raison de la présence d'une adresse de résidence active dans le canton"),
		PP_NOUVEAU_NON_HABITANT("Personne physique changée en non-habitant en raison de l'absence d'une adresse de résidence active dans le canton (ou d'un décès)"),
		MC_FOR_VD_SANS_HABITANT("Le for principal actif du ménage est vaudois alors qu'aucun des membres n'est habitant"),
		MC_FOR_HC_HS_AVEC_HABITANT("Le for principal actif du ménage est non-vaudois alors qu'au moins un des membres est habitant"),
		EXCEPTION(EXCEPTION_DESCRIPTION);

		private final String libelle;

		Message(String libelle) {
			this.libelle = libelle;
		}

		public String getLibelle() {
			return libelle;
		}
	}

	public static final Comparator<ContribuableInfo> COMPARATOR = Comparator.comparingLong(ContribuableInfo::getNoCtb);

	public static class ContribuableInfo {

		private final long noCtb;
		private final Message message;

		public ContribuableInfo(long noCtb, Message message) {
			this.noCtb = noCtb;
			this.message = message;
		}

		public long getNoCtb() {
			return noCtb;
		}

		public Message getMessage() {
			return message;
		}
	}

	public static class ContribuableErreur extends ContribuableInfo {

		private static final String EMPTY_STRING = "";

		public ContribuableErreur(long noCtb, Message message) {
			super(noCtb, message);
		}

		public String getComplementInfo() {
			return EMPTY_STRING;
		}
	}

	public static class ContribuableException extends ContribuableErreur {

		private final String messageException;

		public ContribuableException(long noCtb, String messageException) {
			super(noCtb, Message.EXCEPTION);
			this.messageException = messageException;
		}

		@Override
		public String getComplementInfo() {
			return messageException;
		}
	}

	public boolean isInterrupted() {
		return interrupted;
	}

	public void setInterrupted(boolean interrupted) {
		this.interrupted = interrupted;
	}

	private int nombrePPInspectees;
	private final List<ContribuableInfo> nouveauxHabitants = new LinkedList<>();
	private final List<ContribuableInfo> nouveauxNonHabitants = new LinkedList<>();
	private final List<ContribuableErreur> erreurs = new LinkedList<>();

	public CorrectionFlagHabitantResults(TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
	}

	public void incPPInespectee() {
		nombrePPInspectees++;
	}

	public void addHabitantChangeEnNonHabitant(PersonnePhysique pp) {
		nouveauxNonHabitants.add(new ContribuableInfo(pp.getNumero(), Message.PP_NOUVEAU_NON_HABITANT));
	}

	public void addNonHabitantChangeEnHabitant(PersonnePhysique pp) {
		nouveauxHabitants.add(new ContribuableInfo(pp.getNumero(), Message.PP_NOUVEL_HABITANT));
	}

	@Override
	public void addErrorException(Long noCtb, Exception e) {
		final String message = (StringUtils.isEmpty(e.getMessage()) ? e.getClass().getName() : e.getMessage());
		erreurs.add(new ContribuableException(noCtb, message));
	}

	@Override
	public void addAll(CorrectionFlagHabitantResults right) {
		nombrePPInspectees += right.nombrePPInspectees;
		nouveauxHabitants.addAll(right.nouveauxHabitants);
		nouveauxNonHabitants.addAll(right.nouveauxNonHabitants);
		erreurs.addAll(right.erreurs);
	}

	public List<ContribuableInfo> getNouveauxHabitants() {
		return nouveauxHabitants;
	}

	public List<ContribuableInfo> getNouveauxNonHabitants() {
		return nouveauxNonHabitants;
	}

	public List<ContribuableErreur> getErreurs() {
		return erreurs;
	}

	public void sort() {
		Collections.sort(nouveauxHabitants, COMPARATOR);
		Collections.sort(nouveauxNonHabitants, COMPARATOR);
		Collections.sort(erreurs, COMPARATOR);
	}

	public final int getNombrePPInspectees() {
		return nombrePPInspectees;
	}

	public final int getNombrePersonnesPhysiquesModifiees() {
		return nouveauxHabitants.size() + nouveauxNonHabitants.size();
	}
}
