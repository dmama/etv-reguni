package ch.vd.uniregctb.evenement.civil.regpp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.pagination.ParamPagination;
import ch.vd.uniregctb.evenement.civil.AbstractEvenementCivilDAOImpl;
import ch.vd.uniregctb.evenement.civil.EvenementCivilCriteria;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * DAO pour les événements civils RegPP
 */
public class EvenementCivilRegPPDAOImpl extends AbstractEvenementCivilDAOImpl<EvenementCivilRegPP, TypeEvenementCivil> implements EvenementCivilRegPPDAO {

	private static final List<EtatEvenementCivil> ETATS_NON_TRAITES;

	static {
		final List<EtatEvenementCivil> etats = new ArrayList<>(EtatEvenementCivil.values().length);
		for (EtatEvenementCivil etat : EtatEvenementCivil.values()) {
			if (!etat.isTraite()) {
				etats.add(etat);
			}
		}
		ETATS_NON_TRAITES = Collections.unmodifiableList(etats);
	}

	/**
	 * Constructeur par défaut.
	 */
	public EvenementCivilRegPPDAOImpl() {
		super(EvenementCivilRegPP.class);
	}

	/**
	 * Retourne les evenements d'un individu
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<EvenementCivilRegPP> rechercheEvenementExistantEtTraitable(final RegDate dateEvenement, final TypeEvenementCivil typeEvenement, final Long noIndividu) {
		final StringBuilder b = new StringBuilder();
		b.append("from EvenementCivilRegPP as ec where ec.dateEvenement = :date");
		b.append(" and ec.type = :type");
		b.append(" and ec.numeroIndividuPrincipal = :noIndividu");
		b.append(" and ec.etat in (:etats) ");
		final String sql = b.toString();

		final Session session = getCurrentSession();
		final Query query = session.createQuery(sql);
		query.setParameter("date", dateEvenement);
		query.setParameter("type", typeEvenement);
		query.setParameter("noIndividu", noIndividu);
		query.setParameterList("etats", ETATS_NON_TRAITES);
		return query.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<EvenementCivilRegPP> find(EvenementCivilCriteria<TypeEvenementCivil> criterion, @Nullable ParamPagination paramPagination) {
		return genericFind(criterion, paramPagination);
	}

	@Override
	protected Class getEvenementCivilClass() {
		return EvenementCivilRegPP.class;
	}

	@Override
	public int count(EvenementCivilCriteria<TypeEvenementCivil> criterion){
		return genericCount(criterion);
	}


	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getEvenementCivilsNonTraites() {
		final String s = "select evt.id from EvenementCivilRegPP evt where evt.etat in (:etats) order by evt.dateTraitement desc";
		final Session session = getCurrentSession();
		final Query query = session.createQuery(s);
		query.setParameterList("etats", ETATS_NON_TRAITES);
		return query.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<EvenementCivilRegPP> getEvenementsCivilsNonTraites(final Collection<Long> nosIndividus) {
		final String s = "SELECT e FROM EvenementCivilRegPP e WHERE e.etat IN (:etats) AND (e.numeroIndividuPrincipal IN (:col) OR e.numeroIndividuConjoint IN (:col))";
		final Session session = getCurrentSession();
		final Query query = session.createQuery(s);
		query.setParameterList("etats", ETATS_NON_TRAITES);
		query.setParameterList("col", nosIndividus);
		return query.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getIdsEvenementCivilsErreurIndividu(final Long numIndividu){
		final String s ="select evt.id from EvenementCivilRegPP evt where evt.etat in (:etats) and evt.numeroIndividuPrincipal = :ind order by evt.id asc";
		final Session session = getCurrentSession();
		final Query query = session.createQuery(s);
		query.setParameterList("etats", ETATS_NON_TRAITES);
		query.setParameter("ind", numIndividu);
		return query.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<EvenementCivilRegPP> findEvenementByIndividu(final Long numIndividu) {
		final String s = "select distinct e from EvenementCivilRegPP e where e.numeroIndividuPrincipal = :numIndividu or e.numeroIndividuConjoint = :numIndividu";
		final Session session = getCurrentSession();
		final Query query = session.createQuery(s);
		query.setParameter("numIndividu", numIndividu);
		return query.list();
	}

	@Override
	protected String buildCriterion(Map<String, Object> criteria, EvenementCivilCriteria<TypeEvenementCivil> criterion) {
		String queryWhere = super.buildCriterion(criteria, criterion);

		Long numero = criterion.getNumeroIndividu();
		if (numero != null) {
			queryWhere += " and (evenement.numeroIndividuPrincipal = :noIndividu or evenement.numeroIndividuConjoint = :noIndividu)";
			criteria.put("noIndividu", numero);
		}

		Long numeroCTB = criterion.getNumeroCTB();
		if (numeroCTB != null) {
			queryWhere += "and (evenement.numeroIndividuPrincipal = pp.numeroIndividu or evenement.numeroIndividuConjoint = pp.numeroIndividu) and pp.numero = :noCtb";
			criteria.put("noCtb", numeroCTB);
		}

		return queryWhere;
	}
}
