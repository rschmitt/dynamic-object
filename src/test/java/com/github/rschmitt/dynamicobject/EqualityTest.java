package com.github.rschmitt.dynamicobject;

import static com.github.rschmitt.dynamicobject.DynamicObject.deserialize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class EqualityTest {
    @Test
    public void customEqualityTest() {
        String edn1 = "{:firstName \"Tom\",:lastName \"Brady\",:ssn \"123456789\" }";
        String edn2 = "{:firstName \"Thomas\",:lastName \"Brady\",:ssn \"123456789\" }";
        Person person1 = deserialize(edn1, Person.class);
        Person person2 = deserialize(edn2, Person.class);
        assertEquals(person1, person2);
        
        Set<Person> people = new HashSet<>(Arrays.asList(person1, person2));
        assertTrue(people.contains(person1));
    }
    
    @Test
    public void regualarEqualityTest() {
        String edn1 = "{:accountType \"Credit Card\",:name \"Visa\",:number \"123456789\" }";
        String edn2 = "{:accountType \"Credit Card\",:name \"Master Card\",:number \"123456789\" }";
        Account account1 = deserialize(edn1, Account.class);
        Account account2 = deserialize(edn2, Account.class);
        assertNotEquals(account1, account2);
        
        String edn3 = "{:accountType \"Credit Card\",:name \"Visa\",:number \"123456789\" }";
        String edn4 = "{:accountType \"Credit Card\",:name \"Visa\",:number \"123456789\" }";
        Account account3 = deserialize(edn3, Account.class);
        Account account4 = deserialize(edn4, Account.class);
        assertEquals(account3, account4);
        
        Set<Account> accounts = new HashSet<>(Arrays.asList(account1, account2));
        assertTrue(accounts.contains(account1));
    }
    
    public interface Person extends DynamicObject<Person> {
        @Key(":firstName")
        String getFirstName();
        
        @Key(":firstName")
        Person withFirstName(String firstName);
        
        @Key(":lastName")
        String getLastName();
        
        @Key(":lastName")
        Person withLastName(String lastName);
        
        @Key(":ssn")
        String getSsn();
        
        @Key(":ssn")
        Person withSsn(String ssn);
        
        default Boolean isEqualTo(Object other) {
            if (other == this)
                return true;
            if (other == null)
                return false;
            if (other instanceof Person) {
                Person otherPerson = (Person) other;
                return this.getSsn().equals(otherPerson.getSsn());
            } else {
                return equals(other);
            }
        }
        
        default Integer getHashCode() {
            return getSsn().hashCode();
        }
        
    }
    
    public interface Account extends DynamicObject<Account> {
        @Key(":accountType")
        String getAccountType();
        
        @Key(":accountType")
        Account withAccountType(String accountType);
        
        @Key(":name")
        String getName();
        
        @Key(":name")
        Account withName(String name);
        
        @Key(":number")
        String getAccountNumber();
        
        @Key(":number")
        Account withAccountNumber(String number);
    }
}
