package ch.vd.uniregctb.declaration;

import java.util.List;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.uniregctb.type.TypeDocument;

public interface ModeleDocumentDAO extends GenericDAO<ModeleDocument, Long>  {

	ModeleDocument getModelePourDeclarationImpotOrdinaire(PeriodeFiscale periode, TypeDocument type);

	ModeleDocument getModelePourDeclarationImpotOrdinaire(PeriodeFiscale periode, TypeDocument type, boolean doNotAutoFlush);

	ModeleDocument getModelePourDeclarationImpotSource(PeriodeFiscale periode);

	ModeleDocument getModelePourDeclarationImpotSource(PeriodeFiscale periode, boolean doNotAutoFlush);
	
	List<ModeleDocument> getByPeriodeFiscale(PeriodeFiscale periodeFiscale);

}
