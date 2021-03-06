/*
* Copyright (c) 2013 - present Facebook, Inc.
* All rights reserved.
*
* This source code is licensed under the BSD style license found in the
* LICENSE file in the root directory of this source tree. An additional grant
* of patent rights can be found in the PATENTS file in the same directory.
*/

package codetoanalyze.java.eradicate;

import com.facebook.infer.annotation.Assertions;
import com.facebook.infer.annotation.Initializer;
import com.facebook.infer.annotation.Present;
import com.facebook.infer.annotation.SuppressFieldNotInitialized;
import com.google.common.base.Optional;

import javax.annotation.Nullable;

import android.app.Activity;
import android.os.Bundle;

abstract class A {
  final String fld;

  A(String s) {
    this.fld = s;
  }
}

// for butterknife
@interface InjectView {}

public class FieldNotNullable extends A {
  @Nullable
  String x;
  String y;
  String fld; // Shadow the field defined in A
  String static_s = null; // Static initializer error

  FieldNotNullable(String s) {
    super(s);
    x = null;
    y = s;
    this.fld = s;
  }

  void setXNull() {
    x = null;
  }

  void setXNullable(@Nullable String s) {
    x = s;
  }

  void setYNull() {
    y = null;
  }

  void setYNullable(@Nullable String s) {
    y = s;
  }

  FieldNotNullable(Integer n) {
    super("");
    this.fld = "";
    y = x == null ? "abc" : "def";
  }
}

class MixedInitializers extends Activity {

  private String field1 = "1";
  private String field2;
  private String field3;

  MixedInitializers() {
    field2 = "2";
  }

  protected void onCreate(Bundle bundle) {
    field3 = "3";
  }

}


class TestInitializerBuilder {
  String required1;
  String required2;
  @Nullable String optional;

  // No FIELD_NOT_INITIALIZED error should be reported, because of the @Initializer annotations.
  TestInitializerBuilder() {
  }

  // This is an initializer and must always be called before build().
  @Initializer TestInitializerBuilder setRequired1(String required1) {
    this.required1 = required1;
    return this;
  }

  // This is an initializer and must always be called before build().
  @Initializer TestInitializerBuilder setRequired2(String required2) {
    this.required2 = required2;
    return this;
  }

  TestInitializerBuilder setOptional(String optional) {
    this.optional = optional;
    return this;
  }

  TestInitializer build() {
    // Fail hard if the required fields are not initialzed
    Assertions.assertCondition(required1 != null && required2 != null);

    return new TestInitializer(this);
  }
}

class TestInitializer {
  String required1; // should always be set
  String required2; // should always be set
  @Nullable String optional; // optionally set

  TestInitializer (TestInitializerBuilder b) {
    required1 = b.required1;
    required2 = b.required2;
    optional = b.optional;
  }

  static void testInitializerClientA() {
    TestInitializerBuilder b = new TestInitializerBuilder();
    b.setRequired1("hello");
    b.setRequired2("world");
    TestInitializer x = b.build();
  }

  static void testInitializerClientB() {
    TestInitializerBuilder b = new TestInitializerBuilder();
    b.setRequired1("a");
    b.setRequired2("b");
    b.setOptional("c");
    TestInitializer x = b.build();
  }
}


class NestedFieldAccess {

  class C {
    @Nullable
    String s;
  }

  class CC {
    @Nullable
    C c;
  }

  class CCC {
    @Nullable
    CC cc;
  }


  public class Test {
    @Nullable
    String s;
    C myc;

    Test() {
      myc = new C();
    }

    void test() {
      if (s != null) {
        int n = s.length();
      }
    }

    void test1() {
      if (myc.s != null) {
        int n = myc.s.length();
      }
    }

    void test2(C c) {
      if (c.s != null) {
        int n = c.s.length();
      }
    }

    void test2_local() {
      C c = new C();
      if (c.s != null) {
        int n = c.s.length();
      }
    }

    void test3(CC cc) {
      if (cc.c != null && cc.c.s != null) {
        int n = cc.c.s.length();
      }
    }

