package StringList;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class StringListTest {
    private List<Object> list;
	
	@Before
	public void setUp(){
		list = new StringList();
	}
	
	@Test
	public void testListInit(){
		assertTrue(list.isEmpty());
		assertTrue(list.size() == 0);
	}
	
	@Test
	public void testAddElements(){
		list.add(0, "Karol");
		list.add(1, "Vanessa");
		list.add(2, "Amanda");
		
		assertEquals("Karol", list.get(0));
		assertEquals("Vanessa", list.get(1));
		assertEquals("Amanda", list.get(2));
		
		list.add(1, "Mariana");
		
		assertEquals("Karol", list.get(0));
		assertEquals("Mariana", list.get(1));
		assertEquals("Vanessa", list.get(2));
		assertEquals("Amanda", list.get(3));	
		
		assertTrue(list.size()==4);
	}
	
	@Test
	public void testSetElement(){
		list.add(0, "Karol");
		list.add(1, "Vanessa");
		list.add(2, "Amanda");
		
		list.set(1, "Livia");
		
		assertEquals("Karol", list.get(0));
		assertEquals("Livia", list.get(1));
		assertEquals("Amanda", list.get(2));
	}
	
	@Test
	public void testRemoveElement(){
		list.add(0, "Karol");
		list.add(1, "Vanessa");
		list.add(2, "Amanda");
		
		assertEquals("Amanda", list.remove(2));
		assertTrue(list.size() == 2);
	}
	
	@Test (expected = IndexOutOfBoundsException.class)
	public void testRemoveWithEmptyList(){
		list.remove(0);
	}

	@Test
	public void testMultipleDataTypes() {
		list.add("Heyo");
		list.add(10239);
		Float[] arr = {10.0f, 12.0f, -1324.34987f,};
		list.add(arr);

		assertEquals(list.get(0), "Heyo");
		assertEquals(list.get(1), 10239);
		assertArrayEquals((Object[]) list.get(2), arr);
	}

	@Test
	public void testClearList() {
		for (int i = 0; i < 10; i++) {
			list.add(i);
		}

		assertEquals(list.isEmpty(), false);
		assertEquals(list.size(), 10);
		list.clear();
		assertEquals(list.isEmpty(), true);
		assertEquals(list.size(), 0);
	}
}
