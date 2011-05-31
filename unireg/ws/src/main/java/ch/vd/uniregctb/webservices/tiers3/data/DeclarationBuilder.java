package ch.vd.uniregctb.webservices.tiers3.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.webservices.tiers3.Declaration;
import ch.vd.uniregctb.webservices.tiers3.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.webservices.tiers3.DeclarationImpotSource;
import ch.vd.uniregctb.webservices.tiers3.EtatDeclaration;
import ch.vd.uniregctb.webservices.tiers3.TiersPart;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

public class DeclarationBuilder {

	public static DeclarationImpotOrdinaire newDeclarationImpotOrdinaire(ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire declaration, @Nullable Set<TiersPart> parts) {

		final DeclarationImpotOrdinaire d = new DeclarationImpotOrdinaire();
		fillDeclarationBase(d, declaration);
		fillDeclarationParts(d, declaration, parts);

		d.setNumero(Long.valueOf(declaration.getNumero()));
		d.setTypeDocument(EnumHelper.coreToWeb(declaration.getTypeDeclaration()));

		final Integer numeroOfsForGestion = declaration.getNumeroOfsForGestion();
		if (numeroOfsForGestion == null) {
			d.setNumeroOfsForGestion(0L);
		}
		else {
			d.setNumeroOfsForGestion(numeroOfsForGestion);
		}

		return d;
	}

	public static DeclarationImpotSource newDeclarationImpotSource(ch.vd.uniregctb.declaration.DeclarationImpotSource declaration, @Nullable Set<TiersPart> parts) {
		final DeclarationImpotSource d = new DeclarationImpotSource();
		fillDeclarationBase(d, declaration);
		fillDeclarationParts(d, declaration, parts);
		d.setPeriodicite(EnumHelper.coreToWeb(declaration.getPeriodicite()));
		d.setModeCommunication(EnumHelper.coreToWeb(declaration.getModeCommunication()));
		return d;
	}

	private static void fillDeclarationBase(Declaration d, ch.vd.uniregctb.declaration.Declaration declaration) {
		d.setDateDebut(DataHelper.coreToWeb(declaration.getDateDebut()));
		d.setDateFin(DataHelper.coreToWeb(declaration.getDateFin()));
		d.setDateAnnulation(DataHelper.coreToWeb(declaration.getAnnulationDate()));
		d.setPeriodeFiscale(PeriodeFiscaleBuilder.newPeriodeFiscale(declaration.getPeriode()));
	}

	private static void fillDeclarationParts(Declaration d, ch.vd.uniregctb.declaration.Declaration declaration, Set<TiersPart> parts) {
		if (parts != null && parts.contains(TiersPart.ETATS_DECLARATIONS)) {
			for (ch.vd.uniregctb.declaration.EtatDeclaration etat : declaration.getEtats()) {
				d.getEtats().add(newEtatDeclaration(etat));
			}
		}
	}

	public static EtatDeclaration newEtatDeclaration(ch.vd.uniregctb.declaration.EtatDeclaration etat) {
		final EtatDeclaration e = new EtatDeclaration();
		e.setEtat(EnumHelper.coreToWeb(etat.getEtat()));
		e.setDateObtention(DataHelper.coreToWeb(getDateObtentionFieldContent(etat)));
		e.setDateAnnulation(DataHelper.coreToWeb(etat.getAnnulationDate()));
		return e;
	}

	/**
	 * [UNIREG-3407] Pour les états de sommation, c'est la date de l'envoi du courrier qu'il faut renvoyer
	 *
	 * @param etatDeclaration etat de la déclaration
	 * @return valeur de la date à mettre dans le champ {@link ch.vd.uniregctb.webservices.tiers3.EtatDeclaration#dateObtention}
	 */
	private static RegDate getDateObtentionFieldContent(ch.vd.uniregctb.declaration.EtatDeclaration etatDeclaration) {
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

	public static Declaration clone(Declaration d) {
		if (d == null) {
			return null;
		}
		final ArrayList<EtatDeclaration> clonedEtats = cloneEtats(d.getEtats());
		if (d instanceof DeclarationImpotOrdinaire) {
			return new DeclarationImpotOrdinaire(d.getId(), d.getDateDebut(), d.getDateFin(), d.getDateAnnulation(), d.getPeriodeFiscale(), clonedEtats, ((DeclarationImpotOrdinaire) d).getNumero(),
					((DeclarationImpotOrdinaire) d).getTypeDocument(), ((DeclarationImpotOrdinaire) d).getNumeroOfsForGestion());
		}
		else if (d instanceof DeclarationImpotSource) {
			return new DeclarationImpotSource(d.getId(), d.getDateDebut(), d.getDateFin(), d.getDateAnnulation(), d.getPeriodeFiscale(), clonedEtats, ((DeclarationImpotSource) d).getPeriodicite(),
					((DeclarationImpotSource) d).getModeCommunication());
		}
		else {
			throw new IllegalArgumentException("Type de déclaration d'impôt inconnu = [" + d.getClass() + "]");
		}
	}

	private static ArrayList<EtatDeclaration> cloneEtats(List<EtatDeclaration> etats) {
		final ArrayList<EtatDeclaration> clonedEtats;
		if (etats == null) {
			clonedEtats = null;
		}
		else {
			clonedEtats = new ArrayList<EtatDeclaration>(etats.size());
			for (EtatDeclaration etat : etats) {
				clonedEtats.add(clone(etat));
			}
		}
		return clonedEtats;
	}

	private static EtatDeclaration clone(EtatDeclaration etat) {
		if (etat == null) {
			return null;
		}
		return new EtatDeclaration(etat.getEtat(), etat.getDateObtention(), etat.getDateAnnulation());
	}
}
