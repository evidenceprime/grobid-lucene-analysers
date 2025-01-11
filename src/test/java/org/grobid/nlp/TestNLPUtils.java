package org.grobid.nlp;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestNLPUtils { 
		
	@Test
	public void testCloseUnclosedXmlTags() {
		assertEquals("<b><i>test</i></b>", NLPUtils.closeUnclosedXmlTags("<b><i>test</i>"));		
		
		 String s="<p></p><p>In connection with <B><a class=\"hitmap_a\" name=\"H48\">test</a></B> scripts referencing a particular data item having a particular location in a window, a problem or complication may exist for testing if the data item's location  changes. Such changes with respect to a data item in a GUI may be common. In connection with testing scripts, the testing script may require modification to accommodate such changes when a data item is relocated to a different area, or pane within a same window. An";
		 assertEquals(s+"</p>", NLPUtils.closeUnclosedXmlTags(s));
		 assertEquals(s+s+"</p></p>", NLPUtils.closeUnclosedXmlTags(s+s));
	}
	
	@Test
	public void testPercentageUppercase () {
		assertEquals(100, NLPUtils.percentageUppercase("FIXATION DES ANTICORPS"));
		assertEquals(0, NLPUtils.percentageUppercase("01,(40%) & ! 3_56:[]{};:<>"));
		assertEquals(100, NLPUtils.percentageUppercase("RÉACTION À L'ANTIGÈNE"));
		assertEquals(0, NLPUtils.percentageUppercase("réaction à l'antigène"));
		int pct=NLPUtils.percentageUppercase("TROUSSE DE DETECTION D'IgE ET D'IgG HUMAINES ANTI-GLIADINES");
		assertTrue("Percentage="+pct,pct> 90);
		pct = NLPUtils.percentageUppercase("CELL-FACH state, after acquiring a E-DCH data");
		assertTrue("Percentage="+pct, pct < 50);
	}
	
	@Test
	public void testMd5 () {
		assertEquals("98f6bcd4621d373cade4e832627b4f6",NLPUtils.getMd5OfString("test"));
		String[] t="1 2 3 4 5 6 7 8 9 a b c d e f g h i à è ì ò ù < > aa aà ｅ ｖ ｉ ｃ  ب أ ذ ر ب ي ج ا ن".split(" ");
		for (String s1: t) {
			for (String s2:t) {
				if (s1.equals(s2)) {
					assertEquals(NLPUtils.getMd5OfString(s1),NLPUtils.getMd5OfString(s2));
				} else {
					assertNotSame(NLPUtils.getMd5OfString(s1),NLPUtils.getMd5OfString(s2));	
				}
			}
		}
	}
}
