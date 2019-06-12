package ch.vd.unireg.evenement.reqdes.pdf;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.ApplicationInfo;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.reqdes.EvenementReqDes;
import ch.vd.unireg.reqdes.InformationsActeur;
import ch.vd.unireg.reqdes.PartiePrenante;
import ch.vd.unireg.reqdes.RolePartiePrenante;
import ch.vd.unireg.reqdes.TransactionImmobiliere;
import ch.vd.unireg.reqdes.UniteTraitement;

/**
 * Générateur de PDF pour les unités de traitement ReqDes
 */
public class UniteTraitementPdfDocumentGenerator extends ReqDesPdfDocumentGenerator {

	private static final String DASH = "-";

	private final UniteTraitement ut;

	public UniteTraitementPdfDocumentGenerator(UniteTraitement ut) {
		this.ut = ut;
	}

	public void generatePdf(OutputStream out, ServiceInfrastructureService infraService) throws IOException, DocumentException {

		final Document doc = newDocument();
		final PdfWriter writer = PdfWriter.getInstance(doc, out);
		doc.open();
		addMetaInformation(doc, "ReqDes-" + ut.getId(), "Unité de traitement ReqDes " + ut.getId(), ApplicationInfo.getName());
		addTitrePrincipal(doc, "Unité de traitement " + ut.getId());

		final EvenementReqDes evenement = ut.getEvenement();
		final RegDate dateActe = evenement.getDateActe();

		// données de l'acte
		{
			addEntete(doc, "Acte");

			final PdfPTable table = new PdfPTable(new float[] {.1f, .35f, .2f, .35f});
			table.setWidthPercentage(95);
			table.getDefaultCell().setBorder(Rectangle.NO_BORDER);

			// ligne 1
			table.addCell(new Phrase(new Chunk("Date :", TABLE_HEADER_FONT)));
			table.addCell(new Phrase(new Chunk(RegDateHelper.dateToDisplayString(dateActe), NORMAL_FONT)));
			table.addCell(new Phrase(new Chunk("Numéro de minute :", TABLE_HEADER_FONT)));
			table.addCell(new Phrase(new Chunk(evenement.getNumeroMinute(), NORMAL_FONT)));

			// ligne 2
			table.addCell(new Phrase(new Chunk("Notaire :", TABLE_HEADER_FONT)));
			table.addCell(new Phrase(new Chunk(toDisplayString(evenement.getNotaire()), NORMAL_FONT)));
			table.addCell(new Phrase(new Chunk("Opérateur :", TABLE_HEADER_FONT)));
			table.addCell(new Phrase(new Chunk(toDisplayString(evenement.getOperateur()), NORMAL_FONT)));

			// table ajoutée au document
			doc.add(table);
		}

		// données des parties prenantes
		final List<PartiePrenante> sortedPartiesPrenantes = new ArrayList<>(ut.getPartiesPrenantes());
		sortedPartiesPrenantes.sort(Comparator.comparing(PartiePrenante::getId));
		for (PartiePrenante pp : sortedPartiesPrenantes) {
			addEntete(doc, "Partie prenante");

			final PdfPTable tablePrincipale = new PdfPTable(new float[] {.5f, .5f});
			tablePrincipale.setWidthPercentage(100);
			tablePrincipale.getDefaultCell().setBorder(Rectangle.NO_BORDER);

			// table avec les données de base dans la première colonne
			{
				final PdfPTable tableGauche = new PdfPTable(new float[] {.5f, .5f});
				tableGauche.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
				tableGauche.setWidthPercentage(100);
				tableGauche.setHeaderRows(1);

				final PdfPCell headerCell = new PdfPCell(new Phrase(new Chunk("Identification", TABLE_HEADER_FONT)));
				headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
				headerCell.setColspan(2);
				headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
				tableGauche.addCell(headerCell);

				tableGauche.addCell(new Phrase(new Chunk("Nom :", TABLE_HEADER_FONT)));
				tableGauche.addCell(new Phrase(new Chunk(StringUtils.trimToEmpty(pp.getNom()), NORMAL_FONT)));
				tableGauche.addCell(new Phrase(new Chunk("Nom de naissance :", TABLE_HEADER_FONT)));
				tableGauche.addCell(new Phrase(new Chunk(StringUtils.trimToEmpty(pp.getNomNaissance()), NORMAL_FONT)));
				tableGauche.addCell(new Phrase(new Chunk("Prénoms :", TABLE_HEADER_FONT)));
				tableGauche.addCell(new Phrase(new Chunk(StringUtils.trimToEmpty(pp.getPrenoms()), NORMAL_FONT)));
				tableGauche.addCell(new Phrase(new Chunk("Date de naissance :", TABLE_HEADER_FONT)));
				tableGauche.addCell(new Phrase(new Chunk(RegDateHelper.dateToDisplayString(pp.getDateNaissance()), NORMAL_FONT)));
				tableGauche.addCell(new Phrase(new Chunk("Sexe :", TABLE_HEADER_FONT)));
				tableGauche.addCell(new Phrase(new Chunk(pp.getSexe() != null ? pp.getSexe().getDisplayName() : StringUtils.EMPTY, NORMAL_FONT)));
				tableGauche.addCell(new Phrase(new Chunk("Date de décès :", TABLE_HEADER_FONT)));
				tableGauche.addCell(new Phrase(new Chunk(RegDateHelper.dateToDisplayString(pp.getDateDeces()), NORMAL_FONT)));
				tableGauche.addCell(new Phrase(new Chunk("N° AVS :", TABLE_HEADER_FONT)));
				tableGauche.addCell(new Phrase(new Chunk(FormatNumeroHelper.formatNumAVS(pp.getAvs()), NORMAL_FONT)));

				final NomPrenom mere = new NomPrenom(pp.getNomMere(), pp.getPrenomsMere());
				tableGauche.addCell(new Phrase(new Chunk("Nom de la mère :", TABLE_HEADER_FONT)));
				tableGauche.addCell(new Phrase(new Chunk(mere.getNomPrenom(), NORMAL_FONT)));

				final NomPrenom pere = new NomPrenom(pp.getNomPere(), pp.getPrenomsPere());
				tableGauche.addCell(new Phrase(new Chunk("Nom du père :", TABLE_HEADER_FONT)));
				tableGauche.addCell(new Phrase(new Chunk(pere.getNomPrenom(), NORMAL_FONT)));

				tableGauche.addCell(new Phrase(new Chunk("Etat civil :", TABLE_HEADER_FONT)));
				tableGauche.addCell(new Phrase(new Chunk(pp.getEtatCivil() != null ? pp.getEtatCivil().format() : StringUtils.EMPTY, NORMAL_FONT)));
				tableGauche.addCell(new Phrase(new Chunk("Changement d'état civil :", TABLE_HEADER_FONT)));
				tableGauche.addCell(new Phrase(new Chunk(RegDateHelper.dateToDisplayString(pp.getDateEtatCivil()), NORMAL_FONT)));
				if (pp.getDateSeparation() != null) {
					tableGauche.addCell(new Phrase(new Chunk("Date de séparation :", TABLE_HEADER_FONT)));
					tableGauche.addCell(new Phrase(new Chunk(RegDateHelper.dateToDisplayString(pp.getDateSeparation()), NORMAL_FONT)));
				}
				tableGauche.addCell(new Phrase(new Chunk("Nationalité :", TABLE_HEADER_FONT)));
				tableGauche.addCell(new Phrase(new Chunk(getNomPays(pp.getOfsPaysNationalite(), dateActe, infraService), NORMAL_FONT)));
				tableGauche.addCell(new Phrase(new Chunk("Origine :", TABLE_HEADER_FONT)));
				tableGauche.addCell(new Phrase(new Chunk(pp.getOrigine() != null ? pp.getOrigine().getLibelleAvecCanton() : StringUtils.EMPTY, NORMAL_FONT)));
				tableGauche.addCell(new Phrase(new Chunk("Catégorie d'étranger :", TABLE_HEADER_FONT)));
				tableGauche.addCell(new Phrase(new Chunk(pp.getCategorieEtranger() != null ? pp.getCategorieEtranger().getDisplayName() : StringUtils.EMPTY, NORMAL_FONT)));
				tableGauche.addCell(new Phrase(new Chunk("Conjoint :", TABLE_HEADER_FONT)));
				tableGauche.addCell(new Phrase(new Chunk(getDesignationConjoint(pp), NORMAL_FONT)));
				tableGauche.addCell(new Phrase(new Chunk("Source :", TABLE_HEADER_FONT)));
				tableGauche.addCell(new Phrase(new Chunk(getSource(pp), NORMAL_FONT)));

				final PdfPTable dummyTable = new PdfPTable(1);
				dummyTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
				dummyTable.setWidthPercentage(100);
				dummyTable.addCell(tableGauche);
				dummyTable.addCell(new Phrase(new Chunk(StringUtils.EMPTY)));

				tablePrincipale.addCell(dummyTable);
			}

			// ... et les données dans la colonne de droite
			{
				final PdfPTable tableDroite = new PdfPTable(new float[] {.5f, .5f});
				tableDroite.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
				tableDroite.setWidthPercentage(100);
				tableDroite.setHeaderRows(1);

				final PdfPCell headerCell = new PdfPCell(new Phrase(new Chunk("Résidence", TABLE_HEADER_FONT)));
				headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
				headerCell.setColspan(2);
				headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
				tableDroite.addCell(headerCell);

				tableDroite.addCell(new Phrase(new Chunk("Complément :", TABLE_HEADER_FONT)));
				tableDroite.addCell(new Phrase(new Chunk(StringUtils.trimToEmpty(pp.getTitre()), NORMAL_FONT)));
				tableDroite.addCell(new Phrase(new Chunk("Rue :", TABLE_HEADER_FONT)));
				tableDroite.addCell(new Phrase(new Chunk(StringUtils.trimToEmpty(pp.getRue()), NORMAL_FONT)));
				tableDroite.addCell(new Phrase(new Chunk("Numéro de maison :", TABLE_HEADER_FONT)));
				tableDroite.addCell(new Phrase(new Chunk(StringUtils.trimToEmpty(pp.getNumeroMaison()), NORMAL_FONT)));
				tableDroite.addCell(new Phrase(new Chunk("Numéro d'appartement :", TABLE_HEADER_FONT)));
				tableDroite.addCell(new Phrase(new Chunk(StringUtils.trimToEmpty(pp.getNumeroAppartement()), NORMAL_FONT)));
				tableDroite.addCell(new Phrase(new Chunk("Localité :", TABLE_HEADER_FONT)));
				tableDroite.addCell(new Phrase(new Chunk(StringUtils.trimToEmpty(pp.getLocalite()), NORMAL_FONT)));
				tableDroite.addCell(new Phrase(new Chunk("NPA :", TABLE_HEADER_FONT)));
				tableDroite.addCell(new Phrase(new Chunk(getNPA(pp.getNumeroPostal(), pp.getNumeroPostalComplementaire()), NORMAL_FONT)));
				tableDroite.addCell(new Phrase(new Chunk("Numéro d'ordre postal :", TABLE_HEADER_FONT)));
				tableDroite.addCell(new Phrase(new Chunk(pp.getNumeroOrdrePostal() != null ? pp.getNumeroOrdrePostal().toString() : StringUtils.EMPTY, NORMAL_FONT)));
				tableDroite.addCell(new Phrase(new Chunk("Case postale :", TABLE_HEADER_FONT)));
				tableDroite.addCell(new Phrase(new Chunk(getCasePostale(pp.getTexteCasePostale(), pp.getCasePostale()), NORMAL_FONT)));
				if (pp.getOfsCommune() != null) {
					tableDroite.addCell(new Phrase(new Chunk("Commune :", TABLE_HEADER_FONT)));
					tableDroite.addCell(new Phrase(new Chunk(getNomCommune(pp.getOfsCommune(), true, dateActe, infraService), NORMAL_FONT)));
				}
				else {
					tableDroite.addCell(new Phrase(new Chunk("Pays :", TABLE_HEADER_FONT)));
					if (pp.getOfsPays() != null) {
						tableDroite.addCell(new Phrase(new Chunk(getNomPays(pp.getOfsPays(), dateActe, infraService), NORMAL_FONT)));
					}
					else {
						tableDroite.addCell(new Phrase(new Chunk("Indéterminé", NORMAL_FONT)));
					}
				}

				final PdfPTable dummyTable = new PdfPTable(1);
				dummyTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
				dummyTable.setWidthPercentage(100);
				dummyTable.addCell(tableDroite);
				dummyTable.addCell(new Phrase(new Chunk(StringUtils.EMPTY)));

				tablePrincipale.addCell(dummyTable);
			}

			doc.add(tablePrincipale);

			// les rôles de la partie prenante
			final PdfPTable tableRoles = new PdfPTable(5);
			tableRoles.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
			tableRoles.setWidthPercentage(100);
			tableRoles.setHeaderRows(2);

			final PdfPCell headerCell = new PdfPCell(new Phrase(new Chunk("Rôle(s) dans l'acte", TABLE_HEADER_FONT)));
			headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
			headerCell.setColspan(5);
			headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
			tableRoles.addCell(headerCell);

			tableRoles.addCell(new Phrase(new Chunk("Rôle", TABLE_HEADER_FONT)));
			tableRoles.addCell(new Phrase(new Chunk("Mode d'inscription", TABLE_HEADER_FONT)));
			tableRoles.addCell(new Phrase(new Chunk("Type d'inscription", TABLE_HEADER_FONT)));
			tableRoles.addCell(new Phrase(new Chunk("Description", TABLE_HEADER_FONT)));
			tableRoles.addCell(new Phrase(new Chunk("Commune", TABLE_HEADER_FONT)));

			final List<RolePartiePrenante> sortedRoles = new ArrayList<>(pp.getRoles());
			sortedRoles.sort((o1, o2) -> {
				int comparison = compareNullableValues(o1.getRole(), o2.getRole());
				if (comparison == 0) {
					final TransactionImmobiliere ti1 = o1.getTransaction();
					final TransactionImmobiliere ti2 = o2.getTransaction();
					comparison = compareNullableValues(ti1.getModeInscription(), ti2.getModeInscription());
					if (comparison == 0) {
						comparison = compareNullableValues(ti1.getTypeInscription(), ti2.getTypeInscription());
						if (comparison == 0) {
							comparison = Integer.compare(ti1.getOfsCommune(), ti2.getOfsCommune());
						}
					}
				}
				return comparison;
			});
			for (RolePartiePrenante role : sortedRoles) {
				final TransactionImmobiliere transaction = role.getTransaction();
				tableRoles.addCell(new Phrase(new Chunk(role.getRole().getDisplayName(), NORMAL_FONT)));
				tableRoles.addCell(new Phrase(new Chunk(transaction.getModeInscription().getDisplayName(), NORMAL_FONT)));
				tableRoles.addCell(new Phrase(new Chunk(transaction.getTypeInscription().getDisplayName(), NORMAL_FONT)));
				tableRoles.addCell(new Phrase(new Chunk(StringUtils.trimToEmpty(transaction.getDescription()), NORMAL_FONT)));
				tableRoles.addCell(new Phrase(new Chunk(getNomCommune(transaction.getOfsCommune(), false, dateActe, infraService), NORMAL_FONT)));
			}

			doc.add(tableRoles);
		}

		doc.close();
	}

