package ch.vd.uniregctb.webservices.tiers3.data;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.webservices.tiers3.Declaration;
import ch.vd.uniregctb.webservices.tiers3.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.webservices.tiers3.DeclarationImpotSource;
import ch.vd.uniregctb.webservices.tiers3.EtatDeclaration;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

public class DeclarationBuilder {
	public static DeclarationImpotOrdinaire newDeclarationImpotOrdinaire(ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire declaration) {
		final DeclarationImpotOrdinaire d = new DeclarationImpotOrdinaire();
		fillDeclarationBase(d, declaration);
		d.setNumero(Long.valueOf(declaration.getNumero()));
		d.setTypeDocument(EnumHelper.coreToWeb(declaration.getTypeDeclaration()));
		d.setNumeroOfsForGestion(declaration.getNumeroOfsForGestion());
		return d;
	}

	public static DeclarationImpotSource newDeclarationImpotSource(ch.vd.uniregctb.declaration.DeclarationImpotSource declaration) {
		final DeclarationImpotSource d = new DeclarationImpotSource();
		fillDeclarationBase(d, declaration);
		d.setPeriodicite(EnumHelper.coreToWeb(declaration.getPeriodicite()));
		d.setModeCommunication(EnumHelper.coreToWeb(declaration.getModeCommunication()));
		return d;
	}

	private static void fillDeclarationBase(Declaration d, ch.vd.uniregctb.declaration.Declaration declaration) {
		d.setDateDebut(DataHelper.coreToWeb(declaration.getDateDebut()));
		d.setDateFin(DataHelper.coreToWeb(declaration.getDateFin()));
		d.setDateAnnulation(DataHelper.coreToWeb(declaration.getAnnulationDate()));
		d.setPeriodeFiscale(PeriodeFiscaleBuilder.newPeriodeFiscale(declaration.getPeriode()));
		for (ch.vd.uniregctb.declaration.EtatDeclaration etat : declaration.getEtats()) {
			d.getEtats().add(newEtatDeclaration(etat));
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
}
