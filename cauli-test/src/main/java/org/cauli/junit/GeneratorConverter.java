package org.cauli.junit;

/**
 * Created by celeskyking on 2014/4/23
 */
public interface GeneratorConverter {

    public <T  extends ParameterProvider>T convert(Class<T> clazz,FileGenerator fileGenerator);
}