	private static <T extends Comparable<T>> int compareNullableValues(@Nullable T o1, @Nullable T o2) {
		if (o1 == null && o2 == null) {
			return 0;
		}
		else if (o1 == null) {
			return 1;
		}
		else if (o2 == null) {
			return -1;
		}
		else {
			return o1.compareTo(o2);
		}
	}

	private static String toDisplayString(InformationsActeur info) {
		if (info == null) {
			return DASH;
		}

		final NomPrenom nomPrenom = new NomPrenom(info.getNom(), info.getPrenom());
		return String.format("%s (%s)", nomPrenom.getNomPrenom(), info.getVisa());
	}

	private static String getNomPays(Integer ofs, RegDate dateActe, ServiceInfrastructureService infraService) {
		if (ofs == null) {
			return StringUtils.EMPTY;
		}
		final Pays pays = infraService.getPays(ofs, dateActe);
		return pays == null ? ofs.toString() : pays.getNomCourt();
	}

	private static String getNomCommune(Integer ofs, boolean avecCanton, RegDate dateActe, ServiceInfrastructureService infraService) {
		if (ofs == null) {
			return StringUtils.EMPTY;
		}
		final Commune commune = infraService.getCommuneByNumeroOfs(ofs, dateActe);
		return commune == null ? ofs.toString() : (avecCanton ? commune.getNomOfficielAvecCanton() : commune.getNomOfficiel());
	}

