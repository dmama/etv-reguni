package ch.vd.uniregctb.mouvement;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.JobResults;

public class DeterminerMouvementsDossiersEnMasseResults extends JobResults<Long, DeterminerMouvementsDossiersEnMasseResults> {

	public enum Raison {

		PAS_OID_GESTION("Impossible de déterminer l'OID de gestion"),
		SOURCIER_PUR("Sourcier pur"),
		DEJA_ARCHIVE("Dossier déjà archivé"),
		EXCEPTION(EXCEPTION_DESCRIPTION);

		private final String description;

		private Raison(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public abstract static class Info {
		public final long noCtb;

		public Info(long noCtb) {
			this.noCtb = noCtb;
		}

		public abstract String getTypeInformation();
	}

	public static class NonTraite extends Info {

		public final Raison type;

		public final String complement;

		public NonTraite(long noCtb, Raison type, String complement) {
			super(noCtb);
			this.type = type;
			this.complement = complement;
		}

		@Override
		public String getTypeInformation() {
			return type.description();
		}
	}

	public abstract static class Mouvement extends Info {

		public final int oidActuel;

		public Mouvement(long noCtb, int oidActuel) {
			super(noCtb);
			this.oidActuel = oidActuel;
		}
	}

	public static class MouvementOid extends Mouvement {

		private static final String TEXTE = "Changement d'OID";

		public final int oidDestination;

		public MouvementOid(long noCtb, int oidActuel, int oidDestination) {
			super(noCtb, oidActuel);
			this.oidDestination = oidDestination;
		}

		@Override
		public String getTypeInformation() {
			return TEXTE;
		}
	}

	public static class MouvementArchives extends Mouvement {

		private static final String TEXTE = "Archivage";

		public MouvementArchives(long noCtb, int oidActuel) {
			super(noCtb, oidActuel);
		}

		@Override
		public String getTypeInformation() {
			return TEXTE;
		}
	}

	public final List<NonTraite> erreurs = new LinkedList<NonTraite>();

	public final List<NonTraite> ignores = new LinkedList<NonTraite>();

	public final List<Mouvement> mouvements = new LinkedList<Mouvement>();

	public final RegDate dateTraitement;

	public final boolean archivesSeulement;

	private int nbContribuablesInspectes = 0;

	private boolean interrompu;

	public DeterminerMouvementsDossiersEnMasseResults(RegDate dateTraitement, boolean archivesSeulement) {
		this.dateTraitement = dateTraitement;
		this.archivesSeulement = archivesSeulement;
	}

	public boolean isInterrompu() {
		return interrompu;
	}

	public void setInterrompu(boolean interrompu) {
		this.interrompu = interrompu;
	}

	public void addMouvementVersAutreCollectiviteAdministrative(long numero, int oidAvant, int oidApres) {
		mouvements.add(new MouvementOid(numero, oidAvant, oidApres));
	}

	public void addMouvementVersArchives(long numero, int oid) {
		mouvements.add(new MouvementArchives(numero, oid));
	}

	public void addMouvementNonGenereVersArchivesCarDejaExistant(long numero) {
		addIgnore(numero, null, Raison.DEJA_ARCHIVE);
	}

	public void addErreurDeterminationOidGestion(long numero, RegDate date) {
		erreurs.add(new NonTraite(numero, Raison.PAS_OID_GESTION, RegDateHelper.dateToDisplayString(date)));
	}

	public void addSourcierPurIgnore(long numero) {
		addIgnore(numero, null, Raison.SOURCIER_PUR);
	}

	private void addIgnore(long numero, RegDate date, Raison raison) {
		boolean dejaConnu = false;
		if (ignores.size() > 0) {
			final NonTraite precedent = ignores.get(ignores.size() - 1);
			dejaConnu = (precedent.noCtb == numero && precedent.type == raison);
			if (dejaConnu && date != null) {
				final NonTraite nouvelElement = new NonTraite(numero, raison, String.format("%s, %s", precedent.complement, RegDateHelper.dateToDisplayString(date)));
				ignores.set(ignores.size() - 1, nouvelElement);
			}
		}
		if (!dejaConnu) {
			ignores.add(new NonTraite(numero, raison, RegDateHelper.dateToDisplayString(date)));
		}
	}

	public void addErrorException(Long element, Exception e) {
		final String msg;
		if (!StringUtils.isBlank(e.getMessage())) {
			msg = e.getMessage();
		}
		else {
			msg = e.getClass().getName();
		}
		erreurs.add(new NonTraite(element != null ? element : -1, Raison.EXCEPTION, msg));
	}

	public void addAll(DeterminerMouvementsDossiersEnMasseResults right) {
		mouvements.addAll(right.mouvements);
		erreurs.addAll(right.erreurs);
		ignores.addAll(right.ignores);
		nbContribuablesInspectes += right.nbContribuablesInspectes;
	}

	public void addContribuableInspecte() {
		++ nbContribuablesInspectes;
	}

	public int getNbContribuablesInspectes() {
		return nbContribuablesInspectes;
	}
}
