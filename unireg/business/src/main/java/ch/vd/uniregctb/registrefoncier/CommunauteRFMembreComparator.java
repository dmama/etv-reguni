package ch.vd.uniregctb.registrefoncier;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * [SIFISC-23747] Comparateur qui permet d'ordonner les membres d'une communauté RF selon les règles métier qui détermine le leader de la communauté. Le leader de la communauté est le premier membre de la liste triée.
 */
public class CommunauteRFMembreComparator implements Comparator<Long> {

	private final Function<Long, Tiers> tiersGetter;
	private final Function<Tiers, List<ForFiscalPrincipal>> forsVirtuelsGetter;
	private final Function<PersonnePhysique, NomPrenom> nomPrenomGetter;
	private final Function<Tiers, String> raisonSocialeGetter;
	private final Comparator<Tiers> tiersComparator;

	public CommunauteRFMembreComparator(@NotNull Function<Long, Tiers> tiersGetter,
	                                    @NotNull Function<Tiers, List<ForFiscalPrincipal>> forsVirtuelsGetter,
	                                    @NotNull Function<PersonnePhysique, NomPrenom> nomPrenomGetter,
	                                    @NotNull Function<Tiers, String> raisonSocialeGetter) {
		this.tiersGetter = tiersGetter;
		this.forsVirtuelsGetter = forsVirtuelsGetter;
		this.nomPrenomGetter = nomPrenomGetter;
		this.raisonSocialeGetter = raisonSocialeGetter;
		this.tiersComparator = Comparator
				.comparing(this::getTypeFor)
				.thenComparing(this::getTypeMembre)
				.thenComparing(this::getNom)
				.thenComparing(this::getPrenom);
	}

	public CommunauteRFMembreComparator(@NotNull TiersService tiersService) {
		this(tiersService::getTiers,
		     tiersService::getForsFiscauxVirtuels,
		     pp -> tiersService.getDecompositionNomPrenom(pp, false),
		     tiersService::getNomRaisonSociale);
	}

	@Override
	public int compare(Long o1, Long o2) {
		final Tiers t1 = tiersGetter.apply(o1);
		if (t1 == null) {
			throw new TiersNotFoundException(o1);
		}
		final Tiers t2 = tiersGetter.apply(o2);
		if (t2 == null) {
			throw new TiersNotFoundException(o2);
		}

		return tiersComparator.compare(t1, t2);
	}


	/**
	 * L'ordre métier de tri des types de fors fiscaux.
	 */
	private enum TypeFor {
		VD,
		HC,
		HS,
		UNKNOWN
	}

	/**
	 * @param tiers un tiers
	 * @return le type de for fiscal du tiers spécifié.
	 */
	@NotNull
	private TypeFor getTypeFor(@NotNull Tiers tiers) {

		final List<? extends ForFiscalPrincipal> fors = tiers.getForsFiscauxPrincipauxActifsSorted();

		// on s'intéresse à la situation courante
		ForFiscalPrincipal actif = DateRangeHelper.rangeAt(fors, RegDate.get());
		if (actif == null) {
			// [SIFISC-24521] on essaie avec les fors fiscaux virtuels (il s'agit peut-être d'une personne physique en ménage)
			actif = DateRangeHelper.rangeAt(forsVirtuelsGetter.apply(tiers), RegDate.get());
		}

		if (actif == null) {
			// le contribuable n'a pas de for fiscal actif
			return TypeFor.UNKNOWN;
		}

		switch (actif.getTypeAutoriteFiscale()) {
		case COMMUNE_OU_FRACTION_VD:
			return TypeFor.VD;
		case COMMUNE_HC:
			return TypeFor.HC;
		case PAYS_HS:
			return TypeFor.HS;
		default:
			throw new IllegalArgumentException("Type d'autorité fiscale inconnue = [" + actif.getTypeAutoriteFiscale() + "]");
		}
	}

	/**
	 * L'ordre métier de tri des types de tiers.
	 */
	private enum TypeMembre {
		PM,
		PP,
		OTHER
	}

	/**
	 * @param tiers un tiers
	 * @return le type de membre du tiers spécifié.
	 */
	@NotNull
	private TypeMembre getTypeMembre(@NotNull Tiers tiers) {
		if (tiers instanceof PersonnePhysique) {
			return TypeMembre.PP;
		}
		else if (tiers instanceof Entreprise) {
			return TypeMembre.PM;
		}
		else {
			return TypeMembre.OTHER;
		}
	}

	/**
	 * @param tiers un tiers
	 * @return le nom de la personne physique spécifiée ou la raison sociale ou équivalent si le tiers est une entreprise.
	 */
	@NotNull
	private String getNom(@NotNull Tiers tiers) {
		if (tiers instanceof PersonnePhysique) {
			final NomPrenom nomPrenom = nomPrenomGetter.apply((PersonnePhysique) tiers);
			return StringUtils.trimToEmpty(nomPrenom.getNom());
		}
		else {
			return StringUtils.trimToEmpty(raisonSocialeGetter.apply(tiers));
		}
	}

	/**
	 * @param tiers un tiers
	 * @return le prénom de la personne physique spécifiée ou une chaîne vide ("") si le tiers n'est pas une personne physique.
	 */
	@NotNull
	private String getPrenom(@NotNull Tiers tiers) {
		if (tiers instanceof PersonnePhysique) {
			final NomPrenom nomPrenom = nomPrenomGetter.apply((PersonnePhysique) tiers);
			return StringUtils.trimToEmpty(nomPrenom.getPrenom());
		}
		else {
			return StringUtils.EMPTY;
		}
	}
}
