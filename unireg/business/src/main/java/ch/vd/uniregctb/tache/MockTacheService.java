package ch.vd.uniregctb.tache;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.tache.sync.SynchronizeAction;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.type.ModeImposition;

/**
 * Implémentation du tâche service qui ne fait absolument rien.
 */
public class MockTacheService implements TacheService {

	public void genereTacheDepuisOuvertureForPrincipal(Contribuable contribuable, ForFiscalPrincipal forFiscal, ModeImposition ancienModeImposition) {
	}

	public void genereTacheDepuisOuvertureForSecondaire(Contribuable contribuable, ForFiscalSecondaire forFiscal) {
	}

	public void genereTacheDepuisFermetureForPrincipal(Contribuable contribuable, ForFiscalPrincipal forFiscal) {
	}

	public void genereTacheDepuisFermetureForSecondaire(Contribuable contribuable, ForFiscalSecondaire forFiscal) {
	}

	public void genereTachesDepuisAnnulationDeFor(Contribuable contribuable) {
	}

	public int getTachesEnInstanceCount(Integer oid) {
		return 0;
	}

	public int getDossiersEnInstanceCount(Integer oid) {
		return 0;
	}

	public void onAnnulationContribuable(Contribuable contribuable) {
	}

	public ListeTachesEnIsntanceParOID produireListeTachesEnIstanceParOID(RegDate dateTraitement, StatusManager status) throws Exception {
		return null;
	}

	public List<SynchronizeAction> determineSynchronizeActionsForDIs(Contribuable contribuable) {
		return null;
	}

	public void synchronizeTachesDIs(Contribuable contribuable) {
	}
}
