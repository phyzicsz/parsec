package com.phyzicsz.parsec.reflections.filter;

import com.phyzicsz.parsec.reflections.Reflections;
import com.phyzicsz.parsec.reflections.filter.FilterBuilder;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Test filtering
 */
public class FilterBuilderTest {

  @Test
  public void test_include() {
      FilterBuilder filter = new FilterBuilder().include("com\\.phyzicsz.*");
      assertTrue(filter.test("com.phyzicsz.parsec.reflections.Reflections"));
      assertTrue(filter.test("com.phyzicsz.parsec.reflections.foo.Reflections"));
      assertFalse(filter.test("org.foobar.Reflections"));
  }

    @Test
    public void test_includePackage() {
        FilterBuilder filter = new FilterBuilder().includePackage("com.phyzicsz.parsec.reflections");
        assertTrue(filter.test("com.phyzicsz.parsec.reflections.Reflections"));
        assertTrue(filter.test("com.phyzicsz.parsec.reflections.foo.Reflections"));
        assertFalse(filter.test("org.foobar.Reflections"));
    }

    @Test
    public void test_includePackageMultiple() {
        FilterBuilder filter = new FilterBuilder().includePackage("com.phyzicsz.parsec.reflections", "org.foo");
        assertTrue(filter.test("com.phyzicsz.parsec.reflections.Reflections"));
        assertTrue(filter.test("com.phyzicsz.parsec.reflections.foo.Reflections"));
        assertTrue(filter.test("org.foo.Reflections"));
        assertTrue(filter.test("org.foo.bar.Reflections"));
        assertFalse(filter.test("org.bar.Reflections"));
    }

