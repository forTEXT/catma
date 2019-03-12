package de.catma.ui.di;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Scopes;
import com.vaadin.ui.UI;

import de.catma.ui.CatmaApplication;


public final class VaadinUIScope implements Scope {
	
	/** A sentinel attribute value representing null. */
	enum NullObject {
		INSTANCE
	}
	
	public static final Scope VAADINUI = new VaadinUIScope();
	
	@Override
    public <T> Provider<T> scope(final Key<T> key, final Provider<T> creator) {
      final String name = key.toString();
      return new Provider<T>() {
        @Override
        public T get() {
          CatmaApplication catmaApp = (CatmaApplication)UI.getCurrent();
          
          synchronized (catmaApp) {
            Object obj = catmaApp.getAttribute(name);
            if (NullObject.INSTANCE == obj) {
              return null;
            }
            @SuppressWarnings("unchecked")
            T t = (T) obj;
            if (t == null) {
              t = creator.get();
              if (!Scopes.isCircularProxy(t)) {
            	  catmaApp.setAttribute(name, (t != null) ? t : NullObject.INSTANCE);
              }
            }
            return t;
          }
        }

        @Override
        public String toString() {
          return String.format("%s[%s]", creator,"" );
        }
      };
    }

    @Override
    public String toString() {
      return "ServletScopes.SESSION";
    }
  }
