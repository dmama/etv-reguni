package ch.vd.uniregctb.xml.party.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.party.taxdeclaration.v2.OrdinaryTaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v2.TaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v2.TaxDeclarationDeadline;
import ch.vd.unireg.xml.party.taxdeclaration.v2.TaxDeclarationStatus;
import ch.vd.unireg.xml.party.taxdeclaration.v2.WithholdingTaxDeclaration;
import ch.vd.unireg.xml.party.v2.PartyPart;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;

public class TaxDeclarationBuilder {

	public static OrdinaryTaxDeclaration newOrdinaryTaxDeclaration(ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire declaration, @Nullable Set<PartyPart> parts) {

		final OrdinaryTaxDeclaration d = new OrdinaryTaxDeclaration();
		fillTaxDeclarationBase(d, declaration);
		fillTaxDeclarationParts(d, declaration, parts);

		d.setSequenceNumber(Long.valueOf(declaration.getNumero()));
		d.setDocumentType(EnumHelper.coreToXMLv2(declaration.getTypeDeclaration()));

		final Integer numeroOfsForGestion = declaration.getNumeroOfsForGestion();
		if (numeroOfsForGestion == null) {
			d.setManagingMunicipalityFSOId(0L);
		}
		else {
			d.setManagingMunicipalityFSOId(numeroOfsForGestion);
		}

		Integer cs = declaration.getCodeSegment();
		// SIFISC-3873 : Code segment 0 par défaut pour les DI >= 2011
		if (cs == null && declaration.getPeriode().getAnnee() >= ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE) {
			cs = DeclarationImpotService.VALEUR_DEFAUT_CODE_SEGMENT;
		}
		d.setSegmentationCode(cs); // SIFISC-2528

		return d;
	}

	public static WithholdingTaxDeclaration newWithholdingTaxDeclaration(ch.vd.uniregctb.declaration.DeclarationImpotSource declaration, @Nullable Set<PartyPart> parts) {
		final WithholdingTaxDeclaration d = new WithholdingTaxDeclaration();
		fillTaxDeclarationBase(d, declaration);
		fillTaxDeclarationParts(d, declaration, parts);
		d.setPeriodicity(EnumHelper.coreToXMLv2(declaration.getPeriodicite()));
		d.setCommunicationMode(EnumHelper.coreToXMLv2(declaration.getModeCommunication()));
		return d;
	}

	private static void fillTaxDeclarationBase(TaxDeclaration d, ch.vd.uniregctb.declaration.Declaration declaration) {
		d.setId(declaration.getId()); // [SIFISC-2392]
		d.setDateFrom(DataHelper.coreToXMLv1(declaration.getDateDebut()));
		d.setDateTo(DataHelper.coreToXMLv1(declaration.getDateFin()));
		d.setCancellationDate(DataHelper.coreToXMLv1(declaration.getAnnulationDate()));
		d.setTaxPeriod(TaxPeriodBuilder.newTaxPeriod(declaration.getPeriode()));
	}

	private static void fillTaxDeclarationParts(TaxDeclaration d, ch.vd.uniregctb.declaration.Declaration declaration, Set<PartyPart> parts) {
		if (parts != null && parts.contains(PartyPart.TAX_DECLARATIONS_STATUSES)) {
			for (ch.vd.uniregctb.declaration.EtatDeclaration etat : declaration.getEtatsSorted()) {
				d.getStatuses().add(newTaxDeclarationStatus(etat));
			}
		}
		if (parts != null && parts.contains(PartyPart.TAX_DECLARATIONS_DEADLINES) && d instanceof OrdinaryTaxDeclaration) {
			final OrdinaryTaxDeclaration otd = (OrdinaryTaxDeclaration) d;
			for (DelaiDeclaration delai : declaration.getDelaisSorted()) {
				otd.getDeadlines().add(newTaxDeclarationDeadline(delai));
			}
		}
	}

	private static TaxDeclarationDeadline newTaxDeclarationDeadline(DelaiDeclaration delai) {
		final TaxDeclarationDeadline d = new TaxDeclarationDeadline();
		d.setApplicationDate(DataHelper.coreToXMLv1(delai.getDateDemande()));
		d.setProcessingDate(DataHelper.coreToXMLv1(delai.getDateTraitement()));
		d.setDeadline(DataHelper.coreToXMLv1(delai.getDelaiAccordeAu()));
		d.setCancellationDate(DataHelper.coreToXMLv1(delai.getAnnulationDate()));
		d.setWrittenConfirmation(delai.getConfirmationEcrite() != null && delai.getConfirmationEcrite());
		return d;
	}

	public static TaxDeclarationStatus newTaxDeclarationStatus(ch.vd.uniregctb.declaration.EtatDeclaration etat) {
		final TaxDeclarationStatus e = new TaxDeclarationStatus();
		e.setType(EnumHelper.coreToXMLv2(etat.getEtat()));
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
	 * @return valeur de la date à mettre dans le champ {@link ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclarationStatus#dateFrom}
	 */
	private static RegDate getAdjustedStatusDateFrom(ch.vd.uniregctb.declaration.EtatDeclaration etatDeclaration) {
		final RegDate date;
		if (etatDeclaration instanceof ch.vd.uniregctb.declaration.EtatDeclarationSommee) {
			final ch.vd.uniregctb.declaration.EtatDeclarationSommee etatSommee = (ch.vd.uniregctb.declaration.EtatDeclarationSommee) etatDeclaration;
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