    @Test
    public void test_includePackagebyClass() {
        FilterBuilder filter = new FilterBuilder().includePackage(Reflections.class);
        assertTrue(filter.test("com.phyzicsz.parsec.reflections.Reflections"));
        assertTrue(filter.test("com.phyzicsz.parsec.reflections.foo.Reflections"));
        assertFalse(filter.test("org.foobar.Reflections"));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_exclude() {
        FilterBuilder filter = new FilterBuilder().exclude("com\\.phyzicsz.parsec.reflections.*");
        assertFalse(filter.test("com.phyzicsz.parsec.reflections.Reflections"));
        assertFalse(filter.test("com.phyzicsz.parsec.reflections.foo.Reflections"));
        assertTrue(filter.test("com.foobar.Reflections"));
    }

    @Test
    public void test_excludePackage() {
        FilterBuilder filter = new FilterBuilder().excludePackage("com.phyzicsz.parsec.reflections");
        assertFalse(filter.test("com.phyzicsz.parsec.reflections.Reflections"));
        assertFalse(filter.test("com.phyzicsz.parsec.reflections.foo.Reflections"));
        assertTrue(filter.test("org.foobar.Reflections"));
    }

    @Test
    public void test_excludePackageMultiple() {
      FilterBuilder filter = new FilterBuilder().excludePackage("com.phyzicsz.parsec.reflections", "org.foo");
      assertFalse(filter.test("com.phyzicsz.parsec.reflections.Reflections"));
      assertFalse(filter.test("com.phyzicsz.parsec.reflections.foo.Reflections"));
      assertFalse(filter.test("org.foo.Reflections"));
      assertFalse(filter.test("org.foo.bar.Reflections"));
      assertTrue(filter.test("org.bar.Reflections"));
    }

    @Test
    public void test_excludePackageByClass() {
        FilterBuilder filter = new FilterBuilder().excludePackage(Reflections.class);
        assertFalse(filter.test("com.phyzicsz.parsec.reflections.Reflections"));
        assertFalse(filter.test("com.phyzicsz.parsec.reflections.foo.Reflections"));
        assertTrue(filter.test("org.foobar.Reflections"));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_parse_include() {
        FilterBuilder filter = FilterBuilder.parse("+com.phyzicsz.parsec.reflections.*");
        assertTrue(filter.test("com.phyzicsz.parsec.reflections.Reflections"));
        assertTrue(filter.test("com.phyzicsz.parsec.reflections.foo.Reflections"));
        assertFalse(filter.test("org.foobar.Reflections"));
    }

    @Test
    public void test_parse_include_notRegex() {
        FilterBuilder filter = FilterBuilder.parse("+com.phyzicsz.parsec.reflections");
        assertFalse(filter.test("com.phyzicsz.parsec.reflections.Reflections"));
        assertFalse(filter.test("com.phyzicsz.parsec.reflections.foo.Reflections"));
        assertFalse(filter.test("org.foobar.Reflections"));
        assertFalse(filter.test("org.reflectionsplus.Reflections"));
    }

    @Test
    public void test_parse_exclude() {
        FilterBuilder filter = FilterBuilder.parse("-org.reflections.*");
        assertFalse(filter.test("org.reflections.Reflections"));
        assertFalse(filter.test("org.reflections.foo.Reflections"));
        assertTrue(filter.test("org.foobar.Reflections"));
        assertFalse(filter.test("org.reflectionsplus.Reflections"));
    }

    @Test
    public void test_parse_exclude_notRegex() {
        FilterBuilder filter = FilterBuilder.parse("-org.reflections");
        assertTrue(filter.test("org.reflections.Reflections"));
        assertTrue(filter.test("org.reflections.foo.Reflections"));
        assertTrue(filter.test("org.foobar.Reflections"));
        assertTrue(filter.test("org.reflectionsplus.Reflections"));
    }

    @Test
    public void test_parse_include_exclude() {
        FilterBuilder filter = FilterBuilder.parse("+org.reflections.*, -org.reflections.foo.*");
        assertTrue(filter.test("org.reflections.Reflections"));
        assertFalse(filter.test("org.reflections.foo.Reflections"));
        assertFalse(filter.test("org.foobar.Reflections"));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_parsePackages_include() {
        FilterBuilder filter = FilterBuilder.parsePackages("+org.reflections");
        assertTrue(filter.test("org.reflections.Reflections"));
        assertTrue(filter.test("org.reflections.foo.Reflections"));
        assertFalse(filter.test("org.foobar.Reflections"));
        assertFalse(filter.test("org.reflectionsplus.Reflections"));
    }

    @Test
    public void test_parsePackages_include_trailingDot() {
        FilterBuilder filter = FilterBuilder.parsePackages("+org.reflections.");
        assertTrue(filter.test("org.reflections.Reflections"));
        assertTrue(filter.test("org.reflections.foo.Reflections"));
        assertFalse(filter.test("org.foobar.Reflections"));
        assertFalse(filter.test("org.reflectionsplus.Reflections"));
    }

    @Test
    public void test_parsePackages_exclude() {
        FilterBuilder filter = FilterBuilder.parsePackages("-org.reflections");
        assertFalse(filter.test("org.reflections.Reflections"));
        assertFalse(filter.test("org.reflections.foo.Reflections"));
        assertTrue(filter.test("org.foobar.Reflections"));
        assertTrue(filter.test("org.reflectionsplus.Reflections"));
    }

    @Test
    public void test_parsePackages_exclude_trailingDot() {
        FilterBuilder filter = FilterBuilder.parsePackages("-org.reflections.");
        assertFalse(filter.test("org.reflections.Reflections"));
        assertFalse(filter.test("org.reflections.foo.Reflections"));
        assertTrue(filter.test("org.foobar.Reflections"));
        assertTrue(filter.test("org.reflectionsplus.Reflections"));
    }

    @Test
    public void test_parsePackages_include_exclude() {
        FilterBuilder filter = FilterBuilder.parsePackages("+org.reflections, -org.reflections.foo");
        assertTrue(filter.test("org.reflections.Reflections"));
        assertFalse(filter.test("org.reflections.foo.Reflections"));
        assertFalse(filter.test("org.foobar.Reflections"));
    }

}
