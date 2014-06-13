# dynamic-object

dynamic-object is a library that makes Clojure's powerful data modeling capabilities available to Java developers in an idiomatic way with minimal boilerplate. It reflects the belief that [values](http://www.infoq.com/presentations/Value-Values) should be immutable, cheap to specify, powerful to work with, and easy to convey to other processes.

## A Simple Example

Consider a Clojure map that describes an album:

```
{:artist "Meshuggah", :album "Chaosphere", :tracks 8, :year 1998}
```

This data can be made directly available to Java code with a `DynamicObject` schema declared like this:

```java
public interface Album extends DynamicObject<Album> {
  String artist();
  String album();
  int tracks();
  int year();
}
```

The data is deserialized like so:

```java
String edn = "{:artist \"Meshuggah\", :album \"Chaosphere\", :tracks 8, :year 1998}";
Album album = DynamicObject.deserialize(edn, Album.class);
```

## Features

* **Serialization.** Thanks to Clojure and the Edn data language, serialization is simple, reliable, extensible, and language-agnostic.
* **Immutability.** Because `DynamicObjects` are built out of [Clojure's data structures](http://clojure.org/data_structures), they are not just immutable and thread-safe, but also [persistent](http://en.wikipedia.org/wiki/Persistent_data_structure), which makes copying and modification cheap.
* **Composability.** `DynamicObjects` compose correctly. Different types can be aggregated without losing serializability, equality semantics, or any of the other benefits of Clojure data ([example](https://github.com/rschmitt/dynamic-object/blob/master/src/test/java/com/github/rschmitt/dynamicobject/AcceptanceTest.java)).
* **Copy-on-write support.** dynamic-object supports builder methods, which are similar to Lombok [`@Wither`](http://projectlombok.org/features/experimental/Wither.html) methods: they are used to create a clone of an instance that has a single field changed. They are backed by Clojure's `assoc` function, which is extremely performant, thanks to Clojure's sophisticated immutable data structures.
* **Transparent support for collections.** A `DynamicObject` can contain standard Java collections--namely `List`, `Set`, and `Map` from `java.util`. Collections can even contain `DynamicObject` instances.
* **Structural recursion.** There are no artificial or arbitrary limits on nesting or recursion. The test suite includes an example of a serializable [`LinkedList`](https://github.com/rschmitt/dynamic-object/blob/master/src/test/java/com/github/rschmitt/dynamicobject/RecursionTest.java) implemented with `DynamicObject`.
* **Clojure metadata.** Fields that are annotated with `@Meta` are stored internally as [Clojure metadata](http://clojure.org/metadata). These fields allow data to be annotated in arbitrary ways without actually changing the structure or semantics of the data itself. Metadata does not affect equality: two objects that differ only in metadata are considered equal.
* **A straightforward implementation.**
    * dynamic-object has no dependencies, other than Clojure itself.
    * dynamic-object is implemented entirely with Java's built-in reflection capabilities. There is no bytecode manipulation, no annotation processing, no AOP weaving.
    * dynamic-object calls into Clojure exclusively through Clojure 1.6's public Java API, and does not depend on the implementation details of the current version of Clojure.
* **Easy to work with.** The dynamic-object API has a very small surface area, consisting of a single-digit number of methods, two annotations, and two interfaces. Using dynamic-object productively does not require any new tools: there is no Vim plugin, no Emacs minor mode, no Eclipse update site, no Gradle plugin, no special test runner. dynamic-object works for you, not the other way around.

## Serialization and Deserialization

dynamic-object is designed with an emphasis on preserving Clojure's excellent support for transparent serialization and deserialization. Data is serialized to [Edn](https://github.com/edn-format/edn), Clojure's native data language. In addition to Edn's built-in data types (sets, maps, vectors, `#inst`, `#uuid`, and so forth), there is full support for reader tags, Edn's extension mechanism. This makes it possible to include any Java value class in a `DynamicObject` without compromising serializability or requiring any modifications to the class. This is done through the `EdnTranslator` mechanism; see the [`TaggedReaderTest`](https://github.com/rschmitt/dynamic-object/blob/master/src/test/java/com/github/rschmitt/dynamicobject/TaggedReaderTest.java) or the [`AcceptanceTest`](https://github.com/rschmitt/dynamic-object/blob/master/src/test/java/com/github/rschmitt/dynamicobject/AcceptanceTest.java) for examples.

## Persistent Modification

dynamic-object makes it easy to leverage Clojure's immutable persistent data structures, which use structural sharing to enable cheap copying and "modification." A `DynamicObject` can declare builder methods, which are backed by [`assoc`](http://clojure.github.io/clojure/clojure.core-api.html#clojure.core/assoc). For example:

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

## Constraints and Limitations

* Only keyword keys are supported. Map entries that are keyed off of a different type (e.g. a symbol, a string, a vector) cannot be exposed through a `DynamicObject` schema, although they can still be transparently round tripped.
* dynamic-object only deals with values. The time semantics of Clojure--`atom`, `ref`, `agent` and so on--are currently considered out of scope for this library.
* There is currently no way to distinguish between an explicit null value and a missing entry. It is not at all clear that exposing this distinction would be a good idea.
* Since unboxed primitives cannot be null, any attempt to dereference an unboxed primitive field whose underlying value is null or missing will result in a `NullPointerException`.

## Todos

dynamic-object is currently in beta, and the API is still subject to change. There are a number of outstanding design questions and implementation tasks, such as:

* **Schema validation.** dynamic-object should provide basic validation functionality, such as checking that all mandatory fields are present and all types are correct. However, this should be exposed as a separate instance method call, and not complected with deserialization, as it would be in most object-mapping frameworks.
* **Instance initialization.** Currently, `DynamicObjects` created through calls to `newInstance` are completely blank; all fields are missing by default, rather than explicitly null. This is different from the behavior of `defrecord`, which will initialize all fields to `nil` if an empty map is passed to the constructor, and which will also initialize missing fields to `nil` upon deserialization.
* **Arbitrary precision numbers.** These are actually required by the Edn format specification, and should be exposed accordingly as `BigInteger` (for the `N`-suffixed numbers) and `BigDecimal` (for the `M`-suffixed numbers). Currently only `BigDecimal` is supported.
* **Symbols.** The Edn format specification also calls out symbols, which "are used to represent identifiers, and should map to something other than strings, if possible." How to expose symbols to Java code, or whether to do so at all, is a problem that is going to require some [hammock time](http://www.youtube.com/watch?v=f84n5oFoZBc).
* **[`data.generators`](https://github.com/clojure/data.generators) support.** `DynamicObject` instances could be randomly generated for use as test data, which would facilitate generative testing in the style of [test.check](https://github.com/clojure/test.check) and [QuickCheck](http://www.haskell.org/haskellwiki/Introduction_to_QuickCheck2).
* **Datomic integration.** Datomic can be used from Java, but doing so requires the use of raw `Collection` types. There are worse things in life, such as ORM libraries, but maybe this situation could be improved upon.

## Developing

dynamic-object should work out-of-the-box with [IntelliJ 13](http://www.jetbrains.com/idea/download/). The Community Edition is sufficient. You'll need [JDK8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) installed and configured as an SDK within IntelliJ. You will also need the Gradle plugin for dependency resolution; this is generally included by default.

You can also invoke Gradle directly with `./gradlew build`.

## Influences and Similar Ideas

* [Lombok](http://www.projectlombok.org/) is a boilerplate elimination tool for Java. It offers the excellent [`@Value`](http://projectlombok.org/features/Value.html) annotation, which helps to take the pain out of Java data modeling. Unfortunately, Lombok by itself does little to solve the problem of serialization/deserialization, and its implementation does horrible violence to the internals of the compiler.
* [Prismatic Schema](https://github.com/Prismatic/schema) is a Clojure library that offers declarative data validation and description in terms of "schemas."
* [core.typed](https://github.com/clojure/core.typed) is a pluggable type system for Clojure. Its concept of [heterogeneous maps](https://github.com/clojure/core.typed/wiki/Types#heterogeneous-maps) helped to clarify how Clojure's extremely general map type could be used effectively in a statically typed language like Java.
