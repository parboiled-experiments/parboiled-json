package org.parboiled.json;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.parboiled.Action;
import org.parboiled.Rule;
import org.parboiled.annotations.DontLabel;
import org.parboiled.annotations.SkipNode;
import org.parboiled.matchers.ActionMatcher;

public abstract class DeclParser extends BaseJsonParser {

	protected Rule __ = _0n(AnyOf(" \t\r\n")).suppressNode();
	protected Rule _d = _1n(CharRange('0', '9')).suppressSubnodes();
	protected Rule _w = _1n(_1of(CharRange('a', 'z'), CharRange('A', 'Z'), CharRange('0', '9'))).suppressSubnodes();
	protected Rule _l = _1of(seq("'", _0n(CharRange('a', 'z')), "'"), seq("\"", _0n(CharRange('a', 'z')), "\""), _d)
			.suppressSubnodes();

	@DontLabel
	protected Rule i(String string) {
		return IgnoreCase(string);
	}

	@SkipNode
	protected Rule _01(Object obj) {
		return Optional(obj);
	}

	@SkipNode
	protected Rule _0n(Object obj) {
		return ZeroOrMore(obj);
	}

	protected Rule _1n(Object rule) {
		return OneOrMore(rule);
	}

	protected Rule seq(Object rule1, Object rule2, Object... rules) {
		return Sequence(rule1, rule2, rules);
	}

	@SkipNode
	protected Rule _1of(Object rule1, Object rule2, Object... rules) {
		return FirstOf(rule1, rule2, rules);
	}

	protected static Map<String, Rule> LEXER = new HashMap<>();
	protected Map<String, Action<?>> ACTIONS = new HashMap<>();

	protected void initLexer() {
		LEXER.put("__", __);
		LEXER.put("_d", _d);
		LEXER.put("_l", _l);
		LEXER.put("_w", _w);
	}

	protected void initActions() {
	}

	public Rule start(String start, JsonObject json) {
		initLexer();
		initActions();
		parseRule(null, json);
		Rule startRule = RULE_CACHE.get(start);
		initProxies();
		return startRule;
	}

	@DontLabel
	protected Rule parseRule(String key, JsonValue jsonVal) {

		Rule rule = null;

		// Object
		if (jsonVal instanceof JsonObject) {
			JsonObject jsonObj = (JsonObject) jsonVal;
			rule = parseRule(key, jsonObj);

		}
		// Array
		else if (jsonVal instanceof JsonArray) {
			JsonArray jsonArr = (JsonArray) jsonVal;
			rule = parseRule(key, jsonArr);
			rule = rule.label(key);

		}
		// String
		else if (jsonVal instanceof JsonString) {
			String value = ((JsonString) jsonVal).getString();
			rule = parseRule(key, value);
		}

		if (rule == null) {
			throw new RuntimeException("not handled : " + key + " : " + jsonVal);
		}

		return rule;
	}
	
	@DontLabel
	protected Rule parseRule(String key, JsonObject jsonObj) {
		Rule rule = null;
		for (Entry<String, JsonValue> entry : jsonObj.entrySet()) {

			Rule childRule = parseRule(entry.getKey(), entry.getValue());

			if ("_0n".equalsIgnoreCase(key)) {
				rule = _0n(childRule);
			} else if ("_1n".equalsIgnoreCase(key)) {
				rule = _1n(childRule);
			} else if ("_01".equalsIgnoreCase(key)) {
				rule = _01(childRule);
			} else {
				rule = childRule; // Default
				if (null != key) {
					RULE_CACHE.put(key, rule);
				}
			}
		}
		return rule;
	}

	@DontLabel
	protected Rule parseRule(String key, JsonArray jsonArr) {
		Rule rule = null;
		Object[] childRules = new Object[jsonArr.size()];

		for (int i = 0; i < jsonArr.size(); i++) {
			JsonValue jsonItem = jsonArr.get(i);
			childRules[i] = parseRule(null, jsonItem);
		}

		if ("seq".equalsIgnoreCase(key)) {
			rule = Sequence(childRules);
		} else if ("_1of".equalsIgnoreCase(key)) {
			rule = FirstOf(childRules);
		}
		return rule;
	}

	@DontLabel
	protected Rule parseRule(String key, String value) {
		Rule rule;
		Action<?> action;
		Rule lexerRule;

		if ("AnyOf".equalsIgnoreCase(key)) {
			rule = AnyOf(value);
		} else if ("i".equalsIgnoreCase(key)) {
			rule = i(value);
		} else if ((lexerRule = LEXER.get(value)) != null) {
			rule = lexerRule;
		} else if ((action = ACTIONS.get(value)) != null) {
			rule = new ActionMatcher(action);
		} else if (value.startsWith("%") && value.endsWith("%")) {
			String proxyName = value.substring(1, value.length() - 1);
			rule = createProxy(proxyName);
		} else {
			rule = String(value); // Default String
		}
		return rule;
	}

}