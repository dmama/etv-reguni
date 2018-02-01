package ch.vd.unireg.xml.party.v1;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.party.taxdeclaration.v1.OrdinaryTaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclarationDeadline;
import ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclarationStatus;
import ch.vd.unireg.xml.party.taxdeclaration.v1.WithholdingTaxDeclaration;
import ch.vd.unireg.xml.party.v1.PartyPart;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationRetournee;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.EnumHelper;

public class TaxDeclarationBuilder {

	private static final Set<TypeEtatDocumentFiscal> ETATS_EXPOSES = EnumSet.of(TypeEtatDocumentFiscal.ECHU,
	                                                                            TypeEtatDocumentFiscal.EMIS,
	                                                                            TypeEtatDocumentFiscal.RETOURNE,
	                                                                            TypeEtatDocumentFiscal.SOMME);

	public static OrdinaryTaxDeclaration newOrdinaryTaxDeclaration(ch.vd.unireg.declaration.DeclarationImpotOrdinairePP declaration, @Nullable Set<PartyPart> parts) {

		final OrdinaryTaxDeclaration d = new OrdinaryTaxDeclaration();
		fillTaxDeclarationBase(d, declaration);
		fillTaxDeclarationParts(d, declaration, parts);

		d.setSequenceNumber(Long.valueOf(declaration.getNumero()));
		d.setDocumentType(EnumHelper.coreToXMLv1(declaration.getTypeDeclaration()));

		final Integer numeroOfsForGestion = declaration.getNumeroOfsForGestion();
		if (numeroOfsForGestion == null) {
			d.setManagingMunicipalityFSOId(0L);
		}
		else {
			d.setManagingMunicipalityFSOId(numeroOfsForGestion);
		}

		Integer cs = declaration.getCodeSegment();
		// SIFISC-3873 : Code segment 0 par défaut pour les DI >= 2011
		if (cs == null && declaration.getPeriode().getAnnee() >= ch.vd.unireg.declaration.DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE) {
			cs = DeclarationImpotService.VALEUR_DEFAUT_CODE_SEGMENT;
		}
		d.setSegmentationCode(cs); // SIFISC-2528

		return d;
	}

	public static WithholdingTaxDeclaration newWithholdingTaxDeclaration(ch.vd.unireg.declaration.DeclarationImpotSource declaration, @Nullable Set<PartyPart> parts) {
		final WithholdingTaxDeclaration d = new WithholdingTaxDeclaration();
		fillTaxDeclarationBase(d, declaration);
		fillTaxDeclarationParts(d, declaration, parts);
		d.setPeriodicity(EnumHelper.coreToXMLv1(declaration.getPeriodicite()));
		d.setCommunicationMode(EnumHelper.coreToXMLv1(declaration.getModeCommunication()));
		return d;
	}

	private static void fillTaxDeclarationBase(TaxDeclaration d, ch.vd.unireg.declaration.Declaration declaration) {
		d.setId(declaration.getId()); // [SIFISC-2392]
		d.setDateFrom(DataHelper.coreToXMLv1(declaration.getDateDebut()));
		d.setDateTo(DataHelper.coreToXMLv1(declaration.getDateFin()));
		d.setCancellationDate(DataHelper.coreToXMLv1(declaration.getAnnulationDate()));
		d.setTaxPeriod(TaxPeriodBuilder.newTaxPeriod(declaration.getPeriode()));
	}

	private static void fillTaxDeclarationParts(TaxDeclaration d, ch.vd.unireg.declaration.Declaration declaration, Set<PartyPart> parts) {
		if (parts != null && parts.contains(PartyPart.TAX_DECLARATIONS_STATUSES)) {
			for (ch.vd.unireg.declaration.EtatDeclaration etat : declaration.getEtatsDeclarationSorted()) {
				// on n'expose pas les nouveaux états qui ne sont pas connus par cette version de la XSD
				if (ETATS_EXPOSES.contains(etat.getEtat())) {
					d.getStatuses().add(newTaxDeclarationStatus(etat));
				}
			}
		}
		if (parts != null && parts.contains(PartyPart.TAX_DECLARATIONS_DEADLINES) && d instanceof OrdinaryTaxDeclaration) {
			final OrdinaryTaxDeclaration otd = (OrdinaryTaxDeclaration) d;
			for (ch.vd.unireg.declaration.DelaiDeclaration delai : declaration.getDelaisDeclarationSorted()) {
				if (delai.getEtat() == EtatDelaiDocumentFiscal.ACCORDE) {
					otd.getDeadlines().add(newTaxDeclarationDeadline(delai));
				}
			}
		}
	}

