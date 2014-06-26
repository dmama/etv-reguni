package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.metier.OuvertureForsResults;

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
                @Override
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
                @Override
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
            final String filename = "habitants_traites.csv";
            final String contenu = CsvHelper.asCsvFile(results.habitantTraites, filename, status, new CsvHelper.FileFiller<OuvertureForsResults.Traite>() {
	            @Override
	            public void fillHeader(CsvHelper.LineFiller b) {
		            b.append("OID").append(COMMA);
		            b.append("NO_CTB").append(COMMA);
		            b.append("NOM").append(COMMA);
		            b.append("DATE_OUVERTURE").append(COMMA);
		            b.append("RAISON").append(COMMA);
		            b.append("COMMENTAIRE");
	            }

	            @Override
	            public boolean fillLine(CsvHelper.LineFiller b, OuvertureForsResults.Traite elt) {
		            b.append(elt.officeImpotID != null ? elt.officeImpotID.toString() : EMPTY).append(COMMA);
		            b.append(elt.noCtb).append(COMMA);
		            b.append(escapeChars(elt.nomCtb)).append(COMMA);
		            b.append(elt.dateOuverture).append(COMMA);
		            b.append(escapeChars(elt.getDescriptionRaison()));
		            if (elt.details != null) {
			            b.append(COMMA).append(asCsvField(elt.details));
		            }
		            return true;
	            }
            });
            final String titre = "Liste des habitants traités";
            final String listVide = "(aucun habitant traité)";
            document.addListeDetaillee(writer, titre, listVide, filename, contenu);
        }

        // Habitants en erreurs
        {
            final String filename = "habitants_en_erreur.csv";
            final String contenu = asCsvFile(results.habitantEnErrors, filename, status);
            final String titre = "Liste des habitants en erreur";
            final String listVide = "(aucun habitant en erreur)";
            document.addListeDetaillee(writer, titre, listVide, filename, contenu);
        }

        document.close();

        status.setMessage("Génération du rapport terminée.");
    }}
