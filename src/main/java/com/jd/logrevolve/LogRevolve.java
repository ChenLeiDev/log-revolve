package com.jd.logrevolve;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface LogRevolve {

    String CanonicalName = "com.jd.logrevolve.LogRevolve";

}
