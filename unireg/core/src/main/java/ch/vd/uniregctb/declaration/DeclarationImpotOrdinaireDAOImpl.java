package ch.vd.uniregctb.declaration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class DeclarationImpotOrdinaireDAOImpl extends GenericDAOImpl< DeclarationImpotOrdinaire, Long> implements DeclarationImpotOrdinaireDAO {

	private static final Logger LOGGER = Logger.getLogger(DeclarationImpotOrdinaireDAOImpl.class);

	private final String TOUS = "TOUS";

	public DeclarationImpotOrdinaireDAOImpl() {
		super(DeclarationImpotOrdinaire.class);
	}


	/**
	 * Recherche des declarations d'impot ordinaire selon des criteres
	 *
	 * @param criterion
	 * @return
	 */
	public List<DeclarationImpotOrdinaire> find(DeclarationImpotCriteria criterion) {
		return find(criterion, false);
	}

	@SuppressWarnings({"unchecked"})
	public List<DeclarationImpotOrdinaire> find(DeclarationImpotCriteria criterion, boolean doNotAutoFlush) {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of DeclarationImpotOrdinaireDAO : find");
		}

		final StringBuilder b = new StringBuilder("SELECT di FROM DeclarationImpotOrdinaire di WHERE 1=1");
		final List<Object> criteria = new ArrayList<Object>();

		final Integer annee = criterion.getAnnee();
		if (annee != null) {
			b.append(" AND di.periode.annee = ?");
			criteria.add(annee);
		}
		else {
			final Pair<Integer, Integer> anneeRange = criterion.getAnneeRange();
			if (anneeRange != null) {
				b.append(" AND di.periode.annee BETWEEN ? AND ?");
				criteria.add(anneeRange.getFirst());
				criteria.add(anneeRange.getSecond());
			}
		}

		final Long contribuable = criterion.getContribuable();
		if (contribuable != null) {
			b.append(" AND di.tiers.id = ?");
			criteria.add(contribuable);
		}

		final String query = b.toString();
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("DeclarationImpotCriteria Query: " + query);
			LOGGER.trace("DeclarationImpotCriteria Params: " + Arrays.toString(criteria.toArray()));
		}

		final FlushMode mode = (doNotAutoFlush ? FlushMode.MANUAL : null);
		final List<DeclarationImpotOrdinaire> list = (List<DeclarationImpotOrdinaire>) find(query, criteria.toArray(), mode);
		final List<DeclarationImpotOrdinaire> listRtr = new ArrayList<DeclarationImpotOrdinaire>();

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
	@SuppressWarnings("unchecked")
	public List<DeclarationImpotOrdinaire> findByNumero(Long numero) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of DeclarationImpotOrdinaireDAO : findByNumero");
		}

		String query = " select di from DeclarationImpotOrdinaire di where di.tiers.numero = ? ";
		List<Object> criteria = new ArrayList<Object>();
		criteria.add(numero);
		List<DeclarationImpotOrdinaire> list = getHibernateTemplate().find(query, criteria.toArray());
		return list;
	}

	/**
	 * Retourne le dernier EtatPeriodeDeclaration retournee
	 *
	 * @param numeroCtb
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public EtatDeclaration findDerniereDiEnvoyee(Long numeroCtb) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of DeclarationImpotOrdinaireDAO : findDerniereDiEnvoyee");
		}
		EtatDeclaration etat = null;

		String query = " select etatDeclarationEmise from EtatDeclarationEmise etatDeclarationEmise where etatDeclarationEmise.declaration.annulationDate is null and etatDeclarationEmise.declaration.tiers.numero = ? order by etatDeclarationEmise.dateObtention desc";
		List<Object> criteria = new ArrayList<Object>();
		criteria.add(numeroCtb);
		List<EtatDeclaration> list = getHibernateTemplate().find(query, criteria.toArray());
		if (list.size() == 0) {
			return null;
		}

		// détermine la déclaration la plus récente
		etat = list.get(0);
		for (EtatDeclaration e : list) {
			final RegDate date = etat.getDeclaration().getDateDebut();
			final RegDate autre = e.getDeclaration().getDateDebut();
			if (date == null || (autre != null && autre.isAfter(date))) {
				etat = e;
			}
		}

		return etat;
	}

	@SuppressWarnings("unchecked")
	public Set<DeclarationImpotOrdinaire> getDIsForSommation(final Collection<Long> idsDI) {

		final List<DeclarationImpotOrdinaire> list = (List<DeclarationImpotOrdinaire>) getHibernateTemplate().executeWithNativeSession(
				new HibernateCallback() {
					public Object doInHibernate(Session session) throws HibernateException {
						Criteria crit = session.createCriteria(DeclarationImpotOrdinaire.class);
						crit.add(Restrictions.in("id", idsDI));
						crit.setFetchMode("etats", FetchMode.JOIN);
						crit.setFetchMode("delais", FetchMode.JOIN);
						crit.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

						final FlushMode mode = session.getFlushMode();
						try {
							session.setFlushMode(FlushMode.MANUAL);
							return crit.list();
						}
						finally {
							session.setFlushMode(mode);
						}
					}
				});

		return new HashSet<DeclarationImpotOrdinaire>(list);
	}
}