	private static String getDesignationConjoint(PartiePrenante pp) {
		final boolean autrePartiePrenante;
		final NomPrenom nomPrenom;
		if (pp.getConjointPartiePrenante() != null) {
			autrePartiePrenante = pp.getConjointPartiePrenante() != pp;
			nomPrenom = new NomPrenom(pp.getConjointPartiePrenante().getNom(), pp.getConjointPartiePrenante().getPrenoms());
		}
		else {
			autrePartiePrenante = false;
			nomPrenom = new NomPrenom(pp.getNomConjoint(), pp.getPrenomConjoint());
		}
		if (autrePartiePrenante) {
			return String.format("%s (autre partie prenante)", nomPrenom.getNomPrenom());
		}
		else {
			return nomPrenom.getNomPrenom();
		}
	}

	private static String getSource(PartiePrenante pp) {
		if (pp.isSourceCivile()) {
			return "Données civiles";
		}
		else if (pp.getNumeroContribuable() != null) {
			return String.format("Contribuable %s", FormatNumeroHelper.numeroCTBToDisplay(pp.getNumeroContribuable()));
		}
		else if (pp.getNumeroContribuableCree() != null) {
			return String.format("Création (%s)", FormatNumeroHelper.numeroCTBToDisplay(pp.getNumeroContribuableCree()));
		}
		else {
			return "Création";
		}
	}

	private static String getNPA(String npa, Integer npaComplement) {
		if (StringUtils.isBlank(npa)) {
			return StringUtils.EMPTY;
		}
		if (npaComplement != null) {
			return String.format("%s-%d", npa, npaComplement);
		}
		else {
			return npa;
		}
	}

	private static String getCasePostale(String texteCasePostale, Integer numeroCasePostale) {
		if (StringUtils.isNotBlank(texteCasePostale)) {
			if (numeroCasePostale != null) {
				return String.format("%s %d", texteCasePostale, numeroCasePostale);
			}
			else {
				return texteCasePostale;
			}
		}
		return StringUtils.EMPTY;
	}
}
