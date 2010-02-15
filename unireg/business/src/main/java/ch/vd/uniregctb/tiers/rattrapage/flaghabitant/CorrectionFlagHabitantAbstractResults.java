package ch.vd.uniregctb.tiers.rattrapage.flaghabitant;

import ch.vd.uniregctb.common.JobResults;

import java.util.Comparator;

public abstract class CorrectionFlagHabitantAbstractResults<T extends CorrectionFlagHabitantAbstractResults> extends JobResults<Long, T> {

	private boolean interrupted;

	public static enum Message {
		PP_NOUVEL_HABITANT("Personne physique changée en habitant en raison de la présence d'un for principal vaudois actif"),
		PP_NOUVEAU_NON_HABITANT("Personne physique changée en non-habitant en raison de la présence d'un for principal non vaudois actif"),
		PP_NON_HABITANT_SANS_NUMERO_INDIVIDU("Personne physique avec for principal vaudois actif mais sans numéro d'individu"),
		MC_FOR_VD_SANS_HABITANT("Le for principal actif du ménage est vaudois alors qu'aucun des membres n'est habitant"),
		MC_FOR_HC_HS_AVEC_HABITANT("Le for principal actif du ménage est non-vaudois alors qu'au moins un des membres est habitant"),
		EXCEPTION(EXCEPTION_DESCRIPTION);

		private final String libelle;

		private Message(String libelle) {
			this.libelle = libelle;
		}

		public String getLibelle() {
			return libelle;
		}
	}

	public static final Comparator<ContribuableInfo> COMPARATOR = new Comparator<ContribuableInfo>() {
		public int compare(ContribuableInfo o1, ContribuableInfo o2) {
			return o1.noCtb < o2.noCtb ? -1 : (o1.noCtb > o2.noCtb ? 1 : 0);
		}
	};

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
}
