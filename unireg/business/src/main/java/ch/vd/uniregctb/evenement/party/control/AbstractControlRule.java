package ch.vd.uniregctb.evenement.party.control;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Régle A1.1: Utilisation de l'algorithme Unireg de détermination des assujettissements d'un numéro de tiers sur la PF
 * @param <T> {@link ch.vd.uniregctb.type.ModeImposition} ou {@link ch.vd.uniregctb.metier.assujettissement.TypeAssujettissement}
 */
public abstract class AbstractControlRule<T extends Enum<T>> implements TaxLiabilityControlRule<T> {

	protected final TiersService tiersService;

	protected AbstractControlRule(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	protected static TaxLiabilityControlEchec createEchec(TaxLiabilityControlEchec.EchecType type, @Nullable List<Long> menageCommunsIds, @Nullable List<Long> mcParentsIds, @Nullable List<Long> parentsIds) {
		final TaxLiabilityControlEchec echec = new TaxLiabilityControlEchec(type);
		echec.setMenageCommunIds(menageCommunsIds);
		echec.setMenageCommunParentsIds(mcParentsIds);
		echec.setParentsIds(parentsIds);
		echec.setAssujetissementNonConforme(false);
		return echec;
	}

	public abstract AssujettissementStatut checkAssujettissement(@NotNull Contribuable tiers, Set<T> aRejeter) throws ControlRuleException;

	/**
	 * Les types d'assujettissement portés par le tiers
	 * @param ctb tiers à considérer
	 * @return ensemble (potentiellement vide) des assujettissements portés par le tiers sur la période ou à la date considérée
	 * @throws ControlRuleException en cas de souci
	 */
	public abstract Set<T> getSourceAssujettissement(@NotNull Contribuable ctb) throws ControlRuleException;

	public class AssujettissementStatut {
		final boolean isAssujetti;
		final boolean assujettissementNonConforme;

		public AssujettissementStatut(boolean isAssujetti, boolean assujettissementNonConforme) {
			this.isAssujetti = isAssujetti;
			this.assujettissementNonConforme = assujettissementNonConforme;
		}
	}
}
