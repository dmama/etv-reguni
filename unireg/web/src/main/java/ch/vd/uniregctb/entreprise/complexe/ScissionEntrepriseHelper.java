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
 * Petit recueil de méthodes utilitaires autour des scissions d'entreprise
 */
public abstract class ScissionEntrepriseHelper {

	/**
	 * @param scindee une entreprise vue dans son rôle d'entreprise scindée
	 * @param tiersService le service d'accès aux tiers
	 * @return une {@link Map} des scissions de cette entreprise, indéxée par date du contrat de scission, avec la liste des entreprises résultantes
	 */
	@NotNull
	public static Map<RegDate, List<Entreprise>> getScissions(Entreprise scindee, TiersService tiersService) {
		final Map<RegDate, List<Entreprise>> map = new HashMap<>();
		for (RapportEntreTiers ret : scindee.getRapportsSujet()) {
			if (!ret.isAnnule() && ret.getType() == TypeRapportEntreTiers.SCISSION_ENTREPRISE) {
				final Tiers resultante = tiersService.getTiers(ret.getObjetId());
				if (resultante == null || !(resultante instanceof Entreprise)) {
					throw new TiersNotFoundException(ret.getObjetId());
				}

				final List<Entreprise> resultantes = map.computeIfAbsent(ret.getDateDebut(), k -> new ArrayList<>());
				resultantes.add((Entreprise) resultante);
			}
		}
		return map;
	}
}
