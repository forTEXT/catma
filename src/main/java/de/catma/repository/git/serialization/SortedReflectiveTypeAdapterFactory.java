package de.catma.repository.git.serialization;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.$Gson$Types;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.ObjectConstructor;
import com.google.gson.internal.Primitives;
import com.google.gson.internal.reflect.ReflectionAccessor;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public final class SortedReflectiveTypeAdapterFactory implements TypeAdapterFactory {
	
	final static class TypeAdapterRuntimeTypeWrapper<T> extends TypeAdapter<T> {
	  private final Gson context;
	  private final TypeAdapter<T> delegate;
	  private final Type type;

	  TypeAdapterRuntimeTypeWrapper(Gson context, TypeAdapter<T> delegate, Type type) {
	    this.context = context;
	    this.delegate = delegate;
	    this.type = type;
	  }

	  @Override
	  public T read(JsonReader in) throws IOException {
	    return delegate.read(in);
	  }

	  @SuppressWarnings({"rawtypes", "unchecked"})
	  @Override
	  public void write(JsonWriter out, T value) throws IOException {
	    // Order of preference for choosing type adapters
	    // First preference: a type adapter registered for the runtime type
	    // Second preference: a type adapter registered for the declared type
	    // Third preference: reflective type adapter for the runtime type (if it is a sub class of the declared type)
	    // Fourth preference: reflective type adapter for the declared type

	    TypeAdapter chosen = delegate;
	    Type runtimeType = getRuntimeTypeIfMoreSpecific(type, value);
	    if (runtimeType != type) {
	      TypeAdapter runtimeTypeAdapter = context.getAdapter(TypeToken.get(runtimeType));
	      if (!(runtimeTypeAdapter instanceof SortedReflectiveTypeAdapterFactory.Adapter)) {
	        // The user registered a type adapter for the runtime type, so we will use that
	        chosen = runtimeTypeAdapter;
	      } else if (!(delegate instanceof SortedReflectiveTypeAdapterFactory.Adapter)) {
	        // The user registered a type adapter for Base class, so we prefer it over the
	        // reflective type adapter for the runtime type
	        chosen = delegate;
	      } else {
	        // Use the type adapter for runtime type
	        chosen = runtimeTypeAdapter;
	      }
	    }
	    chosen.write(out, value);
	  }

	  /**
	   * Finds a compatible runtime type if it is more specific
	   */
	  private Type getRuntimeTypeIfMoreSpecific(Type type, Object value) {
	    if (value != null
	        && (type == Object.class || type instanceof TypeVariable<?> || type instanceof Class<?>)) {
	      type = value.getClass();
	    }
	    return type;
	  }
	}
	
	  private final ConstructorConstructor constructorConstructor;
	  private final FieldNamingStrategy fieldNamingPolicy;
	  private final Excluder excluder;
	  
	  private final ReflectionAccessor accessor = ReflectionAccessor.getInstance();

	  public SortedReflectiveTypeAdapterFactory(ConstructorConstructor constructorConstructor,
	      FieldNamingStrategy fieldNamingPolicy, Excluder excluder) {
	    this.constructorConstructor = constructorConstructor;
	    this.fieldNamingPolicy = fieldNamingPolicy;
	    this.excluder = excluder;
	  }

	  public boolean excludeField(Field f, boolean serialize) {
	    return excludeField(f, serialize, excluder);
	  }

	  static boolean excludeField(Field f, boolean serialize, Excluder excluder) {
	    return !excluder.excludeClass(f.getType(), serialize) && !excluder.excludeField(f, serialize);
	  }

	  /** first element holds the default name */
	  private List<String> getFieldNames(Field f) {
	    SerializedName annotation = f.getAnnotation(SerializedName.class);
	    if (annotation == null) {
	      String name = fieldNamingPolicy.translateName(f);
	      return Collections.singletonList(name);
	    }

	    String serializedName = annotation.value();
	    String[] alternates = annotation.alternate();
	    if (alternates.length == 0) {
	      return Collections.singletonList(serializedName);
	    }

	    List<String> fieldNames = new ArrayList<String>(alternates.length + 1);
	    fieldNames.add(serializedName);
	    for (String alternate : alternates) {
	      fieldNames.add(alternate);
	    }
	    return fieldNames;
	  }

	  @Override public <T> TypeAdapter<T> create(Gson gson, final TypeToken<T> type) {
	    Class<? super T> raw = type.getRawType();

	    if (!Object.class.isAssignableFrom(raw)) {
	      return null; // it's a primitive!
	    }
	    
	    //mpetris: we handle only our own types
	    if (!type.getRawType().getPackage().getName().startsWith("de.catma")) {
	    	return null;
	    }

	    ObjectConstructor<T> constructor = constructorConstructor.get(type);
	    return new Adapter<T>(constructor, getBoundFields(gson, type, raw));
	  }

	  private SortedReflectiveTypeAdapterFactory.BoundField createBoundField(
	      final Gson context, final Field field, final String name,
	      final TypeToken<?> fieldType, boolean serialize, boolean deserialize) {
	    final boolean isPrimitive = Primitives.isPrimitive(fieldType.getRawType());
	    // special casing primitives here saves ~5% on Android...
	    
	    // mpetris: we do not use the JsonAdapter annotation
//	    JsonAdapter annotation = field.getAnnotation(JsonAdapter.class);
	    TypeAdapter<?> mapped = null;
//	    if (annotation != null) {
//	      mapped = jsonAdapterFactory.getTypeAdapter(
//	          constructorConstructor, context, fieldType, annotation);
//	    }
	    final boolean jsonAdapterPresent = mapped != null;
	    if (mapped == null) mapped = context.getAdapter(fieldType);

	    final TypeAdapter<?> typeAdapter = mapped;
	    return new SortedReflectiveTypeAdapterFactory.BoundField(name, serialize, deserialize) {
	      @SuppressWarnings({"unchecked", "rawtypes"}) // the type adapter and field type always agree
	      @Override void write(JsonWriter writer, Object value)
	          throws IOException, IllegalAccessException {
	        Object fieldValue = field.get(value);
	        TypeAdapter t = jsonAdapterPresent ? typeAdapter
	            : new TypeAdapterRuntimeTypeWrapper(context, typeAdapter, fieldType.getType());
	        t.write(writer, fieldValue);
	      }
	      @Override void read(JsonReader reader, Object value)
	          throws IOException, IllegalAccessException {
	        Object fieldValue = typeAdapter.read(reader);
	        if (fieldValue != null || !isPrimitive) {
	          field.set(value, fieldValue);
	        }
	      }
	      @Override public boolean writeField(Object value) throws IOException, IllegalAccessException {
	        if (!serialized) return false;
	        Object fieldValue = field.get(value);
	        return fieldValue != value; // avoid recursion for example for Throwable.cause
	      }
	    };
	  }

	  private Map<String, BoundField> getBoundFields(Gson context, TypeToken<?> type, Class<?> raw) {
	    Map<String, BoundField> result = new LinkedHashMap<String, BoundField>();
	    if (raw.isInterface()) {
	      return result;
	    }

	    Type declaredType = type.getType();
	    while (raw != Object.class) {
	      Field[] fields = raw.getDeclaredFields();
	      for (Field field : fields) {
	        boolean serialize = excludeField(field, true);
	        boolean deserialize = excludeField(field, false);
	        if (!serialize && !deserialize) {
	          continue;
	        }
	        accessor.makeAccessible(field);
	        Type fieldType = $Gson$Types.resolve(type.getType(), raw, field.getGenericType());
	        List<String> fieldNames = getFieldNames(field);
	        BoundField previous = null;
	        for (int i = 0, size = fieldNames.size(); i < size; ++i) {
	          String name = fieldNames.get(i);
	          if (i != 0) serialize = false; // only serialize the default name
	          BoundField boundField = createBoundField(context, field, name,
	              TypeToken.get(fieldType), serialize, deserialize);
	          BoundField replaced = result.put(name, boundField);
	          if (previous == null) previous = replaced;
	        }
	        if (previous != null) {
	          throw new IllegalArgumentException(declaredType
	              + " declares multiple JSON fields named " + previous.name);
	        }
	      }
	      type = TypeToken.get($Gson$Types.resolve(type.getType(), raw, raw.getGenericSuperclass()));
	      raw = type.getRawType();
	    }
	    return result;
	  }

	  static abstract class BoundField {
	    final String name;
	    final boolean serialized;
	    final boolean deserialized;

	    protected BoundField(String name, boolean serialized, boolean deserialized) {
	      this.name = name;
	      this.serialized = serialized;
	      this.deserialized = deserialized;
	    }
	    abstract boolean writeField(Object value) throws IOException, IllegalAccessException;
	    abstract void write(JsonWriter writer, Object value) throws IOException, IllegalAccessException;
	    abstract void read(JsonReader reader, Object value) throws IOException, IllegalAccessException;
	  }

	  public static final class Adapter<T> extends TypeAdapter<T> {
	    private final ObjectConstructor<T> constructor;
	    private final Map<String, BoundField> boundFields;

	    Adapter(ObjectConstructor<T> constructor, Map<String, BoundField> boundFields) {
	      this.constructor = constructor;
	      this.boundFields = boundFields;
	    }

	    @Override public T read(JsonReader in) throws IOException {
	      if (in.peek() == JsonToken.NULL) {
	        in.nextNull();
	        return null;
	      }

	      T instance = constructor.construct();

	      try {
	        in.beginObject();
	        while (in.hasNext()) {
	          String name = in.nextName();
	          BoundField field = boundFields.get(name);
	          if (field == null || !field.deserialized) {
	            in.skipValue();
	          } else {
	            field.read(in, instance);
	          }
	        }
	      } catch (IllegalStateException e) {
	        throw new JsonSyntaxException(e);
	      } catch (IllegalAccessException e) {
	        throw new AssertionError(e);
	      }
	      in.endObject();
	      return instance;
	    }

	    @Override public void write(JsonWriter out, T value) throws IOException {
	      if (value == null) {
	        out.nullValue();
	        return;
	      }

	      out.beginObject();
	      Collection<BoundField> sortedFields = boundFields.values()
	        		.stream()
	        		.sorted((bf1,bf2) -> bf1.name.compareTo(bf2.name)) // mpetris we need a fixed order to minimize git conflicts and to support git diff 
	        		.collect(Collectors.toList());
	      try {
	        for (BoundField boundField : sortedFields) {
	          if (boundField.writeField(value)) {
	            out.name(boundField.name);
	            boundField.write(out, value);
	          }
	        }
	      } catch (IllegalAccessException e) {
	        throw new AssertionError(e);
	      }
	      out.endObject();
	    }
	  }
	}

