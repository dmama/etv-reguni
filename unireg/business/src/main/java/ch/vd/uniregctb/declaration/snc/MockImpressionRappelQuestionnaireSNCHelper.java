package ch.vd.uniregctb.declaration.snc;

import org.jetbrains.annotations.Nullable;

import ch.vd.editique.unireg.FichierImpression;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.tiers.Contribuable;

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
	public FichierImpression.Document buildCopieMandataire(FichierImpression.Document original, Contribuable destinataire, RegDate dateReference) throws EditiqueException {
		throw new IllegalArgumentException("no meant to be called");
	}
}
