package org.huangp.beans;

import java.util.Date;
import java.util.Set;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

/**
* @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
*/
@Data
public class Child
{
   @Setter(AccessLevel.PROTECTED)
   private Long id;
   private String name;
   private String job = "play";
   private Integer age;
   private Parent parent;
   private Child bestField;
   private Language speaks;
   private Date dateOfBirth;

   private Set<Toy> toys;
}