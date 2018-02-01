package ch.vd.unireg.documentfiscal;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AbstractJobResults;
import ch.vd.unireg.type.TypeLettreBienvenue;

public class EnvoiLettresBienvenueResults extends AbstractJobResults<Long, EnvoiLettresBienvenueResults> {

	public final RegDate dateTraitement;
	public final int delaiCarence;
	public final RegDate dateOrigine;
	public final int tailleMinimaleTrouAssujettissement;

	public enum RaisonIgnorement {
		NON_ASSUJETTI("Contribuable non-assujetti à la date de traitement."),
		LETTRE_NON_NECESSAIRE("Aucune nouvelle lettre de bienvenue à envoyer.");

		public final String libelle;

		RaisonIgnorement(String libelle) {
			this.libelle = libelle;
		}
	}

	public static class Ignore implements Comparable<Ignore> {

		public final long noCtb;
		public final RaisonIgnorement raison;

		public Ignore(long noCtb, RaisonIgnorement raison) {
			this.noCtb = noCtb;
			this.raison = raison;
		}

		@Override
		public int compareTo(@NotNull Ignore o) {
			return Long.compare(noCtb, o.noCtb);
		}
	}

	public static class EnErreur implements Comparable<EnErreur> {

		public final long noCtb;
		public final String msg;

		public EnErreur(long noCtb, String msg) {
			this.noCtb = noCtb;
			this.msg = msg;
		}

		@Override
		public int compareTo(@NotNull EnErreur o) {
			return Long.compare(noCtb, o.noCtb);
		}
	}

	public static class Traite implements Comparable<Traite> {

		public final long noCtb;
		public final TypeLettreBienvenue typeLettreEnvoyee;

		public Traite(long noCtb, TypeLettreBienvenue typeLettreEnvoyee) {
			this.noCtb = noCtb;
			this.typeLettreEnvoyee = typeLettreEnvoyee;
		}

		@Override
		public int compareTo(@NotNull Traite o) {
			return Long.compare(noCtb, o.noCtb);
		}
	}

	private final List<Ignore> ignores = new LinkedList<>();
	private final List<EnErreur> erreurs = new LinkedList<>();
	private final List<Traite> traites = new LinkedList<>();
	private boolean interrompu = false;

	public EnvoiLettresBienvenueResults(RegDate dateTraitement, int delaiCarence, RegDate dateOrigine, int tailleMinimaleTrouAssujettissement) {
		this.dateTraitement = dateTraitement;
		this.delaiCarence = delaiCarence;
		this.dateOrigine = dateOrigine;
		this.tailleMinimaleTrouAssujettissement = tailleMinimaleTrouAssujettissement;
	}

	public void addIgnoreNonAssujetti(Long element) {
		ignores.add(new Ignore(element, RaisonIgnorement.NON_ASSUJETTI));
	}

	public void addIgnoreLettreDejaEnvoyee(Long element) {
		ignores.add(new Ignore(element, RaisonIgnorement.LETTRE_NON_NECESSAIRE));
	}

	public void addLettreEnvoyee(Long element, TypeLettreBienvenue type) {
		traites.add(new Traite(element, type));
	}

	@Override
	public void addErrorException(Long element, Exception e) {
		erreurs.add(new EnErreur(element, e.getMessage()));
	}

	@Override
	public void addAll(EnvoiLettresBienvenueResults right) {
		ignores.addAll(right.ignores);
		erreurs.addAll(right.erreurs);
		traites.addAll(right.traites);
	}

	@Override
	public void end() {
		Collections.sort(ignores);
		Collections.sort(erreurs);
		Collections.sort(traites);
		super.end();
	}

	public void setInterrompu() {
		interrompu = true;
	}

	public boolean isInterrompu() {
		return interrompu;
	}

	public List<Ignore> getIgnores() {
		return ignores;
	}

	public List<EnErreur> getErreurs() {
		return erreurs;
	}

	public List<Traite> getTraites() {
		return traites;
	}
}
