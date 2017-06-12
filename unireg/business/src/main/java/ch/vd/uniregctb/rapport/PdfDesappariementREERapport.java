package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.rcent.unireg.unpairingree.OrganisationLocation;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.tiers.rattrapage.appariement.sifisc24852.DesappariementREEResults;

/**
 * Rapport PDF contenant les résultats du désappariement des établissements REE chargés à tort
 */
public class PdfDesappariementREERapport extends PdfRapport {

	public void write(final DesappariementREEResults results, final String nom, final String description, final Date dateGeneration,
	                  OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		final boolean simulation = results.isSimulation();

		// Titre
		addTitrePrincipal(String.format("Rapport de désappariement des établissements REE chargés à tort [SIFISC-24852] %s", simulation ? " (Simulation)" : ""));

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
			if (results.isInterrompu()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						+ "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, new TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre total d'établissements à désapparier :", String.valueOf(results.getEtablissementsADesapparier().size()));
					table.addLigne("Nombre total d'actions menées :", String.valueOf(results.getDesappariements().size()));
					table.addLigne("Nombre total d'établissements désappariés :", String.valueOf(results.getDesappariementsOk().size()));
					table.addLigne("Nombre total d'établissements désappariés mais à vérifier:", String.valueOf(results.getDesappariementsAVerifier().size()));
					table.addLigne("Nombre total d'établissements dont le désappariement a échoué :", String.valueOf(results.getDesappariementsEnEchec().size()));
					table.addLigne("Nombre total d'erreurs :", String.valueOf(results.getExceptions().size()));
					table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
					table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
				}
			});
		}

		// Action menées
		{
			final String filename = "actions_menees.csv";
			final String titre = "Liste des actions menées par le job (ok, à vérifier, echec)";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = actionsAsCsvFile(results.getDesappariements(), filename, status)) {
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

	private TemporaryFile actionsAsCsvFile(List<DesappariementREEResults.Desappariement> actions, String fileName, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(actions, fileName, status, new CsvHelper.FileFiller<DesappariementREEResults.Desappariement>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CANTONAL_ENTREPRISE").append(COMMA);
				b.append("NO_TIERS_ENTREPRISE").append(COMMA);
				b.append("NO_CANTONAL_ETABLISSEMENT").append(COMMA);
				b.append("NO_TIERS_ETABLISSEMENT").append(COMMA);
				b.append("TYPE_ETABLISSEMENT").append(COMMA);
				b.append("JARO-WINKLER_RAISONS_SOCIALES").append(COMMA);
				b.append("RAISON_SOCIALE_FISCALE").append(COMMA);
				b.append("RAISON_SOCIALE_CIVILE").append(COMMA);
				b.append("FORME_JURIDIQUE_FISCALE").append(COMMA);
				b.append("FORME_JURIDIQUE_CIVILE").append(COMMA);
				b.append("NO_OFS_COMMUNE_FISCALE").append(COMMA);
				b.append("NO_OFS_COMMUNE_CIVILE").append(COMMA);
				b.append("ADRESSE_FISCALE").append(COMMA);
				b.append("ADRESSE_CIVILE").append(COMMA);

				b.append("RESULTAT").append(COMMA);
				b.append("MESSAGE");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, DesappariementREEResults.Desappariement elt) {
				final OrganisationLocation locationREnt = elt.getEtablissementRCEnt();
				b.append(elt.getNoCantonalEntreprise() == null ? "" : elt.getNoCantonalEntreprise()).append(COMMA);
				b.append(elt.getNoEntreprise() == null ? "" : elt.getNoEntreprise()).append(COMMA);
				b.append(locationREnt.getCantonalId()).append(COMMA);
				b.append(elt.getNoEtablissement() == null ? "" : elt.getNoEtablissement()).append(COMMA);
				b.append(asCsvField(escapeChars(locationREnt.isMain() ? "principal" : "secondaire"))).append(COMMA);
				b.append(elt.getRaisonsSocialesJaroWinker()).append(COMMA);
				b.append(asCsvField(escapeChars(elt.getRaisonSociale()))).append(COMMA);
				b.append(asCsvField(escapeChars(locationREnt.getName()))).append(COMMA);
				b.append(asCsvField(escapeChars(elt.getFormeJuridique()))).append(COMMA);
				b.append(asCsvField(escapeChars(locationREnt.getLegalForm()))).append(COMMA);
				b.append(elt.getNoOFSCommune() == null ? "" : elt.getNoOFSCommune()).append(COMMA);
				b.append(locationREnt.getMunicipality()).append(COMMA);
				b.append(elt.getAdresse() == null ? "" : asCsvField(escapeChars(elt.getAdresse().toString()))).append(COMMA);
				b.append(asCsvField(escapeChars(locationREnt.getAddress().toString()))).append(COMMA); // FIXME: toString() amélioré?

				b.append(asCsvField(elt.getTypeResultat() == null ? "" : escapeChars(elt.getTypeResultat().name()))).append(COMMA);
				b.append(asCsvField(escapeChars(elt.getMessage())));
				return true;
			}
		});
	}

	private TemporaryFile errorsAsCsvFile(List<DesappariementREEResults.ExceptionInfo> exceptions, String fileName, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(exceptions, fileName, status, new CsvHelper.FileFiller<DesappariementREEResults.ExceptionInfo>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CANTONAL_ETABLISSEMENT").append(COMMA);
				b.append("TYPE_ETABLISSEMENT").append(COMMA);
				b.append("RAISON_SOCIALE_CIVILE").append(COMMA);
				b.append("FORME_JURIDIQUE_CIVILE").append(COMMA);
				b.append("NO_OFS_COMMUNE_CIVILE").append(COMMA);
				b.append("ADRESSE_CIVILE").append(COMMA);

				b.append("EXCEPTION");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, DesappariementREEResults.ExceptionInfo elt) {
				final OrganisationLocation locationREnt = elt.etablissementRCEnt;
				b.append(locationREnt.getCantonalId()).append(COMMA);
				b.append(asCsvField(escapeChars(locationREnt.isMain() ? "principal" : "secondaire"))).append(COMMA);
				b.append(asCsvField(escapeChars(locationREnt.getName()))).append(COMMA);
				b.append(asCsvField(escapeChars(locationREnt.getLegalForm()))).append(COMMA);
				b.append(locationREnt.getMunicipality()).append(COMMA);
				b.append(asCsvField(escapeChars(locationREnt.getAddress().toString()))).append(COMMA); // FIXME: toString() amélioré?

				b.append(asCsvField(escapeChars(elt.exceptionMsg)));
				return true;
			}
		});
	}
}

