package ch.vd.uniregctb.xml.party.v3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.party.taxdeclaration.v3.OrdinaryTaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v3.TaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v3.TaxDeclarationDeadline;
import ch.vd.unireg.xml.party.taxdeclaration.v3.TaxDeclarationStatus;
import ch.vd.unireg.xml.party.taxdeclaration.v3.WithholdingTaxDeclaration;
import ch.vd.unireg.xml.party.v3.PartyPart;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;

public class TaxDeclarationBuilder {

	private static final Set<TypeEtatDeclaration> ETATS_EXPOSES = EnumSet.of(TypeEtatDeclaration.ECHUE,
	                                                                         TypeEtatDeclaration.EMISE,
	                                                                         TypeEtatDeclaration.RETOURNEE,
	                                                                         TypeEtatDeclaration.SOMMEE);

	public static OrdinaryTaxDeclaration newOrdinaryTaxDeclaration(ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP declaration, @Nullable Set<PartyPart> parts) {

		final OrdinaryTaxDeclaration d = new OrdinaryTaxDeclaration();
		fillTaxDeclarationBase(d, declaration);
		fillTaxDeclarationParts(d, declaration, parts);

		d.setSequenceNumber(Long.valueOf(declaration.getNumero()));
		d.setDocumentType(EnumHelper.coreToXMLv3(declaration.getTypeDeclaration()));

		final Integer numeroOfsForGestion = declaration.getNumeroOfsForGestion();
		if (numeroOfsForGestion == null) {
			d.setManagingMunicipalityFSOId(0L);
		}
		else {
			d.setManagingMunicipalityFSOId(numeroOfsForGestion);
		}

		Integer cs = declaration.getCodeSegment();
		// SIFISC-3873 : Code segment 0 par défaut pour les DI >= 2011
		if (cs == null && declaration.getPeriode().getAnnee() >= ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE) {
			cs = DeclarationImpotService.VALEUR_DEFAUT_CODE_SEGMENT;
		}
		d.setSegmentationCode(cs); // SIFISC-2528

		return d;
	}

	public static WithholdingTaxDeclaration newWithholdingTaxDeclaration(ch.vd.uniregctb.declaration.DeclarationImpotSource declaration, @Nullable Set<PartyPart> parts) {
		final WithholdingTaxDeclaration d = new WithholdingTaxDeclaration();
		fillTaxDeclarationBase(d, declaration);
		fillTaxDeclarationParts(d, declaration, parts);
		d.setPeriodicity(EnumHelper.coreToXMLv3(declaration.getPeriodicite()));
		d.setCommunicationMode(EnumHelper.coreToXMLv3(declaration.getModeCommunication()));
		return d;
	}

	private static void fillTaxDeclarationBase(TaxDeclaration d, ch.vd.uniregctb.declaration.Declaration declaration) {
		d.setId(declaration.getId()); // [SIFISC-2392]
		d.setDateFrom(DataHelper.coreToXMLv2(declaration.getDateDebut()));
		d.setDateTo(DataHelper.coreToXMLv2(declaration.getDateFin()));
		d.setCancellationDate(DataHelper.coreToXMLv2(declaration.getAnnulationDate()));
		d.setTaxPeriod(TaxPeriodBuilder.newTaxPeriod(declaration.getPeriode()));
	}

	private static void fillTaxDeclarationParts(TaxDeclaration d, ch.vd.uniregctb.declaration.Declaration declaration, Set<PartyPart> parts) {
		if (parts != null && parts.contains(PartyPart.TAX_DECLARATIONS_STATUSES)) {
			for (ch.vd.uniregctb.declaration.EtatDeclaration etat : declaration.getEtatsSorted()) {
				// on n'expose pas les nouveaux états qui ne sont pas connus par cette version de la XSD
				if (ETATS_EXPOSES.contains(etat.getEtat())) {
					d.getStatuses().add(newTaxDeclarationStatus(etat));
				}
			}
		}
		if (parts != null && parts.contains(PartyPart.TAX_DECLARATIONS_DEADLINES)) {
			for (DelaiDeclaration delai : declaration.getDelaisSorted()) {
				d.getDeadlines().add(newTaxDeclarationDeadline(delai));
			}
		}
	}

