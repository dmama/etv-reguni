package ch.vd.unireg.declaration.snc;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.xml.editique.pm.FichierImpression;

public class MockImpressionRappelQuestionnaireSNCHelper implements ImpressionRappelQuestionnaireSNCHelper {

	@Override
	public String getIdDocument(QuestionnaireSNC questionnaire) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public String construitCleArchivageDocument(QuestionnaireSNC questionnaire) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public TypeDocumentEditique getTypeDocumentEditique(QuestionnaireSNC questionnaire) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public FichierImpression.Document buildDocument(QuestionnaireSNC questionnaire, RegDate dateRappel, RegDate dateEnvoiCourrier) throws EditiqueException {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Nullable
	@Override
	public FichierImpression.Document buildCopieMandatairePM(FichierImpression.Document original, Contribuable destinataire, RegDate dateReference) throws EditiqueException {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Nullable
	@Override
	public ch.vd.unireg.xml.editique.pp.FichierImpression.Document buildCopieMandatairePP(ch.vd.unireg.xml.editique.pp.FichierImpression.Document original, Contribuable destinataire, RegDate dateReference) throws EditiqueException {
		throw new IllegalArgumentException("no meant to be called");
	}
}
