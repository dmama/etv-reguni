package ch.vd.uniregctb.hibernate.interceptor;

import org.hibernate.EmptyInterceptor;

/**
 * Intercepteur vide qui ne fait rien. Il permet de désactiver la validation des tiers dans des cas très particuliers.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class HibernateFakeInterceptor extends EmptyInterceptor {

	private static final long serialVersionUID = 5487900097009341447L;
}
