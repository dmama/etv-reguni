package ch.vd.uniregctb.declaration;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.FlushMode;
import org.springframework.util.Assert;

import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.type.TypeDocument;

public class ModeleDocumentDAOImpl extends BaseDAOImpl<ModeleDocument, Long> implements ModeleDocumentDAO {

	public ModeleDocumentDAOImpl() {
		super(ModeleDocument.class);
	}

	@Override
	public ModeleDocument getModelePourDeclarationImpotOrdinaire(PeriodeFiscale periode, TypeDocument type) {
		return getModelePourDeclarationImpotOrdinaire(periode, type, false);
	}

	@Override
	public ModeleDocument getModelePourDeclarationImpotOrdinaire(PeriodeFiscale periode, TypeDocument type, boolean doNotAutoFlush) {

		Assert.notNull(periode, "La période fiscale ne doit pas être nulle.");
		Assert.notNull(type, "Le type de document ne doit pas être nul.");

		// Recherche du modèle de document correspondant
		final Map<String, Object> params = new HashMap<>(2);
		params.put("pf", periode);
		params.put("typeDocument", type);
		final String query = "FROM ModeleDocument m WHERE m.periodeFiscale = :pf AND m.typeDocument = :typeDocument";
		final FlushMode mode = (doNotAutoFlush ? FlushMode.MANUAL : null);
		final List<ModeleDocument> list = find(query, params, mode);
		if (list != null && !list.isEmpty()) {
			if (list.size() > 1) {
				throw new RuntimeException("Trouvé plus d'un modèle de document pour la période fiscale [" + periode.getAnnee()
						+ "] et le type de document [" + type.name() + "].");
			}
			return list.get(0);
		}
		return null;
	}
	
	@Override
	public ModeleDocument getModelePourDeclarationImpotSource(PeriodeFiscale periode) {
		return getModelePourDeclarationImpotSource(periode, false);
	}

	@Override
	public ModeleDocument getModelePourDeclarationImpotSource(PeriodeFiscale periode, boolean doNotAutoFlush) {

		Assert.notNull(periode, "La période fiscale ne doit pas être nulle.");

		// Recherche du modèle de document correspondant
		final Map<String, Object> params = buildNamedParameters(Pair.<String, Object>of("pf", periode),
		                                                        Pair.<String, Object>of("typeDocument", TypeDocument.LISTE_RECAPITULATIVE));
		final String query = "FROM ModeleDocument m WHERE m.periodeFiscale = :pf AND m.typeDocument = :typeDocument";
		final FlushMode mode = (doNotAutoFlush ? FlushMode.MANUAL : null);
		final List<ModeleDocument> list = find(query, params, mode);
		if (list != null && !list.isEmpty()) {
			if (list.size() > 1) {
				throw new RuntimeException("Trouvé plus d'un modèle de document pour la période fiscale [" + periode.getAnnee()
						+ "] et le type de document [" + TypeDocument.LISTE_RECAPITULATIVE.name() + "].");
			}
			return list.get(0);
		}
		return null;
	}

	@Override
	public List<ModeleDocument> getByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		final List<ModeleDocument> list = find("FROM ModeleDocument m WHERE m.periodeFiscale = :pf", buildNamedParameters(Pair.of("pf", periodeFiscale)), null);

		Collections.sort(list, new Comparator<ModeleDocument>() {
			@Override
			public int compare(ModeleDocument o1, ModeleDocument o2) {
				return o1.getTypeDocument().compareTo(o2.getTypeDocument());
			}
		});
		return list;
	}
}
