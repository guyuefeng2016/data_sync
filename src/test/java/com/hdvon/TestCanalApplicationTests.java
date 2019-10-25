package com.hdvon;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestCanalApplicationTests {

	@Autowired
	CacheManager cacheManager;

	@Test
	public void put1() {
		Cache getUserById = cacheManager.getCache("compareCache");
		ArrayList<String> objects = new ArrayList<>();
		objects.add("apple");
		objects.add("pear");
		objects.add("orange");

		getUserById.put("aaaa","apple");
		getUserById.put("aaaa1","pear");
		List a2list = new ArrayList<>();
		a2list.add("pear");
		getUserById.put("aaaa2",a2list);
		List a3ist =new ArrayList<>();
		a3ist.add("orange");
		getUserById.put("aaaa3",a3ist);
		getUserById.put("aaaa4","apple4");

		/*Cache getUserById2 = cacheManager.getCache("compareCache");
		Cache.ValueWrapper aaaa = getUserById2.get("aaaa");
		Cache.ValueWrapper aaaa1 = getUserById2.get("aaaa1");
		Cache.ValueWrapper aaaa2 = getUserById2.get("aaaa2");
		Cache.ValueWrapper aaaa3 = getUserById2.get("aaaa3");
		Cache.ValueWrapper aaaa4 = getUserById2.get("aaaa4");*/

		System.out.println("a2: "+getUserById.get("aaaa2"));
		System.out.println("a3: "+getUserById.get("aaaa3"));

		System.out.println("移除a2键");
		getUserById.evict("aaaa2");

		System.out.println("a22: "+(getUserById.get("aaaa2", List.class) == null));
		System.out.println("a33: "+getUserById.get("aaaa3", List.class));
		/*try{
			Thread.sleep(3000);
		} catch (Exception e){

		}

		get();

		try{
			Thread.sleep(3000);
		} catch (Exception e){

		}

		put2();

		try{
			Thread.sleep(3000);
		} catch (Exception e){

		}

		get();

		put3();

		try{
			Thread.sleep(3000);
		} catch (Exception e){

		}

		get();*/
	}

	public void put2() {
		Cache getUserById = cacheManager.getCache("compareCache");
		List<String> objects = getUserById.get("aaaa",List.class);

		objects.add("fuck");

		getUserById.put("aaaa",objects);
	}

	public void put3() {
		Cache getUserById = cacheManager.getCache("compareCache");
		List<String> objects = getUserById.get("aaaa",List.class);

		objects.add("hehe");

		getUserById.put("aaaa",objects);
	}


	public void get() {
		Cache getUserById = cacheManager.getCache("compareCache");
		List<String> objects = getUserById.get("aaaa",List.class);

		System.out.println(Arrays.asList(objects));
	}


}
