package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.declaration.ordinaire.EnvoiDIsResults;

/**
 * Rapport PDF contenant les résultats de l'exécution d'un job de traitement des DIs.
 */
public class PdfEnvoiDIsRapport extends PdfRapport {

    /**
     * Génère un rapport au format PDF à partir des résultats de job.
     */
    public void write(final EnvoiDIsResults results, final String nom, final String description, final Date dateGeneration,
                          OutputStream os, StatusManager status) throws Exception {

        Assert.notNull(status);

        // Création du document PDF
        PdfWriter writer = PdfWriter.getInstance(this, os);
        open();
        addMetaInfo(nom, description);
        addEnteteUnireg();

        // Titre
        addTitrePrincipal("Rapport d'exécution du job d'envoi des DIs en masse pour l'année " + results.annee);

        // Paramètres
        addEntete1("Paramètres");
        {
            addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                @Override
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Période fiscale considérée :", String.valueOf(results.annee));
                    table.addLigne("Catégorie de contribuables :", results.categorie.getDescription());
                    table.addLigne("Nombre maximum d'envois :", String.valueOf(results.nbMax));
	                table.addLigne("Nombre de threads :", String.valueOf(results.nbThreads));
	                table.addLigne("Numéro de contribuable minimal :", results.noCtbMin == null ? "-" : FormatNumeroHelper.numeroCTBToDisplay(results.noCtbMin));
	                table.addLigne("Numéro de contribuable maximal :", results.noCtbMax == null ? "-" : FormatNumeroHelper.numeroCTBToDisplay(results.noCtbMax));
	                if(results.dateExclureDecede!=null){
		                 table.addLigne("Date de debut d'exclusion des contribuables décédés :",  RegDateHelper.dateToDisplayString(results.dateExclureDecede));
	                }
                    table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.dateTraitement));
                }
            });
        }

        // Résultats
        addEntete1("Résultats");
        {
            if (results.interrompu) {
                addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
                        + "les valeurs ci-dessous sont donc incomplètes.");
            }

            addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                @Override
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Nombre total de contribuables:", String.valueOf(results.nbCtbsTotal));
                    table.addLigne("Nombre de périodes d'imposition traitées:", String.valueOf(results.ctbsAvecDiGeneree.size()));
                    table.addLigne("Nombre d'indigents traités:", String.valueOf(results.ctbsIndigents.size()));
                    table.addLigne("Nombre de contribuables ignorés:", String.valueOf(results.ctbsIgnores.size()));
                    table.addLigne("Nombre de contribuables en erreur:", String.valueOf(results.ctbsEnErrors.size()));
	                table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
                    table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
                }
            });
        }

        // CTBs traités
        {
            String filename = "contribuables_traites.csv";
	        byte[] contenu = ctbIdsAsCsvFile(results.ctbsAvecDiGeneree, filename, status);
            String titre = "Liste des contribuables traités";
            String listVide = "(aucun contribuable traité)";
            addListeDetaillee(writer, titre, listVide, filename, contenu);
        }

        // CTBs indigents
        {
            String filename = "contribuables_indigents.csv";
	        byte[] contenu = ctbIdsAsCsvFile(results.ctbsIndigents, filename, status);
            String titre = "Liste des contribuables indigents";
            String listVide = "(aucun contribuable indigent)";
            addListeDetaillee(writer, titre, listVide, filename, contenu);
        }

        // CTBs ignorés
        {
            String filename = "contribuables_ignores.csv";
	        byte[] contenu = asCsvFile(results.ctbsIgnores, filename, status);
            String titre = "Liste des contribuables ignorés";
            String listVide = "(aucun contribuable ignoré)";
            addListeDetaillee(writer, titre, listVide, filename, contenu);
        }

        // CTBs en erreurs
        {
            String filename = "contribuables_en_erreur.csv";
	        byte[] contenu = asCsvFile(results.ctbsEnErrors, filename, status);
            String titre = "Liste des contribuables en erreur";
            String listVide = "(aucun contribuable en erreur)";
            addListeDetaillee(writer, titre, listVide, filename, contenu);
        }

        close();

        status.setMessage("Génération du rapport terminée.");
    }
}