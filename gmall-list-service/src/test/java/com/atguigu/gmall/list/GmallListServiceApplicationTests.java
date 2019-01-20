package com.atguigu.gmall.list;

import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallListServiceApplicationTests {

	@Autowired
	private JestClient jestClient;

	@Test
	public void contextLoads() {
	}

	@Test
	public void testES() throws IOException {
		// dsl语句
		String query = "{\n" +
				"  \"query\": {\n" +
				"    \"term\": {\n" +
				"      \"actorList.name\": \"张译\"\n" +
				"    }\n" +
				"  }\n" +
				"}";
		// 准备执行dsl
		Search search = new Search.Builder(query).addIndex("movie_chn").addType("movie").build();
		//执行dsl语句 并返回结果集
		SearchResult searchResult = jestClient.execute(search);
		//取出hits数据
		List<SearchResult.Hit<Map, Void>> hits = searchResult.getHits(Map.class);
		for (SearchResult.Hit<Map, Void> hit : hits) {
			// 数据都在hit的source里面
			Map map = hit.source;
			System.out.println(map.get("name"));
			System.out.println(map.get("doubanScore"));
		}
	}
}

