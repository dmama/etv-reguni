package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.ordinaire.EnvoiSommationsDIsResults;


/**
 * Rapport PDF concernant l'execution du batch des sommations DI 
 */
public class PdfEnvoiSommationsDIsRapport extends PdfRapport {


    public void write(final EnvoiSommationsDIsResults results, final String nom, final String description, final Date dateGeneration,
                          OutputStream os, StatusManager status) throws Exception {

        Assert.notNull(status);

        // Création du document PDF
        PdfWriter writer = PdfWriter.getInstance(this, os);
        open();
        addMetaInfo(nom, description);
        addEnteteUnireg();

        // Titre
        addTitrePrincipal("Sommation des déclarations d'impôt");

        // Paramètres
        addEntete1("Paramètres");
        addTableSimple(2, new PdfRapport.TableSimpleCallback() {
            @Override
            public void fillTable(PdfTableSimple table) throws DocumentException {
                table.addLigne("Date de traitement: ", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
                table.addLigne("Mise sous pli impossible: ", Boolean.toString(results.isMiseSousPliImpossible()));
            }
        });
        // Résultats
        addEntete1("Résultats");
        {
            if (results.isInterrompu()) {
                addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
                        + "les valeurs ci-dessous sont donc incomplètes.");
            }

            addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                @Override
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Nombre total de DI sommées:", String.valueOf(results.getTotalDisSommees()));
                    for (Integer annee : results.getListeAnnees()) {
                        table.addLigne(String.format("Période %s :", annee), String.valueOf(results.getTotalSommations(annee)));
                    }
                    table.addLigne("Nombre de DI non sommées pour cause de non assujettisement :", String.valueOf(results.getTotalNonAssujettissement()));
                    table.addLigne("Nombre de DI non sommées pour cause de contribuable indigent :", String.valueOf(results.getTotalIndigent()));
	                table.addLigne("Nombre de DI non sommées pour cause de contribuable sourcier Pur :", String.valueOf(results.getTotalSourcierPur()));
                    table.addLigne("Nombre de DI non sommées pour cause d'optionnalité :", String.valueOf(results.getTotalDisOptionnelles()));
                    table.addLigne("Nombre de sommations en erreur :", String.valueOf(results.getTotalSommationsEnErreur()));
	                table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
                    table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
                }
            });
        }

        // Sommations DI en erreurs
        {
            String filename = "sommations_DI_en_erreur.csv";
            String contenu = asCsvFileSommationDI(results.getListeSommationsEnErreur(), filename, status);
            String titre = "Liste des déclarations impossibles à sommer";
            String listVide = "(aucune déclaration à sommer en erreur)";
            addListeDetaillee(writer, results.getTotalSommationsEnErreur(), titre, listVide, filename, contenu);
        }

        // DI avec contribuables non assujettis.
        {
            String filename = "non_assujettissement.csv";
            String contenu = asCsvFileSommationDI(results.getListeNonAssujettissement(), filename, status);
            String titre = "Liste des déclarations dont les contribuables ne sont pas assujettis";
            String listVide = "(aucune déclaration n'est liée à un contribuable non assujetti)";
            addListeDetaillee(writer, results.getTotalNonAssujettissement(), titre, listVide, filename, contenu);
        }

        // DI avec contribuables indigents.
        {
            String filename = "indigents.csv";
            String contenu = asCsvFileSommationDI(results.getListeIndigent(), filename, status);
            String titre = "Liste des déclarations dont les contribuables sont indigents";
            String listVide = "(aucune déclaration n'est liée à un contribuable indigent)";
            addListeDetaillee(writer, results.getTotalIndigent(), titre, listVide, filename, contenu);
        }

	     // DI avec contribuables sourcier purs.
        {
            String filename = "sourciersPurs.csv";
            String contenu = asCsvFileSommationDI(results.getListeSourcierPur(), filename, status);
            String titre = "Liste des déclarations dont les contribuables sont sourciers";
            String listVide = "(aucune déclaration n'est liée à un contribuable sourcier)";
            addListeDetaillee(writer, results.getTotalSourcierPur(), titre, listVide, filename, contenu);
        }

        // DI optionnelles
        {
            String filename = "optionnelles.csv";
            String contenu = asCsvFileSommationDI(results.getDisOptionnelles(), filename, status);
            String titre = "Liste des déclarations non-sommées car optionnelles";
            String listVide = "(aucune déclaration sommable n'est optionnelle)";
            addListeDetaillee(writer, results.getTotalDisOptionnelles(), titre, listVide, filename, contenu);
        }

        // DI sommées.
        {
            String filename = "sommations.csv";
            String contenu = asCsvFileSommationDI(results.getSommations(), filename, status);
            String titre = "Liste des déclarations sommées";
            String listVide = "(aucune déclaration sommée)";
            addListeDetaillee(writer, results.getTotalDisSommees(), titre, listVide, filename, contenu);
        }

        close();
        status.setMessage("Génération du rapport terminée.");
    }

	@SuppressWarnings({"unchecked"})
	private String asCsvFileSommationDI(final List<? extends EnvoiSommationsDIsResults.Info> list, String filename, StatusManager status) {
		final String content;
		if (list.size() > 0) {
			content = CsvHelper.asCsvFile((List<EnvoiSommationsDIsResults.Info>) list, filename,  status, AVG_LINE_LEN, new CsvHelper.Filler<EnvoiSommationsDIsResults.Info>() {
				@Override
				public void fillHeader(StringBuilder b) {
					b.append(list.get(0).getCsvEntete());
				}

				@Override
				public void fillLine(StringBuilder b, EnvoiSommationsDIsResults.Info elt) {
					b.append(elt.getCsv());
				}
			});
		}
		else {
			content = null;
		}
		return content;
	}
}

