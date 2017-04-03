package ch.vd.uniregctb.foncier;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.common.AbstractJobResults;
import ch.vd.uniregctb.registrefoncier.SituationRF;

public class RappelFormulairesDemandeDegrevementICIResults extends AbstractJobResults<Long, RappelFormulairesDemandeDegrevementICIResults> {

	public final RegDate dateTraitement;

	public enum RaisonIgnorement {
		FORMULAIRE_DEJA_RETOURNE("Le formulaire de demande de dégrèvement a déjà été retourné."),
		FORMULAIRE_DEJA_RAPPELE("Un rappel a déjà eu lieu."),
		DELAI_ADMINISTRATIF_NON_ECHU("Délai administratif non-échu.")
		;

		public final String libelle;

		RaisonIgnorement(String libelle) {
			this.libelle = libelle;
		}
	}

	private static final Comparator<Integer> NULLABLE_INTEGER_COMPARATOR = Comparator.nullsLast(Comparator.naturalOrder());
	private static final Comparator<Long> NULLABLE_LONG_COMPARATOR = Comparator.nullsLast(Comparator.naturalOrder());

	public static class Info<T extends Info<T>> implements Comparable<T> {

		public final Long noCtb;
		public final String nomCommune;
		public final Integer ofsCommune;
		public final Integer noParcelle;
		public final Integer index1;
		public final Integer index2;
		public final Integer index3;
		public final Integer periodeFiscale;
		public final long idFormulaire;

		public Info(long noCtb, int periodeFiscale, @Nullable SituationRF situation, @Nullable Commune commune, long idFormulaire) {
			this.noCtb = noCtb;
			if (situation != null) {
				this.noParcelle = situation.getNoParcelle();
				this.index1 = situation.getIndex1();
				this.index2 = situation.getIndex2();
				this.index3 = situation.getIndex3();
			}
			else {
				this.noParcelle = null;
				this.index1 = null;
				this.index2 = null;
				this.index3 = null;
			}
			if (commune != null) {
				this.nomCommune = commune.getNomOfficiel();
				this.ofsCommune = commune.getNoOFS();
			}
			else {
				this.nomCommune = null;
				this.ofsCommune = null;
			}
			this.periodeFiscale = periodeFiscale;
			this.idFormulaire = idFormulaire;
		}

		public Info(long idFormulaire) {
			this.noCtb = null;
			this.nomCommune = null;
			this.ofsCommune = null;
			this.noParcelle = null;
			this.index1 = null;
			this.index2 = null;
			this.index3 = null;
			this.periodeFiscale = null;
			this.idFormulaire = idFormulaire;
		}

		@Override
		public int compareTo(@NotNull T o) {
			int comparison = Objects.compare(noCtb, o.noCtb, NULLABLE_LONG_COMPARATOR);
			if (comparison == 0) {
				comparison = Objects.compare(ofsCommune, o.ofsCommune, NULLABLE_INTEGER_COMPARATOR);
			}
			if (comparison == 0) {
				comparison = Objects.compare(noParcelle, o.noParcelle, NULLABLE_INTEGER_COMPARATOR);
			}
			if (comparison == 0) {
				comparison = Objects.compare(index1, o.index1, NULLABLE_INTEGER_COMPARATOR);
			}
			if (comparison == 0) {
				comparison = Objects.compare(index2, o.index2, NULLABLE_INTEGER_COMPARATOR);
			}
			if (comparison == 0) {
				comparison = Objects.compare(index3, o.index3, NULLABLE_INTEGER_COMPARATOR);
			}
			if (comparison == 0) {
				comparison = Long.compare(idFormulaire, o.idFormulaire);
			}
			return comparison;
		}
	}

	public static class Ignore extends Info<Ignore> {

		public final RegDate dateEnvoi;
		public final RaisonIgnorement raison;

		public Ignore(long noCtb, int periodeFiscale, @Nullable SituationRF situation, @Nullable Commune commune, long idFormulaire, RegDate dateEnvoi, RaisonIgnorement raison) {
			super(noCtb, periodeFiscale, situation, commune, idFormulaire);
			this.dateEnvoi = dateEnvoi;
			this.raison = raison;
		}
	}

	public static class EnErreur extends Info<EnErreur> {

		public final String msg;

		public EnErreur(long idFormulaire, String msg) {
			super(idFormulaire);
			this.msg = msg;
		}

		public EnErreur(long noCtb, int periodeFiscale, @Nullable SituationRF situation, @Nullable Commune commune, long idFormulaire, String msg) {
			super(noCtb, periodeFiscale, situation, commune, idFormulaire);
			this.msg = msg;
		}
	}

	public static class Traite extends Info<Traite> {

		public final RegDate dateEnvoiFormulaire;

		public Traite(long noCtb, int periodeFiscale, @Nullable SituationRF situation, @Nullable Commune commune, long idFormulaire, RegDate dateEnvoiFormulaire) {
			super(noCtb, periodeFiscale, situation, commune, idFormulaire);
			this.dateEnvoiFormulaire = dateEnvoiFormulaire;
		}
	}

	private final List<Ignore> ignores = new LinkedList<>();
	private final List<EnErreur> erreurs = new LinkedList<>();
	private final List<Traite> traites = new LinkedList<>();
	private boolean interrompu = false;

	public RappelFormulairesDemandeDegrevementICIResults(RegDate dateTraitement) {
		this.dateTraitement = dateTraitement;
	}

	public void addFormulaireIgnore(long idContribuable, int periodeFiscale, @Nullable SituationRF situation, @Nullable Commune commune, long idFormulaire, RegDate dateEnvoi, RappelFormulairesDemandeDegrevementICIResults.RaisonIgnorement raison) {
		ignores.add(new Ignore(idContribuable, periodeFiscale, situation, commune, idFormulaire, dateEnvoi, raison));
	}

	public void addRappelEnvoye(long idContribuable, int periodeFiscale, @Nullable SituationRF situation, @Nullable Commune commune, long idFormulaire, RegDate dateEnvoiFormulaire) {
		traites.add(new Traite(idContribuable, periodeFiscale, situation, commune, idFormulaire, dateEnvoiFormulaire));
	}

	public void addRappelErreur(long idContribuable, int periodeFiscale, @Nullable SituationRF situation, @Nullable Commune commune, long idFormulaire, String message) {
		erreurs.add(new EnErreur(idContribuable, periodeFiscale, situation, commune, idFormulaire, message));
	}

	public boolean isInterrompu() {
		return interrompu;
	}

	public void setInterrompu(boolean interrompu) {
		this.interrompu = interrompu;
	}

	@Override
	public void addErrorException(Long idFormulaire, Exception e) {
		erreurs.add(new EnErreur(idFormulaire, ExceptionUtils.extractCallStack(e)));
	}

	@Override
	public void addAll(RappelFormulairesDemandeDegrevementICIResults source) {
		this.ignores.addAll(source.ignores);
		this.traites.addAll(source.traites);
		this.erreurs.addAll(source.erreurs);
	}

	@Override
	public void end() {
		Collections.sort(ignores);
		Collections.sort(traites);
		Collections.sort(erreurs);
		super.end();
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
