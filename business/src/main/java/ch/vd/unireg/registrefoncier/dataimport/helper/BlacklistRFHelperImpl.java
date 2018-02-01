package ch.vd.uniregctb.registrefoncier.dataimport.helper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BlacklistRFHelperImpl implements BlacklistRFHelper {

	// [SIFISC-22366] On ignore l'unique immeuble sur la commune 'Autre Registre foncier'.
	// [SIFISC-24611] On ignore les 9 immeubles sur les communes Neuchâteloises de Montalchez et Fresens
	private Set<String> blacklistedImmeubles;

	public void setBlacklistedImmeubles(String[] blacklistedImmeubles) {
		this.blacklistedImmeubles = new HashSet<>(Arrays.asList(blacklistedImmeubles));
	}

	@Override
	public boolean isBlacklisted(String idRF) {
		return blacklistedImmeubles.contains(idRF);
	}
}