    void test4(CCC ccc) {
      if (ccc.cc != null && ccc.cc.c != null && ccc.cc.c.s != null) {
        int n = ccc.cc.c.s.length();
      }
    }

    void test5(@Nullable CCC ccc) {
      if (ccc == null || ccc.cc == null ||
          ccc.cc.c == null || ccc.cc.c.s == null) {
      } else {
        int n = ccc.cc.c.s.length();
      }
    }
  }

  class TestPresentAnnotationBasic {
    void testBasicConditional(Optional<String> o) {
      if (o.isPresent()) {
        o.get();
      }
    }

    Optional<String> absent = Optional.absent();
    @Present Optional<String> present = Optional.of("abc");

    @Present Optional<String> returnPresent() {
      if (absent.isPresent()) {
        return absent;
      }
      else return Optional.of("abc");
    }

    void expectPresent(@Present Optional<String> x) {
    }

    void bar() {
      expectPresent(present);
      String s;
      s = returnPresent().get();
      s = present.get();

      Assertions.assertCondition(absent.isPresent());
      expectPresent(absent);
    }
  }

  class TestPresentFieldOfInnerClass {
    class D {
      @SuppressFieldNotInitialized Optional<String> s;
    }

    class D1 {
      // Different bytecode generated when the field is private
      @SuppressFieldNotInitialized private Optional<String> s;
    }

    void testD(D d) {
      if (d.s.isPresent()) {
        d.s.get();
      }
    }

    void testD1(D1 d1) {
      if (d1.s.isPresent()) {
        d1.s.get();
      }
    }

    void testD1Condition(D1 d1) {
      Assertions.assertCondition(d1.s.isPresent());
      d1.s.get();
    }
  }

  class TestFunctionsIdempotent {
    @Nullable String s;
    String dontAssignNull;

    TestFunctionsIdempotent() {
      dontAssignNull = "";
    }

    @Nullable String getS(int n) {
      return s;
    }

    TestFunctionsIdempotent getSelf() {
      return this;
    }

    void FlatOK1(TestFunctionsIdempotent x) {
      if(getS(3) != null) {
        dontAssignNull = getS(3);
      }
    }

    void FlatOK2(TestFunctionsIdempotent x) {
      if(x.getS(3) != null) {
        dontAssignNull = x.getS(3);
      }
    }

    void FlatBAD1(TestFunctionsIdempotent x) {
      if(x.getS(3) != null) {
        dontAssignNull = getS(3);
      }
    }

    void FlatBAD2(TestFunctionsIdempotent x) {
      if(x.getS(3) != null) {
        dontAssignNull = x.getS(4);
      }
    }

    void NestedOK1() {
      if(getSelf().getS(3) != null) {
        dontAssignNull = getSelf().getS(3);
      }
    }

    void NestedOK2() {
      if(getSelf().getSelf().getS(3) != null) {
        dontAssignNull = getSelf().getSelf().getS(3);
      }
    }

    void NestedBAD1() {
      if(getSelf().getS(3) != null) {
        dontAssignNull = getSelf().getS(4);
      }
    }

    void NestedBAD2() {
      if(getS(3) != null) {
        dontAssignNull = getSelf().getS(3);
      }
    }

    void NestedBAD3() {
      if(getSelf().getSelf().getS(3) != null) {
        dontAssignNull = getSelf().getS(3);
      }
    }
  }

  class TestContainsKey {
    void testMapContainsKey (java.util.Map<Integer, String> m) {
      if (m.containsKey(3)) {
        m.get(3).isEmpty();
      }
    }

    void testImmutableMapContainsKey (com.google.common.collect.ImmutableMap<Integer, String> m) {
      if (m.containsKey(3)) {
        m.get(3).isEmpty();
      }
    }
  }

  // support assignments of null to @InjectView fields, generated by butterknife
  class SupportButterKnife {
     @InjectView String s;

     SupportButterKnife() {
     }

     void dereferencingIsOK() {
       int n = s.length();
     }

     void assignNullIsOK() {
       s = null;
     }
  }

}
