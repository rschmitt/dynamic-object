# dynamic-object

dynamic-object is a library that makes Clojure's powerful data modeling capabilities available to Java developers in an idiomatic way with minimal boilerplate. A `DynamicObject` is simply a reflective proxy around a Clojure `PersistentHashMap` that exposes map entries as fields. For example, maps with a structure like this:

```
{:artist "Meshuggah", :album "Chaosphere", :tracks 8, :year 1998}
```

can be represented with a `DynamicObject` schema declared like this:

```java
public interface Album extends DynamicObject<Album> {
  String artist();
  String album();
  int tracks();
  int year();
}
```

dynamic-object also supports `Set`, `List`, `Map`, custom key names, and composition with other schemas:

```java
public interface AlbumCollection extends DynamicObject<AlbumCollection> {
  Set<Album> albums();
  @Key(":favorite-album-title") String favoriteAlbumTitle();
}
```

## Serialization and deserialization

dynamic-object is designed with an emphasis on preserving Clojure's excellent support for transparent serialization and deserialization. Data is serialized to [Edn](https://github.com/edn-format/edn), Clojure's native data language. In addition to Edn's built-in data types (sets, maps, vectors, `#inst`, `#uuid`, and so forth), there is full support for reader tags, Edn's extension mechanism. This makes it possible to include any Java value class in a `DynamicObject` without compromising serializability or requiring any modifications to the class. This is done through the `EdnTranslator` mechanisms; see the [`TaggedReaderTest`](https://github.com/rschmitt/dynamic-object/blob/master/src/test/java/com/github/rschmitt/dynamicobject/TaggedReaderTest.java) or the [`AcceptanceTest`](https://github.com/rschmitt/dynamic-object/blob/master/src/test/java/com/github/rschmitt/dynamicobject/AcceptanceTest.java) for examples.

## Persistent modification

dynamic-object makes it easy to leverage Clojure's immutable persistent data structures, which use structural sharing to enable cheap copying and "modification." A `DynamicObject` can declare "wither" methods (inspired by Lombok's [`@Wither`](http://projectlombok.org/features/experimental/Wither.html) annotation); these map directly to `IPersistentMap#assoc`. For example:

```java
interface Buildable extends DynamicObject<Buildable> {
    String str();
    Buildable str(String str);
}

@Test
public void invokeBuilderMethod() {
  Buildable obj = DynamicObject.newInstance(Buildable.class).str("string");
  assertEquals("{:str \"string\"}", DynamicObject.serialize(obj));
}

```

## Todos

dynamic-object is currently an early prototype. There are a number of outstanding design questions and implementation tasks, such as:

* **User-defined methods.** While the ability to add arbitrary methods to a `DynamicObject` could easily be abused, there are many cases where it would be both harmless and idiomatic.
* **Validation routines.** One excellent property of a traditional Java value class is that they can be made both immutable and self-validating: the constructor can verify that the object is internally consistent before publishing it. Similar functionality should be made available to `DynamicObjects` without compromising the ability to incrementally build and modify existing objects.
* **`defrecord` interop.** Clojure's `defrecord` creates a type that can be handled like a generic map, but which also has a distinct identity, including its own reader tag. Although dynamic-object is mainly intended for near-pure Java codebases, it should be able to work with serialized records produced by Clojure code.
* **[`data.generators`](https://github.com/clojure/data.generators) support.** `DynamicObject` instances could be randomly generated for use as test data, which would facilitate generative testing in the style of [test.check](https://github.com/clojure/test.check) and [QuickCheck](http://www.haskell.org/haskellwiki/Introduction_to_QuickCheck2).
* **Clojure metadata.** It might be possible to expose Clojure's support for metadata in some limited way, e.g. as a `Map<String, String>`.

## Constraints and Limitations

* Only keyword keys are supported. Map entries that are keyed off of a different type (e.g. a symbol, a string, a vector) cannot be exposed through a `DynamicObject` schema, although they can still be transparently round tripped.

## Influences and similar ideas

* [Lombok](http://www.projectlombok.org/) is a boilerplate elimination tool for Java. It offers the excellent [`@Value`](http://projectlombok.org/features/Value.html) annotation, which helps to take the pain out of Java data modeling. Unfortunately, Lombok by itself does little to solve the problem of serialization/deserialization, and its implementation does horrible violence to the internals of the compiler, whereas dynamic-object is implemented entirely with Java's built-in reflection capabilities--no bytecode manipulation, no annotation processors, no IDE plugins, no Gradle task.
* [Prismatic Schema](https://github.com/Prismatic/schema) is a Clojure library that offers declarative data validation and description in terms of "schemas."
* [core.typed](https://github.com/clojure/core.typed) is a pluggable type system for Clojure. Its concept of [heterogeneous maps](https://github.com/clojure/core.typed/wiki/Types#heterogeneous-maps) helped to clarify how Clojure's extremely general map type could be used effectively in a statically typed language like Java.
