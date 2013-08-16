package org.huangp.beans;

import java.util.Date;
import java.util.List;

import lombok.Data;

/**
* @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
*/
@Data
public class Parent
{
   private String name;
   private int age;
   private Date dateOfBirth;
   private Parent parent;
   private List<Child> children;
   private Language speaks;
}
