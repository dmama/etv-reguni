package ch.vd.uniregctb.declaration;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.hibernate.FlushMode;
import org.springframework.util.Assert;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.uniregctb.type.TypeDocument;

public class ModeleDocumentDAOImpl extends GenericDAOImpl<ModeleDocument, Long> implements ModeleDocumentDAO {

	public ModeleDocumentDAOImpl() {
		super(ModeleDocument.class);
	}

	public ModeleDocument getModelePourDeclarationImpotOrdinaire(PeriodeFiscale periode, TypeDocument type) {
		return getModelePourDeclarationImpotOrdinaire(periode, type, false);
	}

	public ModeleDocument getModelePourDeclarationImpotOrdinaire(PeriodeFiscale periode, TypeDocument type, boolean doNotAutoFlush) {

		Assert.notNull(periode, "La période fiscale ne doit pas être nulle.");
		Assert.notNull(type, "Le type de document ne doit pas être nul.");

		// Recherche du modèle de document correspondant
		final String query1 = "FROM ModeleDocument m WHERE m.periodeFiscale = ? AND m.typeDocument = ?";
		final FlushMode mode = (doNotAutoFlush ? FlushMode.MANUAL : null);
		List<?> list = find(query1, new Object[] {
				periode, type.name()
		}, mode);

		if (list != null && list.size() > 0) {
			if (list.size() > 1) {
				throw new RuntimeException("Trouvé plus d'un modèle de document pour la période fiscale [" + periode.getAnnee()
						+ "] et le type de document [" + type.name() + "].");
			}
			return (ModeleDocument) list.get(0);
		}
		return null;
	}
	
	public ModeleDocument getModelePourDeclarationImpotSource(PeriodeFiscale periode) {
		return getModelePourDeclarationImpotSource(periode, false);
	}

	public ModeleDocument getModelePourDeclarationImpotSource(PeriodeFiscale periode, boolean doNotAutoFlush) {

		Assert.notNull(periode, "La période fiscale ne doit pas être nulle.");

		// Recherche du modèle de document correspondant
		final String query1 = "FROM ModeleDocument m WHERE m.periodeFiscale = ? AND m.typeDocument = ?";
		final FlushMode mode = (doNotAutoFlush ? FlushMode.MANUAL : null);
		List<?> list = find(query1, new Object[] {
				periode, TypeDocument.LISTE_RECAPITULATIVE.name()
		}, mode);

		if (list != null && list.size() > 0) {
			if (list.size() > 1) {
				throw new RuntimeException("Trouvé plus d'un modèle de document pour la période fiscale [" + periode.getAnnee()
						+ "] et le type de document [" + TypeDocument.LISTE_RECAPITULATIVE.name() + "].");
			}
			return (ModeleDocument) list.get(0);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public List<ModeleDocument> getByPeriodeFiscale(PeriodeFiscale periodeFiscale) {
		List list = find(
				"FROM ModeleDocument m WHERE m.periodeFiscale = ?",
				new Object[] {periodeFiscale},
				null);
			Collections.sort(
					list,
					new Comparator<ModeleDocument>() {
						public int compare(ModeleDocument o1, ModeleDocument o2) {
							return o1.getTypeDocument().compareTo(o2.getTypeDocument());
						}}
			);
			return list;

	}

}