	private static TaxDeclarationDeadline newTaxDeclarationDeadline(DelaiDeclaration delai) {
		final TaxDeclarationDeadline d = new TaxDeclarationDeadline();
		d.setApplicationDate(DataHelper.coreToXMLv1(delai.getDateDemande()));
		d.setProcessingDate(DataHelper.coreToXMLv1(delai.getDateTraitement()));
		d.setDeadline(DataHelper.coreToXMLv1(delai.getDelaiAccordeAu()));
		d.setCancellationDate(DataHelper.coreToXMLv1(delai.getAnnulationDate()));
		d.setWrittenConfirmation(StringUtils.isNotBlank(delai.getCleArchivageCourrier()));
		return d;
	}

	public static TaxDeclarationStatus newTaxDeclarationStatus(ch.vd.unireg.declaration.EtatDeclaration etat) {
		final TaxDeclarationStatus e = new TaxDeclarationStatus();
		e.setType(EnumHelper.coreToXMLv1(etat.getEtat()));
		e.setDateFrom(DataHelper.coreToXMLv1(getAdjustedStatusDateFrom(etat)));
		e.setCancellationDate(DataHelper.coreToXMLv1(etat.getAnnulationDate()));
		if (etat instanceof EtatDeclarationRetournee) {
			e.setSource(((EtatDeclarationRetournee) etat).getSource());
		}
		return e;
	}

	/**
	 * [UNIREG-3407] Pour les états de sommation, c'est la date de l'envoi du courrier qu'il faut renvoyer
	 *
	 * @param etatDeclaration etat de la déclaration
	 * @return valeur de la date à mettre dans le champ {@link TaxDeclarationStatus#dateFrom}
	 */
	private static RegDate getAdjustedStatusDateFrom(ch.vd.unireg.declaration.EtatDeclaration etatDeclaration) {
		final RegDate date;
		if (etatDeclaration instanceof ch.vd.unireg.declaration.EtatDeclarationSommee) {
			final ch.vd.unireg.declaration.EtatDeclarationSommee etatSommee = (ch.vd.unireg.declaration.EtatDeclarationSommee) etatDeclaration;
			date = etatSommee.getDateEnvoiCourrier();
		}
		else {
			date = etatDeclaration.getDateObtention();
		}
		return date;
	}

	public static TaxDeclaration clone(TaxDeclaration d) {
		if (d == null) {
			return null;
		}
		final ArrayList<TaxDeclarationStatus> clonedEtats = cloneStatuses(d.getStatuses());
		if (d instanceof OrdinaryTaxDeclaration) {
			return new OrdinaryTaxDeclaration(d.getId(), d.getDateFrom(), d.getDateTo(), d.getCancellationDate(), d.getTaxPeriod(), clonedEtats, ((OrdinaryTaxDeclaration) d).getSequenceNumber(),
					((OrdinaryTaxDeclaration) d).getDocumentType(), ((OrdinaryTaxDeclaration) d).getManagingMunicipalityFSOId(), ((OrdinaryTaxDeclaration) d).getSegmentationCode(), 0, null, 0, null);
		}
		else if (d instanceof WithholdingTaxDeclaration) {
			return new WithholdingTaxDeclaration(d.getId(), d.getDateFrom(), d.getDateTo(), d.getCancellationDate(), d.getTaxPeriod(), clonedEtats, ((WithholdingTaxDeclaration) d).getPeriodicity(),
					((WithholdingTaxDeclaration) d).getCommunicationMode(), null);
		}
		else {
			throw new IllegalArgumentException("Type de déclaration d'impôt inconnu = [" + d.getClass() + ']');
		}
	}

	private static ArrayList<TaxDeclarationStatus> cloneStatuses(List<TaxDeclarationStatus> etats) {
		final ArrayList<TaxDeclarationStatus> clonedEtats;
		if (etats == null) {
			clonedEtats = null;
		}
		else {
			clonedEtats = new ArrayList<>(etats.size());
			for (TaxDeclarationStatus etat : etats) {
				clonedEtats.add(clone(etat));
			}
		}
		return clonedEtats;
	}

	private static TaxDeclarationStatus clone(TaxDeclarationStatus etat) {
		if (etat == null) {
			return null;
		}
		return new TaxDeclarationStatus(etat.getDateFrom(), etat.getCancellationDate(), etat.getType(), etat.getSource(), 0, null);
	}
}
