
package org.cauli.junit;

import org.cauli.junit.build.FrameworksBuilderFactory;
import org.junit.Test;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class Feeder extends BlockJUnit4ClassRunner {

	
	static {
        ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
    }
	
	private List<FrameworkMethod> children;
	
	public Feeder(Class<?> testClass) throws InitializationError {
		super(testClass);
	}

	@Override
	protected String testName(FrameworkMethod method) {
		return (method instanceof FrameworkMethodWithParameters ? method.toString() : super.testName(method));
	}

    protected void validateTestMethods(List<Throwable> errors) {
        validatePublicVoidMethods(Test.class, false, errors);
    }

    private void validatePublicVoidMethods(Class<? extends Annotation> annotation, boolean isStatic, List<Throwable> errors) {
        List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(annotation);
        for (FrameworkMethod eachTestMethod : methods)
            eachTestMethod.validatePublicVoid(isStatic, errors);
    }
	
	@Override
	protected List<FrameworkMethod> computeTestMethods() {
		if (children == null) {
			children = new ArrayList<FrameworkMethod>();
			TestClass testClass = getTestClass();
			children= FrameworksBuilderFactory.getInstance().getFrameworkBuilder().build(testClass);
		}
		return children;
	}



}
