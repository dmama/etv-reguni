package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.declaration.ordinaire.pm.EnvoiDIsPMResults;

/**
 * Rapport PDF contenant les résultats de l'exécution d'un job de traitement des DIs PM.
 */
public class PdfEnvoiDIsPMRapport extends PdfRapport {

    /**
     * Génère un rapport au format PDF à partir des résultats de job.
     */
    public void write(final EnvoiDIsPMResults results, final String nom, final String description, final Date dateGeneration,
                          OutputStream os, StatusManager status) throws Exception {

        Assert.notNull(status);

        // Création du document PDF
        PdfWriter writer = PdfWriter.getInstance(this, os);
        open();
        addMetaInfo(nom, description);
        addEnteteUnireg();

        // Titre
        addTitrePrincipal("Rapport d'exécution du job d'envoi des DIs PM en masse pour la période fiscale " + results.getPeriodeFiscale());

        // Paramètres
        addEntete1("Paramètres");
        {
            addTableSimple(2, table -> {
                table.addLigne("Période fiscale considérée :", String.valueOf(results.getPeriodeFiscale()));
                table.addLigne("Date limite de bouclement :", RegDateHelper.dateToDisplayString(results.getDateLimiteBouclements()));
                table.addLigne("Type d'envoi :", results.getCategorieEnvoi().name());
                table.addLigne("Nombre maximum d'envois :", results.getNbMaxEnvois() == null ? "-" : String.valueOf(results.getNbMaxEnvois()));
	            table.addLigne("Nombre de threads :", String.valueOf(results.getNbThreads()));
                table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
            });
        }

        // Résultats
        addEntete1("Résultats");
        {
            if (results.interrompu) {
                addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
                        + "les valeurs ci-dessous sont donc incomplètes.");
            }

            addTableSimple(2, table -> {
                table.addLigne("Nombre total de contribuables inspectés :", String.valueOf(results.getNbContribuablesVus()));
                table.addLigne("Nombre de périodes d'imposition traitées :", String.valueOf(results.getEnvoyees().size()));
                table.addLigne("Nombre de périodes d'imposition ignorées :", String.valueOf(results.getIgnorees().size()));
                table.addLigne("Nombre de contribuables en erreur :", String.valueOf(results.getErreurs().size()));
	            table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
                table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
            });
        }

        // CTBs traités
        {
            String filename = "contribuables_traites.csv";
            String titre = "Liste des contribuables traités";
	        String listVide = "(aucun contribuable traité)";
	        try (TemporaryFile contenu = asCsvFileTraites(results.getEnvoyees(), filename, status)) {
		        addListeDetaillee(writer, titre, listVide, filename, contenu);
	        }
        }

        // CTBs ignorés
        {
            String filename = "contribuables_ignores.csv";
            String titre = "Liste des contribuables ignorés";
	        String listVide = "(aucun contribuable ignoré)";
	        try (TemporaryFile contenu = asCsvFileIgnores(results.getIgnorees(), filename, status)) {
		        addListeDetaillee(writer, titre, listVide, filename, contenu);
	        }
        }

        // CTBs en erreurs
        {
            String filename = "contribuables_en_erreur.csv";
            String titre = "Liste des contribuables en erreur";
	        String listVide = "(aucun contribuable en erreur)";
	        try (TemporaryFile contenu = asCsvFileErreurs(results.getErreurs(), filename, status)) {
		        addListeDetaillee(writer, titre, listVide, filename, contenu);
	        }
        }

        close();

        status.setMessage("Génération du rapport terminée.");
    }

    private static TemporaryFile asCsvFileTraites(List<EnvoiDIsPMResults.DiEnvoyee> list, String filename, StatusManager status) {
        return CsvHelper.asCsvTemporaryFile(list, filename, status, new CsvHelper.FileFiller<EnvoiDIsPMResults.DiEnvoyee>() {
            @Override
            public void fillHeader(CsvHelper.LineFiller b) {
                b.append("NO_CTB").append(COMMA);
                b.append("DATE_DEBUT").append(COMMA);
                b.append("DATE_FIN");
            }

            @Override
            public boolean fillLine(CsvHelper.LineFiller b, EnvoiDIsPMResults.DiEnvoyee elt) {
                b.append(elt.getNoCtb()).append(COMMA);
                b.append(RegDateHelper.dateToDashString(elt.getDateDebut())).append(COMMA);
                b.append(RegDateHelper.dateToDashString(elt.getDateFin()));
                return true;
            }
        });
    }

    private static TemporaryFile asCsvFileIgnores(List<EnvoiDIsPMResults.TacheIgnoree> list, String filename, StatusManager status) {
        return CsvHelper.asCsvTemporaryFile(list, filename, status, new CsvHelper.FileFiller<EnvoiDIsPMResults.TacheIgnoree>() {
            @Override
            public void fillHeader(CsvHelper.LineFiller b) {
                b.append("NO_CTB").append(COMMA);
                b.append("DATE_DEBUT").append(COMMA);
                b.append("DATE_FIN").append(COMMA);
                b.append("RAISON");
            }

            @Override
            public boolean fillLine(CsvHelper.LineFiller b, EnvoiDIsPMResults.TacheIgnoree elt) {
                b.append(elt.getNoCtb()).append(COMMA);
                b.append(RegDateHelper.dateToDashString(elt.getDateDebut())).append(COMMA);
                b.append(RegDateHelper.dateToDashString(elt.getDateFin())).append(COMMA);
                b.append(CsvHelper.escapeChars(elt.getType().getDescription()));
                return true;
            }
        });
    }

    private static TemporaryFile asCsvFileErreurs(List<EnvoiDIsPMResults.Erreur> list, String filename, StatusManager status) {
        return CsvHelper.asCsvTemporaryFile(list, filename, status, new CsvHelper.FileFiller<EnvoiDIsPMResults.Erreur>() {
            @Override
            public void fillHeader(CsvHelper.LineFiller b) {
                b.append("NO_CTB").append(COMMA);
                b.append("ERREUR").append(COMMA);
                b.append("DETAILS");
            }

            @Override
            public boolean fillLine(CsvHelper.LineFiller b, EnvoiDIsPMResults.Erreur elt) {
                b.append(elt.getNoCtb()).append(COMMA);
                b.append(CsvHelper.escapeChars(elt.getType().getDescription())).append(COMMA);
                b.append(CsvHelper.escapeChars(elt.getDetail()));
                return true;
            }
        });
    }
}