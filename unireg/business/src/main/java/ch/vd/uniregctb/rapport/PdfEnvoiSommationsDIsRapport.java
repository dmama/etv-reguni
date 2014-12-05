package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CsvHelper;
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
        addTableSimple(new float[] {70, 30}, new PdfRapport.TableSimpleCallback() {
            @Override
            public void fillTable(PdfTableSimple table) throws DocumentException {
                table.addLigne("Date de traitement: ", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
                table.addLigne("Mise sous pli impossible: ", Boolean.toString(results.isMiseSousPliImpossible()));
	            if (results.getNombreMaxSommations() > 0) {
		            table.addLigne("Nombre maximal de sommations à émettre: ", Integer.toString(results.getNombreMaxSommations()));
	            }
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
                    table.addLigne("Nombre de DI non sommées pour cause de délai effectif non-échu :", String.valueOf(results.getTotalDelaisEffectifsNonEchus()));
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
	        byte[] contenu = asCsvFileSommationDI(results.getListeSommationsEnErreur(), filename, status);
            String titre = "Liste des déclarations impossibles à sommer";
            String listVide = "(aucune déclaration à sommer en erreur)";
            addListeDetaillee(writer, titre, listVide, filename, contenu);
        }

        // DI avec délai effectif non échu
        {
            String filename = "delai_effectif_non_echu.csv";
	        byte[] contenu = asCsvFileSommationDI(results.getListeDisDelaiEffectifNonEchu(), filename, status);
            String titre = "Liste des déclarations dont le délai effectif n'est pas échu";
            String listVide = "(aucune)";
            addListeDetaillee(writer, titre, listVide, filename, contenu);
        }

        // DI avec contribuables non assujettis.
        {
            String filename = "non_assujettissement.csv";
	        byte[] contenu = asCsvFileSommationDI(results.getListeNonAssujettissement(), filename, status);
            String titre = "Liste des déclarations dont les contribuables ne sont pas assujettis";
            String listVide = "(aucune déclaration n'est liée à un contribuable non assujetti)";
            addListeDetaillee(writer, titre, listVide, filename, contenu);
        }

        // DI avec contribuables indigents.
        {
            String filename = "indigents.csv";
	        byte[] contenu = asCsvFileSommationDI(results.getListeIndigent(), filename, status);
            String titre = "Liste des déclarations dont les contribuables sont indigents";
            String listVide = "(aucune déclaration n'est liée à un contribuable indigent)";
            addListeDetaillee(writer, titre, listVide, filename, contenu);
        }

	     // DI avec contribuables sourcier purs.
        {
            String filename = "sourciersPurs.csv";
	        byte[] contenu = asCsvFileSommationDI(results.getListeSourcierPur(), filename, status);
            String titre = "Liste des déclarations dont les contribuables sont sourciers";
            String listVide = "(aucune déclaration n'est liée à un contribuable sourcier)";
            addListeDetaillee(writer, titre, listVide, filename, contenu);
        }

        // DI optionnelles
        {
            String filename = "optionnelles.csv";
	        byte[] contenu = asCsvFileSommationDI(results.getDisOptionnelles(), filename, status);
            String titre = "Liste des déclarations non-sommées car optionnelles";
            String listVide = "(aucune déclaration sommable n'est optionnelle)";
            addListeDetaillee(writer, titre, listVide, filename, contenu);
        }

        // DI sommées.
        {
            String filename = "sommations.csv";
	        byte[] contenu = asCsvFileSommationDI(results.getSommations(), filename, status);
            String titre = "Liste des déclarations sommées";
            String listVide = "(aucune déclaration sommée)";
            addListeDetaillee(writer, titre, listVide, filename, contenu);
        }

        close();
        status.setMessage("Génération du rapport terminée.");
    }

	@SuppressWarnings({"unchecked"})
	private byte[] asCsvFileSommationDI(final List<? extends EnvoiSommationsDIsResults.Info> list, String filename, StatusManager status) {
		final byte[] content;
		if (!list.isEmpty()) {
			content = CsvHelper.asCsvFile((List<EnvoiSommationsDIsResults.Info>) list, filename,  status, new CsvHelper.FileFiller<EnvoiSommationsDIsResults.Info>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append(list.get(0).getCsvEntete());
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, EnvoiSommationsDIsResults.Info elt) {
					final String csv = elt.getCsv();
					if (StringUtils.isNotBlank(csv)) {
						b.append(elt.getCsv());
						return true;
					}
					return false;
				}
			});
		}
		else {
			content = null;
		}
		return content;
	}
}

