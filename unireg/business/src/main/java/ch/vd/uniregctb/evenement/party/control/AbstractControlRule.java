package ch.vd.uniregctb.evenement.party.control;

import java.util.List;

import com.sun.istack.Nullable;

import ch.vd.uniregctb.evenement.party.TaxliabilityControlEchec;
import ch.vd.uniregctb.evenement.party.TaxliabilityControlEchecType;
import ch.vd.uniregctb.evenement.party.TaxliabilityControlResult;
import ch.vd.uniregctb.xml.Context;

/**
 * Régle A1.1: Utilisation de l'algorithme Unireg de détermination des assujettissements d'un numéro de tiers sur la PF
 */
public abstract class AbstractControlRule implements TaxliabilityControlRule {


	protected Context context;
	protected Long tiersId;


	public AbstractControlRule(Context nouveauContext, Long tiersId) {
		this.context = nouveauContext;
		this.tiersId = tiersId;

	}

	protected void setErreur(TaxliabilityControlResult result, TaxliabilityControlEchecType type,
	                         @Nullable List<Long> menageCommunsIds, @Nullable List<Long> mcParentsIds, @Nullable List<Long> parentsIds) {
		TaxliabilityControlEchec echec = new TaxliabilityControlEchec(type);
		echec.setMenageCommunIds(menageCommunsIds);
		echec.setMenageCommunParentsIds(mcParentsIds);
		echec.setParentsIds(parentsIds);
		result.setEchec(echec);
	}

	public abstract boolean isAssujetti(Long tiersId) throws ControlRuleException;

}
