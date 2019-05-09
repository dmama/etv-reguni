package ch.vd.unireg.declaration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.support.DataAccessUtils;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.dbutils.QueryFragment;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.ModeCommunication;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

public class ListeRecapitulativeDAOImpl extends DeclarationDAOImpl<DeclarationImpotSource> implements  ListeRecapitulativeDAO {

	private static final Logger LOGGER = LoggerFactory.getLogger(ListeRecapitulativeDAOImpl.class);

	public ListeRecapitulativeDAOImpl() {
		super(DeclarationImpotSource.class);
	}


	/**
	 * Recherche des listes recapitulatives selon des criteres
	 *
	 * @param criterion
	 * @return
	 */
	@Override
	public List<DeclarationImpotSource> find(final ListeRecapitulativeCriteria criterion, @Nullable final ParamPagination paramPagination) {

		final Session session = getCurrentSession();
		final Map<String, Object> paramsWhereClause = new HashMap<>();
		final String whereClause = buildWhereClauseFromCriteria(criterion, paramsWhereClause);

		final QueryFragment fragment = new QueryFragment("SELECT lr FROM DeclarationImpotSource lr WHERE 1=1 " + whereClause, paramsWhereClause);
		if (paramPagination != null) {
			fragment.add(paramPagination.buildOrderClause("lr", null, true, null));
		}
		else {
			fragment.add("ORDER BY lr.id ASC");
		}

		final Query queryObject = fragment.createQuery(session);

		if (paramPagination != null) {
			final int firstResult = paramPagination.getSqlFirstResult();
			final int maxResult = paramPagination.getSqlMaxResults();
			queryObject.setFirstResult(firstResult);
			queryObject.setMaxResults(maxResult);
		}

		//noinspection unchecked
		return queryObject.list();
	}

	/**
	 * @see ch.vd.unireg.declaration.ListeRecapitulativeDAO#count(ListeRecapitulativeCriteria)
	 */
	@Override
	public int count(ListeRecapitulativeCriteria criterion) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of ListeRecapitulativeDAO : count");
		}

		final Map<String, Object> parameters = new HashMap<>();
		final String query = String.format("SELECT COUNT(lr) FROM DeclarationImpotSource lr WHERE 1=1 %s",
											buildWhereClauseFromCriteria(criterion, parameters));

		return DataAccessUtils.intResult(find(query, parameters, null));
	}


	/**
	 * Construit une clause where avec les critères demandés et remplit la liste passée en paramètre en conséquence
	 * @param criterion
	 * @param parameters
	 * @return " and ... and ... "
	 */
    private String buildWhereClauseFromCriteria(ListeRecapitulativeCriteria criterion, Map<String, Object> parameters) {

    	final StringBuilder builder = new StringBuilder();

	    final PeriodiciteDecompte periodicite = criterion.getPeriodicite();
	    if (periodicite != null) {
			builder.append(" and lr.periodicite = :periodicite");
			parameters.put("periodicite", periodicite);
		}

	    final CategorieImpotSource categorie = criterion.getCategorie();
	    if (categorie != null) {
			builder.append(" and lr.tiers.categorieImpotSource = :categorieIS");
			parameters.put("categorieIS", categorie);
		}

	    final ModeCommunication modeCommunication = criterion.getModeCommunication();
	    if (modeCommunication != null) {
			builder.append(" and lr.modeCommunication = :modeCommunication");
			parameters.put("modeCommunication", modeCommunication);
		}

		final RegDate periode = criterion.getPeriode();
		if (periode != null) {
			builder.append(" and lr.dateDebut = :dateDebut");
			parameters.put("dateDebut", periode);
		}

	    final TypeEtatDocumentFiscal etat = criterion.getEtat();
	    if (etat != null) {
			if (etat != TypeEtatDocumentFiscal.EMIS) {
				builder.append(" and exists (select etat.id from EtatDeclaration etat where etat.declaration.id = lr.id and type(etat) = ");

				final Class<? extends EtatDeclaration> classeOfEtatDeclaration = EtatDeclarationHelper.getClasseOfEtatDeclaration(etat);
				builder.append(classeOfEtatDeclaration.getName());
				builder.append(" and etat.annulationDate is null)");
			}

			if (etat != TypeEtatDocumentFiscal.RETOURNE) {

				final List<Class<? extends EtatDeclaration>> classesEtatDeclarationsInterdits = new ArrayList<>(3);
				classesEtatDeclarationsInterdits.add(EtatDeclarationRetournee.class);
				switch (etat) {
					case EMIS:
						classesEtatDeclarationsInterdits.add(EtatDeclarationSommee.class);
						classesEtatDeclarationsInterdits.add(EtatDeclarationEchue.class);
						break;
					case SOMME:
						classesEtatDeclarationsInterdits.add(EtatDeclarationEchue.class);
						break;
					case ECHU:
						break;
					default:
						throw new IllegalArgumentException("Valeur de l'état non-supportée : " + etat);
				}

				builder.append(" and not exists (select etat.id from EtatDeclaration etat where etat.declaration.id = lr.id and type(etat) in (");
				boolean first = true;
				for (Class<? extends EtatDeclaration> classeEtatInterdit : classesEtatDeclarationsInterdits) {
					if (!first) {
						builder.append(" ,");
					}
					builder.append(classeEtatInterdit.getName());
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
	@Override
	@SuppressWarnings("unchecked")
	public List<DeclarationImpotSource> findByNumero(Long numero) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of ListeRecapitulativeDAO : find");
		}

		final String query = " select lr from DeclarationImpotSource lr where lr.tiers.numero = :tiersId";
		final Map<String, Long> params = new HashMap<>(1);
		params.put("tiersId", numero);
		return find(query, params, null);

	}

	/**
	 * Retourne le dernier EtatPeriodeDeclaration envoyé et non annulé
	 *
	 * @param numeroDpi
	 * @return
	 */
	@Override
	@SuppressWarnings("unchecked")
	public EtatDeclaration findDerniereLrEnvoyee(Long numeroDpi) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of ListeRecapitulativeDAO : findDerniereLrEnvoyee");
		}

		final Map<String, Long> params = new HashMap<>(1);
		params.put("noDpi", numeroDpi);
		final String query = "select etatPeriode from EtatDeclarationEmise etatPeriode where etatPeriode.declaration.tiers.numero = :noDpi and etatPeriode.declaration.annulationDate is null order by etatPeriode.dateObtention desc";
		final List<EtatDeclaration> list = find(query, params, null);
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
	@Override
	@SuppressWarnings("unchecked")
	public List<DateRange> findIntersection(long numeroDpi, DateRange range) {

		if (range.getDateDebut() == null) {
			throw new IllegalArgumentException();
		}
		if (range.getDateFin() == null) {
			throw new IllegalArgumentException();
		}

		final Map<String, Object> params = new HashMap<>(3);
		params.put("noDpi", numeroDpi);
		params.put("debutMax", range.getDateFin());
		params.put("finMin", range.getDateDebut());
		final String query = "SELECT lr.dateDebut, lr.dateFin FROM DeclarationImpotSource lr WHERE lr.tiers.numero = :noDpi AND lr.dateDebut <= :debutMax AND lr.dateFin >= :finMin AND lr.annulationDate IS NULL ORDER BY lr.dateDebut ASC";
		final List<Object[]> queryResult = find(query, params, null);
		final List<DateRange> resultat = new ArrayList<>(queryResult.size());
		for (Object[] intersection : queryResult) {
			resultat.add(new DateRangeHelper.Range((RegDate) intersection[0], (RegDate) intersection[1]));
		}
		return resultat;
	}



}
