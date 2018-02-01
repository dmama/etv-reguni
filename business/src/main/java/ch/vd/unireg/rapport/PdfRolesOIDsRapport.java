package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.common.AutoCloseableContainer;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.role.before2016.InfoContribuable;
import ch.vd.unireg.role.before2016.InfoContribuablePP;
import ch.vd.unireg.role.before2016.ProduireRolesOIDsResults;

/**
 * Rapport PDF contenant les résultats de l'exécution du job de production des rôles pour les OID
 */
public class PdfRolesOIDsRapport extends PdfRolesRapport<ProduireRolesOIDsResults> {

	public PdfRolesOIDsRapport(ServiceInfrastructureService infraService) {
		super(infraService);
	}

	public void write(final ProduireRolesOIDsResults[] results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		status.setMessage("Génération du rapport...");

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);

		if (results.length == 1) {
			writePageOid(results[0], dateGeneration, status, writer, null);
		}
		else {

			addEnteteUnireg();
			addTitrePrincipal("Rapport des rôles pour tous les OID vaudois");

			// Paramètres
			addEntete1("Paramètres");
			{
			    addTableSimple(2, table -> {
			        table.addLigne("Année fiscale :", String.valueOf(results[0].annee));
			        table.addLigne("Nombre de threads :", String.valueOf(results[0].nbThreads));
			        table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results[0].dateTraitement));
			    });
			}

			// Résultats
			addEntete1("Résumé général");
			{
				if (status.isInterrupted()) {
					addWarning("Attention ! Le job a été interrompu par l'utilisateur,\nles valeurs ci-dessous sont donc incomplètes.");
			    }

				final int nbOidTraites = results.length;

			    addTableSimple(2, table -> {

				    long dureeTotale = 0;
				    for (ProduireRolesOIDsResults part : results) {
					    dureeTotale += getDureeExecution(part);
				    }

		            table.addLigne("Nombre d'offices traités:", String.valueOf(nbOidTraites));
				    table.addLigne("Durée d'exécution du job:", formatDureeExecution(dureeTotale));
			        table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
			    });
			}

			writeTousOid(results, dateGeneration, status, writer);
		}

		close();
		status.setMessage("Génération du rapport terminée.");
	}

	private void writeTousOid(ProduireRolesOIDsResults[] results, Date dateGeneration, StatusManager status, PdfWriter writer) throws ServiceInfrastructureException, DocumentException {

		// une nouvelle page à chaque OID
		for (ProduireRolesOIDsResults res : results) {
			newPage();

			final OfficeImpot oid = getOfficeImpot(res.noColOID);
			writePageOid(res, dateGeneration, status, writer, human2file(oid.getNomCourt()));
		}
	}

	private void writePageOid(final ProduireRolesOIDsResults results, final Date dateGeneration, StatusManager status, PdfWriter writer, String prefixeNomsFichiersNonTraites) throws ServiceInfrastructureException, DocumentException {

		addEnteteUnireg();

		final OfficeImpot office = getOfficeImpot(results.noColOID);
		final String titrePrincipal = String.format("Rapport des rôles pour l'%s", office.getNomCourt());
		addTitrePrincipal(titrePrincipal);

		// Paramètres
		addEntete1("Paramètres");
		{
		    addTableSimple(2, table -> {
		        table.addLigne("Année fiscale :", String.valueOf(results.annee));
		        table.addLigne("Nombre de threads :", String.valueOf(results.nbThreads));
		        table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.dateTraitement));
		    });
		}

		// Résultats
		addEntete1("Résumé");
		{
			if (results.interrompu) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\nles valeurs ci-dessous sont donc incomplètes.");
		    }

			final int nbCommunesTraitees;
			if (results.noColOID != null) {
				// calcule le compte exact des communes gérées par l'OID (le résultats du job
				// peut en contenir plus en fonction des déménagements, ...)
				final List<Integer> list = getListeCommunesDansOid(getListeCommunes(results.getNoOfsCommunesTraitees(), results.annee, false), results.noColOID);
				nbCommunesTraitees = list.size();
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
		    final String filename;
			if (!StringUtils.isBlank(prefixeNomsFichiersNonTraites)) {
				filename = String.format("%s_erreurs.csv", prefixeNomsFichiersNonTraites);
			}
			else {
				filename = "contribuables_en_erreur.csv";
			}
		    final String titre = "Liste des contribuables en erreur";
			final String listVide = "(aucun contribuable en erreur)";
			try (TemporaryFile contenu = asCsvFile(results.ctbsEnErrors, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		if (!results.ctbsIgnores.isEmpty()) {
		    final String filename;
			if (!StringUtils.isBlank(prefixeNomsFichiersNonTraites)) {
				filename = String.format("%s_ignores.csv", prefixeNomsFichiersNonTraites);
			}
			else {
				filename = "contribuables_ignores.csv";
			}
		    final String titre = "Liste des contribuables ignorés";
			final String listVide = "(aucun contribuable ignoré)";
			try (TemporaryFile contenu = asCsvFile(results.ctbsIgnores, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		writeResultatsOid(results, status, writer);
	}

	private List<Integer> getListeCommunesDansOid(List<Commune> communes, int noColOID) throws ServiceInfrastructureException {
		final List<Integer> ofsCommunesDansOID = new ArrayList<>(communes.size());
		for (Commune commune : communes) {
			final OfficeImpot office = getInfraService().getOfficeImpotDeCommune(commune.getNoOFS());
			if (office != null && office.getNoColAdm() == noColOID) {
				ofsCommunesDansOID.add(commune.getNoOFS());
			}
		}
		return ofsCommunesDansOID;
	}

	private void writeResultatsOid(ProduireRolesOIDsResults results, StatusManager status, PdfWriter writer) throws ServiceInfrastructureException, DocumentException {

		final List<Commune> communes = getListeCommunes(results.getNoOfsCommunesTraitees(), results.annee, false);
		final Map<Integer, String> nomsCommunes = buildNomsCommunes(communes);

		// filtrage des seules communes qui sont effectivement dans l'OID
		final List<Integer> ofsCommunesDansOID = getListeCommunesDansOid(communes, results.noColOID);
		final List<InfoContribuablePP> infoOid = results.buildInfoPourRegroupementCommunes(ofsCommunesDansOID);

		// Résumé des types de contribuables
		addEntete1("Résumé des types de contribuables trouvés");
		{
		    final Map<InfoContribuable.TypeContribuable, Integer> nombreParType = extractNombreParType(infoOid);
		    addTableSimple(new float[]{2.0f, 1.0f}, table -> {
			    addLignesStatsParTypeCtb(table, nombreParType);
		    });
		}

		// Fichier CVS détaillé
		{
			final OfficeImpot office = getOfficeImpot(results.noColOID);
			try (AutoCloseableContainer<TemporaryFile> container = new AutoCloseableContainer<>(asCsvFiles(nomsCommunes, infoOid, status))) {
				writeFichierDetail(results, writer, container.getElements(), infoOid.isEmpty(), String.format("OID%02d", office.getNoColAdm()));
			}
		}
	}

	/**
	 * Utilisé par le traitement d'un OID complet
	 */
	private TemporaryFile[] asCsvFiles(Map<Integer, String> nomsCommunes, List<InfoContribuablePP> infos, StatusManager status) {
		status.setMessage("Génération du rapport");
		return traiteListeContribuablesPP(infos, nomsCommunes, InfoContribuable::getNoOfsDerniereCommune);
	}
}