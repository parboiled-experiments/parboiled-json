package org.parboiled.json;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.parboiled.Action;
import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.DontLabel;
import org.parboiled.annotations.SkipNode;
import org.parboiled.matchers.Matcher;
import org.parboiled.matchers.ProxyMatcher;

public class DeclParser<V> extends BaseParser<V> {

    protected Matcher __ = (Matcher) _0n(AnyOf(" \t\r\n")).suppressNode();

	protected Matcher _d = (Matcher) _1n(CharRange('0', '9')).suppressSubnodes();

	protected Matcher _w = (Matcher) _1n(_1of(//
			CharRange('a', 'z'), //
			CharRange('A', 'Z'), //
			CharRange('0', '9') //
	)).suppressSubnodes();

	protected Matcher _l = (Matcher) _1of(//
			seq("'", _0n(CharRange('a', 'z')), "'"), //
			seq("\"", _0n(CharRange('a', 'z')), "\""), //
			_d //
	).suppressSubnodes();

	@DontLabel
	protected Matcher i(String string) {
		return (Matcher) IgnoreCase(string);
	}

	@SkipNode
	protected Matcher _01(Object obj) {
		return (Matcher) Optional(obj);
	}

	@SkipNode
	protected Matcher _0n(Object obj) {
		return (Matcher) ZeroOrMore(obj);
	}

	protected Matcher _1n(Object rule) {
		return (Matcher) OneOrMore(rule);
	}

	protected Matcher seq(Object rule1, Object rule2, Object... rules) {
		return (Matcher) Sequence(rule1, rule2, rules);
	}

	@SkipNode
	protected Matcher _1of(Object rule1, Object rule2, Object... rules) {
		return (Matcher) FirstOf(rule1, rule2, rules);
	}

	protected Map<String, Matcher> RULE_CACHE = new HashMap<>();
	protected Map<String, List<ProxyMatcher>> PROXY_CACHE = new HashMap<>();
	protected static Map<String, Matcher> LEXER = new HashMap<>();
	protected Map<String, Action<?>> ACTIONS = new HashMap<>();

	protected String start;
	protected JsonObject json;

	protected DeclParser(String start, String jsonString) {
		this.start = start;
		this.json = Json.createReader(new StringReader(jsonString)).readObject();
	}
	
	protected DeclParser(String start, InputStream jsonStream) {
		this.start = start;
		this.json = Json.createReader(new InputStreamReader(jsonStream)).readObject();
	}

	protected DeclParser(JsonObject jsonObject) {
		this.json = jsonObject;
	}

	protected void initProxies() {
		PROXY_CACHE.entrySet().forEach(e -> e.getValue().forEach(p -> p.arm(RULE_CACHE.get(e.getKey()))));
	}

	protected void initLexer() {
		LEXER.put("__", __);
		LEXER.put("_d", _d);
		LEXER.put("_l", _l);
		LEXER.put("_w", _w);
	}

	protected void initActions() {
	}

	public Rule start() {
		// System.out.println(this.json);
		initLexer();
		initActions();
		parseDecl(null, this.json);
		initProxies();
		Rule startRule = RULE_CACHE.get(start);
		System.out.println("start rule : " + this.start + " : " + startRule);
		return startRule;
	}

	@DontLabel
	protected Matcher parseDecl(String key, JsonValue jsonVal) {

		Matcher rule = null;

		// Object
		if (jsonVal instanceof JsonObject) {
			JsonObject jsonObj = (JsonObject) jsonVal;

			for (Entry<String, JsonValue> entry : jsonObj.entrySet()) {

				Matcher childRule = parseDecl(entry.getKey(), entry.getValue());

				if ("_0n".equalsIgnoreCase(key)) {
					rule = (Matcher) _0n(childRule);
				} else if ("_1n".equalsIgnoreCase(key)) {
					rule = (Matcher) _1n(childRule);
				} else if ("_01".equalsIgnoreCase(key)) {
					rule = (Matcher) _01(childRule);
				} else {
					rule = childRule; // Default
					if (null != key) {
						RULE_CACHE.put(key, rule);
					}
				}
			}

			// Array
		} else if (jsonVal instanceof JsonArray) {

			JsonArray jsonArr = (JsonArray) jsonVal;
			Object[] childRules = new Object[jsonArr.size()];

			for (int i = 0; i < jsonArr.size(); i++) {
				JsonValue jsonItem = jsonArr.get(i);
				
				Action<?> action = null;
				if (jsonItem instanceof JsonString) {
					String value = ((JsonString) jsonItem).getString();
					action = getAction(value);
				}
				
				if (action != null) {
					childRules[i] = action;
				} else {
					childRules[i] = parseDecl(null, jsonItem);
				}
			}

			if ("seq".equalsIgnoreCase(key)) {
				rule = (Matcher) Sequence(childRules);
			} else if ("_1of".equalsIgnoreCase(key)) {
				rule = (Matcher) FirstOf(childRules);
			}

			rule = (Matcher) rule.label(key);

			// String
		} else if (jsonVal instanceof JsonString) {

			String value = ((JsonString) jsonVal).getString();

			Matcher lexerRule = getLexerRule(value);
			if (lexerRule != null) {
				rule = lexerRule;
			} else if ("AnyOf".equalsIgnoreCase(key)) {
				rule = (Matcher) AnyOf(value);
			} else if ("i".equalsIgnoreCase(key)) {
				rule = i(value);
			} else if (value.startsWith("%") && value.endsWith("%")) {
				ProxyMatcher proxy = new ProxyMatcher();
				String proxyName = value.substring(1, value.length() - 1);
				List<ProxyMatcher> proxies = PROXY_CACHE.get(proxyName);
				if (proxies == null) {
					proxies = new ArrayList<>();
					PROXY_CACHE.put(proxyName, proxies);
				}
				proxies.add(proxy);
				rule = proxy;
			} else {
				rule = (Matcher) String(value); // Default String Matcher
			}

		}

		if (rule == null) {
			// System.out.println("not handled : " + key + " : " + jsonVal);
			throw new RuntimeException("not handled : " + key + " : " + jsonVal);
		} else {
			// System.out.println("handled : " + key + " : " + jsonVal + " rule : " + rule);
		}

		return rule;
	}

	protected Matcher getLexerRule(String value) {
		Matcher lexerRule = LEXER.get(value);
		return lexerRule;
	}

	protected Action<?> getAction(String value) {
		Action<?> action = ACTIONS.get(value);
		return action;
	}

}