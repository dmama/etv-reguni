package ch.vd.uniregctb.migration.pm.historizer.extractor;

import java.util.function.Function;

@FunctionalInterface
public interface Extractor<T, R> extends Function<T, R> {

}
