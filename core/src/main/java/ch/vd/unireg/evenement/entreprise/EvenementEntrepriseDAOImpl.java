package ch.vd.unireg.evenement.entreprise;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.support.DataAccessUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BaseDAOImpl;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.dbutils.QueryFragment;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.TypeEvenementEntreprise;

public class EvenementEntrepriseDAOImpl extends BaseDAOImpl<EvenementEntreprise, Long> implements EvenementEntrepriseDAO {

	private static final Set<EtatEvenementEntreprise> ETATS_NON_TRAITES;
	private static final Set<EtatEvenementEntreprise> ETATS_TRAITES;

	static {
		final Set<EtatEvenementEntreprise> etatsNonTraites = EnumSet.noneOf(EtatEvenementEntreprise.class);
		final Set<EtatEvenementEntreprise> etatsTraitesSucces = EnumSet.noneOf(EtatEvenementEntreprise.class);
		for (EtatEvenementEntreprise etat : EtatEvenementEntreprise.values()) {
			if (etat.isTraite()) {
				if (etat != EtatEvenementEntreprise.FORCE) {
					etatsTraitesSucces.add(etat);
				}
			} else {
				etatsNonTraites.add(etat);
			}
		}
		ETATS_NON_TRAITES = Collections.unmodifiableSet(etatsNonTraites);
		ETATS_TRAITES = Collections.unmodifiableSet(etatsTraitesSucces);
	}

	public EvenementEntrepriseDAOImpl() {
		super(EvenementEntreprise.class);
	}

	@Override
	public List<EvenementEntreprise> getEvenementsNonTraites(long noEntrepriseCivile) {
		return getEvenementsNonTraites(Collections.singletonList(noEntrepriseCivile), true);

	}

	@Override
	public List<EvenementEntreprise> getEvenements(long noEntrepriseCivile) {
		return getEvenementsNonTraites(Collections.singletonList(noEntrepriseCivile), false);
	}

	private List<EvenementEntreprise> getEvenementsNonTraites(Collection<Long> nosEntreprisesCiviles, boolean nonTraitesSeulement) {

		final CriteriaBuilder builder = getCurrentSession().getCriteriaBuilder();
		final CriteriaQuery<EvenementEntreprise> query = builder.createQuery(EvenementEntreprise.class);

		final Root<EvenementEntreprise> root = query.from(EvenementEntreprise.class);
		query.distinct(true);

		final List<Predicate> predicates = new ArrayList<>(3);
		predicates.add(root.get("annulationDate").isNull());
		predicates.add(root.get("noEntrepriseCivile").in(nosEntreprisesCiviles));
		if (nonTraitesSeulement) {
			predicates.add(root.get("etat").in(ETATS_NON_TRAITES));
		}
		query.where(predicates.toArray(new Predicate[0]));

		query.orderBy(builder.asc(root.get("dateEvenement")));

		return getCurrentSession().createQuery(query).list();
	}

	@Override
	public List<EvenementEntreprise> getEvenementsARelancer() {
		final Query query = getCurrentSession().createQuery("from EvenementEntreprise where annulationDate is null and etat = :etat");
		query.setParameter("etat", EtatEvenementEntreprise.A_TRAITER);
		//noinspection unchecked
		return query.list();
	}

	@Override
	@NotNull
	public List<EvenementEntreprise> getEvenementsApresDateNonAnnules(Long noEntrepriseCivile, RegDate date) {
		final EvenementEntrepriseCriteria<TypeEvenementEntreprise> criteria = new EvenementEntrepriseCriteria<>();
		criteria.setNumeroEntrepriseCivile(noEntrepriseCivile);
		criteria.setRegDateEvenementDebut(date.getOneDayAfter());
		final List<EvenementEntreprise> trouves = find(criteria, null);
		final List<EvenementEntreprise> filtres = new ArrayList<>(trouves.size());
		for (EvenementEntreprise trouve : trouves) {
			if (!trouve.isAnnule()) {
				filtres.add(trouve);
			}
		}
		return filtres;
	}

