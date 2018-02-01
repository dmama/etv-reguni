package ch.vd.unireg.tache;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.metier.assujettissement.AssujettissementException;
import ch.vd.unireg.tache.sync.SynchronizeAction;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.TypeTache;

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
	public void genereTacheControleDossier(Contribuable contribuable, String commentaire) {
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

	@NotNull
	@Override
	public List<SynchronizeAction> determineSynchronizeActionsForDIs(Contribuable contribuable) throws AssujettissementException {
		return Collections.emptyList();
	}

	@Override
	public TacheSyncResults synchronizeTachesDeclarations(Collection<Long> ctbIds) {
		return new TacheSyncResults(false);
	}

    @Override
    public void annuleTachesObsoletes(Collection<Long> set) {
    }

	@Override
	public void updateStats() {
	}

	@NotNull
	@Override
	public List<String> getCommentairesDistincts(TypeTache typeTache) {
		return Collections.emptyList();
	}

	@NotNull
	@Override
	public Set<Integer> getCollectivitesAdministrativesAvecTaches() {
		return Collections.emptySet();
	}
}
