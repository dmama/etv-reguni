package ch.vd.uniregctb.security;

import java.util.List;

import org.springframework.security.core.AuthenticationException;

import ch.vd.infrastructure.model.CollectiviteAdministrative;

public class MultipleOIDFoundException extends AuthenticationException {

	private static final long serialVersionUID = 7716081770572018516L;
	private final List<CollectiviteAdministrative> collectivites;

	public MultipleOIDFoundException(String msg, List<CollectiviteAdministrative> collectivites) {
		super(msg);
		this.collectivites = collectivites;
	}

	public List<CollectiviteAdministrative> getCollectivites() {
		return collectivites;
	}
}
