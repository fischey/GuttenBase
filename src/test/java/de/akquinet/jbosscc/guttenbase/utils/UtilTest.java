package de.akquinet.jbosscc.guttenbase.utils;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
import java.util.Arrays;

import org.junit.Test;

public class UtilTest
{
  @Test
  public void testFormatTime() throws Exception
  {
    assertEquals("00:00:00", Util.formatTime(0));
    assertEquals("00:11:25", Util.formatTime(685000));
    assertEquals("02:04:14", Util.formatTime(((2 * 60 * 60) + (4 * 60) + 14) * 1000));
  }

  @Test
  public void testParseSelect() throws Exception
  {
    assertEquals(Arrays.asList("a", "b"), Util.parseSelectedColumns("select a,  b From bla"));
    assertEquals(Arrays.asList("a"), Util.parseSelectedColumns("SELECT a FROM bla"));
  }

  @Test(expected = SQLException.class)
  public void testNoSelect() throws Exception
  {
    Util.parseSelectedColumns("Hallo");
  }

  @Test(expected = SQLException.class)
  public void testNoFrom() throws Exception
  {
    Util.parseSelectedColumns("SELECT a FOrM bla");
  }
}
