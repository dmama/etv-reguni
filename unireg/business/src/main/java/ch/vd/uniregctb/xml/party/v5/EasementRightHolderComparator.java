package ch.vd.uniregctb.xml.party.v5;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.xml.party.landregistry.v1.RightHolder;
import ch.vd.uniregctb.registrefoncier.CommunauteRFMembreComparator;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * [SIFISC-23957] Comparateur qui permet d'ordonner les membres d'une servitude RF selon les mêmes règles métier qui détermine le leader d'une communauté de propriétaires.
 *
 * @see ch.vd.uniregctb.registrefoncier.CommunauteRFMembreComparator
 */
public class EasementRightHolderComparator implements Comparator<RightHolder> {

	private final Comparator<RightHolder> typeComparator;
	private final CommunauteRFMembreComparator ctbComparator;
	private final Comparator<RightHolder> immeubleIdComparator;
	private final Comparator<RightHolder> identityComparator;

	public EasementRightHolderComparator(@NotNull Function<Long, Tiers> tiersGetter,
	                                     @NotNull Function<Tiers, List<ForFiscalPrincipal>> forsVirtuelsGetter,
	                                     @NotNull Function<PersonnePhysique, NomPrenom> nomPrenomGetter,
	                                     @NotNull Function<Tiers, String> raisonSocialeGetter) {
		this.typeComparator = Comparator.comparing(this::getHolderType);
		this.ctbComparator = new CommunauteRFMembreComparator(tiersGetter, forsVirtuelsGetter, nomPrenomGetter, raisonSocialeGetter, null);
		this.immeubleIdComparator = Comparator.comparing(RightHolder::getImmovablePropertyId);
		this.identityComparator = Comparator.comparing((RightHolder r) -> r.getIdentity().getId());
	}

	public EasementRightHolderComparator(@NotNull TiersService tiersService) {
		this(tiersService::getTiers,
		     tiersService::getForsFiscauxVirtuels,
		     pp -> tiersService.getDecompositionNomPrenom(pp, false),
		     tiersService::getNomRaisonSociale);
	}

	@Override
	public int compare(RightHolder o1, RightHolder o2) {

		// on trie en premier par type de bénéficiaire
		int c = typeComparator.compare(o1, o2);
		if (c != 0) {
			return c;
		}

		// on trie ensuite selon les règles propres à chaque type de bénéficiaire
		final HolderType type = getHolderType(o1);
		switch (type) {
		case CTB:
			c = ctbComparator.compare(o1.getTaxPayerNumber().longValue(), o2.getTaxPayerNumber().longValue());
			break;
		case IMMEUBLE:
			c = immeubleIdComparator.compare(o1, o2);
			break;
		case NON_RAPPROCHE:
			c = identityComparator.compare(o1, o2);
			break;
		default:
			throw new IllegalArgumentException("Type de bénéficiaire inconnu  = [" + type + "]");
		}
		if (c != 0) {
			return c;
		}

		return 0;
	}

	/**
	 * L'ordre métier de tri des types de bénéficiaire.
	 */
	private enum HolderType {
		CTB,
		IMMEUBLE,
		NON_RAPPROCHE
	}

	@NotNull
	private HolderType getHolderType(@NotNull RightHolder holder) {
		if (holder.getTaxPayerNumber() != null) {
			return HolderType.CTB;
		}
		else if (holder.getImmovablePropertyId() != null) {
			return HolderType.IMMEUBLE;
		}
		else {
			return HolderType.NON_RAPPROCHE;
		}
	}
}
