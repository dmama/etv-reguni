package ch.vd.unireg.evenement.reqdes.engine;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.common.StatusManager;

public interface EvenementReqDesRetryProcessor {

	/**
	 * Lance le retraitement (appel bloquant) des unités de traitement ReqDes non encore traitées
	 * @param statusManager le status manager
	 */
	void relancerEvenementsReqDesNonTraites(@Nullable StatusManager statusManager);
}
