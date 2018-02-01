package ch.vd.unireg.tache.sync;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.tiers.TacheAnnulationDeclarationImpot;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatTache;

/**
 * Action permettant d'annuler une déclaration d'impôt PM créant une tâche d'annulation.
 */
public class DeleteDIPM extends DeleteDI<DeclarationImpotOrdinairePM> {

	private final TypeDocument typeDocument;

	public DeleteDIPM(DeclarationImpotOrdinairePM declaration) {
		super(declaration);
		this.typeDocument = declaration.getTypeDeclaration();
	}

	@Override
	public void execute(Context context) {
		final DeclarationImpotOrdinairePM declaration = (DeclarationImpotOrdinairePM) context.diDAO.get(diId);
		final TacheAnnulationDeclarationImpot tache = new TacheAnnulationDeclarationImpot(TypeEtatTache.EN_INSTANCE, null, context.contribuable, declaration, context.collectivite);
		context.tacheDAO.save(tache);
	}

	@Override
	public String toString() {
		final String descriptionTypeContribuable = (typeContribuable == null ? "de type inconnu" : typeContribuable.description());
		final String descriptionTypeDocument = (typeDocument == null ? "déclaration d'impôt" : typeDocument.getDescription());
		return String.format("création d'une tâche d'annulation la %s %s couvrant la période du %s au %s",
		                     descriptionTypeDocument,
		                     descriptionTypeContribuable,
		                     RegDateHelper.dateToDisplayString(dateDebut),
		                     RegDateHelper.dateToDisplayString(dateFin));
	}
}