	@Override
	public boolean isEvenementDateValeurDansLePasse(EvenementEntreprise event) {
		final EvenementEntrepriseCriteria<TypeEvenementEntreprise> criteria = new EvenementEntrepriseCriteria<>();
		criteria.setNumeroEntrepriseCivile(event.getNoEntrepriseCivile());
		criteria.setRegDateEvenementDebut(event.getDateEvenement().getOneDayAfter());
		final List<EvenementEntreprise> trouves = find(criteria, null);
		final List<EvenementEntreprise> filtres = new ArrayList<>(trouves.size());
		for (EvenementEntreprise trouve : trouves) {
			if (!trouve.isAnnule()) {
				filtres.add(trouve);
			}
		}
		final List<EvenementEntreprise> evtRecusAvant = new ArrayList<>();
		for (EvenementEntreprise evt : filtres) {
			if (evt.getId() < event.getId()) {
				evtRecusAvant.add(evt);
			}
		}
		return !evtRecusAvant.isEmpty();
	}

	@Override
	@NotNull
	public List<EvenementEntreprise> evenementsPourDateValeurEtEntreprise(RegDate date, Long noEntrepriseCivile) {
		final EvenementEntrepriseCriteria<TypeEvenementEntreprise> criteria = new EvenementEntrepriseCriteria<>();
		criteria.setNumeroEntrepriseCivile(noEntrepriseCivile);
		criteria.setRegDateEvenementDebut(date);
		criteria.setRegDateEvenementFin(date);
		final List<EvenementEntreprise> trouves = find(criteria, null);
		final List<EvenementEntreprise> filtres = new ArrayList<>(trouves.size());
		for (EvenementEntreprise trouve : trouves) {
			if (!trouve.isAnnule()) {
				filtres.add(trouve);
			}
		}
		return filtres;
	}

