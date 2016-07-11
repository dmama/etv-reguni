package ch.vd.uniregctb.migration.pm.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Factory qui fournit une implémentation de l'interface {@link TiersService} qui ne délègue
 * que certains appels à l'implémentation réelle et explose ({@link NotImplementedException}) pour les autres
 * (parce qu'en fait, la véritable implémentation du TiersService est assez lourde à mettre en place, et qu'on veut contrôler ce qu'on utilise dans la migration)
 */
public class TiersServiceFactory implements FactoryBean<TiersService>, InitializingBean {

	private TiersService target;
	private Set<String> supportedMethodNames;
	private TiersService proxy;

	public void setTarget(TiersService target) {
		this.target = target;
	}

	public void setSupportedMethodNames(Set<String> supportedMethodNames) {
		this.supportedMethodNames = supportedMethodNames;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.proxy = (TiersService) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { TiersService.class }, new Handler());
	}

	@Override
	public TiersService getObject() throws Exception {
		return proxy;
	}

	@Override
	public Class<TiersService> getObjectType() {
		return TiersService.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	private class Handler implements InvocationHandler {
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (supportedMethodNames != null && supportedMethodNames.contains(method.getName())) {
				try {
					return method.invoke(target, args);
				}
				catch (InvocationTargetException e) {
					throw e.getTargetException();
				}
			}
			throw new NotImplementedException("La méthode '" + method.getName() + "' n'est pas actuellement supportée...");
		}
	}
}
