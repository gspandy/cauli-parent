package org.cauli.junit.anno;

import java.lang.annotation.*;

/**
 * Created by celeskyking on 14-3-1
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface CauliRule {
}
