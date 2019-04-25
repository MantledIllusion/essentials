package com.mantledillusion.essentials.string;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DeepReplaceTest {

	private static class ReplaceRecorder implements Function<String, Object> {
		
		private final Map<String, Object> replacements = new HashMap<>();
		private final List<String> requestedKeys = new ArrayList<>();
		
		@Override
		public Object apply(String key) {
			this.requestedKeys.add(key);
			return this.replacements.get(key);
		}
		
		public ReplaceRecorder provide(String key, Object value) {
			this.replacements.put(key, value);
			return this;
		}
		
		public ReplaceRecorder test(String template, String expected) {
			assertEquals(expected, StringEssentials.deepReplace(template, this));
			return this;
		}
		
		public void withRequested(String... keys) {
			assertEquals(Arrays.asList(keys), this.requestedKeys);
		}
	}
	
	@Test
	public void testNoReplace() {
		new ReplaceRecorder()
			.test("Some String", "Some String")
			.withRequested();
	}
	
	@Test
	public void testCompleteReplace() {
		new ReplaceRecorder()
			.provide("key", "value")
			.test("${key}", "value")
			.withRequested("key");
	}
	
	@Test
	public void testPartialReplace() {
		new ReplaceRecorder()
			.provide("key", "long")
			.test("Some ${key} String", "Some long String")
			.withRequested("key");
	}
	
	@Test
	public void testMultiReplace() {
		new ReplaceRecorder()
			.provide("vegetable", "Kale")
			.provide("adjective", "disgusting")
			.test("${vegetable} is a ${adjective} vegetable!", "Kale is a disgusting vegetable!")
			.withRequested("vegetable", "adjective");
	}
	
	@Test
	public void testCompleteDeepReplace() {
		new ReplaceRecorder()
			.provide("key", "first")
			.provide("first", "second")
			.test("${${key}}", "second")
			.withRequested("key", "first");
	}
	
	@Test
	public void testPartialDeepReplace() {
		new ReplaceRecorder()
			.provide("adjective.identifier", "RHF")
			.provide("character.initials", "hp")
			.provide("adjectives.hp.RHF", "Red Head Fan")
			.test("Harry '${adjectives.${character.initials}.${adjective.identifier}}' Potter", "Harry 'Red Head Fan' Potter")
			.withRequested("character.initials", "adjective.identifier", "adjectives.hp.RHF");
	}
}
