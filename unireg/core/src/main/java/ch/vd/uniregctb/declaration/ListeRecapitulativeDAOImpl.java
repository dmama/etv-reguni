package ch.vd.uniregctb.declaration;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.orm.hibernate3.HibernateCallback;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class ListeRecapitulativeDAOImpl extends GenericDAOImpl< DeclarationImpotSource, Long> implements  ListeRecapitulativeDAO {

	private static final Logger LOGGER = Logger.getLogger(ListeRecapitulativeDAOImpl.class);

	private final String TOUS = "TOUS";

	public ListeRecapitulativeDAOImpl() {
		super(DeclarationImpotSource.class);
	}


	/**
	 * Recherche des listes recapitulatives selon des criteres
	 *
	 * @param criterion
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<DeclarationImpotSource> find(final ListeRecapCriteria criterion, final ParamPagination paramPagination) {

		return (List<DeclarationImpotSource>) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {

				final List<Object> parameters = new ArrayList<Object>();
				final String query = String.format("SELECT lr FROM DeclarationImpotSource lr WHERE 1=1 %s%s", buildWhereClauseFromCriteria(
						criterion, parameters), buildOrderClause(paramPagination));

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("EvenementCriteria Query: " + query);
					LOGGER.debug("EvenementCriteria Table size: " + parameters.size());
				}

				final Query queryObject = session.createQuery(query);
				final Object[] values = parameters.toArray();
				if (values != null) {
					for (int i = 0; i < values.length; i++) {
						queryObject.setParameter(i, values[i]);
					}
				}

				if (paramPagination != null) {
					final int firstResult = (paramPagination.getNumeroPage() - 1) * paramPagination.getTaillePage();
					final int maxResult = paramPagination.getTaillePage();
					queryObject.setFirstResult(firstResult);
					queryObject.setMaxResults(maxResult);
				}

				return queryObject.list();
			}
		});
	}

	/**
	 * Construit la clause order pour pouvoir assurer la pagination
	 *
	 * @param paramPagination
	 * @return
	 */
	private String buildOrderClause(ParamPagination paramPagination) {

		final String clauseOrder;

		if (paramPagination != null && paramPagination.getChamp() != null) {

			final StringBuilder builder = new StringBuilder(" ORDER BY lr.");
			if (paramPagination.getChamp().equals("type")) {
				builder.append("class");
			}
			else {
				builder.append(paramPagination.getChamp());
			}
			builder.append(paramPagination.isSensAscending() ? " ASC" : " DESC");
			clauseOrder = builder.toString();
		}
		else {
			clauseOrder = " ORDER BY lr.id ASC";

		}

		return clauseOrder;
	}


	/**
	 * @see ch.vd.uniregctb.declaration.ListeRecapitulativeDAO#count(ch.vd.uniregctb.declaration.ListeRecapCriteria)
	 */
	public int count(ListeRecapCriteria criterion) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of ListeRecapitulativeDAO : count");
		}

		final List<Object> parameters = new ArrayList<Object>();
		final String query = String.format("SELECT COUNT(lr) FROM DeclarationImpotSource lr WHERE 1=1 %s",
											buildWhereClauseFromCriteria(criterion, parameters));

		return DataAccessUtils.intResult(getHibernateTemplate().find(query, parameters.toArray()));
	}


	/**
	 * Construit une clause where avec les critères demandés et remplit la liste passée en paramètre en conséquence
	 * @param criterion
	 * @param parameters
	 * @return " and ... and ... "
	 */
    private String buildWhereClauseFromCriteria(ListeRecapCriteria criterion, List<Object> parameters) {

    	final StringBuilder builder = new StringBuilder();

    	if (!criterion.getPeriodicite().equals(TOUS)) {
			final PeriodiciteDecompte periodicite = PeriodiciteDecompte.valueOf(criterion.getPeriodicite());
			builder.append(" and lr.periodicite = ? ");
			parameters.add(periodicite.name());
		}

		if (!criterion.getCategorie().equals(TOUS)) {
			final CategorieImpotSource categorie = CategorieImpotSource.valueOf(criterion.getCategorie());
			builder.append(" and lr.tiers.categorieImpotSource = ? ");
			parameters.add(categorie.name());
		}

		if (!criterion.getModeCommunication().equals(TOUS)) {
			final ModeCommunication mode = ModeCommunication.valueOf(criterion.getModeCommunication());
			builder.append(" and lr.modeCommunication = ? ");
			parameters.add(mode.name());
		}

		final RegDate periode = criterion.getPeriode();
		if (periode != null) {
			builder.append(" and lr.dateDebut = ? ");
			parameters.add(periode.index());
		}

		if (!criterion.getEtat().equals(TOUS)) {
			final TypeEtatDeclaration etat = TypeEtatDeclaration.valueOf(criterion.getEtat());
			if (etat != TypeEtatDeclaration.EMISE) {
				builder.append(" and exists (select etat.id from EtatDeclaration etat where etat.declaration.id = lr.id and etat.class = ? and etat.annulationDate is null)");
				EtatDeclaration etatCourant = EtatDeclaration.getInstanceOfEtatDeclaration(etat);
				parameters.add(etatCourant.getClass().getName());
			}

			if (etat != TypeEtatDeclaration.RETOURNEE) {

				final List<Class<? extends EtatDeclaration>> classesEtatDeclarationsInterdits = new ArrayList<Class<? extends EtatDeclaration>>(3);
				classesEtatDeclarationsInterdits.add(EtatDeclarationRetournee.class);
				switch (etat) {
					case EMISE:
						classesEtatDeclarationsInterdits.add(EtatDeclarationSommee.class);
						classesEtatDeclarationsInterdits.add(EtatDeclarationEchue.class);
						break;
					case SOMMEE:
						classesEtatDeclarationsInterdits.add(EtatDeclarationEchue.class);
						break;
					case ECHUE:
						break;
					default:
						throw new IllegalArgumentException("Valeur de l'état non-supportée : " + etat);
				}

				builder.append(" and not exists (select etat.id from EtatDeclaration etat where etat.declaration.id = lr.id and etat.class in (");
				boolean first = true;
				for (Class classeEtatInterdit : classesEtatDeclarationsInterdits) {
					if (!first) {
						builder.append(",");
					}
					builder.append("?");
					parameters.add(classeEtatInterdit.getName());
					first = false;
				}
				builder.append(") and etat.annulationDate is null)");
			}
		}

		return builder.toString();
    }

	/**
	 * Recherche toutes les LR en fonction du numero de debiteur
	 *
	 * @param numero
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<DeclarationImpotSource> findByNumero(Long numero) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of ListeRecapitulativeDAO : find");
		}

		String query = " select lr from DeclarationImpotSource lr where lr.tiers.numero = ? ";
		List<Object> criteria = new ArrayList<Object>();
		criteria.add(numero);
		List<DeclarationImpotSource> list = getHibernateTemplate().find(query, criteria.toArray());
		return list;

	}

	/**
	 * Retourne le dernier EtatPeriodeDeclaration envoyé et non annulé
	 *
	 * @param lrId
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public EtatDeclaration findDerniereLrEnvoyee(Long numeroDpi) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of ListeRecapitulativeDAO : findDerniereLrEnvoyee");
		}

		final String query = "select etatPeriode from EtatDeclarationEmise etatPeriode where etatPeriode.declaration.tiers.numero = ?  and etatPeriode.declaration.annulationDate is null order by etatPeriode.dateObtention desc";
		final List<Object> criteria = new ArrayList<Object>();
		criteria.add(numeroDpi);
		final List<EtatDeclaration> list = getHibernateTemplate().find(query, criteria.toArray());
		EtatDeclaration etat = null;
		for (EtatDeclaration etatCourant : list) {
			if(etat == null || etat.getDeclaration().getDateDebut().isBefore(etatCourant.getDeclaration().getDateDebut())) {
				etat = etatCourant;
			}
		}
		return etat;

	}


	/**
	 * Retourne une liste de date ranges représentant des LR qui intersectent
	 * avec la période donnée pour le débiteur donné
	 *
	 * @param numeroDpi
	 * @param range
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<DateRange> findIntersection(long numeroDpi, DateRange range) {

		Assert.notNull(range.getDateDebut());
		Assert.notNull(range.getDateFin());

		final String query = "SELECT lr.dateDebut, lr.dateFin FROM DeclarationImpotSource lr WHERE lr.tiers.numero = ? AND lr.dateDebut <= ? AND lr.dateFin >= ? AND lr.annulationDate IS NULL ORDER BY lr.dateDebut ASC";
		final Object[] criteres = {numeroDpi, range.getDateFin().index(), range.getDateDebut().index() };
		final List<Object[]> queryResult = getHibernateTemplate().find(query, criteres);
		final List<DateRange> resultat = new ArrayList<DateRange>(queryResult.size());
		for (Object[] intersection : queryResult) {
			resultat.add(new DateRangeHelper.Range((RegDate) intersection[0], (RegDate) intersection[1]));
		}
		return resultat;
	}



}
