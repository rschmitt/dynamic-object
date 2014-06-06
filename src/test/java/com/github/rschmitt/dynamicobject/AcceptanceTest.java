package com.github.rschmitt.dynamicobject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;

public class AcceptanceTest {
    @Before
    public void setup() {
        DynamicObject.registerType(Path.class, new PathTranslator());
    }

    @After
    public void teardown() {
        DynamicObject.deregisterType(Path.class);
    }

    @Test
    public void acceptanceTest() {
        Document document = DynamicObject.newInstance(Document.class);
        roundTrip(document);

        document = document.name("Mr. Show").uuid(randomUUID()).date(new Date());
        roundTrip(document);

        document = document.documentPointer(DynamicObject.deserialize("{:location \"/prod-bucket/home\"}", DocumentPointer.class));
        roundTrip(document);

        Set<Path> paths = new LinkedHashSet<>();
        paths.add(newPath());
        paths.add(newPath());
        paths.add(newPath());
        document = document.paths(paths);
        roundTrip(document);
    }

    private void roundTrip(Document document) {
        assertEquals(document, DynamicObject.deserialize(DynamicObject.serialize(document), Document.class));
    }

    private Path newPath() {
        return new Path(randomString(), randomString(), randomString());
    }

    private String randomString() {
        return randomUUID().toString().substring(0, 8);
    }
}

class Path {
    private final String a;
    private final String b;
    private final String c;

    Path(String a, String b, String c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public String getA() {
        return a;
    }

    public String getB() {
        return b;
    }

    public String getC() {
        return c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Path path = (Path) o;

        if (a != null ? !a.equals(path.a) : path.a != null) return false;
        if (b != null ? !b.equals(path.b) : path.b != null) return false;
        if (c != null ? !c.equals(path.c) : path.c != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = a != null ? a.hashCode() : 0;
        result = 31 * result + (b != null ? b.hashCode() : 0);
        result = 31 * result + (c != null ? c.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Path{" +
                "a='" + a + '\'' +
                ", b='" + b + '\'' +
                ", c='" + c + '\'' +
                '}';
    }
}

class PathTranslator extends EdnTranslator<Path> {
    @Override
    public Path read(Object obj) {
        String str = (String) obj;
        String[] split = str.split("\\/");
        return new Path(split[0], split[1], split[2]);
    }

    @Override
    public String write(Path obj) {
        return String.format("\"%s/%s/%s\"", obj.getA(), obj.getB(), obj.getC());
    }

    @Override
    public String getTag() {
        return "Path";
    }
}

interface DocumentPointer extends DynamicObject<DocumentPointer> {
    String location();

    DocumentPointer location(String location);
}

interface Document extends DynamicObject<Document> {
    UUID uuid();
    String name();
    Date date();
    Set<Path> paths();
    @Key(":document-pointer") DocumentPointer documentPointer();

    Document uuid(UUID uuid);
    Document name(String name);
    Document date(Date date);
    Document paths(Set<Path> paths);
    Document documentPointer(DocumentPointer documentPointer);
}
