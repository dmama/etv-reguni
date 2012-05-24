package ch.vd.uniregctb.tache;

import java.util.Collection;
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

	@Override
	public void genereTacheDepuisOuvertureForPrincipal(Contribuable contribuable, ForFiscalPrincipal forFiscal, ModeImposition ancienModeImposition) {
	}

	@Override
	public void genereTacheDepuisOuvertureForSecondaire(Contribuable contribuable, ForFiscalSecondaire forFiscal) {
	}

	@Override
	public void genereTacheDepuisFermetureForPrincipal(Contribuable contribuable, ForFiscalPrincipal forFiscal) {
	}

	@Override
	public void genereTacheDepuisFermetureForSecondaire(Contribuable contribuable, ForFiscalSecondaire forFiscal) {
	}

	@Override
	public void genereTachesDepuisAnnulationDeFor(Contribuable contribuable) {
	}

	@Override
	public int getTachesEnInstanceCount(Integer oid) {
		return 0;
	}

	@Override
	public int getDossiersEnInstanceCount(Integer oid) {
		return 0;
	}

	@Override
	public void onAnnulationContribuable(Contribuable contribuable) {
	}

	@Override
	public ListeTachesEnInstanceParOID produireListeTachesEnInstanceParOID(RegDate dateTraitement, StatusManager status) throws Exception {
		return null;
	}

	@Override
	public List<SynchronizeAction> determineSynchronizeActionsForDIs(Contribuable contribuable) {
		return null;
	}

	@Override
	public void synchronizeTachesDIs(Collection<Long> ctbIds) {
	}

    @Override
    public void annuleTachesObsoletes(Collection<Long> set) {
    }

    @Override
	public void updateStats() {
	}
}
