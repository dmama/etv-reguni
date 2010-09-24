package ch.vd.uniregctb.rapport;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.metier.OuvertureForsResults;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import java.io.OutputStream;
import java.util.Date;

/**
 * Rapport PDF contenant les résultats de l'exécution d'un job d'ouverture des fors des habitants majeurs
 */
public class PdfMajoriteRapport extends PdfRapport {

    /**
     * Génère un rapport au format PDF à partir des résultats de job.
     */
    public void write(final OuvertureForsResults results, final String nom, final String description, final Date dateGeneration,
                          OutputStream os, StatusManager status) throws Exception {

        Assert.notNull(status);

        // Création du document PDF
        PdfMajoriteRapport document = new PdfMajoriteRapport();
        PdfWriter writer = PdfWriter.getInstance(document, os);
        document.open();
        document.addMetaInfo(nom, description);
        document.addEnteteUnireg();

        // Titre
        document.addTitrePrincipal("Rapport d'exécution du job d'ouverture des fors des habitants majeurs");

        // Paramètres
        document.addEntete1("Paramètres");
        {
            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
                }
            });
        }

        // Résultats
        document.addEntete1("Résultats");
        {
            if (results.interrompu) {
                document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
                        + "les valeurs ci-dessous sont donc incomplètes.");
            }

            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Nombre total d'habitants:", String.valueOf(results.nbHabitantsTotal));
                    table.addLigne("Nombre d'habitants traités:", String.valueOf(results.habitantTraites.size()));
                    table.addLigne("Nombre d'habitants en erreur:", String.valueOf(results.habitantEnErrors.size()));
	                table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
                    table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
                }
            });
        }

        // Habitants traités
        {
            String filename = "habitants_traites.csv";
            String contenu = asCsvFile(results.habitantTraites, filename, status);
            String titre = "Liste des habitants traités";
            String listVide = "(aucun habitant traité)";
            document.addListeDetaillee(writer, results.habitantTraites.size(), titre, listVide, filename, contenu);
        }

        // Habitants en erreurs
        {
            String filename = "habitants_en_erreur.csv";
            String contenu = asCsvFile(results.habitantEnErrors, filename, status);
            String titre = "Liste des habitants en erreur";
            String listVide = "(aucun habitant en erreur)";
            document.addListeDetaillee(writer, results.habitantEnErrors.size(), titre, listVide, filename, contenu);
        }

        document.close();

        status.setMessage("Génération du rapport terminée.");
    }}
