package ch.vd.uniregctb.evenement.party.control;

import java.util.List;

import com.sun.istack.Nullable;
import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Régle A1.1: Utilisation de l'algorithme Unireg de détermination des assujettissements d'un numéro de tiers sur la PF
 */
public abstract class AbstractControlRule implements TaxLiabilityControlRule {

	protected final TiersService tiersService;

	protected AbstractControlRule(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	protected static void setErreur(TaxLiabilityControlResult result, TaxLiabilityControlEchec.EchecType type,
	                                @Nullable List<Long> menageCommunsIds, @Nullable List<Long> mcParentsIds, @Nullable List<Long> parentsIds, boolean assuNonConforme) {
		final TaxLiabilityControlEchec echec = new TaxLiabilityControlEchec(type);
		echec.setMenageCommunIds(menageCommunsIds);
		echec.setMenageCommunParentsIds(mcParentsIds);
		echec.setParentsIds(parentsIds);
		echec.setAssujetissementNonConforme(assuNonConforme);
		result.setEchec(echec);
	}


	public abstract boolean isAssujetti(@NotNull Tiers tiers) throws ControlRuleException;

	public abstract  boolean isAssujettissementNonConforme(@NotNull Tiers tiers) throws ControlRuleException;

}