	@Override
	public Set<Long> getNosEntreprisesCivilesConcerneesParEvenementsPourRetry() {
		final Query query = getCurrentSession().createQuery("select noEntrepriseCivile from EvenementEntreprise where annulationDate is null and noEntrepriseCivile is not null and etat in (:etats)");
		query.setParameter("etats", ETATS_NON_TRAITES);
		//noinspection unchecked
		return new HashSet<>(query.list());
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<EvenementEntreprise> find(final EvenementEntrepriseCriteria<TypeEvenementEntreprise> criterion, @Nullable final ParamPagination paramPagination) {
		if (criterion == null) {
			throw new IllegalArgumentException("Les critères de recherche peuvent pas être nuls");
		}

		final Map<String, Object> paramsWhere = new HashMap<>();
		final String queryWhere = buildCriterion(paramsWhere, criterion);
		if (queryWhere == null) {
			return Collections.emptyList();
		}

		final String fromComplement = criterion.isJoinOnEntreprise() ? ", Entreprise en" : "";
		final String select = String.format("select evenement from %s evenement %s where 1=1 %s", EvenementEntreprise.class.getSimpleName(), fromComplement, queryWhere);
		final QueryFragment fragment = new QueryFragment(select, paramsWhere);

		// tri par défaut
		if (paramPagination != null) {
			fragment.add(paramPagination.buildOrderClause("evenement", "dateEvenement", true, null));
		}

		final Session session = getCurrentSession();
		final Query queryObject = fragment.createQuery(session);
		if (paramPagination != null) {
			final int firstResult = paramPagination.getSqlFirstResult();
			final int maxResult = paramPagination.getSqlMaxResults();

			queryObject.setFirstResult(firstResult);
			queryObject.setMaxResults(maxResult);
		}
		return queryObject.list();
	}

	@Override
	public List<EvenementEntreprise> getEvenementsForNoEvenement(long noEvenement) {
		final Query query = getCurrentSession().createQuery("from EvenementEntreprise where noEvenement = :noEvenement");
		query.setParameter("noEvenement", noEvenement);
		//noinspection unchecked
		return query.list();
	}

	@Override
	public EvenementEntreprise getEvenementForNoAnnonceIDE(long noAnnonce) {
		final Query query = getCurrentSession().createQuery("from EvenementEntreprise where referenceAnnonceIDE.id = :noAnnonce");
		query.setParameter("noAnnonce", noAnnonce);
		return (EvenementEntreprise) query.uniqueResult();
	}

	@Override
	public List<EvenementEntreprise> getEvenementsForBusinessId(String businessId) {
		final Query query = getCurrentSession().createQuery("from EvenementEntreprise where businessId = :businessId");
		query.setParameter("businessId", businessId);
		//noinspection unchecked
		return query.list();
	}

	@Override
	public int count(EvenementEntrepriseCriteria<TypeEvenementEntreprise> criterion) {
		if (criterion == null) {
			throw new IllegalArgumentException("Les critères de recherche peuvent pas être nuls");
		}
		final Map<String, Object> criteria = new HashMap<>();
		String queryWhere = buildCriterion(criteria, criterion);
		String query = String.format(
				"select count(*) from %s evenement %s where 1=1 %s",
				EvenementEntreprise.class.getSimpleName(),
				criterion.isJoinOnEntreprise() ? ", Entreprise en" : "",
				queryWhere);
		return DataAccessUtils.intResult(find(query, criteria, null));
	}

	/**
	 * @param criteria target
	 * @param criterion source
	 * @return la clause where correspondante à l'objet criterion
	 */
	protected String buildCriterion(Map<String, Object> criteria, EvenementEntrepriseCriteria<TypeEvenementEntreprise> criterion) {
		String queryWhere = "";

		// Si la valeur n'existe pas (TOUS par exemple), type = null
		final TypeEvenementEntreprise type = criterion.getType();
		if (type != null) {
			queryWhere += " and evenement.type = :type";
			criteria.put("type", type);
		}

		// Si la valeur n'existe pas (TOUS par exemple), etat = null
		final EtatEvenementEntreprise etat = criterion.getEtat();
		if (etat != null) {
			queryWhere += " and evenement.etat = :etat";
			criteria.put("etat", etat);
		}

		// Si la valeur n'existe pas (TOUTES par exemple), formeJuridique = null
		final FormeJuridiqueEntreprise formeJuridique = criterion.getFormeJuridique();
		if (formeJuridique != null) {
			queryWhere += " and evenement.formeJuridique = :formeJuridique";
			criteria.put("formeJuridique", formeJuridique);
		}

		Date dateTraitementDebut = criterion.getDateTraitementDebut();
		if (dateTraitementDebut != null) {
			queryWhere += " and evenement.dateTraitement >= :dateTraitementMin";
			// On prends la date a Zero Hour
			criteria.put("dateTraitementMin", dateTraitementDebut);
		}

		Date dateTraitementFin = criterion.getDateTraitementFin();
		if (dateTraitementFin != null) {
			queryWhere += " and evenement.dateTraitement <= :dateTraitementMax";
			// On prends la date a 24 Hour
			criteria.put("dateTraitementMax", dateTraitementFin);
		}

		RegDate dateEvenementDebut = criterion.getRegDateEvenementDebut();
		if (dateEvenementDebut != null) {
			queryWhere += " and evenement.dateEvenement >= :dateEvtMin";
			criteria.put("dateEvtMin", dateEvenementDebut);
		}

		RegDate dateEvenementFin = criterion.getRegDateEvenementFin();
		if (dateEvenementFin != null) {
			queryWhere += " and evenement.dateEvenement <= :dateEvtMax";
			criteria.put("dateEvtMax", dateEvenementFin);
		}

		Long numero = criterion.getNumeroEntrepriseCivile();
		if (numero != null) {
			queryWhere += " and evenement.noEntrepriseCivile = :noEntrepriseCivile";
			criteria.put("noEntrepriseCivile", numero);
		}

		Long numeroCTB = criterion.getNumeroCTB();
		if (numeroCTB != null) {
			queryWhere += " and (evenement.noEntrepriseCivile = en.numeroEntreprise) and en.numero = :noCtb";
			criteria.put("noCtb", numeroCTB);
		}


		return queryWhere;
	}

}
