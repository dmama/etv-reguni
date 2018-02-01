package ch.vd.unireg.tache.sync;

import ch.vd.unireg.metier.assujettissement.PeriodeImposition;
import ch.vd.unireg.tiers.TacheEnvoiDeclarationImpot;

/**
 * Action permettant de mettre à jour une tâche d'envoi de déclaration d'impôt existante à partir des données nouvellement
 * calculées pour cette tâche (pour les champs qui servent plus à du requêtage qu'à autre chose...)
 * @param <T> classe de la tâche d'envoi de déclaration d'impôt
 * @param <P> classe de la période d'imposition calculée
 */
public abstract class UpdateTacheEnvoiDI<T extends TacheEnvoiDeclarationImpot, P extends PeriodeImposition> implements TacheSynchronizeAction {

	public final T tacheEnvoi;
	public final AddDI<P> addAction;

	public UpdateTacheEnvoiDI(T tacheEnvoi, AddDI<P> addAction) {
		this.tacheEnvoi = tacheEnvoi;
		this.addAction = addAction;
	}

	@Override
	public boolean willChangeEntity() {
		return false;
	}

	@Override
	public int getPeriodeFiscale() {
		return tacheEnvoi.getDateFin().year();
	}
}
