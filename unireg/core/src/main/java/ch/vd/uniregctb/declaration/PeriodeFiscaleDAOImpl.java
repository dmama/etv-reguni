package ch.vd.uniregctb.declaration;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.FlushMode;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.registre.base.utils.Assert;

public class PeriodeFiscaleDAOImpl extends GenericDAOImpl< PeriodeFiscale, Long> implements  PeriodeFiscaleDAO {


	private static final Logger LOGGER = Logger.getLogger(PeriodeFiscaleDAOImpl.class);


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

		String query = " select periode from PeriodeFiscale periode order by periode.annee desc";

		List<PeriodeFiscale> list = getHibernateTemplate().find(query);

		return list;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public PeriodeFiscale getPeriodeFiscaleByYear(final int year) {

		final Object[] params = new Object[] {
				year
		};

		/**
		 * FlushMode.MANUAL => Ici, on applique un petit hack pour éviter un side-effect indésirable de l'intercepteur de validation des
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
		final Iterator<?> i = iterate("from PeriodeFiscale periode where periode.annee = ?", params, FlushMode.MANUAL);

		PeriodeFiscale periode = null;

		while (i.hasNext()) {
			Assert.isNull(periode, "Plusieurs périodes trouvée pour la même année fiscale !");
			periode = (PeriodeFiscale) i.next();
		}

		return periode;
	}

}
