package ch.vd.uniregctb.declaration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class DeclarationImpotOrdinaireDAOImpl extends BaseDAOImpl< DeclarationImpotOrdinaire, Long> implements DeclarationImpotOrdinaireDAO {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeclarationImpotOrdinaireDAOImpl.class);

	private static final String TOUS = "TOUS";

	public DeclarationImpotOrdinaireDAOImpl() {
		super(DeclarationImpotOrdinaire.class);
	}

	/**
	 * Recherche des declarations d'impot ordinaire selon des criteres
	 *
	 * @param criterion
	 * @return
	 */
	@Override
	public List<DeclarationImpotOrdinaire> find(DeclarationImpotCriteria criterion) {
		return find(criterion, false);
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List<DeclarationImpotOrdinaire> find(DeclarationImpotCriteria criterion, boolean doNotAutoFlush) {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of DeclarationImpotOrdinaireDAO : find");
		}


		final StringBuilder b = new StringBuilder("SELECT di FROM DeclarationImpotOrdinaire di WHERE 1=1");
		final Map<String, Object> params = new HashMap<>();

		final Integer annee = criterion.getAnnee();
		if (annee != null) {
			b.append(" AND di.periode.annee = :pf");
			params.put("pf", annee);
		}
		else {
			final Pair<Integer, Integer> anneeRange = criterion.getAnneeRange();
			if (anneeRange != null) {
				b.append(" AND di.periode.annee BETWEEN :pfMin AND :pfMax");
				params.put("pfMin", anneeRange.getFirst());
				params.put("pfMax", anneeRange.getSecond());
			}
		}

		final Long contribuable = criterion.getContribuable();
		if (contribuable != null) {
			b.append(" AND di.tiers.id = :tiersId");
			params.put("tiersId", contribuable);
		}

		final String query = b.toString();
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("DeclarationImpotCriteria Query: " + query);
			LOGGER.trace("DeclarationImpotCriteria Params: " + Arrays.toString(params.entrySet().toArray(new Map.Entry[params.size()])));
		}

		final FlushMode mode = (doNotAutoFlush ? FlushMode.MANUAL : null);
		final List<DeclarationImpotOrdinaire> list = find(query, params, mode);
		final List<DeclarationImpotOrdinaire> listRtr = new ArrayList<>();

		if (criterion.getEtat() == null || criterion.getEtat().equals(TOUS)) {
			for (DeclarationImpotOrdinaire di : list) {
				listRtr.add(di);
			}
		}
		else {
			final TypeEtatDeclaration etat = TypeEtatDeclaration.valueOf(criterion.getEtat());
			for (DeclarationImpotOrdinaire di : list) {
				if (di.getDernierEtat().getEtat() == etat) {
					listRtr.add(di);
				}
			}
		}
		return listRtr;
	}

	/**
	 * Recherche toutes les DI en fonction du numero de contribuable
	 *
	 * @param numero
	 * @return
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<DeclarationImpotOrdinaire> findByNumero(Long numero) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of DeclarationImpotOrdinaireDAO : findByNumero");
		}

		final Map<String, Long> params = new HashMap<>(1);
		params.put("tiersId", numero);
		final String query = "select di from DeclarationImpotOrdinaire di where di.tiers.numero = :tiersId";
		return find(query, params, null);
	}

	/**
	 * Retourne le dernier EtatPeriodeDeclaration retournee
	 *
	 * @param numeroCtb
	 * @return
	 */
	@Override
	@SuppressWarnings("unchecked")
	public EtatDeclaration findDerniereDiEnvoyee(Long numeroCtb) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of DeclarationImpotOrdinaireDAO : findDerniereDiEnvoyee");
		}

		final Map<String, Long> params = new HashMap<>(1);
		params.put("tiersId", numeroCtb);
		final String query = " select etatDeclarationEmise from EtatDeclarationEmise etatDeclarationEmise where etatDeclarationEmise.declaration.annulationDate is null and etatDeclarationEmise.declaration.tiers.numero = :tiersId order by etatDeclarationEmise.dateObtention desc";
		final List<EtatDeclaration> list = find(query, params, null);
		if (list.isEmpty()) {
			return null;
		}

		// détermine la déclaration la plus récente
		EtatDeclaration etat = list.get(0);
		for (EtatDeclaration e : list) {
			final RegDate date = etat.getDeclaration().getDateDebut();
			final RegDate autre = e.getDeclaration().getDateDebut();
			if (date == null || (autre != null && autre.isAfter(date))) {
				etat = e;
			}
		}

		return etat;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<DeclarationImpotOrdinairePP> getDeclarationsImpotPPForSommation(final Collection<Long> idsDI) {

		final Session session = getCurrentSession();
		final Criteria crit = session.createCriteria(DeclarationImpotOrdinairePP.class);
		crit.add(Restrictions.in("id", idsDI));
		crit.setFetchMode("etats", FetchMode.JOIN);
		crit.setFetchMode("delais", FetchMode.JOIN);
		crit.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

		final FlushMode mode = session.getFlushMode();
		try {
			session.setFlushMode(FlushMode.MANUAL);
			return new HashSet<>(crit.list());
		}
		finally {
			session.setFlushMode(mode);
		}
	}
}