	private static TaxDeclarationDeadline newTaxDeclarationDeadline(DelaiDeclaration delai) {
		final TaxDeclarationDeadline d = new TaxDeclarationDeadline();
		d.setApplicationDate(DataHelper.coreToXMLv2(delai.getDateDemande()));
		d.setProcessingDate(DataHelper.coreToXMLv2(delai.getDateTraitement()));
		d.setDeadline(DataHelper.coreToXMLv2(delai.getDelaiAccordeAu()));
		d.setCancellationDate(DataHelper.coreToXMLv2(delai.getAnnulationDate()));
		d.setWrittenConfirmation(delai.getConfirmationEcrite() != null && delai.getConfirmationEcrite());
		return d;
	}

	public static TaxDeclarationStatus newTaxDeclarationStatus(ch.vd.uniregctb.declaration.EtatDeclaration etat) {
		final TaxDeclarationStatus e = new TaxDeclarationStatus();
		e.setType(EnumHelper.coreToXMLv3(etat.getEtat()));
		e.setDateFrom(DataHelper.coreToXMLv2(getAdjustedStatusDateFrom(etat)));
		e.setCancellationDate(DataHelper.coreToXMLv2(etat.getAnnulationDate()));
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
		final List<TaxDeclarationStatus> clonedEtats = cloneStatuses(d.getStatuses());
		final List<TaxDeclarationDeadline> clonedDeadlines = cloneDeadlines(d.getDeadlines());
		if (d instanceof OrdinaryTaxDeclaration) {
			return new OrdinaryTaxDeclaration(d.getId(), d.getDateFrom(), d.getDateTo(), d.getCancellationDate(), d.getTaxPeriod(), clonedEtats, clonedDeadlines, ((OrdinaryTaxDeclaration) d).getSequenceNumber(),
					((OrdinaryTaxDeclaration) d).getDocumentType(), ((OrdinaryTaxDeclaration) d).getManagingMunicipalityFSOId(), ((OrdinaryTaxDeclaration) d).getSegmentationCode(), 0, null);
		}
		else if (d instanceof WithholdingTaxDeclaration) {
			return new WithholdingTaxDeclaration(d.getId(), d.getDateFrom(), d.getDateTo(), d.getCancellationDate(), d.getTaxPeriod(), clonedEtats, clonedDeadlines, ((WithholdingTaxDeclaration) d).getPeriodicity(),
					((WithholdingTaxDeclaration) d).getCommunicationMode(), null);
		}
		else {
			throw new IllegalArgumentException("Type de déclaration d'impôt inconnu = [" + d.getClass() + ']');
		}
	}

	private static List<TaxDeclarationStatus> cloneStatuses(List<TaxDeclarationStatus> etats) {
		final List<TaxDeclarationStatus> clonedEtats;
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

	private static List<TaxDeclarationDeadline> cloneDeadlines(List<TaxDeclarationDeadline> deadlines) {
		final List<TaxDeclarationDeadline> clonedDeadlines;
		if (deadlines == null) {
			clonedDeadlines = null;
		}
		else {
			clonedDeadlines = new ArrayList<>(deadlines.size());
			for (TaxDeclarationDeadline deadline : deadlines) {
				clonedDeadlines.add(clone(deadline));
			}
		}
		return clonedDeadlines;
	}

	private static TaxDeclarationDeadline clone(TaxDeclarationDeadline deadline) {
		if (deadline == null) {
			return null;
		}
		return new TaxDeclarationDeadline(deadline.getApplicationDate(), deadline.getProcessingDate(), deadline.getDeadline(), deadline.getCancellationDate(), deadline.isWrittenConfirmation(), null);
	}
}
