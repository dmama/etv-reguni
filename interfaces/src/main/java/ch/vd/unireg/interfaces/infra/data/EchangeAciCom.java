package ch.vd.unireg.interfaces.infra.data;

import ch.vd.registre.base.date.DateRange;

/**
 * Represente les échanges possibles pour une collectivité.
 */
public interface EchangeAciCom extends DateRange {

	TypeCommunication getTypeCommunication();

	SupportEchange getSupportEchange();
}
