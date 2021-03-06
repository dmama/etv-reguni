package ch.vd.unireg.declaration;

import javax.persistence.FlushModeType;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.common.BaseDAOImpl;

public class PeriodeFiscaleDAOImpl extends BaseDAOImpl< PeriodeFiscale, Long> implements  PeriodeFiscaleDAO {


	private static final Logger LOGGER = LoggerFactory.getLogger(PeriodeFiscaleDAOImpl.class);


	public PeriodeFiscaleDAOImpl() {
		super(PeriodeFiscale.class);
	}

	/**
	 * Renvoie toutes les periodes fiscales triée par orde decroissant
	 *
	 * @return une liste de PeriodeFiscale
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<PeriodeFiscale> getAllDesc() {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of PeriodeFiscaleDAO : getAllDesc");
		}

		final String query = " select periode from PeriodeFiscale periode order by periode.annee desc";
		return find(query, null);
	}

	@Override
	public PeriodeFiscale getPeriodeFiscaleByYear(final int year) {

		/**
		 * FlushModeType.COMMIT => Ici, on applique un petit hack pour éviter un side-effect indésirable de l'intercepteur de validation des
		 * tiers.
		 *
		 * <pre>
		 * Situation:
		 *  - dans une session hibernate, on a créé un contribuable qui ne valide pas (il manque un for principal par exemple)
		 *  - on appel la méthode 'getPeriodeFiscaleByYear'
		 *     - si le flush mode est en AUTO, la session va être flushée pour s'assurer que la query retourne des résultats
		 *       cohérents
		 *       - l'intercepteur intercepte le flush, se rend compte que le contribuable n'est pas valide et pète une
		 *         exception, alors même que la requête exécutée n'a rien à voir avec les tiers !
		 * Hack:
		 *  - en passant temporairement le flush mode à 'MANUAL', on évite de flusher la session au moment de l'exécution de
		 *    la query.
		 * Risque:
		 *  - en faisant comme ça, le seul risque est de ne pas voir une période créée ou modifiée dans la session courante.
		 * Au final, le risque est acceptable puisque il s'agit de données qui seront créées/modifiées au plus 2 ou 3 fois
		 * par année..
		 * </pre>
		 */
		final Map<String, Integer> params = new HashMap<>(1);
		params.put("year", year);
		final Iterator<?> i = iterate("from PeriodeFiscale periode where periode.annee = :year", params, FlushModeType.COMMIT);

		PeriodeFiscale periode = null;

		while (i.hasNext()) {
			if (periode != null) {
				throw new IllegalArgumentException("Plusieurs périodes trouvée pour la même année fiscale !");
			}
			periode = (PeriodeFiscale) i.next();
		}

		return periode;
	}

}
