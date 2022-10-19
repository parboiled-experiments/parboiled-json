package org.parboiled.json;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;

import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.matchers.Matcher;
import org.parboiled.matchers.ProxyMatcher;

public abstract class BaseJsonParser extends BaseParser<Object> {

	protected Map<String, Rule> RULE_CACHE = new HashMap<>();
	protected Map<String, List<ProxyMatcher>> PROXY_CACHE = new HashMap<>();

	protected String start;
	protected JsonObject json;

	protected BaseJsonParser(String start, String jsonString) {
		this.start = start;
		this.json = Json.createReader(new StringReader(jsonString)).readObject();
	}

	protected BaseJsonParser(String start, InputStream jsonStream) {
		this.start = start;
		this.json = Json.createReader(new InputStreamReader(jsonStream)).readObject();
	}

	protected void initProxies() {
		PROXY_CACHE.entrySet().forEach(e -> e.getValue().forEach(p -> {
			p.label(e.getKey());
			p.arm((Matcher) RULE_CACHE.get(e.getKey()));
		}));
		PROXY_CACHE.clear();
		RULE_CACHE.clear();
	}

	public abstract Rule start();

}