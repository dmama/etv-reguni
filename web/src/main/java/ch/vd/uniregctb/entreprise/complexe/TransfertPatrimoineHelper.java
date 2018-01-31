package ch.vd.uniregctb.entreprise.complexe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Petit recueil de méthodes utilitaires autour des transferts de patrimoine entre entreprises
 */
public abstract class TransfertPatrimoineHelper {

	/**
	 * @param emettrice une entreprise vue dans son rôle d'entreprise émettrice de patrimoine
	 * @param tiersService le service d'accès aux tiers
	 * @return une {@link Map} des transferts de patrimoine passés de cette entreprise, indéxée par date du transfert, avec la liste des entreprises réceptrices
	 */
	@NotNull
	public static Map<RegDate, List<Entreprise>> getTransferts(Entreprise emettrice, TiersService tiersService) {
		final Map<RegDate, List<Entreprise>> map = new HashMap<>();
		for (RapportEntreTiers ret : emettrice.getRapportsSujet()) {
			if (!ret.isAnnule() && ret.getType() == TypeRapportEntreTiers.TRANSFERT_PATRIMOINE) {
				final Tiers receptrice = tiersService.getTiers(ret.getObjetId());
				if (receptrice == null || !(receptrice instanceof Entreprise)) {
					throw new TiersNotFoundException(ret.getObjetId());
				}

				final List<Entreprise> receptrices = map.computeIfAbsent(ret.getDateDebut(), k -> new ArrayList<>());
				receptrices.add((Entreprise) receptrice);
			}
		}
		return map;
	}
}
