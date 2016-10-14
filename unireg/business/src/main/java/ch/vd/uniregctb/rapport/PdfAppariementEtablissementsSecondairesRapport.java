package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.tiers.rattrapage.appariement.AppariementEtablissementsSecondairesResults;

/**
 * Rapport PDF contenant les résultats du job d'appariement des établissements secondaires
 */
public class PdfAppariementEtablissementsSecondairesRapport extends PdfRapport {

	public void write(final AppariementEtablissementsSecondairesResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job d'appariement des établissements secondaires");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Nombre de threads :", String.valueOf(results.nbThreads));
				table.addLigne("Mode simulation :", String.valueOf(results.simulation));
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.wasInterrupted()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						+ "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, table -> {
				table.addLigne("Nombre d'entreprises à inspecter :", String.valueOf(results.idsEntreprises.size()));
				table.addLigne("Nombre d'appariements trouvés :", String.valueOf(results.getAppariements().size()));
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.getErreurs().size()));
				table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
				table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
			});
		}

		// Entreprises inspectées
		{
			final String filename = "entreprises_inspectees.csv";
			final String titre = "Liste des entreprises inspectées";
			final String listVide = "(aucune)";
			try (TemporaryFile contenu = inspectesAsCsvFile(results.idsEntreprises, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Appariements trouvés
		{
			final String filename = "appariements.csv";
			final String titre = "Liste des cas appariés";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = appariesAsCsvFile(results.getAppariements(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Cas en erreur
		{
			final String filename = "erreurs.csv";
			final String titre = " Liste des cas en erreur";
			final String listeVide = "(aucun)";
			try (TemporaryFile contenu = erreursAsCsvFile(results.getErreurs(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private TemporaryFile inspectesAsCsvFile(List<Long> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<Long>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("ID_ENTREPRISE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, Long elt) {
					b.append(elt);
					return true;
				}
			});
		}
		return contenu;
	}

	private TemporaryFile appariesAsCsvFile(List<AppariementEtablissementsSecondairesResults.AppariementEtablissement> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<AppariementEtablissementsSecondairesResults.AppariementEtablissement>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("ID_ENTREPRISE").append(COMMA);
					b.append("ID_ETABLISSEMENT").append(COMMA);
					b.append("ID_CANTONAL_SITE").append(COMMA);
					b.append("TYPE_AUTORITE_FISCALE_SIEGE").append(COMMA);
					b.append("OFS_SIEGE").append(COMMA);
					b.append("CAUSE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, AppariementEtablissementsSecondairesResults.AppariementEtablissement elt) {
					b.append(elt.idEntreprise).append(COMMA);
					b.append(elt.idEtablissement).append(COMMA);
					b.append(elt.idSite).append(COMMA);
					b.append(elt.tafSiege).append(COMMA);
					b.append(elt.ofsSiege).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.raison.getLibelle()));
					return true;
				}
			});
		}
		return contenu;
	}

	private TemporaryFile erreursAsCsvFile(List<AppariementEtablissementsSecondairesResults.AppariementErreur> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<AppariementEtablissementsSecondairesResults.AppariementErreur>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("ID_ENTREPRISE").append(COMMA);
					b.append("DETAILS");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, AppariementEtablissementsSecondairesResults.AppariementErreur elt) {
					b.append(elt.idEntreprise).append(COMMA);
					b.append(CsvHelper.asCsvField(elt.stack));
					return true;
				}
			});
		}
		return contenu;
	}
}