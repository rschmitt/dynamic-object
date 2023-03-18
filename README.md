[![Build Status](http://img.shields.io/travis/rschmitt/dynamic-object.svg)](https://travis-ci.org/rschmitt/dynamic-object)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.rschmitt/dynamic-object.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.rschmitt/dynamic-object)
[![License: CC0-1.0](https://img.shields.io/badge/License-CC0%201.0-lightgrey.svg)](http://creativecommons.org/publicdomain/zero/1.0/)

DynamicObject is a library that makes Clojure's powerful data modeling capabilities available to Java developers in an idiomatic way with minimal boilerplate. It reflects the belief that [values](http://www.infoq.com/presentations/Value-Values) should be immutable, cheap to specify, powerful to work with, and easy to convey to other processes. Browse the Javadoc [online](http://rschmitt.github.io/dynamic-object/javadoc/).

## The Problem

The Java programming language is flawlessly incompetent at representing data. Classes are the main programming language construct in Java, and the entire point of classes is to *combine* data and code such that data is "encapsulated" and can only be accessed through an operational interface, if at all. Creating a class that does nothing but hold data requires the creation of *pages* of boilerplate--getters, setters, constructors, `clone`, `equals`, `hashCode`, `toString`, and so on. Not only is this labor-intensive to do by hand, it's surprisingly error prone: copy and paste can result in getters/setters touching the wrong fields, methods like `equals` can be overlooked when a new field is added, pervasive unmoderated mutability is the default, the `equals`/`hashCode`/`compareTo` contract is far more subtle than most realize, implementation inheritance is almost intractably complex, and concurrency and serialization have to be figured out from scratch.

There have been countless attempts to address various subsets (or supersets) of these problems: IDE code generation, compile-time code generation, object mapping, test case generators, annotation processors, public fields, and so on. These tools are usually helpful in the right context, but they're really only workarounds for a fundamentally broken approach to working with data. Instead of trying to fix a broken approach, why not switch to a better one altogether?

Anyone who has used Clojure's [data structures](http://clojure.org/data_structures) knows that there is a night-and-day difference between the Java and Clojure approaches to modeling data, and that Clojure's approach yields dramatically more leverage. One reason for Clojure's flexibility and power in working with data is its philosophy that *data belongs in maps*. Immutable persistent maps can be safely shared, compose well, can be sent to other programs (serialized and deserialized), are expressive enough to model almost anything, and can be generically processed by a collections library, which makes for excellent code reuse. Unfortunately, Clojure is radically different from what most developers are familiar with, in both superficial and profound ways. As a consequence, Clojure adoption is usually an uphill battle at best, and outright impossible at worst. But is it possible to get the benefits of Clojure without writing a line of Clojure code?

Because Clojure targets the JVM, it is possible for Java shops to use Clojure data without actually writing any Clojure code. However, there is a significant impedance mismatch between the two languages, and trying to call Clojure directly from Java results in a number of ergonomic problems, such as loss of type information, pervasive downcasting, and a Java API that is largely internal and undocumented. The goal of DynamicObject is to bridge the gap between the two languages and retain the advantages of both.

## A Simple Example

Consider a Clojure map that describes an album:

```clojure
{:artist "Meshuggah", :album "Chaosphere", :tracks 8, :year 1998}
```

Getting access to this data from Java is kind of a mess if you go directly through the Clojure runtime. It might look something like this:

```java
String edn = "{:artist \"Meshuggah\", :album \"Chaosphere\", :tracks 8, :year 1998}";
Map albumDescription = (Map) Clojure.var("clojure.edn/read-string").invoke(edn);
String album = (String) albumDescription.get(Clojure.read(":album"));
```

This is where DynamicObject comes in. The first thing to do is to create an `Album` type:

```java
public interface Album extends DynamicObject<Album> {
  @Key(":artist") String getArtist();
  @Key(":album")  String getAlbum();
  @Key(":tracks") int getTracks();
  @Key(":year")   int getYear();

  @Key(":artist") Album withArtist(String artist);
  @Key(":album")  Album withAlbum(String album);
  @Key(":tracks") Album withTracks(int tracks);
  @Key(":year")   Album withYear(int year);
}
```

The purpose of this type is not to create a class that laboriously defines operations on data, but rather to establish a *schema* that tells DynamicObject which types are associated with each key. With DynamicObject, the above deserialization example looks like this:

```java
String edn = "{:artist \"Meshuggah\", :album \"Chaosphere\", :tracks 8, :year 1998}";
Album album = DynamicObject.deserialize(edn, Album.class);
String artist = album.getArtist();
assertEquals("Meshuggah", artist);
```

DynamicObjects can also be constructed in-memory through the use of builder methods (prefixed with `with-` in the above schema).

```java
Album album = DynamicObject.newInstance(Album.class)
                           .withArtist("Meshuggah")
                           .withAlbum("Chaosphere")
                           .withTracks(8)
                           .withYear(1998);
album.prettyPrint();
// {:year 1998, :tracks 8, :album "Chaosphere", :artist "Meshuggah"}
```

## Features

* **Serialization.** Thanks to Clojure and the Edn data language, serialization is simple, reliable, extensible, and language-agnostic.
* **Binary serialization.** In addition to the human-readable Edn format, DynamicObject now offers full support for [Fressian](https://github.com/Datomic/fressian/wiki/Rationale), a self-describing, high-performance, language-independent binary data encoding.
* **Immutability.** Because `DynamicObjects` are built out of [Clojure's data structures](http://clojure.org/data_structures), they are not just immutable and thread-safe, but also [persistent](http://en.wikipedia.org/wiki/Persistent_data_structure), which makes copying and modification cheap.
* **Composability.** DynamicObjects compose correctly. Different types can be aggregated without losing serializability, equality semantics, or any of the other benefits of Clojure data. DynamicObjects even support structural recursion, such as this [example](https://github.com/rschmitt/dynamic-object/blob/master/src/test/java/com/github/rschmitt/dynamicobject/RecursionTest.java) of a linked list implemented as a DynamicObject.
* **Schema validation.** DynamicObject offers basic schema validation à la carte. The `validate` method will verify that all of an instance's fields are of the correct type, and that any fields annotated with `@Required` are not null. Additionally, validation can be extended by the end user in order to validate arbitrary constraints.
* **Clojure metadata.** Fields that are annotated with `@Meta` are stored internally as [Clojure metadata](http://clojure.org/metadata). These fields allow data to be annotated in arbitrary ways without actually changing the structure or semantics of the data itself. Metadata does not affect equality: two objects that differ only in metadata are considered equal.
* **User-defined methods.** A DynamicObject can declare methods just like an ordinary class can. This is particularly helpful for supporting high-quality code completion.
* **Copy-on-write support.** DynamicObject supports builder methods, which are similar to Lombok [`@Wither`](http://projectlombok.org/features/experimental/Wither.html) methods: they are used to create a clone of an instance that has a single field changed. They are backed by Clojure's `assoc` function, which is extremely performant, thanks to Clojure's sophisticated immutable data structures.
* **Easy to work with.** The DynamicObject API has a very small surface area, and using DynamicObject productively does not require any new tools: there is no Vim plugin, no Emacs minor mode, no Eclipse update site, no Gradle plugin, no special test runner. DynamicObject works for you, not the other way around.
* **Seamless Clojure interop.** DynamicObjects implement all of Clojure's map abstractions, such as `IPersistentMap`. This means that DynamicObjects have excellent ergonomics in both languages: the same DynamicObject instance looks and works like a Clojure map or record from Clojure code, and like an immutable POJO from Java code. Advanced functions from Clojure's collections library, such as `update-in` and `diff`, work exactly as expected.

## Some More Examples

### Creating DynamicObject types

A DynamicObject type is a Java interface. The simplest possible DynamicObject type is this:

```java
public interface Empty extends DynamicObject<Empty> {}
```

Although this type doesn't declare any particular fields, it is still usable as a map. For example:

```java
Empty empty = DynamicObject.deserialize("{144 233}", Empty.class);

assertTrue(empty instanceof Map);
assertEquals(Long.valueOf(233), empty.get(144));
assertEquals(1, empty.size());
```

In addition to the interface itself, it is strongly recommended that reader tags be created for each DynamicObject type that will be serialized. Otherwise, serialized DynamicObjects look indistinguishable from regular maps, and the full type information cannot be recovered without some additional out-of-band information (such as the second argument to `deserialize`).

```java
public interface T1 extends DynamicObject<T1> {}
public interface T2 extends DynamicObject<T2> {}

DynamicObject.registerTag(T1.class, "T1");
DynamicObject.registerTag(T2.class, "T2");

// Note the lack of type hints given to deserialize:
assertTrue(DynamicObject.deserialize("#T1{}", Object.class) instanceof T1);
assertTrue(DynamicObject.deserialize("#T2{}", Object.class) instanceof T2);
```

### Extending Edn

In addition to Edn's built-in data types (sets, maps, vectors, `#inst`, `#uuid`, and so forth), DynamicObject offers full support for reader tags, Edn's extension mechanism. This makes it possible to include any Java value class in a `DynamicObject` without compromising serializability or requiring any modifications to the class. This is done through the `EdnTranslator` mechanism.

For instance, consider the following Edn data:

```clojure
{:name "Mike Jones",
 :phone #phonenumber "(330) 281-8004"}
```

The `:name` field is an ordinary string, but the `:phone` field is tagged. When we deserialize this data, we don't want the phone number to be returned as a string, but rather as an instance of the following type:

```java
public class PhoneNumber {
  public final String areaCode;
  public final String firstThree;
  public final String lastFour;

  // constructor, etc omitted
}
```

To do this, we'll define a class that translates this type to and from Edn:

```java
public class PhoneNumberTranslator implements EdnTranslator<PhoneNumber> {
  // Translate the tagged Edn data (in this case, a String) to our actual type.
  public PhoneNumber read(Object obj) {
    String str = (String) obj;
    Pattern pattern = Pattern.compile("\\((\\d+)\\) (\\d+)-(\\d+)");
    Matcher matcher = pattern.matcher(str);
    if (!matcher.matches())
      throw new IllegalArgumentException("Malformed phone number: " + str);
    return new PhoneNumber(matcher.group(1), matcher.group(2), matcher.group(3));
  }

  // Return an Edn string representing the serialized phone number.
  public String write(PhoneNumber phoneNumber) {
    return String.format("\"(%s) %s-%s\"",
      phoneNumber.areaCode, phoneNumber.firstThree, phoneNumber.lastFour);
  }

  // Return the reader tag associated with this type.
  public String getTag() {
    return "phonenumber";
  }
}
```

Finally, we'll register this translator with DynamicObject:

```java
DynamicObject.registerType(PhoneNumber.class, new PhoneNumberTranslator());
```

The `PhoneNumber` type is now fully interoperable with DynamicObject and Edn serialization. It can even be deserialized à la carte:

```java
PhoneNumber actual = DynamicObject.deserialize("#phonenumber \"(330) 281-8004\"", PhoneNumber.class);
PhoneNumber expected = new PhoneNumber("330", "281", "8004");
assertEquals(expected, actual);
```

### Fressian serialization

DynamicObject exposes Fressian's `FressianReader` and `FressianWriter` classes directly, in order to maximize flexibility. Instances of these classes are obtained by calling `createFressianReader` and `createFressianWriter`. These methods return readers and writers that understand:

* all of the basic Fressian and data.fressian types
* extension types that have been registered with DynamicObject
* DynamicObject types that have a registered reader tag

Convenience methods are also offered: `toFressianByteArray` takes an Object and returns a byte array, and `fromFressianByteArray` does the reverse.

Conceptually, extending Fressian is no different from extending Edn. The `PhoneNumber` class from above can be translated to and from Fressian like so:

```java
public class PhoneNumberEncoder implements ReadHandler, WriteHandler {
  @Override
  public Object read(Reader r, Object tag, int componentCount) throws IOException {
    String areaCode = (String) r.readObject();
    String firstThree = (String) r.readObject();
    String lastFour = (String) r.readObject();
    return new PhoneNumber(areaCode, firstThree, lastFour);
  }

  @Override
  public void write(Writer w, Object instance) throws IOException {
    PhoneNumber phoneNumber = (PhoneNumber) instance;
    // In addition to the reader tag, we must specify how many fields we will write.
    w.writeTag("phonenumber", 3);
    w.writeObject(phoneNumber.areaCode);
    w.writeObject(phoneNumber.firstThree);
    w.writeObject(phoneNumber.lastFour);
  }
}
```

As with Edn, this type must be registered with DynamicObject:

```java
PhoneNumberEncoder encoder = new PhoneNumberEncoder();
DynamicObject.registerType(PhoneNumber.class, "phonenumber", encoder, encoder);

PhoneNumber before = new PhoneNumber("123", "456", "7890");
byte[] byteArray = DynamicObject.toFressianByteArray(PhoneNumber);
PhoneNumber after = DynamicObject.fromFressianByteArray(byteArray);
assertEquals(before, after);
```

### Schema Validation

Traditional object mappers pretend to transparently put static types on the wire. As a consequence, it is difficult to get them to accept any data that does not exactly match the object type they are expecting to see. For instance, if they see an unknown field, they will likely discard it, or they might just throw an exception, causing deserialization to fail altogether. The problem is usually not obvious right away; generally, it only becomes obvious once the software is running in production and needs to accommodate changes.

DynamicObject takes a completely different approach in which deserialization and validation are decoupled into two separate phases, each of which is independently available to the user. Any well-formed Edn data can be deserialized into any given `DynamicObject` type, and all of the data that was present on the wire will be preserved in its entirety in memory. Validation of the data can then proceed as a separate step. (Note that validation can also be performed on objects that were created using builders, rather than deserialized. This can be a way to ensure that none of the `@Required` fields were overlooked during construction.)

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

DynamicObject makes it easy to leverage Clojure's immutable persistent data structures, which use structural sharing to enable cheap copying and "modification." A DynamicObject can declare builder methods, which are backed by [`assoc`](http://clojure.github.io/clojure/clojure.core-api.html#clojure.core/assoc). These methods perform a functional update of the data structure, returning an updated instance and leaving the current instance unchanged. For example:

```java
interface Buildable extends DynamicObject<Buildable> {
  @Key(":hello") String getHello();
  @Key(":hello") Buildable withHello(String hello);
}

@Test
public void invokeBuilderMethod() {
  Buildable hw = DynamicObject.newInstance(Buildable.class).withHello("world");
  assertEquals("{:hello \"world\"}", DynamicObject.serialize(hw)); // This is the original value

  Buildable hk = hw.withHello("kitty"); // Build a new value from the old one
  assertEquals("{:hello \"kitty\"}", DynamicObject.serialize(hk));
  assertEquals("{:hello \"world\"}", DynamicObject.serialize(hw)); // The original value is unchanged
}

```

### Metadata

DynamicObject allows direct access to Clojure's metadata facilities with the `@Meta` annotation. This allows information to be annotated in arbitrary ways without this information being part of the data itself. For example, if you're using DynamicObject to communicate across processes using a distributed queue like [SQS](http://aws.amazon.com/sqs/), metadata is a great place to stash information about the messages themselves, such as the [message receipt handle](http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/ImportantIdentifiers.html):

```java
interface WorkerJob extends DynamicObject<WorkerJob> {
  UUID jobId();
  String inputLocation();
  // and so forth
  @Meta long messageAgeInSeconds(); // Use this for visibility purposes (e.g. are we falling behind?)
  @Meta String messageReceiptHandle(); // Use this later to delete the message once the job is done

  // The metadata fields can be set using builder methods:
  @Meta WorkerJob messageAgeInSeconds(long seconds);
  @Meta WorkerJob messageReceiptHandle(String handle);
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
                   .mapToInt(Album::getTracks)
                   .sum();
  }
}
```

### Custom Keys

DynamicObject does not have an elaborate system of conventions to map Java method names to Clojure map keys. By default, the name of the getter method is exactly the name of the keyword. `str()` maps to `:str`, and `myString()` maps to `:myString`, not `:my-string`. This default can be overridden with the `@Key` annotation. Suppose we want access to the following data:

```clojure
{:camelCase 1,
 :kebab-case 2,
 "quoted string" 3}
```

The corresponding DynamicObject getters for each field look like this:

```java
long camelCase(); // corresponds to the :camelCase field
@Key(":kebab-case") long kebabCase(); // corresponds to the :kebab-case field, as opposed to the default :kebabCase
@Key("quoted string") long quotedString(); // corresponds to the "quoted string" field
```

Note the convention: an initial colon yields a Clojure keyword, and anything else yields a string.

Custom keys have a second purpose: they decouple getters and setters. Without custom keys, the getter method for a given field must have the same name as the builder method for the same field. But if getters and builders both have `@Key` annotations, the methods can take on arbitrary names:

```java
public interface MyType extends DynamicObject<MyType> {
  @Key(":flubber")    String getFlubber();     // Pretty standard for field access in Java...
  @Key(":is-flubbed") boolean isFlubbed();     // ...except for booleans, which generally use "is" and not "get"

  @Key(":flubber") MyType setFlubber(String flubber);   // You can prefix these with "set" if you want...
  @Key(":flubber") MyType withFlubber(String flubber);  // ...but "with" makes more sense for persistent updates
}
```

### Clojure Interop

In addition to providing a sane alternative to POJOs, DynamicObject implements the map abstractions of both Java and Clojure. This allows the same values to be sent back and forth between Java and Clojure code without wrapping, unwrapping, conversion, or reflection. Java and Clojure code can both idiomatically inspect DynamicObject values and build new ones.

As an example, we'll use the RecursionTest's definition of a DynamicObject-based linked list:

```java
public interface LinkedList extends DynamicObject<LinkedList> {
  long value();
  LinkedList next();

  LinkedList value(long value);
  LinkedList next(LinkedList linkedList);
}
```

The following REPL session shows the use of this type from Clojure (various imports omitted):

```clojure
; We'll start by creating a three-node linked list. First, let's register a reader tag:
(DynamicObject/registerTag RecursionTest$LinkedList "LL")

; Now we'll deserialize the tail node from Edn:
(def tail (DynamicObject/deserialize "#LL{:value 3, :next nil}" RecursionTest$LinkedList))
=> #'user/tail

; We'll build the middle node by using standard Clojure functions:
(def middle (-> tail empty (assoc :value 2) (assoc :next tail)))
=> #'user/middle

; Finally, we'll construct the head node using DynamicObject's builder methods:
(def head (-> middle (.value 1) (.next middle)))
=> #'user/head

; Let's print the result (note the reader tags):
head
=> #LL{:next #LL{:next #LL{:value 3, :next nil}, :value 2}, :value 1}

; Let's navigate around a bit, first in a Clojure style:
(get-in head [:next :next :value])
=> 3

; Let's try the equivalent Java style as well:
(-> head .next .next .value)
=> 3

; We can use the DynamicObject instance as a function:
(tail :value)
=> 3

; Let's modify a deeply nested value:
(assoc-in head [:next :next :value] 19)
=> #LL{:next #LL{:next #LL{:value 19, :next nil}, :value 2}, :value 1}

; DynamicObject validation can be used from Clojure for runtime type checking:
(.validate (assoc head :next 4))
IllegalStateException The following fields had the wrong type:
  next (expected LinkedList, got Long)
  com.github.rschmitt.dynamicobject.internal.Validation.validateInstance (Validation.java:33)

; What about pretty printing? Let's make a value that wraps:
(def long-list (-> head (assoc :some-long-key :some-long-value :another-long-key :another-long-value)))
(pprint long-list)
#LL{:another-long-key :another-long-value,
    :some-long-key :some-long-value,
    :next #LL{:next #LL{:value 3, :next nil}, :value 2},
    :value 1}

; Just for grins, let's round-trip the output:
(DynamicObject/deserialize (with-out-str (pprint long-list)) RecursionTest$LinkedList)
=> #LL{:another-long-key :another-long-value, :some-long-key :some-long-value, :next #LL{:next #LL{:value 3, :next nil}, :value 2}, :value 1}
(= *1 long-list)
=> true

; Let's play with Fressian:
(def head-bytes (DynamicObject/toFressianByteArray head))
=> #'user/head-bytes
(vec head-bytes)
=> [-17 -36 76 76 1 -64 -24 -54 -9 -51 -34 110 101 120 116 -96 -64 -24 -54 -9 -128 -96 -64 -24 -54 -9 -51 -33 118 97 108 117 101 3 -54 -9 -128 -9 -54 -9 -127 2 -54 -9 -127 1]
(DynamicObject/fromFressianByteArray head-bytes)
=> #LL{:next #LL{:next #LL{:value 3, :next nil}, :value 2}, :value 1}
```

## Guidelines

* Always register a reader tag for any `DynamicObject` that will be serialized. This reader tag should be namespaced with some appropriate prefix (e.g. a Java package name), as all unprefixed reader tags are reserved for future use by the Edn specification.
* Always include a version number in data that will be serialized. This way, older consumers can check the version number and decline any messages that they are not capable of handling properly.
* Annotate required fields with `@Required` and call `validate()` to ensure that all required fields are present.
* Use [`java.util.Optional`](http://docs.oracle.com/javase/8/docs/api/java/util/Optional.html) in your schema with fields that are not `@Required`. Internally, DynamicObject unwraps `Optional` values; they do not affect serialization, and they provide additional null safety by making it obvious (at the actual call site, not just the schema) that a given field might not be present.
  * Correspondingly, unboxed primitive fields should always be marked `@Required`, as they cannot be effectively checked for null. Optional fields should always use the boxed type.
* It is okay to submit a mutable collection such as a `java.util.ArrayList` to a `DynamicObject` builder method. Internally, all collection elements are copied to an immutable Clojure collection.
  * Similarly, all collection getter methods return an immutable persistent collection. Attempts to mutate these collections will result in an `UnsupportedOperationException`.
* Do not abuse user-defined methods. A [pure function](http://en.wikipedia.org/wiki/Pure_function) is often a good candidate for a custom method; anything else should be viewed with suspicion.

## Constraints and Limitations

* Since unboxed primitives cannot be null, any attempt to dereference an unboxed primitive field whose underlying value is null or missing will result in a `NullPointerException`.
* DynamicObject has no concept of inheritance. There is no provision for deriving one DynamicObject type from another.
* DynamicObject's builders and getters copy and transform collections--for instance, a getter returning a `List<Integer>` must copy from a `List<Long>`. This can have performance impact.
* There is no way to teach DynamicObject to make defensive copies of mutable types in getters and builders.
* Java arrays are not currently supported.
* Fressian does not support Clojure sets, vectors, or lists. The Fressian reader will return Java's mutable collection types instead.

## Developing

DynamicObject should work out-of-the-box with [IntelliJ 14.1](http://www.jetbrains.com/idea/download/); just import it as a Maven project using the pom.xml file. The Community Edition is sufficient. You'll need [JDK8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) installed and configured as an SDK within IntelliJ.

You can also run the build from the command line using `mvn package`. To just run the unit tests, use `mvn test`.

## Influences and Similar Ideas

* [Lombok](http://www.projectlombok.org/) is a boilerplate elimination tool for Java. It offers the excellent [`@Value`](http://projectlombok.org/features/Value.html) annotation, which helps to take the pain out of Java data modeling. Unfortunately, Lombok by itself does little to solve the problem of serialization/deserialization, and its implementation does horrible violence to the internals of the compiler.
* [Prismatic Schema](https://github.com/Prismatic/schema) is a Clojure library that offers declarative data validation and description in terms of "schemas."
* [core.typed](https://github.com/clojure/core.typed) is a pluggable type system for Clojure. Its concept of [heterogeneous maps](https://github.com/clojure/core.typed/wiki/Types#heterogeneous-maps) helped to clarify how Clojure's extremely general map type could be used effectively in a statically typed language like Java.
