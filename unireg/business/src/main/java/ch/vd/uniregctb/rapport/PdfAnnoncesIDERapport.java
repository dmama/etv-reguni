package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.evenement.ide.AnnonceIDEJobResults;

/**
 * Rapport PDF contenant les résultats de l'évaluation et de l'envoi des annonces à l'IDE pour les entreprises sous contrôle de l'ACI.
 */
public class PdfAnnoncesIDERapport extends PdfRapport {

	public void write(final AnnonceIDEJobResults results, final String nom, final String description, final Date dateGeneration,
	                  OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		final boolean simulation = results.isSimulation();

		// Titre
		addTitrePrincipal(String.format("Rapport d'annonce à l'IDE des entreprises sous contrôle de l'ACI%s", simulation ? " (Simulation)" : ""));

		// Paramètres
		addEntete1("Paramètre");
		{
			addTableSimple(2, new TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Simulation : ", String.valueOf(simulation));
				}
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.isInterrupted()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						+ "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, new TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre total d'actions menées :", String.valueOf(results.getAnnoncesIDE().size()));
					table.addLigne("Nombre total d'erreurs :", String.valueOf(results.getExceptions().size()));
					table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
					table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
				}
			});
		}

		// Action menées
		{
			final String filename = "actions_menees.csv";
			final String titre = "Liste des actions menées par le job";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = actionsAsCsvFile(results.getAnnoncesIDE(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Erreurs
		{
			final String filename = "erreurs.csv";
			final String titre = "Liste des erreurs";
			final String listVide = "(aucune)";
			try (TemporaryFile contenu = errorsAsCsvFile(results.getExceptions(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		close();
		status.setMessage("Génération du rapport terminée.");
	}

	private TemporaryFile actionsAsCsvFile(List<AnnonceIDEJobResults.AnnonceInfo> actions, String fileName, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(actions, fileName, status, new CsvHelper.FileFiller<AnnonceIDEJobResults.AnnonceInfo>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("ENTREPRISE_ID").append(COMMA);
				b.append("ANNONCE_ID").append(COMMA);
				b.append("ANNONCE_TYPE").append(COMMA);
				b.append("ANNONCE_DATE").append(COMMA);
				b.append("ENTREPRISE_IDE").append(COMMA);
				b.append("ENTREPRISE_RAISON_SOCIALE").append(COMMA);
				b.append("ENTREPRISE_FORME_JURIDIQUE").append(COMMA);
				b.append("ENTREPRISE_SECTEUR_ACTIVITE").append(COMMA);
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, AnnonceIDEJobResults.AnnonceInfo elt) {
				final BaseAnnonceIDE ann = elt.annonceIDE;
				b.append(elt.entrepriseId).append(COMMA);
				String ligneNumero = ann instanceof AnnonceIDE ? ((AnnonceIDE) ann).getNumero().toString() : "";
				b.append(asCsvField(escapeChars(ligneNumero))).append(COMMA);
				b.append(asCsvField(escapeChars(ann.getType().toString()))).append(COMMA);
				b.append(asCsvField(escapeChars(DateHelper.dateTimeToDisplayString(ann.getDateAnnonce())))).append(COMMA);
				b.append(asCsvField(escapeChars(ann.getNoIde() == null ? "" : ann.getNoIde().getValeur()))).append(COMMA);
				final BaseAnnonceIDE.Contenu contenu = ann.getContenu();
				b.append(asCsvField(escapeChars(contenu == null ? "" : contenu.getNom()))).append(COMMA);
				b.append(asCsvField(escapeChars(contenu == null ? "" : contenu.getFormeLegale() == null ? "" : contenu.getFormeLegale().toString()))).append(COMMA);
				b.append(asCsvField(escapeChars(contenu == null ? "" : contenu.getSecteurActivite()))).append(COMMA);
				return true;
			}
		});
	}

	private TemporaryFile errorsAsCsvFile(List<AnnonceIDEJobResults.ExceptionInfo> exceptions, String fileName, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(exceptions, fileName, status, new CsvHelper.FileFiller<AnnonceIDEJobResults.ExceptionInfo>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("ENTREPRISE_ID").append(COMMA);
				b.append("EXCEPTION");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, AnnonceIDEJobResults.ExceptionInfo elt) {
				b.append(elt.entrepriseId).append(COMMA);
				b.append(asCsvField(escapeChars(elt.exceptionMsg)));
				return true;
			}
		});
	}
}

