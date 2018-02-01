package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.AutoCloseableContainer;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.role.before2016.InfoCommune;
import ch.vd.unireg.role.before2016.InfoContribuable;
import ch.vd.unireg.role.before2016.ProduireRolesCommunesResults;


/**
 * Rapport PDF contenant les résultats de l'exécution du job de production des rôles pour les communes
 */
public abstract class PdfRolesCommunesRapport<T extends ProduireRolesCommunesResults<T>, ICTB extends InfoContribuable<ICTB>, ICOM extends InfoCommune<ICTB, ICOM>> extends PdfRolesRapport<T> {

	public PdfRolesCommunesRapport(ServiceInfrastructureService infraService) {
		super(infraService);
	}

	public void write(final T results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {
		Assert.notNull(status);

		status.setMessage("Génération du rapport...");

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		final String titrePrincipal;
		if (results.noOfsCommune != null) {
		    final Commune commune = getCommune(results.noOfsCommune, RegDate.get(results.annee, 12, 31));
		    titrePrincipal = String.format("Rapport des rôles pour la commune de %s", commune.getNomOfficiel());
		}
		else {
			titrePrincipal = "Rapport des rôles pour toutes les communes vaudoises";
		}
		addTitrePrincipal(titrePrincipal);

		// Paramètres
		addEntete1("Paramètres");
		{
		    addTableSimple(2, table -> {
		        table.addLigne("Année fiscale :", String.valueOf(results.annee));
			    table.addLigne("Type de rôles :", results.getTypeRoles().name());
		        table.addLigne("Nombre de threads :", String.valueOf(results.nbThreads));
		        table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.dateTraitement));
		    });
		}

		// Résultats
		addEntete1("Résumé général");
		{
			if (results.interrompu) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\nles valeurs ci-dessous sont donc incomplètes.");
		    }

			final int nbCommunesTraitees;
			if (results.noOfsCommune != null) {
			    nbCommunesTraitees = 1; // par définition
			}
			else {
			    nbCommunesTraitees = results.getNoOfsCommunesTraitees().size();
			}

		    addTableSimple(2, table -> {
		        table.addLigne("Nombre de communes traitées:", String.valueOf(nbCommunesTraitees));
		        table.addLigne("Nombre de contribuables traités:", String.valueOf(results.ctbsTraites));
		        table.addLigne("Nombre de contribuables ignorés:", String.valueOf(results.ctbsIgnores.size()));
		        table.addLigne("Nombre de contribuables en erreur:", String.valueOf(results.ctbsEnErrors.size()));
			    table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
		        table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
		    });
		}

		// Détails des contribuables en erreur ou ignorés
		if (!results.ctbsEnErrors.isEmpty()) {
		    final String filename = "contribuables_en_erreur.csv";
		    final String titre = "Liste des contribuables en erreur";
			final String listVide = "(aucun contribuable en erreur)";
			try (TemporaryFile contenu = asCsvFile(results.ctbsEnErrors, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		if (!results.ctbsIgnores.isEmpty()) {
		    final String filename = "contribuables_ignores.csv";
		    final String titre = "Liste des contribuables ignorés";
			final String listVide = "(aucun contribuable ignoré)";
			try (TemporaryFile contenu = asCsvFile(results.ctbsIgnores, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		writeCommuneParCommune(results, dateGeneration, status, writer);

		close();
		status.setMessage("Génération du rapport terminée.");
	}

	/**
	 * Extraction des données des communes depuis le résultats
	 * @param results le résultat de l'extraction
	 * @return une map des données des communes indexée par le numéro OFS de la commune
	 */
	protected abstract Map<Integer, ICOM> getInfosCommunes(T results);

	/**
	 * Boucle sur les communes et génération du rapport page par page
	 */
	private void writeCommuneParCommune(final T results, final Date dateGeneration, StatusManager status, PdfWriter writer) throws ServiceInfrastructureException, DocumentException {
		final Map<Integer, ICOM> infosCommunes = getInfosCommunes(results);
		final List<Commune> communes = getListeCommunes(infosCommunes.keySet(), results.annee, true);
		final Map<Integer, String> nomsCommunes = buildNomsCommunes(communes);

		// Détail commune par commune
		for (final Commune commune : communes) {

		    if (results.noOfsCommune != null) {
		        if (commune.getNoOFS() != results.noOfsCommune) {
		            /*
		             * On ignore toutes les autres communes lorsqu'un rapport a été demandé spécifiquement pour une commune (il est possible
		             * et normal d'avoir des informations pour d'autres communes en raison des contribuables qui ont déménagé durant
		             * l'année, et donc qui produisent des informations sur deux communes).
		             */
		            continue;
		        }
		    }

		    final ICOM infoCommune = infosCommunes.get(commune.getNoOFS());
		    if (infoCommune == null) {
		        Audit.error("Rôle des communes: Impossible de trouver les informations pour la commune " + commune.getNomOfficiel()
		                + "(n°ofs " + commune.getNoOFS() + ')');
		        continue;
		    }

			newPage();

		    // Entête de la commune
		    final String nomCommune = commune.getNomOfficiel();
		    final int totalContribuables = infoCommune.getInfosContribuables().size();
		    addTitrePrincipal("Liste des rôles " + results.annee + " pour la commune de\n" + nomCommune);

		    if (results.interrompu) {
		        addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
		                + "les valeurs ci-dessous sont donc incomplètes.");
		    }

		    // Résumé de la commune
		    addEntete1("Résumé");
		    {
		        final Map<InfoContribuable.TypeContribuable, Integer> nombreParType = extractNombreParType(infoCommune.getInfosContribuables());
		        addTableSimple(2, table -> {
		            table.setWidths(new float[]{2.0f, 1.0f});
		            table.addLigne("Nombre total de contribuables traités:", String.valueOf(totalContribuables));
			        addLignesStatsParTypeCtb(table, nombreParType);
			        table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
		            table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
		        });
		    }

		    // Fichier CVS détaillé
		    {
			    try (AutoCloseableContainer<TemporaryFile> container = new AutoCloseableContainer<>(asCsvFiles(nomsCommunes, infoCommune, status))) {
				    writeFichierDetail(results, writer, container.getElements(), totalContribuables == 0, Integer.toString(commune.getNoOFS()));
			    }
		    }
		}
	}

	/**
	 * Utilisé par le traitement commune par commune
	 */
	protected abstract TemporaryFile[] asCsvFiles(final Map<Integer, String> nomsCommunes, ICOM infoCommune, StatusManager status);
}
