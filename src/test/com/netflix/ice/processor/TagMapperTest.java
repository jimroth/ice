/*
 *
 *  Copyright 2013 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.ice.processor;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Maps;
import com.netflix.ice.common.TagMappings;

public class TagMapperTest {

	private TagMappings loadYaml(String yaml) throws Exception {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		TagMappings tm = new TagMappings();
		return mapper.readValue(yaml, tm.getClass());
	}
	
	@Test
	public void testIsOneOf() throws Exception {
		String yaml =
			"maps:\n" +
			"  DestValue1:\n" +
			"    key: TagKey1\n" +
			"    operator: isOneOf\n" + 
			"    values: [SrcValue1a]\n" +
			"";
		
		TagMappings config = loadYaml(yaml);
		Map<String, Integer> tagKeyIndeces = Maps.newHashMap();
		tagKeyIndeces.put("TagKey1", 0);
		tagKeyIndeces.put("TagKey2", 1);
		
		TagMapper tm = new TagMapper(1, config, tagKeyIndeces);
		
		// Test matching case
		String[] tags = {"SrcValue1a",""};
		String got = tm.apply(1, "123456789012", tags, "");
		assertEquals("wrong mapped value", "DestValue1", got);
		
		// Test non-matching case
		tags = new String[]{"Srcvalue1a",""};
		got = tm.apply(1, "123456789012", tags, "");
		assertEquals("wrong mapped value", "DestValue1", got);
	}

	@Test
	public void testIsOneOfRegex() throws Exception {
		String yaml =
			"maps:\n" +
			"  DestValue1:\n" +
			"    key: TagKey1\n" +
			"    operator: isOneOf\n" + 
			"    values: [Src.*]\n" +
			"";
		
		TagMappings config = loadYaml(yaml);
		Map<String, Integer> tagKeyIndeces = Maps.newHashMap();
		tagKeyIndeces.put("TagKey1", 0);
		tagKeyIndeces.put("TagKey2", 1);
		
		TagMapper tm = new TagMapper(1, config, tagKeyIndeces);
		String[] tags = {"SrcValue1a",""};
		String got = tm.apply(1, "123456789012", tags, "");
		assertEquals("wrong mapped value", "DestValue1", got);
	}

	@Test
	public void testIsNotOneOf() throws Exception {
		String yaml =
			"maps:\n" +
			"  DestValue1:\n" +
			"    key: TagKey1\n" +
			"    operator: isNotOneOf\n" + 
			"    values: [SrcValue1a]\n" +
			"";
		
		TagMappings config = loadYaml(yaml);
		Map<String, Integer> tagKeyIndeces = Maps.newHashMap();
		tagKeyIndeces.put("TagKey1", 0);
		tagKeyIndeces.put("TagKey2", 1);
		
		TagMapper tm = new TagMapper(1, config, tagKeyIndeces);
		String[] tags = {"SrcValue1b",""};
		String got = tm.apply(1, "123456789012", tags, "");
		assertEquals("wrong mapped value", "DestValue1", got);
	}

	@Test
	public void testIsOneOfForced() throws Exception {
		String yaml =
			"force: true\n" +
			"maps:\n" +
			"  DestValue1:\n" +
			"    key: TagKey1\n" +
			"    operator: isOneOf\n" + 
			"    values: [SrcValue1a]\n" +
			"";
		
		TagMappings config = loadYaml(yaml);
		Map<String, Integer> tagKeyIndeces = Maps.newHashMap();
		tagKeyIndeces.put("TagKey1", 0);
		tagKeyIndeces.put("TagKey2", 1);
		
		TagMapper tm = new TagMapper(0, config, tagKeyIndeces);
		String[] tags = {"SrcValue1a",""};
		String got = tm.apply(0, "123456789012", tags, "SrcValue1a");
		assertEquals("wrong mapped value", "DestValue1", got);
	}

	@Test
	public void testOr() throws Exception {
		String yaml =
			"maps:\n" +
			"  DestValue1:\n" +
			"    operator: or\n" +
			"    terms:\n" +
			"    - key: TagKey1\n" +
			"      operator: isOneOf\n" +
			"      values: [SrcValue1]\n" +
			"    - key: TagKey2\n" +
			"      operator: isOneOf\n" +
			"      values: [SrcValue2]\n" +
			"";
		
		TagMappings config = loadYaml(yaml);
		Map<String, Integer> tagKeyIndeces = Maps.newHashMap();
		tagKeyIndeces.put("TagKey1", 0);
		tagKeyIndeces.put("TagKey2", 1);
		tagKeyIndeces.put("TagKey3", 2);
		TagMapper tm = new TagMapper(2, config, tagKeyIndeces);
		
		// Test for no terms true
		String[] tags = {"","",""};
		String got = tm.apply(2, "123456789012", tags, "");
		assertEquals("wrong mapped value", "", got);
		
		// Test for first term true
		tags = new String[]{"SrcValue1","",""};
		got = tm.apply(2, "123456789012", tags, "");
		assertEquals("wrong mapped value", "DestValue1", got);
		
		// Test for second term true
		tags = new String[]{"","SrcValue2",""};
		got = tm.apply(2, "123456789012", tags, "");
		assertEquals("wrong mapped value", "DestValue1", got);
		
		// Test for both terms true
		tags = new String[]{"SrcValue1","SrcValue2",""};
		got = tm.apply(2, "123456789012", tags, "");
		assertEquals("wrong mapped value", "DestValue1", got);
	}
	
	@Test
	public void testAnd() throws Exception {
		String yaml =
			"maps:\n" +
			"  DestValue1:\n" +
			"    operator: and\n" +
			"    terms:\n" +
			"    - key: TagKey1\n" +
			"      operator: isOneOf\n" +
			"      values: [SrcValue1]\n" +
			"    - key: TagKey2\n" +
			"      operator: isOneOf\n" +
			"      values: [SrcValue2]\n" +
			"";
		
		TagMappings config = loadYaml(yaml);
		Map<String, Integer> tagKeyIndeces = Maps.newHashMap();
		tagKeyIndeces.put("TagKey1", 0);
		tagKeyIndeces.put("TagKey2", 1);
		tagKeyIndeces.put("TagKey3", 2);
		TagMapper tm = new TagMapper(2, config, tagKeyIndeces);
		
		// Test for no terms true
		String[] tags = {"","",""};
		String got = tm.apply(2, "123456789012", tags, "");
		assertEquals("wrong mapped value", "", got);
		
		// Test for first term true
		tags = new String[]{"SrcValue1","",""};
		got = tm.apply(2, "123456789012", tags, "");
		assertEquals("wrong mapped value", "", got);
		
		// Test for second term true
		tags = new String[]{"","SrcValue2",""};
		got = tm.apply(2, "123456789012", tags, "");
		assertEquals("wrong mapped value", "", got);
		
		// Test for both terms true
		tags = new String[]{"SrcValue1","SrcValue2",""};
		got = tm.apply(2, "123456789012", tags, "");
		assertEquals("wrong mapped value", "DestValue1", got);
	}

}
