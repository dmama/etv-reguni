package ch.vd.uniregctb.declaration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.FlushMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

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
			listRtr.addAll(list);
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
}
