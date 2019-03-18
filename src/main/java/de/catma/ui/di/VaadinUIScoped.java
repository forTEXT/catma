package de.catma.ui.di;
/*
  */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.inject.ScopeAnnotation;

/**
 * Apply this to implementation classes when you want one instance per UI.
 *
 * @see com.google.inject.Scopes#SINGLETON
 * @author crazybob@google.com (Bob Lee)
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ScopeAnnotation
public @interface VaadinUIScoped {

}
