package ch.vd.unireg.declaration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

public class DeclarationImpotOrdinaireDAOImpl extends DeclarationDAOImpl<DeclarationImpotOrdinaire> implements DeclarationImpotOrdinaireDAO {

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
				params.put("pfMin", anneeRange.getLeft());
				params.put("pfMax", anneeRange.getRight());
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
			LOGGER.trace("DeclarationImpotCriteria Params: " + Arrays.toString(params.entrySet().toArray(new Map.Entry[0])));
		}

		final FlushMode mode = (doNotAutoFlush ? FlushMode.MANUAL : null);
		final List<DeclarationImpotOrdinaire> list = find(query, params, mode);
		final List<DeclarationImpotOrdinaire> listRtr = new ArrayList<>();

		if (criterion.getEtat() == null || criterion.getEtat().equals(TOUS)) {
			listRtr.addAll(list);
		}
		else {
			final TypeEtatDocumentFiscal etat = TypeEtatDocumentFiscal.valueOf(criterion.getEtat());
			for (DeclarationImpotOrdinaire di : list) {
				if (di.getDernierEtatDeclaration().getEtat() == etat) {
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
		final Map<String, Long> params = Collections.singletonMap("tiersId", numero);
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
		final Map<String, Long> params = Collections.singletonMap("tiersId", numeroCtb);
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
	public List<Long> findIdsDeclarationsOrdinairesEmisesFrom(int periodeDebut) {
		final Query query = getCurrentSession().createQuery("select di.id " +
				                                                    "from EtatDeclarationEmise etat " +
				                                                    "join etat.documentFiscal di " +
				                                                    "where etat.annulationDate is null " +
				                                                    "and di.annulationDate is null " +
				                                                    "and type(di) in (DeclarationImpotOrdinairePP, DeclarationImpotOrdinairePM) " +
				                                                    "and di.periode.annee >= :periode " +
				                                                    "and di.tiers.annulationDate is null " +
				                                                    "order by di.tiers.id, di.id");
		query.setParameter("periode", periodeDebut);

		//noinspection unchecked
		final List<Object> list = query.list();
		return list.stream()
				.map(id -> ((Number) id).longValue())
				.collect(Collectors.toList());
	}
}
