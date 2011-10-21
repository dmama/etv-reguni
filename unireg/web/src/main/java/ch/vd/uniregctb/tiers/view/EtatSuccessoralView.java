package ch.vd.uniregctb.tiers.view;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * La vue de l'état successoral d'un ménage-commun. C'est-à-dire les numéros de contribuables des membres décédés dans le cas d'un ménage-commun (SIFISC-156).
 */
@SuppressWarnings({"UnusedDeclaration"})
public class EtatSuccessoralView {

	/**
	 * Numéro de contribuable du principal d'un couple si celui-ci est décédé. Renseigné que si le contribuable édité est un couple et que la personne physique principale est décédée.
	 */
	private Long numeroPrincipalDecede;

	/**
	 * Numéro de contribuable du conjoint d'un couple si celui-ci est décédé. Renseigné que si le contribuable édité est un couple et que la personne physique conjoint est décédée.
	 */
	private Long numeroConjointDecede;

	public EtatSuccessoralView() {
	}

	public EtatSuccessoralView(Long numeroPrincipalDecede, Long numeroConjointDecede) {
		this.numeroPrincipalDecede = numeroPrincipalDecede;
		this.numeroConjointDecede = numeroConjointDecede;
	}

	/**
	 * Détermine si le tiers est un ménage-commun et si au moins un de ces composants est décédé, et si c'est le cas retourne la vue de l'état successoral. Dans tous les autres cas, retourne null.
	 *
	 * @param tiers        un tiers
	 * @param tiersService le tiers service
	 * @return la vue de l'état successoral selon les conditions énumérées ci-dessous; ou <b>null</b> si ces conditions ne sont pas remplies.
	 */
	@Nullable
	public static EtatSuccessoralView determine(Tiers tiers, TiersService tiersService) {
		if (tiers instanceof MenageCommun) {
			final MenageCommun menage = (MenageCommun) tiers;

			final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(menage, null);
			final PersonnePhysique principal = ensemble.getPrincipal();
			final PersonnePhysique conjoint = ensemble.getConjoint();

			Long numeroPrincipalDecede = null;
			if (principal != null && tiersService.isDecede(principal)) {
				numeroPrincipalDecede = principal.getNumero();
			}

			Long numeroConjointDecede = null;
			if (conjoint != null && tiersService.isDecede(conjoint)) {
				numeroConjointDecede = conjoint.getNumero();
			}

			if (numeroPrincipalDecede != null || numeroConjointDecede != null) {
				return new EtatSuccessoralView(numeroPrincipalDecede, numeroConjointDecede);
			}
		}
		return null;
	}

	public Long getNumeroPrincipalDecede() {
		return numeroPrincipalDecede;
	}

	public void setNumeroPrincipalDecede(Long numeroPrincipalDecede) {
		this.numeroPrincipalDecede = numeroPrincipalDecede;
	}

	public Long getNumeroConjointDecede() {
		return numeroConjointDecede;
	}

	public void setNumeroConjointDecede(Long numeroConjointDecede) {
		this.numeroConjointDecede = numeroConjointDecede;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final EtatSuccessoralView that = (EtatSuccessoralView) o;

		if (numeroConjointDecede != null ? !numeroConjointDecede.equals(that.numeroConjointDecede) : that.numeroConjointDecede != null) return false;
		//noinspection RedundantIfStatement
		if (numeroPrincipalDecede != null ? !numeroPrincipalDecede.equals(that.numeroPrincipalDecede) : that.numeroPrincipalDecede != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = numeroPrincipalDecede != null ? numeroPrincipalDecede.hashCode() : 0;
		result = 31 * result + (numeroConjointDecede != null ? numeroConjointDecede.hashCode() : 0);
		return result;
	}
}
