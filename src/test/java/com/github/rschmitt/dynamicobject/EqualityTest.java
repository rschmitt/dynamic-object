package com.github.rschmitt.dynamicobject;

import static com.github.rschmitt.dynamicobject.DynamicObject.deserialize;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EqualityTest {
    @Test
    public void customBuilderSupport() {
        String edn1 = "{:firstName \"Tom\",:lastName \"Brady\",:ssn \"123456789\" }";
        String edn2 = "{:firstName \"Thomas\",:lastName \"Brady\",:ssn \"123456789\" }";
        Person person1 = deserialize(edn1, Person.class);
        Person person2 = deserialize(edn2, Person.class);      
        assertEquals(person1, person2);
    }
    public interface Person extends DynamicObject<Person>,Equality {
        @Key(":firstName") String getFirstName();
        @Key(":firstName") Person withFirstName(String firstName);
        
        @Key(":lastName") String getLastName();
        @Key(":lastName") Person withLastName(String lastName);
        
        @Key(":ssn") String getSsn();
        @Key(":ssn") Person withSsn(String ssn);
        
        default boolean isEqualTo(Object other){
            if (other == this) return true;
            if (other == null) return false;
            if (other instanceof Person){
                Person otherPerson=(Person)other;
                return this.getSsn().equals(otherPerson.getSsn());
            }else{
                return equals(other);
            }
        }
        
        default int getHashCode(){
            return getSsn().hashCode();
        }
    }
}
