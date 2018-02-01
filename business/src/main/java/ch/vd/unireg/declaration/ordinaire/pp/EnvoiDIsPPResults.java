package ch.vd.uniregctb.declaration.ordinaire.pp;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.metier.assujettissement.CategorieEnvoiDIPP;
import ch.vd.uniregctb.tiers.TiersService;

public class EnvoiDIsPPResults extends AbstractEnvoiDIsPPResults<EnvoiDIsPPResults> {

	public EnvoiDIsPPResults(int annee, CategorieEnvoiDIPP categorie, RegDate dateTraitement, int nbMax, @Nullable Long noCtbMin,
	                         @Nullable Long noCtbMax, @Nullable RegDate dateExclureDecede, int nbThreads, TiersService tiersService,
	                         AdresseService adresseService) {
		super(annee, categorie, dateTraitement, nbMax, noCtbMin, noCtbMax, dateExclureDecede, nbThreads, tiersService, adresseService);
	}
}
