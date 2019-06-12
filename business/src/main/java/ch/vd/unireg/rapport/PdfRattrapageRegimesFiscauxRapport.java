package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.regimefiscal.RegimeFiscalConsolide;
import ch.vd.unireg.regimefiscal.rattrapage.RattrapageRegimesFiscauxJobResults;

/**
 * Rapport PDF contenant les résultats de du rattrapage des régimes fiscaux.
 */
public class PdfRattrapageRegimesFiscauxRapport extends PdfRapport {

	public void write(final RattrapageRegimesFiscauxJobResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		if (status == null) {
			throw new IllegalArgumentException();
		}

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		final boolean simulation = results.isSimulation();

		// Titre
		addTitrePrincipal(String.format("Rapport du rattrapage des régimes fiscaux%s", simulation ? " (Simulation)" : ""));

		// Paramètres
		addEntete1("Paramètre");
		{
			addTableSimple(2, table -> table.addLigne("Simulation : ", String.valueOf(simulation)));
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (status.isInterrupted()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						           + "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, table -> {
				table.addLigne("Nombre total de tiers modifiés :", String.valueOf(results.getRegimeFiscalInfos().size()));
				table.addLigne("Nombre total d'erreurs :", String.valueOf(results.getExceptions().size()));
				table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
				table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
			});
		}

		// Action menées
		{
			final String filename = "actions_menees.csv";
			final String titre = "Liste des tiers pour lesquels le job a créé un nouveau jeu de régimes fiscaux VD CH.";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = actionsAsCsvFile(results.getRegimeFiscalInfos(), filename, status)) {
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

	private TemporaryFile actionsAsCsvFile(List<RattrapageRegimesFiscauxJobResults.RegimeFiscalInfo> actions, String fileName, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(actions, fileName, status, new CsvHelper.FileFiller<RattrapageRegimesFiscauxJobResults.RegimeFiscalInfo>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("ENTREPRISE_ID").append(COMMA);
				b.append("DERNIERE_RAISON_SOCIALE").append(COMMA);
				b.append("FORME_JURIDIQUE").append(COMMA);
				b.append("DATE_CREATION").append(COMMA);
				b.append("REGIME_FISCAL_DATE").append(COMMA);
				b.append("REGIME_FISCAL_CODE").append(COMMA);
				b.append("REGIME_FISCAL_LIBELLE").append(COMMA);
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, RattrapageRegimesFiscauxJobResults.RegimeFiscalInfo elt) {
				final List<RegimeFiscalConsolide> regimes = elt.regimesFiscauxConsolides;
				b.append(elt.entrepriseId).append(COMMA);
				b.append(asCsvField(escapeChars(elt.derniereRaisonSociale))).append(COMMA);
				b.append(asCsvField(escapeChars(elt.formeLegale.getLibelle()))).append(COMMA);
				b.append(asCsvField(escapeChars(RegDateHelper.dateToDisplayString(elt.dateDeCreation)))).append(COMMA);
				RegimeFiscalConsolide regime;
				if (regimes.isEmpty()) {
					return false;
				}
				else {
					regime = regimes.get(0);
				}
				b.append(asCsvField(escapeChars(RegDateHelper.dateToDisplayString(regime.getDateDebut())))).append(COMMA);
				b.append(asCsvField(escapeChars(regime.getCode()))).append(COMMA);
				b.append(asCsvField(escapeChars(regime.getLibelle()))).append(COMMA);
				return true;
			}
		});
	}

	private TemporaryFile errorsAsCsvFile(List<RattrapageRegimesFiscauxJobResults.ExceptionInfo> exceptions, String fileName, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(exceptions, fileName, status, new CsvHelper.FileFiller<RattrapageRegimesFiscauxJobResults.ExceptionInfo>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("ENTREPRISE_ID").append(COMMA);
				b.append("EXCEPTION");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, RattrapageRegimesFiscauxJobResults.ExceptionInfo elt) {
				b.append(elt.entrepriseId).append(COMMA);
				b.append(asCsvField(escapeChars(elt.exceptionMsg)));
				return true;
			}
		});
	}
}

