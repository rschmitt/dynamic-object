# dynamic-object

dynamic-object is a library that makes Clojure's powerful data modeling capabilities available to Java developers in an idiomatic way with minimal boilerplate. It reflects the belief that [values](http://www.infoq.com/presentations/Value-Values) should be immutable, cheap to specify, powerful to work with, and easy to convey to other processes.

Get it from [Maven](http://search.maven.org/#artifactdetails|com.github.rschmitt|dynamic-object|1.2.0|jar):

`com.github.rschmitt:dynamic-object:1.2.0`

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
* **Schema validation.** dynamic-object offers basic schema validation à la carte. The `validate` method will verify that all of an instance's fields are of the correct type, and that any fields annotated with `@Required` are not null.
* **Clojure metadata.** Fields that are annotated with `@Meta` are stored internally as [Clojure metadata](http://clojure.org/metadata). These fields allow data to be annotated in arbitrary ways without actually changing the structure or semantics of the data itself. Metadata does not affect equality: two objects that differ only in metadata are considered equal.
* **User-defined methods.** A `DynamicObject` type can declare arbitrary user-defined methods directly on the interface.
* **Copy-on-write support.** dynamic-object supports builder methods, which are similar to Lombok [`@Wither`](http://projectlombok.org/features/experimental/Wither.html) methods: they are used to create a clone of an instance that has a single field changed. They are backed by Clojure's `assoc` function, which is extremely performant, thanks to Clojure's sophisticated immutable data structures.
* **Transparent support for collections.** A `DynamicObject` can contain standard Java collections--namely `List`, `Set`, and `Map` from `java.util`. Collections can even contain `DynamicObject` instances.
* **Structural recursion.** There are no artificial or arbitrary limits on nesting or recursion. The test suite includes an example of a serializable [`LinkedList`](https://github.com/rschmitt/dynamic-object/blob/master/src/test/java/com/github/rschmitt/dynamicobject/RecursionTest.java) implemented with `DynamicObject`.
* **A straightforward implementation.**
    * dynamic-object has no dependencies, other than Clojure itself.
    * dynamic-object is implemented entirely with Java's built-in reflection capabilities. There is no bytecode manipulation, no annotation processing, no AOP weaving.
    * dynamic-object calls into Clojure exclusively through Clojure 1.6's public Java API, and does not depend on the implementation details of the current version of Clojure.
* **Easy to work with.** The dynamic-object API has a very small surface area, consisting of a single-digit number of methods, three annotations, and two interfaces. Using dynamic-object productively does not require any new tools: there is no Vim plugin, no Emacs minor mode, no Eclipse update site, no Gradle plugin, no special test runner. dynamic-object works for you, not the other way around.

## Some More Examples

### Serialization and Deserialization

dynamic-object is designed with an emphasis on preserving Clojure's excellent support for transparent serialization and deserialization. Data is serialized to [Edn](https://github.com/edn-format/edn), Clojure's native data language. In addition to Edn's built-in data types (sets, maps, vectors, `#inst`, `#uuid`, and so forth), there is full support for reader tags, Edn's extension mechanism. This makes it possible to include any Java value class in a `DynamicObject` without compromising serializability or requiring any modifications to the class. This is done through the `EdnTranslator` mechanism.

For instance, suppose we have a legacy POJO:

```java
class DumbClass {
  private final long version;
  private final String str;

  // Constructor, getters, equals, hashCode, and toString omitted
}
```

We can plan to represent instances of this class in Edn as a map: `{:version 1, :str "a string"}`. (This choice is somewhat arbitrary; for instance, we could also use a tagged string.) We implement this translation through the `EdnTranslator` interface:

```java
class DumbClassTranslator implements EdnTranslator<DumbClass> {
  @Override
  public DumbClass read(Object obj) {
    DumbClassProxy proxy = DynamicObject.wrap(obj, DumbClassProxy.class);
    return new DumbClass(proxy.version(), proxy.str());
  }

  @Override
  public String write(DumbClass obj) {
    DumbClassProxy proxy = DynamicObject.newInstance(DumbClassProxy.class);
    proxy = proxy.str(obj.getStr());
    proxy = proxy.version(obj.getVersion());
    return DynamicObject.serialize(proxy);
  }

  @Override
  public String getTag() {
    return "MyDumbClass";
  }

  interface DumbClassProxy extends DynamicObject<DumbClassProxy> {
    long version();
    String str();

    DumbClassProxy version(long version);
    DumbClassProxy str(String str);
  }
}
```

The Edn reader gives us the map that was tagged; we can wrap this map in an intermediate `DynamicObject` to use as a kind of deserialization proxy. This makes it unnecessary to call Clojure directly when reading, or to manually build an Edn string when writing.

The last step is to register the translator:

```java
DynamicObject.registerType(DumbClass.class, new DumbClassTranslator());
```

The POJO type is now fully interoperable with `DynamicObject` and Edn serialization:

```java
DumbClassHolder holder = DynamicObject.deserialize("{:dumb [#MyDumbClass{:version 1, :str \"str\"}]}", DumbClassHolder.class);
assertEquals(new DumbClass(1, "str"), holder.dumb().get(0));
```

### Schema Validation

Traditional object mappers pretend to transparently put static types on the wire. As a consequence, it is difficult to get them to accept any data that does not exactly match the object type they are expecting to see. For instance, if they see an unknown field, they will likely discard it, or they might just throw an exception, causing deserialization to fail altogether. The problem is usually not obvious right away; generally, it only becomes obvious once the software is running in production and needs to accommodate changes.

dynamic-object takes a completely different approach in which deserialization and validation are decoupled into two separate phases, each of which is independently available to the user. Any well-formed Edn data can be deserialized into any given `DynamicObject` type, and all of the data that was present on the wire will be preserved in its entirety in memory. Validation of the data can then proceed as a separate step. (Note that validation can also be performed on objects that were created using builders, rather than deserialized. This can be a way to ensure that none of the `@Required` fields were overlooked during construction.)

Validation checks that all `@Required` fields are present (they must not be null), and that all of the types are correct. Successful validation is a guarantee that any getter method can be invoked without resulting in a `ClassCastException`. For example, consider the following type:

```java
interface Validated extends DynamicObject<Validated> {
  @Required int x();
  @Required int y();
  String str();
}
```

After deserializing instances of this type, we can use validation to ensure that they are correct. The `validate()` method will throw an exception if an instance doesn't validate. The exception message will give a detailed description of what went wrong:

```java
DynamicObject.deserialize("{}", Validated.class).validate();
//  Exception in thread "main" java.lang.IllegalStateException: The following @Required fields were missing: x, y

DynamicObject.deserialize("{:x 1, :y 2, :str 3}", Validated.class).validate();
//  Exception in thread "main" java.lang.IllegalStateException: The following fields had the wrong type:
//    str (expected String, got Long)

DynamicObject.deserialize("{:x 1, :y 2, :str \"hello\"}", Validated.class).validate();
//  Success!
```

It is possible to add custom validation logic to a type by implementing `validate()` as a custom method. For example:

```java
interface Custom extends DynamicObject<Custom> {
  @Required int oddsOnly();

  @Override
  default Custom validate() {
    if (oddsOnly() % 2 == 0)
      throw new IllegalStateException("Odd number expected");
    return this;
  }
}
```

This validation logic will be run in addition to the standard validation checks:

```java
DynamicObject.deserialize("{:oddsOnly 4}", Custom.class).validate();
//  Exception in thread "main" java.lang.IllegalStateException: Odd number expected

DynamicObject.deserialize("{:oddsOnly nil}", Custom.class).validate();
//  Exception in thread "main" java.lang.IllegalStateException: The following @Required fields were missing: oddsOnly

DynamicObject.deserialize("{:oddsOnly 5}", Custom.class).validate();
//  Success!
```

### Persistent Modification

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

### Metadata

dynamic-object allows direct access to Clojure's metadata facilities with the `@Meta` annotation. This allows information to be annotated in arbitrary ways without this information being part of the data itself. For example, if you're using dynamic-object to communicate across processes using a distributed queue like [SQS](http://aws.amazon.com/sqs/), metadata is a great place to stash information about the messages themselves, such as the [message receipt handle](http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/ImportantIdentifiers.html):

```java
interface WorkerJob extends DynamicObject<WorkerJob> {
  UUID jobId();
  String inputLocation();
  // and so forth
  @Meta long messageAgeInSeconds(); // Use this for visibility purposes (e.g. are we falling behind?)
  @Meta String messageReceiptHandle(); // Use this later to delete the message once the job is done

  // The metadata fields can be set using builder methods:
  WorkerJob messageAgeInSeconds(long seconds);
  WorkerJob messageReceiptHandle(String handle);
  // Note that no redundant @Meta annotation is required on builders
}
```

Remember that metadata is never serialized, and is ignored for purposes of equality.

### User-defined Methods

Thanks to Java 8's [default methods](http://docs.oracle.com/javase/tutorial/java/IandI/defaultmethods.html), it is straightforward to declare custom methods on a `DynamicObject`, even though all `DynamicObject` types are interfaces. For example, we could extend the above `Album` example with an `AlbumCollection` type:

```java
public interface AlbumCollection extends DynamicObject<AlbumCollection> {
  Set<Album> albums();

  default int totalTracksInCollection() {
    return albums().stream()
                   .map(album -> album.tracks())
                   .reduce((x, y) -> x + y)
                   .get();
  }
}
```

### Custom Keys

dynamic-object does not have an elaborate system of conventions to map Java method names to Clojure map keys. By default, the name of the getter method is exactly the name of the keyword. `str()` maps to `:str`, and `myString()` maps to `:myString`, not `:my-string`. This default can be overridden with the `@Key` annotation:

```java
String camelCase(); // corresponds to the :camelCase field
@Key(":kebab-case") String kebabCase(); // corresponds to the :kebab-case field, as opposed to the default :kebabCase
```

This is particularly useful for Clojure interop, where kebab-case, rather than Java's camelCase, is idiomatic.

## Guidelines

* Always register a reader tag for any `DynamicObject` that will be serialized. This reader tag should be namespaced with some appropriate prefix (e.g. a Java package name), as all unprefixed reader tags are reserved for future use by the Edn specification.
* Always include a version number in data that will be serialized. This way, older consumers can check the version number and decline any messages that they are not capable of handling properly.
* Annotate required fields with `@Required` and call `validate()` to ensure that all required fields are present.
* Use [`java.util.Optional`](http://docs.oracle.com/javase/8/docs/api/java/util/Optional.html) in your schema with fields that are not `@Required`. Internally, dynamic-object unwraps `Optional` values; they do not affect serialization, and they provide additional null safety by making it obvious (at the actual call site, not just the schema) that a given field might not be present.
  * Correspondingly, unboxed primitive fields should always be marked `@Required`, as they cannot be effectively checked for null. Optional fields should always use the boxed type.
* It is okay to submit a mutable collection such as a `java.util.ArrayList` to a `DynamicObject` builder method. Internally, all collection elements are copied to an immutable Clojure collection.
  * Similarly, all collection getter methods return an immutable persistent collection. Attempts to mutate these collections will result in an `UnsupportedOperationException`.
* Do not abuse user-defined methods. A [pure function](http://en.wikipedia.org/wiki/Pure_function) is often a good candidate for a custom method; anything else should be viewed with suspicion.

## Constraints and Limitations

* Only keyword keys are supported. Map entries that are keyed off of a different type (e.g. a symbol, a string, a vector) cannot be exposed through a `DynamicObject` schema, although they can still be transparently round tripped.
* dynamic-object only deals with values. The time semantics of Clojure--`atom`, `ref`, `agent` and so on--are currently considered out of scope for this library.
* There is currently no way to distinguish between an explicit null value and a missing entry. It is not at all clear that exposing this distinction would be a good idea.
* Since unboxed primitives cannot be null, any attempt to dereference an unboxed primitive field whose underlying value is null or missing will result in a `NullPointerException`.

## Developing

dynamic-object should work out-of-the-box with [IntelliJ 14](http://www.jetbrains.com/idea/download/). The Community Edition is sufficient. You'll need [JDK8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) installed and configured as an SDK within IntelliJ. You will also need the Maven plugin for dependency resolution; this is generally included by default.

You can also run the build from the command line using `mvn package`. To just run the unit tests, use `mvn test`.

## Influences and Similar Ideas

* [Lombok](http://www.projectlombok.org/) is a boilerplate elimination tool for Java. It offers the excellent [`@Value`](http://projectlombok.org/features/Value.html) annotation, which helps to take the pain out of Java data modeling. Unfortunately, Lombok by itself does little to solve the problem of serialization/deserialization, and its implementation does horrible violence to the internals of the compiler.
* [Prismatic Schema](https://github.com/Prismatic/schema) is a Clojure library that offers declarative data validation and description in terms of "schemas."
* [core.typed](https://github.com/clojure/core.typed) is a pluggable type system for Clojure. Its concept of [heterogeneous maps](https://github.com/clojure/core.typed/wiki/Types#heterogeneous-maps) helped to clarify how Clojure's extremely general map type could be used effectively in a statically typed language like Java.
